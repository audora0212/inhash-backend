package com.inhash.backend.controller;

import com.inhash.backend.dto.DirectCrawlRequest;
import com.inhash.backend.dto.DirectCrawlResponse;
import com.inhash.backend.service.DirectCrawlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * [임시 코드 - 배포 시 삭제 필요]
 * 개발자 전용 직접 크롤링 컨트롤러
 * 웹 개발 환경에서만 사용
 * 
 * WARNING: 이 컨트롤러는 개발 환경 테스트 용도로만 사용됩니다.
 * 프로덕션 배포 전 반드시 삭제해야 합니다.
 * LMS 계정 정보를 서버로 전송받는 것은 보안상 위험합니다.
 */
@RestController
@RequestMapping("/api/crawl")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DirectCrawlController {

    @Autowired
    private DirectCrawlService directCrawlService;

    @PostMapping("/direct")
    public ResponseEntity<DirectCrawlResponse> directCrawl(@RequestBody DirectCrawlRequest request) {
        try {
            // 개발 환경에서만 동작
            String env = System.getProperty("spring.profiles.active", "dev");
            if ("production".equals(env)) {
                return ResponseEntity.status(403).body(
                    DirectCrawlResponse.builder()
                        .success(false)
                        .message("This feature is only available in development environment")
                        .build()
                );
            }

            // LMS 크롤링 실행
            DirectCrawlResponse response = directCrawlService.crawlLMS(
                request.getStudentId(),
                request.getLmsUsername(),
                request.getLmsPassword()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                DirectCrawlResponse.builder()
                    .success(false)
                    .message("크롤링 중 오류 발생: " + e.getMessage())
                    .build()
            );
        }
    }
}
