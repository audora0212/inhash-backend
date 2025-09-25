package com.inhash.backend.domain;

import jakarta.persistence.*;

/**
 * 서비스의 기본 사용자(학생) 정보를 저장합니다.
 * - email은 로그인 식별자로 고유(unique)합니다.
 * - passwordHash는 현재 MD5로 저장되며, 추후 BCrypt로 강화 예정입니다.
 */
@Entity
@Table(name = "students")
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    @Column(length = 255)
    private String name;
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}


