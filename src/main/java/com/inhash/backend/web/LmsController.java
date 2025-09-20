package com.inhash.backend.controller;

import com.inhash.backend.service.CrawlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lms")
public class LmsController {

    private final CrawlService crawlService;

    public LmsController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncNow() {
        crawlService.runCrawlAndImport();
        return ResponseEntity.ok(Map.of("status", "triggered"));
    }
}


