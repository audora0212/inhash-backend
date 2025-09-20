package com.inhash.backend.controller;

import com.inhash.backend.service.InternalCrawler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawl")
public class CrawlDebugController {

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody(required = false) Map<String, String> body) {
        String username = body != null && body.get("username") != null ? body.get("username") : "12215234";
        String password = body != null && body.get("password") != null ? body.get("password") : "dudcks!@34";
        try {
            InternalCrawler crawler = new InternalCrawler();
            List<InternalCrawler.Item> items = crawler.crawl(username, password);
            Map<String, Object> res = new HashMap<>();
            res.put("count", items.size());
            res.put("items", items);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}


