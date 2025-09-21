package com.inhash.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 학생의 LMS(학교 학습관리시스템) 로그인 계정을 보관합니다.
 * - Student와 1:1로 연결되며, username/password를 저장합니다.
 * - status와 lastSyncedAt으로 최신 동기화 상태를 추적합니다.
 *   (보안 개선: password는 향후 암호화/비밀관리로 전환 예정)
 */
@Entity
@Table(name = "lms_accounts")
public class LMSAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;
    @Column(nullable = false)
    private String username;
    @Column
    private String password; // TODO: 보안 개선 필요(해시/시크릿)
    private String status; // CONNECTED / ERROR
    private Instant lastSyncedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}


