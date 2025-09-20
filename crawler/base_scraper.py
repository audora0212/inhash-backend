import json
import sys
import time
import urllib.parse
from dataclasses import dataclass, asdict
from typing import List, Optional, Dict, Any, Tuple

import requests
from bs4 import BeautifulSoup


LMS_LOGIN_URL = "https://learn.inha.ac.kr/login/index.php"
LMS_BASE_URL = "https://learn.inha.ac.kr/"


# ------------------------------
# Utilities
# ------------------------------

def absolute_url(base: str, url: str) -> str:
    try:
        return urllib.parse.urljoin(base, url)
    except Exception:
        return url


def normalize_whitespace(text: str) -> str:
    return " ".join((text or "").split())


def get_soup(html: str) -> BeautifulSoup:
    return BeautifulSoup(html, "html.parser")


def create_session() -> requests.Session:
    s = requests.Session()
    s.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                       " AppleWebKit/537.36 (KHTML, like Gecko)"
                       " Chrome/127.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "ko,en;q=0.9",
        "Connection": "keep-alive",
        "Origin": LMS_BASE_URL.rstrip("/"),
        "Referer": LMS_BASE_URL,
    })
    return s


def http_get(session: requests.Session, url: str, **kwargs) -> Tuple[str, requests.Response]:
    resp = session.get(url, timeout=30, **kwargs)
    resp.raise_for_status()
    return resp.text, resp


def http_post(session: requests.Session, url: str, data: Dict[str, Any]) -> Tuple[str, requests.Response]:
    resp = session.post(url, data=data, timeout=30)
    resp.raise_for_status()
    return resp.text, resp


# ------------------------------
# Data Models
# ------------------------------

@dataclass
class MaterialItem:
    name: str
    url: str
    icon_url: Optional[str] = None


@dataclass
class AssignmentItem:
    title: str
    url: str
    due: Optional[str] = None  # raw string


@dataclass
class QuizItem:
    title: str
    url: str
    open_time: Optional[str] = None
    close_time: Optional[str] = None


@dataclass
class IClassLectureItem:
    title: str
    url: Optional[str] = None
    due: Optional[str] = None


@dataclass
class CourseItem:
    id: str
    name: str
    main_link: str
    file_list_link: Optional[str]
    materials: List[MaterialItem]
    assignments: List[AssignmentItem]
    quizzes: List[QuizItem]
    iclass_lectures: List[IClassLectureItem]


# ------------------------------
# Login & Validation
# ------------------------------

def login(session: requests.Session, username: str, password: str) -> bool:
    # fetch CSRF token if present
    logintoken = None
    try:
        login_page_html, _ = http_get(session, LMS_LOGIN_URL)
        soup = get_soup(login_page_html)
        token_el = soup.select_one("input[name=logintoken]")
        if token_el and token_el.get("value"):
            logintoken = token_el.get("value")
    except Exception:
        logintoken = None

    form = {
        "username": username,
        "password": password,
    }
    if logintoken:
        form["logintoken"] = logintoken

    html, resp = http_post(session, LMS_LOGIN_URL, data=form)

    # Heuristics to validate login: cookie + no login form on homepage
    moodle_cookie = any("MoodleSession" in c.name for c in session.cookies)
    try:
        home_html, _ = http_get(session, LMS_BASE_URL)
    except Exception:
        home_html = ""

    login_form_in_home = "loginform" in home_html.lower()

    redirected_to_login = "login" in (resp.url or "").lower()
    return moodle_cookie and not login_form_in_home and not redirected_to_login


# ------------------------------
# Parsing
# ------------------------------

def parse_courses(html: str) -> List[Dict[str, str]]:
    soup = get_soup(html)
    courses: List[Dict[str, str]] = []

    items = soup.select("div.course_lists ul.my-course-lists > li")
    for li in items:
        link = li.select_one("div.course_box a.course_link")
        title_el = li.select_one("div.course_box a.course_link div.course-name div.course-title h3")
        if not link or not title_el:
            link = li.select_one("a.course_link")
            title_el = title_el or link
        if not link:
            continue

        href = link.get("href", "")
        name = normalize_whitespace(title_el.get_text(strip=True)) if title_el else href
        cid = ""
        try:
            parsed = urllib.parse.urlparse(href)
            qs = urllib.parse.parse_qs(parsed.query)
            if "id" in qs and len(qs["id"]) > 0:
                cid = qs["id"][0]
        except Exception:
            pass

        courses.append({
            "id": cid,
            "name": name,
            "link": absolute_url(LMS_BASE_URL, href),
        })

    return courses


