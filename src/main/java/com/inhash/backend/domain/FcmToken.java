package com.inhash.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 푸시 알림 전송을 위한 FCM 토큰을 저장합니다.
 * - 각 학생은 여러 기기에서 발급된 토큰을 가질 수 있습니다.
 * - token은 전역 유니크로 중복 저장을 방지합니다.
 */
@Entity
@Table(name = "fcm_tokens")
public class FcmToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Student student;
    @Column(nullable = false, unique = true, length = 500)
    private String token;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}


