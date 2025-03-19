package com.audora.inhash.controller;

import com.audora.inhash.model.SwNotice;
import com.audora.inhash.service.SwNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sw-notices")
@RequiredArgsConstructor
public class SwNoticeController {
    private final SwNoticeService swNoticeService;

    @GetMapping
    public ResponseEntity<List<SwNotice>> getAllSwNotices() {
        return new ResponseEntity<>(swNoticeService.getAllNotices(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SwNotice> getSwNoticeById(@PathVariable Long id) {
        SwNotice notice = swNoticeService.getNoticeById(id);
        return notice != null ? new ResponseEntity<>(notice, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public ResponseEntity<SwNotice> createSwNotice(@RequestBody SwNotice notice) {
        return new ResponseEntity<>(swNoticeService.saveNotice(notice), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSwNotice(@PathVariable Long id) {
        swNoticeService.deleteNotice(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
