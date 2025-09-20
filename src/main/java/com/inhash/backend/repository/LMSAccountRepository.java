package com.inhash.backend.repository;

import com.inhash.backend.domain.LMSAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LMSAccountRepository extends JpaRepository<LMSAccount, Long> {
    Optional<LMSAccount> findByStudentId(Long studentId);
}


