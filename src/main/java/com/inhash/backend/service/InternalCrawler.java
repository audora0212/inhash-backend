package com.inhash.backend.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class InternalCrawler {

    private static final String BASE = "https://learn.inha.ac.kr/";
    private static final String LOGIN_URL = "https://learn.inha.ac.kr/login/index.php";

    public static class Item {
        public String type; // assignment | class
        public String courseName;
        public String title;
        public String url;
        public String due;
    }

    public List<Item> crawl(String username, String password) throws Exception {
        Map<String, String> cookies = new HashMap<>();
        // 1) 로그인 페이지에서 logintoken 획득
        Document loginDoc = get(LOGIN_URL, cookies);
        String token = Optional.ofNullable(loginDoc.selectFirst("input[name=logintoken]")).map(e -> e.attr("value")).orElse(null);
        // 2) 로그인 시도
        Map<String, String> form = new LinkedHashMap<>();
        form.put("username", username);
        form.put("password", password);
        if (token != null) form.put("logintoken", token);
        post(LOGIN_URL, form, cookies);
        // 3) 메인 페이지에서 과목 목록 수집
        Document home = get(BASE, cookies);
        List<CourseRef> courses = parseCourses(home);
        // 4) 과목별 과제 인덱스 페이지에서 과제 수집
        List<Item> result = new ArrayList<>();
        for (CourseRef c : courses) {
            if (c.id == null || c.id.isBlank()) continue;
            String assignIndex = BASE + "mod/assign/index.php?id=" + urlEncode(c.id);
            try {
                Document assignDoc = get(assignIndex, cookies);
                List<Item> assigns = parseAssignmentsFromIndex(assignDoc, c);
                result.addAll(assigns);
            } catch (Exception ignore) {
            }
            // 과목 메인에서 VOD 등 수업 항목 수집(마감 텍스트에 ~ 포함 시 종료 시각을 due로)
            try {
                Document courseMain = get(c.link, cookies);
                result.addAll(parseVodFromCourse(courseMain, c));
            } catch (Exception ignore) {
            }
        }
        return result;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static Document get(String url, Map<String, String> cookies) throws Exception {
        Connection.Response res = Jsoup.connect(url)
                .timeout(30000)
                .userAgent("Mozilla/5.0")
                .cookies(cookies)
                .method(Connection.Method.GET)
                .execute();
        cookies.putAll(res.cookies());
        return res.parse();
    }

    private static Document post(String url, Map<String, String> data, Map<String, String> cookies) throws Exception {
        Connection.Response res = Jsoup.connect(url)
                .timeout(30000)
                .userAgent("Mozilla/5.0")
                .cookies(cookies)
                .data(data)
                .method(Connection.Method.POST)
                .execute();
        cookies.putAll(res.cookies());
        return res.parse();
    }

    private static class CourseRef {
        String id;
        String name;
        String link;
    }

    private static List<CourseRef> parseCourses(Document doc) {
        List<CourseRef> list = new ArrayList<>();
        Elements items = doc.select("div.course_lists ul.my-course-lists > li");
        for (Element li : items) {
            Element link = li.selectFirst("div.course_box a.course_link");
            if (link == null) link = li.selectFirst("a.course_link");
            if (link == null) continue;
            String href = link.attr("abs:href");
            Element titleEl = li.selectFirst("div.course_box a.course_link div.course-name div.course-title h3");
            String name = titleEl != null ? titleEl.text().trim() : href;
            String id = extractQueryParam(href, "id");
            CourseRef c = new CourseRef();
            c.id = id;
            c.name = name;
            c.link = href;
            list.add(c);
        }
        return list;
    }

    private static String extractQueryParam(String url, String key) {
        try {
            String q = url.contains("?") ? url.substring(url.indexOf('?') + 1) : "";
            for (String part : q.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && kv[0].equals(key)) return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Item> parseAssignmentsFromIndex(Document doc, CourseRef course) {
        List<Item> out = new ArrayList<>();
        for (Element table : doc.select("table")) {
            List<String> headers = new ArrayList<>();
            Elements ths = table.select("thead th");
            if (ths.isEmpty()) {
                Element firstTr = table.selectFirst("tr");
                if (firstTr != null) ths = firstTr.select("th,td");
            }
            for (Element th : ths) headers.add(th.text().trim().toLowerCase());
            if (headers.isEmpty()) continue;
            Integer titleCol = findCol(headers, List.of("과제", "assignment", "활동", "activity"));
            Integer dueCol = findCol(headers, List.of("종료", "마감", "due", "마감일", "종료 일시", "due date"));
            if (titleCol == null || dueCol == null) continue;
            Elements rows = table.select("tbody tr");
            if (rows.isEmpty()) rows = table.select("tr");
            if (!rows.isEmpty() && rows.get(0).select("th").size() > 0) {
                // skip header
                rows = rows.next();
            }
            for (Element tr : rows) {
                Elements tds = tr.select("td,th");
                if (tds.size() <= Math.max(titleCol, dueCol)) continue;
                Element titleCell = tds.get(titleCol);
                Element dueCell = tds.get(dueCol);
                Element a = titleCell.selectFirst("a[href]");
                if (a == null) continue;
                String atitle = a.text().trim();
                String ahref = a.attr("abs:href");
                String dueText = dueCell.text().trim();
                Item item = new Item();
                item.type = "assignment";
                item.courseName = course.name;
                item.title = atitle;
                item.url = ahref;
                item.due = normalizeDue(dueText);
                out.add(item);
            }
            if (!out.isEmpty()) break;
        }
        return out;
    }

    private static Integer findCol(List<String> headers, List<String> keys) {
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i);
            for (String k : keys) {
                if (h.contains(k)) return i;
            }
        }
        return null;
    }

    private static String normalizeDue(String s) {
        if (s == null) return null;
        s = s.replace('\u00A0', ' ').trim();
        if (s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) return s;
        if (s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) return s;
        return s;
    }

    private static List<Item> parseVodFromCourse(Document doc, CourseRef c) {
        List<Item> out = new ArrayList<>();
        for (Element li : doc.select("li.activity.vod.modtype_vod")) {
            Element a = li.selectFirst(".activityinstance a[href]");
            Element titleEl = li.selectFirst(".activityinstance .instancename");
            String title = titleEl != null ? titleEl.text().trim() : null;
            String url = a != null ? a.attr("abs:href") : null;
            String due = null;
            Element period = li.selectFirst(".displayoptions .text-ubstrap");
            if (period != null) {
                String txt = period.text().trim();
                if (txt.contains("~")) {
                    String[] parts = txt.split("~", 2);
                    due = parts[1].trim();
                }
            }
            if (title != null) {
                Item item = new Item();
                item.type = "class";
                item.courseName = c.name;
                item.title = title;
                item.url = url;
                item.due = normalizeDue(due);
                out.add(item);
            }
        }
        return out;
    }
}


