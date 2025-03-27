package com.audora.inhash.repository;

import com.audora.inhash.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 기존 findByEmail 대신 findByUsername 사용
    Optional<User> findByUsername(String username);
}