def extract_file_list_link(course_main_html: str, course_link: str, course_id: str) -> Optional[str]:
    soup = get_soup(course_main_html)
    selector = f"div#coursemos-course-menu ul.add_activities a[href*='/mod/ubfile/index.php?id={course_id}']"
    a = soup.select_one(selector)
    if not a:
        a = soup.select_one("a[href*='/mod/ubfile/index.php']")
    if a and a.get("href"):
        return absolute_url(course_link, a.get("href"))
    return None


def parse_materials(file_list_html: str, base_url: str) -> List[MaterialItem]:
    soup = get_soup(file_list_html)
    materials: List[MaterialItem] = []
    for a in soup.select("a[href]"):
        href = a.get("href", "")
        text_nodes = [normalize_whitespace(t) for t in a.stripped_strings]
        name = normalize_whitespace(" ".join(text_nodes))
        if not name or name == "파일":
            continue
        if any(key in href for key in ["/mod_ubfile/", "/resource/view.php", "/ubboard/view.php", "/assign/view.php"]):
            icon = None
            img = a.select_one("img.activityicon")
            if img and img.get("src"):
                icon = absolute_url(LMS_BASE_URL, img.get("src"))
            materials.append(MaterialItem(name=name, url=absolute_url(base_url, href), icon_url=icon))

    seen = set()
    unique: List[MaterialItem] = []
    for m in materials:
        k = (m.name, m.url)
        if k in seen:
            continue
        seen.add(k)
        unique.append(m)
    return unique


def find_module_links_in_course(course_main_html: str, course_base: str) -> Tuple[List[AssignmentItem], List[QuizItem], List[str]]:
    soup = get_soup(course_main_html)
    assignments: List[AssignmentItem] = []
    quizzes: List[QuizItem] = []
    iclass_links: List[str] = []

    for a in soup.select("a[href]"):
        href = a.get("href", "")
        text = normalize_whitespace(a.get_text(strip=True))
        if "/mod/assign/" in href:
            assignments.append(AssignmentItem(title=text or "과제", url=absolute_url(course_base, href)))
        elif "/mod/quiz/" in href:
            quizzes.append(QuizItem(title=text or "퀴즈", url=absolute_url(course_base, href)))
        if any(key in href for key in ["/local/ubion/", "/mod/ubion", "/ubion/"]):
            iclass_links.append(absolute_url(course_base, href))

    def dedup(items, key=lambda x: x.url):
        seen = set()
        out = []
        for it in items:
            k = key(it)
            if k in seen:
                continue
            seen.add(k)
            out.append(it)
        return out

    iclass_links = list(dict.fromkeys(iclass_links))

    return dedup(assignments), dedup(quizzes), iclass_links


def parse_due_from_detail(html: str) -> Dict[str, Optional[str]]:
    soup = get_soup(html)
    th_keywords = [
        "마감", "제출기한", "제출 마감", "Due", "Due date", "마감일",
        "Open", "Open the quiz", "열림", "Close", "Close the quiz", "닫힘", "종료"
    ]
    results: Dict[str, Optional[str]] = {
        "due": None,
        "open": None,
        "close": None,
    }

    for row in soup.select("table tr"):
        th = row.find("th")
        td = row.find("td")
        if not th or not td:
            continue
        key = normalize_whitespace(th.get_text(" ", strip=True))
        val = normalize_whitespace(td.get_text(" ", strip=True))
        if not key or not val:
            continue
        if any(k.lower() in key.lower() for k in ["due", "마감", "제출"]):
            results["due"] = val
        elif any(k.lower() in key.lower() for k in ["open", "열림"]):
            results["open"] = val
        elif any(k.lower() in key.lower() for k in ["close", "닫힘", "종료"]):
            results["close"] = val

    return results


