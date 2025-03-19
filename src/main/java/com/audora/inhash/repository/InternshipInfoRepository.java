package com.audora.inhash.repository;

import com.audora.inhash.model.InternshipInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternshipInfoRepository extends JpaRepository<InternshipInfo, Long> {
}
