package com.inhash.backend.controller;

import com.inhash.backend.service.CrawlService;
import com.inhash.backend.service.LmsAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lms/accounts")
public class LmsAccountController {

    private final LmsAccountService lmsAccountService;
    private final CrawlService crawlService;

    public LmsAccountController(LmsAccountService lmsAccountService, CrawlService crawlService) {
        this.lmsAccountService = lmsAccountService;
        this.crawlService = crawlService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Long studentId = Long.parseLong(body.get("studentId"));
        String username = body.get("username");
        String password = body.get("password");
        var acc = lmsAccountService.registerLmsAccount(studentId, username, password);
        try {
            int imported = crawlService.runCrawlAndImportForStudent(studentId, username, password);
            return ResponseEntity.ok(Map.of("id", acc.getId(), "synced", true, "imported", imported));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("id", acc.getId(), "synced", false, "error", e.getMessage()));
        }
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


