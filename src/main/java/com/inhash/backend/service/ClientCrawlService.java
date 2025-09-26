package com.inhash.backend.service;

import com.inhash.backend.domain.*;
import com.inhash.backend.repository.*;
import com.inhash.backend.web.dto.ClientCrawlDataDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 클라이언트에서 크롤링한 데이터를 처리하는 서비스
 * LMS 계정을 서버에 저장하지 않고, 클라이언트에서 직접 크롤링한 데이터만 수신
 */
@Service
public class ClientCrawlService {
    
    private final StudentRepository studentRepository;
    // Course 테이블 더 이상 사용하지 않음
    private final AssignmentRepository assignmentRepository;
    private final LectureRepository lectureRepository;
    private final StudentUpdateStatusRepository updateStatusRepository;
    private final SyncLogRepository syncLogRepository;
    
    public ClientCrawlService(
            StudentRepository studentRepository,
            AssignmentRepository assignmentRepository,
            LectureRepository lectureRepository,
            StudentUpdateStatusRepository updateStatusRepository,
            SyncLogRepository syncLogRepository) {
        this.studentRepository = studentRepository;
        this.assignmentRepository = assignmentRepository;
        this.lectureRepository = lectureRepository;
        this.updateStatusRepository = updateStatusRepository;
        this.syncLogRepository = syncLogRepository;
    }
    
