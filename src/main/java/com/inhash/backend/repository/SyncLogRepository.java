package com.inhash.backend.repository;

import com.inhash.backend.domain.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {}


