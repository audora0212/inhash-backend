package com.audora.inhash.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// src/main/java/com/audora/inhash/model/User.java

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(unique = true)
    private String username;

    private String password;

    // --- 가입일시 필드 추가 ---
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinDate;

    @PrePersist
    public void prePersist() {
        this.joinDate = LocalDateTime.now();
    }
}