    /**
     * 과목명 정리 (불필요한 접두사 제거)
     */
    private String cleanCourseName(String courseName) {
        if (courseName == null) return "";
        
        // 제거할 접두사 패턴들
        String[] prefixesToRemove = {
            "비러닝학부",
            "오프라인학부",
            "원격활용학부",
            "블렌디드러닝학부",
            "온라인학부",
            "비대면학부",
            "대면학부"
        };
        
        String cleaned = courseName;
        for (String prefix : prefixesToRemove) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
                break;
            }
        }
        
        return cleaned;
    }
    
    /**
     * 학생 데이터 삭제
     */
    @Transactional
    public boolean deleteStudentData(Long studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        
        // 학생의 모든 과제와 수업 삭제
        assignmentRepository.deleteByStudent(student);
        lectureRepository.deleteByStudent(student);
        
        System.out.println("Deleted all data for student: " + studentId);
        return true;
    }
    
    @Transactional
    public int processCrawlData(Long studentId, ClientCrawlDataDto data) {
        SyncLog log = new SyncLog();
        log.setSource("client:" + studentId + ":" + data.getClientPlatform());
        
        try {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                // 이메일로 먼저 조회 (중복 방지)
                String email = "client-" + String.valueOf(studentId) + "@local";
                student = studentRepository.findByEmail(email).orElse(null);
                
                if (student == null) {
                    // 테스트/초기 온보딩 편의: 학생이 없으면 임시로 생성
                    Student ns = new Student();
                    ns.setEmail(email);
                    ns.setName("Client " + String.valueOf(studentId));
                    ns.setPasswordHash("NOT_USED"); // 클라이언트 크롤링에서는 사용 안 함
                    student = studentRepository.save(ns);
                }
            }
            
            // 과목 정보는 더 이상 별도 테이블에 저장하지 않음
            // Assignment와 Lecture에 과목명을 직접 저장
            
            // 디버깅: 받은 데이터 확인
            if (data.getItems() != null && !data.getItems().isEmpty()) {
                System.out.println("\n=== First 10 items from client ===");
                int debugCount = 0;
                for (ClientCrawlDataDto.ItemDto item : data.getItems()) {
                    if (debugCount++ >= 10) break;
                    System.out.println(String.format("  [%s] %s -> Course: '%s'", 
                        item.getType(), item.getTitle(), item.getCourseName()));
                }
                System.out.println();
            }
            
            // 과제 및 수업 정보 처리
            int imported = 0;
            int debugIdCount = 0; // ID 디버깅용 카운터
            Instant nowKst = Instant.now();
            Instant oneMonthLater = nowKst.plus(30, java.time.temporal.ChronoUnit.DAYS);
            
                // 기존 학생의 과제/수업 삭제 (새 데이터로 대체)
                try {
                    assignmentRepository.deleteByStudent(student);
                    lectureRepository.deleteByStudent(student);
                    assignmentRepository.flush();
                    lectureRepository.flush();
                } catch (Exception e) {
                    // 삭제 실패 시 로그만 남기고 계속 진행
                    System.err.println("Failed to delete existing data: " + e.getMessage());
                }
            
            if (data.getItems() != null) {
                for (ClientCrawlDataDto.ItemDto item : data.getItems()) {
                    if (item.getCourseName() == null || item.getTitle() == null) {
                        continue;
                    }
                    
                    // 과목명 정리 (불필요한 접두사 제거)
                    String originalCourseName = item.getCourseName();
                    String cleanedCourseName = cleanCourseName(originalCourseName);
                    
                    // 과목명에서 코드 부분만 추출 (더 짧고 명확하게)
                    // 예: "디지털논리회로[202502-EEC2106-001]박재현" -> "디지털논리회로"
                    int bracketIndex = cleanedCourseName.indexOf('[');
                    if (bracketIndex > 0) {
                        cleanedCourseName = cleanedCourseName.substring(0, bracketIndex).trim();
                    }
                    
                    // 과목명 길이 제한 (50자로 늘림)
                    if (cleanedCourseName.length() > 50) {
                        cleanedCourseName = cleanedCourseName.substring(0, 50);
                    }
                    
                    // 디버깅: 실제 과목 매칭 상황 확인
                    if (item.getTitle().toLowerCase().contains("vivado")) {
                        System.out.println("📍 Found 'vivado' item: '" + item.getTitle() + 
                            "' from original course: '" + originalCourseName + 
                            "' -> cleaned: '" + cleanedCourseName + "'");
                    }
                    
                    Instant dueAt = parseDue(item.getDue());
                    
                    // 과거 마감 항목 제외
                    if (dueAt != null && dueAt.isBefore(nowKst)) {
                        continue;
                    }
                    
                    // 1달 이후 마감 항목 제외
                    if (dueAt != null && dueAt.isAfter(oneMonthLater)) {
                        continue;
                    }
                    
                    // 고유 ID 생성 - 제목, 학생ID, 과목명, 마감일로 생성
                    // 과목명도 포함하여 같은 제목이라도 다른 과목이면 다른 ID
                    String idSource = item.getTitle() + "|" + cleanedCourseName + "|" + studentId + "|" + 
                                    (item.getDue() != null ? item.getDue() : "NO_DUE");
                    String id = digest(idSource);
                    
                    // 디버깅: ID 생성 확인 (처음 몇 개만)
                    if (debugIdCount++ < 5) {
                        System.out.println("ID Source: " + idSource + " -> ID: " + id);
                    }
                    
                        if ("assignment".equalsIgnoreCase(item.getType())) {
                            try {
                                Assignment existing = assignmentRepository.findById(id).orElse(null);
                                if (existing == null) {
                                    // 새로운 과제 생성
                                    Assignment assignment = new Assignment();
                                    assignment.setId(id);
                                    assignment.setCourseName(cleanedCourseName);
                                    assignment.setTitle(item.getTitle());
                                    assignment.setUrl(null); // URL 저장하지 않음 (개인정보 보호)
                                    assignment.setDueAt(dueAt);
                                    assignment.setStudent(student);
                                    assignmentRepository.saveAndFlush(assignment);
                                    imported++;
                                    System.out.println("✓ Saved assignment: " + item.getTitle() + " -> " + cleanedCourseName);
                                } else {
                                    // 이미 존재하는 경우 업데이트만
                                    if (!cleanedCourseName.equals(existing.getCourseName())) {
                                        System.out.println("⚠ Assignment '" + item.getTitle() + 
                                            "' already in: " + existing.getCourseName() + 
                                            " (not " + cleanedCourseName + ")");
                                    }
                                    // 마감일 업데이트
                                    if (dueAt != null && !dueAt.equals(existing.getDueAt())) {
                                        existing.setDueAt(dueAt);
                                        assignmentRepository.save(existing);
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error saving assignment: " + item.getTitle() + " - " + e.getMessage());
                            }
                        } else if ("class".equalsIgnoreCase(item.getType()) || "lecture".equalsIgnoreCase(item.getType())) {
                            try {
                                Lecture existing = lectureRepository.findById(id).orElse(null);
                                if (existing == null) {
                                    // 새로운 강의 생성
                                    Lecture lecture = new Lecture();
                                    lecture.setId(id);
                                    lecture.setCourseName(cleanedCourseName);
                                    lecture.setTitle(item.getTitle());
                                    lecture.setUrl(null); // URL 저장하지 않음 (개인정보 보호)
                                    lecture.setDueAt(dueAt);
                                    lecture.setStudent(student);
                                    lectureRepository.saveAndFlush(lecture);
                                    imported++;
                                    System.out.println("✓ Saved lecture: " + item.getTitle() + " -> " + cleanedCourseName);
                                } else {
                                    // 이미 존재하는 경우 업데이트만
                                    if (!cleanedCourseName.equals(existing.getCourseName())) {
                                        System.out.println("⚠ Lecture '" + item.getTitle() + 
                                            "' already in: " + existing.getCourseName() + 
                                            " (not " + cleanedCourseName + ")");
                                    }
                                    // 마감일 업데이트
                                    if (dueAt != null && !dueAt.equals(existing.getDueAt())) {
                                        existing.setDueAt(dueAt);
                                        lectureRepository.save(existing);
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error saving lecture: " + item.getTitle() + " - " + e.getMessage());
                            }
                        }
                }
            }
            
            // 업데이트 상태 기록 (lambda에서 비-final 변수 참조 회피)
            StudentUpdateStatus status = updateStatusRepository.findByStudent(student)
                    .orElseGet(StudentUpdateStatus::new);
            if (status.getStudent() == null) {
                status.setStudent(student);
            }
            
            status.setLastUpdatedAt(Instant.now());
            status.setClientVersion(data.getClientVersion());
            status.setClientPlatform(data.getClientPlatform());
            status.setNotificationCount(0); // 업데이트 성공 시 알림 카운트 리셋
            updateStatusRepository.save(status);
            
            log.setStatus("success");
            log.setMessage("imported=" + imported);
            
            return imported;
            
        } catch (Exception e) {
            log.setStatus("error");
            log.setMessage(e.getMessage());
            throw new RuntimeException("Failed to process crawl data", e);
        } finally {
            syncLogRepository.save(log);
        }
    }
    
    private static String digest(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(s));
        }
    }
    
    private static Instant parseDue(String due) {
        if (due == null || due.isBlank()) return null;
        try {
            String norm = due.trim();
            
            // 다양한 날짜 형식 처리
            LocalDateTime ldt;
            
            if (norm.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                // YYYY-MM-DD HH:MM:SS 형식
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                ldt = LocalDateTime.parse(norm, formatter);
            } else if (norm.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                // YYYY-MM-DD HH:MM 형식 (초 없음)
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                ldt = LocalDateTime.parse(norm, formatter);
            } else {
                // ISO 형식으로 시도 (T 구분자 사용)
                if (norm.length() == 16) norm = norm + ":00";
                ldt = LocalDateTime.parse(norm.replace(' ', 'T'));
            }
            
            return ldt.atZone(ZoneId.of("Asia/Seoul")).toInstant();
        } catch (Exception e) {
            System.err.println("Failed to parse date: " + due + " - " + e.getMessage());
            return null;
        }
    }
}

