package com.inhash.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

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


