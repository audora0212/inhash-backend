package com.audora.inhash.service;

import com.audora.inhash.model.JobPosting;
import com.audora.inhash.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobPostingService {
    private final JobPostingRepository jobPostingRepository;

    public List<JobPosting> getAllJobPostings() {
        return jobPostingRepository.findAll();
    }

    public JobPosting saveJobPosting(JobPosting jobPosting) {
        return jobPostingRepository.save(jobPosting);
    }

    public JobPosting getJobPostingById(Long id) {
        return jobPostingRepository.findById(id).orElse(null);
    }

    public void deleteJobPosting(Long id) {
        jobPostingRepository.deleteById(id);
    }
}
