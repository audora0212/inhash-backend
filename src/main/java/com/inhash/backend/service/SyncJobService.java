package com.inhash.backend.service;

import com.inhash.backend.domain.SyncJob;
import com.inhash.backend.repository.SyncJobRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class SyncJobService {

    private final SyncJobRepository syncJobRepository;
    private final CrawlService crawlService;

    public SyncJobService(SyncJobRepository syncJobRepository, CrawlService crawlService) {
        this.syncJobRepository = syncJobRepository;
        this.crawlService = crawlService;
    }

    public String submit(Long studentId, String username, String password) {
        String jobId = UUID.randomUUID().toString();
        SyncJob job = new SyncJob();
        job.setJobId(jobId);
        job.setStudentId(studentId);
        job.setStatus("queued");
        syncJobRepository.save(job);
        runAsync(jobId, studentId, username, password);
        return jobId;
    }

    @Async("crawlExecutor")
    public void runAsync(String jobId, Long studentId, String username, String password) {
        SyncJob job = syncJobRepository.findByJobId(jobId).orElseThrow();
        job.setStatus("running");
        job.setStartedAt(Instant.now());
        syncJobRepository.save(job);
        try {
            int imported = crawlService.runCrawlAndImportForStudent(studentId, username, password);
            job.setImported(imported);
            job.setStatus("completed");
        } catch (Exception e) {
            job.setStatus("failed");
            job.setError(e.getMessage());
        } finally {
            job.setFinishedAt(Instant.now());
            syncJobRepository.save(job);
        }
    }
}



