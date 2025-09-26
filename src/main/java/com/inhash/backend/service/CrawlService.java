package com.inhash.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhash.backend.domain.*;
import com.inhash.backend.repository.AssignmentRepository;
import com.inhash.backend.repository.CourseRepository;
import com.inhash.backend.repository.LectureRepository;
import com.inhash.backend.repository.SyncLogRepository;
import com.inhash.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class CrawlService {

    private final AssignmentRepository assignmentRepository;
    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final SyncLogRepository syncLogRepository;
    private final StudentRepository studentRepository;
    private final LmsAccountService lmsAccountService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${inhash.crawl.python.workingDir:}")
    private String workingDir;
    @Value("${inhash.crawl.python.executable:}")
    private String pythonExe;
    @Value("${inhash.crawl.python.script:}")
    private String script;
    @Value("${inhash.crawl.internal.username:12215234}")
    private String internalUsername;
    @Value("${inhash.crawl.internal.password:dudcks!@34}")
    private String internalPassword;

    public CrawlService(AssignmentRepository assignmentRepository, LectureRepository lectureRepository, CourseRepository courseRepository, SyncLogRepository syncLogRepository, StudentRepository studentRepository, LmsAccountService lmsAccountService) {
        this.assignmentRepository = assignmentRepository;
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
        this.syncLogRepository = syncLogRepository;
        this.studentRepository = studentRepository;
        this.lmsAccountService = lmsAccountService;
    }

    public int runCrawlAndImport() {
        // 내부 계정으로 파이썬 실행 (fallback/디폴트)
        return runCrawlAndImportForStudent(null, internalUsername, internalPassword);
    }

    public int runCrawlAndImportForStudent(Long studentId, String username, String password) {
        SyncLog log = new SyncLog();
        log.setSource("python-final" + (studentId != null ? (":" + studentId) : ""));
        int imported = 0;
        String outputPath = null;
        try {
            if (pythonExe == null || pythonExe.isBlank()) throw new IllegalStateException("python executable not configured");
            if (script == null || script.isBlank()) throw new IllegalStateException("python script not configured");

            Path resolvedWorking = resolveWorkingDir(workingDir);
            File resolvedDirFile = resolvedWorking.toFile();

            // 작업별 output 경로를 분리하여 동시 실행 충돌 방지
            outputPath = resolvedWorking.resolve("output").resolve("final-" + UUID.randomUUID() + ".json").toString();
            // ensure parent directory exists
            try {
                Files.createDirectories(resolvedWorking.resolve("output"));
            } catch (Exception ignore) {}
            ProcessBuilder pb = new ProcessBuilder(pythonExe, script);
            pb.directory(resolvedDirFile);
            pb.redirectErrorStream(true);
            // 강제 UTF-8 콘솔 인코딩 설정(Windows cp949 Unicode 에러 방지)
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            if (username != null) pb.environment().put("INHASH_USERNAME", username);
            if (password != null) pb.environment().put("INHASH_PASSWORD", password);
            pb.environment().put("INHASH_OUTPUT_PATH", outputPath);
            Process proc = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }
            int code = proc.waitFor();
            if (code != 0) throw new RuntimeException("final.py exit code " + code + "\n" + out);

            Path jsonPath = Path.of(outputPath);
            if (!Files.exists(jsonPath)) throw new RuntimeException("final.json not found: " + jsonPath);

            JsonNode root = objectMapper.readTree(Files.readString(jsonPath, StandardCharsets.UTF_8));
            JsonNode items = root.has("items") ? root.get("items") : null;
            if (items == null || !items.isArray()) throw new RuntimeException("invalid final.json: items[] missing");
            System.out.println("[CrawlService] final.json items size=" + items.size());

            // 먼저 전체 과목 목록을 courses[]로부터 보장 생성 (과제/수업이 없어도 과목은 추가)
            JsonNode courseList = root.has("courses") ? root.get("courses") : null;
            if (courseList != null && courseList.isArray()) {
                for (JsonNode c : courseList) {
                    String courseName = optText(c, "name");
                    String courseId = digest(courseName);
                    courseRepository.findById(courseId).orElseGet(() -> {
                        Course nc = new Course();
                        nc.setId(courseId);
                        nc.setName(courseName);
                        nc.setMainLink(optText(c, "main_link"));
                        return courseRepository.save(nc);
                    });
                }
            }

            imported = 0;
            int seen = 0;
            final Instant nowKstInstant = Instant.now();
            for (JsonNode it : items) {
                String type = optText(it, "type");
                if (type != null) {
                    String t = type.toLowerCase(Locale.ROOT);
                    if ("assignments".equals(t)) type = "assignment";
                    else if ("classes".equals(t)) type = "class";
                }
                String courseName = optText(it, "course_name");
                String title = optText(it, "title");
                String url = optText(it, "url");
                String due = optText(it, "due");
                if (courseName == null || title == null) continue;
                if (seen < 3) {
                    System.out.println("[CrawlService] item type=" + type + ", course=" + courseName + ", title=" + title);
                    seen++;
                }

                String courseId = digest(courseName);
                Course course = courseRepository.findById(courseId).orElseGet(() -> {
                    Course c = new Course();
                    c.setId(courseId);
                    c.setName(courseName);
                    c.setMainLink(null);
                    return courseRepository.save(c);
                });

                Instant dueAt = parseDue(due);
                // KST 기준 과거 마감 항목은 제외
                if (dueAt != null && dueAt.isBefore(nowKstInstant)) {
                    continue;
                }
                String id = digest(title + "|" + (url == null ? "" : url) + "|" + courseId + "|" + String.valueOf(studentId));

                if ("assignment".equalsIgnoreCase(type)) {
                    Assignment a = new Assignment();
                    a.setId(id);
                    a.setCourseName(course != null && course.getName() != null ? 
                        (course.getName().length() > 20 ? course.getName().substring(0, 20) : course.getName()) : "");
                    a.setTitle(title);
                    a.setUrl(url);
                    a.setDueAt(dueAt);
                    if (studentId != null) a.setStudent(studentRepository.getReferenceById(studentId));
                    assignmentRepository.save(a);
                    imported++;
                } else if ("class".equalsIgnoreCase(type)) {
                    Lecture l = new Lecture();
                    l.setId(id);
                    l.setCourseName(course != null && course.getName() != null ? 
                        (course.getName().length() > 20 ? course.getName().substring(0, 20) : course.getName()) : "");
                    l.setTitle(title);
                    l.setUrl(url);
                    l.setDueAt(dueAt);
                    if (studentId != null) l.setStudent(studentRepository.getReferenceById(studentId));
                    lectureRepository.save(l);
                    imported++;
                }
            }

            log.setStatus("success");
            log.setMessage("items=" + items.size() + ", imported=" + imported);
            System.out.println("[CrawlService] imported=" + imported);
            if (studentId != null) {
                try { lmsAccountService.markSynced(studentId, true); } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            log.setStatus("error");
            log.setMessage(e.getMessage());
            if (studentId != null) {
                try { lmsAccountService.markSynced(studentId, false); } catch (Exception ignore) {}
            }
        } finally {
            syncLogRepository.save(log);
            // cleanup temporary output file
            try {
                if (outputPath != null) {
                    Files.deleteIfExists(Path.of(outputPath));
                }
            } catch (Exception ignore) {}
        }
        return imported;
    }

    private static Path resolveWorkingDir(String configured) {
        if (configured == null || configured.isBlank()) {
            // default: project-relative crawler
            return Paths.get("crawler").toAbsolutePath().normalize();
        }
        Path p = Paths.get(configured);
        if (p.isAbsolute()) return p;
        // treat as relative to current working dir (usually project root when running via gradle bootRun/jar)
        return p.toAbsolutePath().normalize();
    }

    private static String optText(JsonNode n, String f) {
        return n.hasNonNull(f) ? n.get(f).asText() : null;
    }

    private static String digest(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(s));
        }
    }

    private static Instant parseDue(String due) {
        if (due == null || due.isBlank()) return null;
        try {
            // base_scraper 출력 형식: "YYYY-MM-DD HH:MM[:SS]"
            String norm = due.trim();
            if (norm.length() == 16) norm = norm + ":00";
            LocalDateTime ldt = LocalDateTime.parse(norm.replace(' ', 'T'));
            return ldt.atZone(ZoneId.of("Asia/Seoul")).toInstant();
        } catch (Exception e) {
            return null;
        }
    }
}


