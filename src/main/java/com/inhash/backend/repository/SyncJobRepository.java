package com.inhash.backend.repository;

import com.inhash.backend.domain.SyncJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {
    Optional<SyncJob> findByJobId(String jobId);
}



