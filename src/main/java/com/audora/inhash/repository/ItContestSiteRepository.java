package com.audora.inhash.repository;

import com.audora.inhash.model.ItContestSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItContestSiteRepository extends JpaRepository<ItContestSite, Long> {
}
