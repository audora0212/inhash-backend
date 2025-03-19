package com.audora.inhash.controller;

import com.audora.inhash.model.InternshipInfo;
import com.audora.inhash.service.InternshipInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internship-infos")
@RequiredArgsConstructor
public class InternshipInfoController {
    private final InternshipInfoService internshipInfoService;

    @GetMapping
    public ResponseEntity<List<InternshipInfo>> getAllInternshipInfos() {
        return new ResponseEntity<>(internshipInfoService.getAllInternshipInfos(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternshipInfo> getInternshipInfoById(@PathVariable Long id) {
        InternshipInfo info = internshipInfoService.getInternshipInfoById(id);
        return info != null ? new ResponseEntity<>(info, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public ResponseEntity<InternshipInfo> createInternshipInfo(@RequestBody InternshipInfo info) {
        return new ResponseEntity<>(internshipInfoService.saveInternshipInfo(info), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInternshipInfo(@PathVariable Long id) {
        internshipInfoService.deleteInternshipInfo(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
