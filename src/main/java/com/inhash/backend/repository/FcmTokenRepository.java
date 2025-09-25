package com.inhash.backend.repository;

import com.inhash.backend.domain.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByStudentId(Long studentId);
    Optional<FcmToken> findByToken(String token);
    void deleteByToken(String token);
}