def enrich_assignments_and_quizzes(session: requests.Session, items: List[Any]) -> None:
    for it in items:
        try:
            html, _ = http_get(session, it.url)
        except Exception:
            continue
        dates = parse_due_from_detail(html)
        if isinstance(it, AssignmentItem):
            it.due = dates.get("due")
        elif isinstance(it, QuizItem):
            it.open_time = dates.get("open")
            it.close_time = dates.get("close")
        time.sleep(0.2)


# ------------------------------
# Index pages (assign/quiz) parsing for dates
# ------------------------------

def _extract_id_param(url: str) -> Optional[str]:
    try:
        parsed = urllib.parse.urlparse(url)
        qs = urllib.parse.parse_qs(parsed.query)
        vals = qs.get("id")
        if vals:
            return vals[0]
    except Exception:
        return None
    return None


def parse_assign_index(session: requests.Session, course_id: str, course_base: str) -> List[AssignmentItem]:
    url = absolute_url(LMS_BASE_URL, f"mod/assign/index.php?id={course_id}")
    try:
        html, _ = http_get(session, url)
    except Exception:
        return []

    soup = get_soup(html)
    tables = soup.select("table")
    results: List[AssignmentItem] = []
    for table in tables:
        headers = [normalize_whitespace(th.get_text(" ", strip=True)) for th in table.select("thead th")]
        if not headers:
            first_tr = table.find("tr")
            if first_tr:
                headers = [normalize_whitespace(th.get_text(" ", strip=True)) for th in first_tr.find_all(["th", "td"])]
        if not headers:
            continue

        def find_col(predicates):
            for idx, h in enumerate(headers):
                low = h.lower()
                if any(p in low for p in predicates):
                    return idx
            return None

        title_col = find_col(["과제", "assignment", "활동", "activity"])
        due_col = find_col(["종료", "마감", "due", "마감일", "종료 일시", "due date"]) 
        if due_col is None or title_col is None:
            continue

        body_rows = table.select("tbody tr") or table.find_all("tr")[1:]
        for tr in body_rows:
            tds = tr.find_all(["td", "th"])
            if not tds or len(tds) <= max(title_col, due_col):
                continue
            title_cell = tds[title_col]
            due_cell = tds[due_col]
            a = title_cell.find("a", href=True)
            if not a:
                continue
            atitle = normalize_whitespace(a.get_text(strip=True)) or "과제"
            ahref = absolute_url(course_base, a.get("href", ""))
            due_text = normalize_whitespace(due_cell.get_text(" ", strip=True)) or None
            results.append(AssignmentItem(title=atitle, url=ahref, due=due_text))
        if results:
            break
    return results


def parse_iclass_lectures(session: requests.Session, iclass_urls: List[str]) -> List[IClassLectureItem]:
    lectures: List[IClassLectureItem] = []
    for url in iclass_urls:
        try:
            html, _ = http_get(session, url)
        except Exception:
            continue
        soup = get_soup(html)

        tables = soup.select("table")
        for table in tables:
            headers = [normalize_whitespace(th.get_text(" ", strip=True)) for th in table.select("thead th")]
            if not headers:
                first_tr = table.find("tr")
                if first_tr:
                    headers = [normalize_whitespace(th.get_text(" ", strip=True)) for th in first_tr.find_all(["th", "td"])]
            if not headers:
                continue

            def find_col(predicates):
                for idx, h in enumerate(headers):
                    low = h.lower()
                    if any(p in low for p in predicates):
                        return idx
                return None

            title_col = find_col(["강의", "차시", "제목", "lecture", "title", "학습", "콘텐츠"]) 
            due_col = find_col(["수강", "마감", "종료", "기간", "due", "close", "end"])

            body_rows = table.select("tbody tr") or table.find_all("tr")[1:]
            for tr in body_rows:
                cells = tr.find_all(["td", "th"])
                if not cells:
                    continue
                title_text = None
                link_url = None
                due_text = None
                if title_col is not None and title_col < len(cells):
                    tcell = cells[title_col]
                    a = tcell.find("a", href=True)
                    title_text = normalize_whitespace(tcell.get_text(" ", strip=True))
                    if a:
                        link_url = absolute_url(url, a.get("href", ""))
                if due_col is not None and due_col < len(cells):
                    due_text = normalize_whitespace(cells[due_col].get_text(" ", strip=True)) or None
                if title_text:
                    lectures.append(IClassLectureItem(title=title_text, url=link_url, due=due_text))

        for row in soup.select(".ubion, .list, .card, .content, li"):
            text = normalize_whitespace(row.get_text(" ", strip=True))
            if not text:
                continue
            if any(k in text for k in ["수강", "마감", "종료", "기간", "Due", "Close"]):
                a = row.find("a", href=True)
                title_text = None
                link_url = None
                if a:
                    title_text = normalize_whitespace(a.get_text(" ", strip=True))
                    link_url = absolute_url(url, a.get("href", ""))
                else:
                    h = row.find(["h3", "h4", "strong"])
                    if h:
                        title_text = normalize_whitespace(h.get_text(" ", strip=True))
                due_text = None
                if title_text:
                    lectures.append(IClassLectureItem(title=title_text, url=link_url, due=due_text))

    dedup: Dict[Tuple[str, Optional[str]], IClassLectureItem] = {}
    for lec in lectures:
        key = (lec.title, lec.url)
        if key not in dedup or (lec.due and not dedup[key].due):
            dedup[key] = lec
    return list(dedup.values())


