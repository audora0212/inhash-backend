package com.inhash.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 크롤링/동기화 실행에 대한 결과 로그를 남깁니다.
 * - source: 실행 소스/식별자
 * - status: success / error
 * - message: 상세 로그(TEXT)
 * - syncedAt: 로그 기록 시각
 */
@Entity
@Table(name = "sync_logs")
public class SyncLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 255)
    private String source;
    @Column(length = 50)
    private String status;
    @Lob
    private String message;
    private Instant syncedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }
}


