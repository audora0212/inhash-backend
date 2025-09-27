package com.inhash.backend.devtools.crawler;

import com.inhash.backend.service.ClientCrawlService;
import com.inhash.backend.web.dto.ClientCrawlDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * [개발 전용] 서버 사이드 크롤링 서비스
 * - 프로덕션에서는 사용하지 마세요
 * - 자격증명은 메모리에만 임시 보관됩니다
 * - 이 패키지(devtools)는 배포 시 통째로 삭제 가능합니다
 */
@Service
@Profile({"dev", "test"}) // 개발/테스트 프로파일에서만 활성화
public class DevCrawlerService {
    
    // 메모리 임시 저장소 (DB 저장하지 않음)
    private final Map<Long, TemporaryCredentials> credentialsCache = new ConcurrentHashMap<>();
    
    @Autowired
    private ClientCrawlService clientCrawlService;
    
    @Value("${inhash.devtools.crawler.enabled:false}")
    private boolean enabled;
    
    @Value("${inhash.devtools.crawler.python.path:python}")
    private String pythonPath;
    
    @Value("${inhash.devtools.crawler.script.path:final.py}")
    private String scriptPath;
    
    /**
     * 임시 자격증명 저장 (메모리 only)
     */
    public void storeTemporaryCredentials(Long studentId, String username, String password) {
        if (!enabled) {
            throw new IllegalStateException("Dev crawler is disabled. Set inhash.devtools.crawler.enabled=true");
        }
        
        TemporaryCredentials creds = new TemporaryCredentials(username, password);
        credentialsCache.put(studentId, creds);
        
        // 30분 후 자동 삭제
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                credentialsCache.remove(studentId);
            }
        }, 30 * 60 * 1000);
    }
    
    /**
     * 서버 크롤링 실행
     */
    public Map<String, Object> executeCrawling(Long studentId) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("Dev crawler is disabled");
        }
        
        TemporaryCredentials creds = credentialsCache.get(studentId);
        if (creds == null) {
            throw new IllegalStateException("No credentials found. Please store credentials first.");
        }
        
        // Python 크롤러 실행
        ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
        pb.environment().put("INHASH_USERNAME", creds.username);
        pb.environment().put("INHASH_PASSWORD", creds.password);
        // Python UTF-8 인코딩 강제
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("PYTHONDONTWRITEBYTECODE", "1");
        
        // 임시 출력 파일
        String outputPath = "devtools-output-" + UUID.randomUUID() + ".json";
        pb.environment().put("INHASH_OUTPUT_PATH", outputPath);
        
        // 작업 디렉토리 설정 (crawler 폴더)
        pb.directory(new java.io.File("crawler"));
        pb.redirectErrorStream(true); // 에러 스트림도 표준 출력으로 합침
        
        Process process = pb.start();
        
        // 출력 수집 (Python 출력은 무시하고 JSON 파일만 읽음)
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 디버깅용 출력 (콘솔에는 출력하지만 에러로 처리하지 않음)
                System.out.println("[Python] " + line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Crawling failed: " + output.toString());
        }
        
        // JSON 파싱 및 ClientCrawlService로 전달
        Path jsonPath = Paths.get("crawler", outputPath);
        try {
            String json = Files.readString(jsonPath, StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> rawData = mapper.readValue(json, Map.class);
            
            // ClientCrawlDataDto 형식으로 변환
            ClientCrawlDataDto dto = convertToDto(rawData);
            
            // 기존 서비스로 처리
            int imported = clientCrawlService.processCrawlData(studentId, dto);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("imported", imported);
            result.put("message", "Server-side crawling completed");
            
            return result;
            
        } finally {
            // 임시 파일 삭제
            Files.deleteIfExists(jsonPath);
            // 자격증명 즉시 삭제
            credentialsCache.remove(studentId);
        }
    }
    
    /**
     * 모든 임시 자격증명 삭제
     */
    public void clearAllCredentials() {
        credentialsCache.clear();
    }
    
    /**
     * 현재 저장된 자격증명 개수
     */
    public int getCredentialCount() {
        return credentialsCache.size();
    }
    
    private ClientCrawlDataDto convertToDto(Map<String, Object> rawData) {
        ClientCrawlDataDto dto = new ClientCrawlDataDto();
        dto.setClientVersion("devtools-1.0.0");
        dto.setClientPlatform("ServerCrawler");
        dto.setCrawledAt(new Date().toInstant().toString());
        
        // courses 변환
        List<ClientCrawlDataDto.CourseDto> courses = new ArrayList<>();
        if (rawData.containsKey("courses")) {
            List<Map<String, Object>> rawCourses = (List<Map<String, Object>>) rawData.get("courses");
            for (Map<String, Object> rc : rawCourses) {
                ClientCrawlDataDto.CourseDto course = new ClientCrawlDataDto.CourseDto();
                course.setName((String) rc.get("name"));
                course.setMainLink((String) rc.get("main_link"));
                courses.add(course);
            }
        }
        dto.setCourses(courses);
        
        // items 변환
        List<ClientCrawlDataDto.ItemDto> items = new ArrayList<>();
        if (rawData.containsKey("items")) {
            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) rawData.get("items");
            for (Map<String, Object> ri : rawItems) {
                ClientCrawlDataDto.ItemDto item = new ClientCrawlDataDto.ItemDto();
                item.setType((String) ri.get("type"));
                item.setCourseName((String) ri.get("course_name"));
                item.setTitle((String) ri.get("title"));
                item.setUrl(null); // 보안상 URL은 저장하지 않음
                item.setDue((String) ri.get("due"));
                item.setRemainingSeconds(null);
                items.add(item);
            }
        }
        dto.setItems(items);
        
        return dto;
    }
    
    /**
     * 임시 자격증명 클래스 (메모리 전용)
     */
    private static class TemporaryCredentials {
        final String username;
        final String password;
        final long createdAt;
        
        TemporaryCredentials(String username, String password) {
            this.username = username;
            this.password = password;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