def parse_vod_from_course(course_main_html: str, course_base: str) -> List[IClassLectureItem]:
    soup = get_soup(course_main_html)
    lectures: List[IClassLectureItem] = []
    for li in soup.select("li.activity.vod.modtype_vod"):
        a = li.select_one(".activityinstance a[href]")
        title_el = li.select_one(".activityinstance .instancename")
        title = normalize_whitespace(title_el.get_text(" ", strip=True)) if title_el else None
        url = absolute_url(course_base, a.get("href", "")) if a and a.get("href") else None
        period_el = li.select_one(".displayoptions .text-ubstrap")
        due = None
        if period_el:
            period_text = normalize_whitespace(period_el.get_text(" ", strip=True))
            if "~" in period_text:
                parts = [p.strip() for p in period_text.split("~", 1)]
                if len(parts) == 2:
                    due = parts[1]
        if title:
            lectures.append(IClassLectureItem(title=title, url=url, due=due))
    merged: Dict[Tuple[str, Optional[str]], IClassLectureItem] = {}
    for lec in lectures:
        key = (lec.title, lec.url)
        if key not in merged or (lec.due and not merged[key].due):
            merged[key] = lec
    return list(merged.values())


# ------------------------------
# Main Flow
# ------------------------------

def fetch_all(username: str, password: str) -> Dict[str, Any]:
    session = create_session()
    ok = login(session, username, password)
    if not ok:
        raise RuntimeError("로그인 실패: 아이디/비밀번호를 확인하거나, 포털 접근 여부를 확인하세요.")

    home_html, _ = http_get(session, LMS_BASE_URL)
    course_refs = parse_courses(home_html)

    courses: List[CourseItem] = []
    for cref in course_refs:
        cid = cref["id"]
        cname = cref["name"]
        clink = cref["link"]
        try:
            course_html, _ = http_get(session, clink)
        except Exception:
            course_html = ""

        file_list_link = extract_file_list_link(course_html, clink, cid) if course_html else None
        materials: List[MaterialItem] = []
        if file_list_link:
            try:
                fhtml, _ = http_get(session, file_list_link)
                materials = parse_materials(fhtml, file_list_link)
            except Exception:
                materials = []

        assignments, quizzes, iclass_links = find_module_links_in_course(course_html, clink)

        assign_index_items = parse_assign_index(session, cid, clink)
        if assign_index_items:
            by_url = {a.url: a for a in assignments}
            for it in assign_index_items:
                if it.url in by_url:
                    if it.due:
                        by_url[it.url].due = it.due
                else:
                    assignments.append(it)

        missing_due = [a for a in assignments if not a.due]
        enrich_assignments_and_quizzes(session, missing_due)
        enrich_assignments_and_quizzes(session, quizzes)

        iclass_lectures = parse_iclass_lectures(session, iclass_links)
        vod_lectures = parse_vod_from_course(course_html, clink)
        by_key: Dict[Tuple[str, Optional[str]], IClassLectureItem] = {}
        for lec in iclass_lectures + vod_lectures:
            k = (lec.title, lec.url)
            if k not in by_key or (lec.due and not by_key[k].due):
                by_key[k] = lec
        iclass_lectures = list(by_key.values())

        courses.append(CourseItem(
            id=cid,
            name=cname,
            main_link=clink,
            file_list_link=file_list_link,
            materials=materials,
            assignments=assignments,
            quizzes=quizzes,
            iclass_lectures=iclass_lectures,
        ))
        time.sleep(0.2)

    output = {
        "fetched_at": time.strftime("%Y-%m-%d %H:%M:%S"),
        "user": username,
        "courses": [
            {
                "id": c.id,
                "name": c.name,
                "main_link": c.main_link,
                "file_list_link": c.file_list_link,
                "materials": [asdict(m) for m in c.materials],
                "assignments": [asdict(a) for a in c.assignments],
                "quizzes": [asdict(q) for q in c.quizzes],
                "iclass_lectures": [asdict(l) for l in c.iclass_lectures],
            }
            for c in courses
        ],
    }
    return output


