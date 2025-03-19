package com.audora.inhash.controller;

import com.audora.inhash.model.JobPosting;
import com.audora.inhash.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-postings")
@RequiredArgsConstructor
public class JobPostingController {
    private final JobPostingService jobPostingService;

    @GetMapping
    public ResponseEntity<List<JobPosting>> getAllJobPostings() {
        return new ResponseEntity<>(jobPostingService.getAllJobPostings(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPosting> getJobPostingById(@PathVariable Long id) {
        JobPosting jobPosting = jobPostingService.getJobPostingById(id);
        return jobPosting != null ? new ResponseEntity<>(jobPosting, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public ResponseEntity<JobPosting> createJobPosting(@RequestBody JobPosting jobPosting) {
        return new ResponseEntity<>(jobPostingService.saveJobPosting(jobPosting), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobPosting(@PathVariable Long id) {
        jobPostingService.deleteJobPosting(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
