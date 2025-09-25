package com.inhash.backend.controller;

import com.inhash.backend.service.ClientCrawlService;
import com.inhash.backend.web.dto.ClientCrawlDataDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 클라이언트에서 크롤링한 데이터를 수신하는 컨트롤러
 * 앱에서 WebView로 직접 크롤링한 결과를 받아 처리
 */
@RestController
@RequestMapping("/api/crawl")
public class ClientCrawlController {
    
    private final ClientCrawlService clientCrawlService;
    
    public ClientCrawlController(ClientCrawlService clientCrawlService) {
        this.clientCrawlService = clientCrawlService;
    }
    
    /**
     * 클라이언트에서 크롤링한 데이터 수신 및 처리
     * 
     * @param studentId 학생 ID (인증된 사용자)
     * @param data 크롤링 데이터
     * @return 처리 결과
     */
    @PostMapping("/submit/{studentId}")
    public ResponseEntity<Map<String, Object>> submitCrawlData(
            @PathVariable Long studentId,
            @RequestBody ClientCrawlDataDto data) {
        
        try {
            System.out.println("=== Received crawl data for student: " + studentId);
            System.out.println("Client version: " + data.getClientVersion());
            System.out.println("Client platform: " + data.getClientPlatform());
            System.out.println("Courses count: " + (data.getCourses() != null ? data.getCourses().size() : 0));
            System.out.println("Items count: " + (data.getItems() != null ? data.getItems().size() : 0));
            
            int imported = clientCrawlService.processCrawlData(studentId, data);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("imported", imported);
            response.put("message", "데이터가 성공적으로 업데이트되었습니다.");
            
            System.out.println("Successfully processed " + imported + " items");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("=== Error processing crawl data: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to process crawl data");
            
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 학생의 마지막 업데이트 상태 조회
     */
    @GetMapping("/status/{studentId}")
    public ResponseEntity<Map<String, Object>> getUpdateStatus(@PathVariable Long studentId) {
        Map<String, Object> response = new HashMap<>();
        response.put("studentId", studentId);
        // 간단한 핑 응답 (실서비스에서는 StudentUpdateStatus 리얼 값 조회)
        response.put("lastUpdated", null);
        response.put("needsUpdate", true);
        return ResponseEntity.ok(response);
    }
}

