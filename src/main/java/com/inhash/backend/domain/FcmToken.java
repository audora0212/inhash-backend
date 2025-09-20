package com.inhash.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "fcm_tokens")
public class FcmToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Student student;
    @Column(nullable = false, unique = true, length = 2048)
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


