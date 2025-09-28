package com.inhash.backend.devtools.crawler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * [개발 전용] 서버 사이드 크롤링 API
 * 
 * 경고: 이 컨트롤러는 개발/테스트 환경에서만 사용하세요.
 * 프로덕션에서는 절대 활성화하지 마세요.
 */
@RestController
@RequestMapping("/api/devtools/crawler")
@Profile({"dev", "test"})
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DevCrawlerController {
    
    @Autowired
    private DevCrawlerService devCrawlerService;
    
    /**
     * 서버 크롤링 설정 및 실행
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeCrawling(@RequestBody CrawlRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Step 1: 자격증명 임시 저장
            devCrawlerService.storeTemporaryCredentials(
                request.getStudentId(), 
                request.getUsername(), 
                request.getPassword()
            );
            
            // Step 2: 크롤링 실행
            Map<String, Object> result = devCrawlerService.executeCrawling(request.getStudentId());
            
            response.put("success", true);
            response.put("result", result);
            response.put("warning", "This is a development feature. Credentials were temporarily stored in memory and have been deleted.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("type", e.getClass().getSimpleName());
            
            // 에러 발생 시에도 자격증명 삭제
            devCrawlerService.clearAllCredentials();
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 저장된 자격증명 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("credentialCount", devCrawlerService.getCredentialCount());
        response.put("warning", "Development mode only");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 모든 자격증명 삭제
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearCredentials() {
        devCrawlerService.clearAllCredentials();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All temporary credentials cleared");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 크롤링 요청 DTO
     */
    public static class CrawlRequest {
        private Long studentId;
        private String username;
        private String password;
        
        public Long getStudentId() { return studentId; }
        public void setStudentId(Long studentId) { this.studentId = studentId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
