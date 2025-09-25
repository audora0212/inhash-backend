package com.inhash.backend.repository;

import com.inhash.backend.domain.Student;
import com.inhash.backend.domain.StudentUpdateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 학생 업데이트 상태 Repository
 */
@Repository
public interface StudentUpdateStatusRepository extends JpaRepository<StudentUpdateStatus, Long> {
    
    Optional<StudentUpdateStatus> findByStudent(Student student);
    
    Optional<StudentUpdateStatus> findByStudentId(Long studentId);
    
    /**
     * 특정 시간 이전에 마지막으로 업데이트된 활성 학생들 조회
     * 미업데이트 알림 대상 선정용
     */
    @Query("SELECT s FROM StudentUpdateStatus s WHERE s.isActive = true AND s.lastUpdatedAt < ?1")
    List<StudentUpdateStatus> findActiveStudentsNotUpdatedSince(Instant since);
    
    /**
     * 알림 발송 대상 조회
     * - 활성 상태
     * - 마지막 업데이트가 특정 시간 이전
     * - 마지막 알림 발송이 특정 시간 이전 또는 없음
     */
    @Query("SELECT s FROM StudentUpdateStatus s WHERE s.isActive = true " +
           "AND s.lastUpdatedAt < ?1 " +
           "AND (s.lastNotificationSentAt IS NULL OR s.lastNotificationSentAt < ?2)")
    List<StudentUpdateStatus> findNotificationTargets(Instant lastUpdateBefore, Instant lastNotificationBefore);
}

