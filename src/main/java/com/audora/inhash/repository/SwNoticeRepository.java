package com.audora.inhash.repository;

import com.audora.inhash.model.SwNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SwNoticeRepository extends JpaRepository<SwNotice, Long> {
}
