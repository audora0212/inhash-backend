package com.inhash.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 학생의 데이터 업데이트 상태를 추적합니다.
 * - 클라이언트 크롤링 방식으로 전환하면서 LMS 계정 대신 업데이트 상태만 관리
 * - 마지막 업데이트 시간을 추적하여 미업데이트 알림 발송 기준으로 사용
 */
@Entity
@Table(name = "student_update_status")
public class StudentUpdateStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;
    
    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;
    
    @Column(name = "last_notification_sent_at")
    private Instant lastNotificationSentAt;
    
    @Column(name = "notification_count")
    private Integer notificationCount = 0;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // 클라이언트 정보 (디버깅용)
    @Column(name = "client_version")
    private String clientVersion;
    
    @Column(name = "client_platform")
    private String clientPlatform; // iOS, Android, Web
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    
    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    
    public Instant getLastNotificationSentAt() { return lastNotificationSentAt; }
    public void setLastNotificationSentAt(Instant lastNotificationSentAt) { 
        this.lastNotificationSentAt = lastNotificationSentAt; 
    }
    
    public Integer getNotificationCount() { return notificationCount; }
    public void setNotificationCount(Integer notificationCount) { 
        this.notificationCount = notificationCount; 
    }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public String getClientVersion() { return clientVersion; }
    public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }
    
    public String getClientPlatform() { return clientPlatform; }
    public void setClientPlatform(String clientPlatform) { this.clientPlatform = clientPlatform; }
}