def _try_parse_datetime(s: Optional[str]) -> Optional[time.struct_time]:
    if not s:
        return None
    s = s.strip()
    if s.startswith("\u00a0"):
        s = s.replace("\u00a0", " ").strip()
    fmts = [
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%d %H:%M",
    ]
    for fmt in fmts:
        try:
            return time.strptime(s, fmt)
        except Exception:
            continue
    return None


def build_due_summary(full_data: Dict[str, Any]) -> Dict[str, Any]:
    now = time.localtime()

    def to_ts(st: Optional[time.struct_time]) -> Optional[float]:
        try:
            return time.mktime(st) if st else None
        except Exception:
            return None

    overdue_assign: List[Dict[str, Any]] = []
    upcoming_assign: List[Dict[str, Any]] = []
    overdue_class: List[Dict[str, Any]] = []
    upcoming_class: List[Dict[str, Any]] = []

    for course in full_data.get("courses", []):
        course_id = course.get("id")
        course_name = course.get("name")

        for a in course.get("assignments", []):
            due_str = a.get("due")
            due_st = _try_parse_datetime(due_str)
            due_ts = to_ts(due_st)
            if not due_ts:
                continue
            delta_sec = int(due_ts - time.mktime(now))
            item = {
                "type": "assignment",
                "course_id": course_id,
                "course_name": course_name,
                "title": a.get("title"),
                "url": a.get("url"),
                "due": due_str,
                "remaining_seconds": delta_sec,
            }
            if delta_sec < 0:
                overdue_assign.append(item)
            else:
                upcoming_assign.append(item)

        for lec in course.get("iclass_lectures", []):
            due_str = lec.get("due")
            due_st = _try_parse_datetime(due_str)
            due_ts = to_ts(due_st)
            if not due_ts:
                continue
            delta_sec = int(due_ts - time.mktime(now))
            item = {
                "type": "class",
                "course_id": course_id,
                "course_name": course_name,
                "title": lec.get("title"),
                "url": lec.get("url"),
                "due": due_str,
                "remaining_seconds": delta_sec,
            }
            if delta_sec < 0:
                overdue_class.append(item)
            else:
                upcoming_class.append(item)

    upcoming_assign.sort(key=lambda x: x["remaining_seconds"])  # soonest first
    upcoming_class.sort(key=lambda x: x["remaining_seconds"])   # soonest first
    overdue_assign.sort(key=lambda x: x["remaining_seconds"], reverse=True)
    overdue_class.sort(key=lambda x: x["remaining_seconds"], reverse=True)

    return {
        "generated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
        "overdue": {
            "assignments": overdue_assign,
            "classes": overdue_class,
        },
        "upcoming": {
            "assignments": upcoming_assign,
            "classes": upcoming_class,
        },
    }


def main():
    try:
        import getpass
        username = input("아이디(학번): ").strip()
        password = getpass.getpass("비밀번호: ")
        if not username or not password:
            print("아이디/비밀번호를 입력하세요.")
            sys.exit(1)

        data = fetch_all(username, password)
        out_file = "lms_data.json"
        with open(out_file, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"완료: {out_file} 저장")

        due_summary = build_due_summary(data)
        out_due = "lms_due.json"
        with open(out_due, "w", encoding="utf-8") as f:
            json.dump(due_summary, f, ensure_ascii=False, indent=2)
        print(f"완료: {out_due} 저장")
    except Exception as e:
        print(f"오류: {e}")
        sys.exit(2)


if __name__ == "__main__":
    main()


