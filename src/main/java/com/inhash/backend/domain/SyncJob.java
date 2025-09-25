package com.inhash.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 비동기 동기화 작업(Job)의 상태를 추적합니다.
 * - jobId: 외부에 제공되는 조회용 식별자(UUID)
 * - status: queued / running / completed / failed
 * - imported: 이번 실행에서 반영된 항목 수
 * - error: 실패 시 상세 메시지(TEXT)
 * - createdAt/startedAt/finishedAt: 실행 타임라인
 */
@Entity
@Table(name = "sync_jobs")
public class SyncJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String jobId;

    private Long studentId;
    @Column(length = 50)
    private String status; // queued, running, completed, failed
    private Integer imported;
    @Lob
    private String error;

    private Instant createdAt = Instant.now();
    private Instant startedAt;
    private Instant finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getImported() { return imported; }
    public void setImported(Integer imported) { this.imported = imported; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}


