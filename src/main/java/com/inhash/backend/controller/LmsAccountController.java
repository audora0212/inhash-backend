package com.inhash.backend.controller;

import com.inhash.backend.service.CrawlService;
import com.inhash.backend.service.SyncJobService;
import com.inhash.backend.service.LmsAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lms/accounts")
public class LmsAccountController {

    private final LmsAccountService lmsAccountService;
    private final CrawlService crawlService;
    private final SyncJobService syncJobService;

    public LmsAccountController(LmsAccountService lmsAccountService, CrawlService crawlService, SyncJobService syncJobService) {
        this.lmsAccountService = lmsAccountService;
        this.crawlService = crawlService;
        this.syncJobService = syncJobService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Long studentId = Long.parseLong(body.get("studentId"));
        String username = body.get("username");
        String password = body.get("password");
        var acc = lmsAccountService.registerLmsAccount(studentId, username, password);
        String jobId = syncJobService.submit(studentId, username, password);
        return ResponseEntity.accepted().body(Map.of("id", acc.getId(), "jobId", jobId, "status", "queued"));
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody Map<String, String> body) {
        Long studentId = Long.parseLong(body.get("studentId"));
        String username = body.get("username");
        String password = body.get("password");
        var acc = lmsAccountService.registerLmsAccount(studentId, username, password);
        return ResponseEntity.ok(Map.of("id", acc.getId(), "status", "updated"));
    }
}


