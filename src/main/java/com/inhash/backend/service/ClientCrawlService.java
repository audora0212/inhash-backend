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
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final LectureRepository lectureRepository;
    private final StudentUpdateStatusRepository updateStatusRepository;
    private final SyncLogRepository syncLogRepository;
    
    public ClientCrawlService(
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AssignmentRepository assignmentRepository,
            LectureRepository lectureRepository,
            StudentUpdateStatusRepository updateStatusRepository,
            SyncLogRepository syncLogRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.assignmentRepository = assignmentRepository;
        this.lectureRepository = lectureRepository;
        this.updateStatusRepository = updateStatusRepository;
        this.syncLogRepository = syncLogRepository;
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
            
            // 과목 정보 처리
            if (data.getCourses() != null) {
                for (ClientCrawlDataDto.CourseDto courseDto : data.getCourses()) {
                    try {
                        String courseId = digest(courseDto.getName());
                        Course existingCourse = courseRepository.findById(courseId).orElse(null);
                        if (existingCourse == null) {
                            Course course = new Course();
                            course.setId(courseId);
                            course.setName(courseDto.getName());
                            course.setMainLink(courseDto.getMainLink());
                            courseRepository.save(course);
                        } else {
                            // 기존 과목 정보 업데이트
                            if (courseDto.getMainLink() != null && !courseDto.getMainLink().equals(existingCourse.getMainLink())) {
                                existingCourse.setMainLink(courseDto.getMainLink());
                                courseRepository.save(existingCourse);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to process course: " + courseDto.getName() + " - " + e.getMessage());
                    }
                }
            }
            
            // 트랜잭션 플러시 (과목 저장 확정)
            courseRepository.flush();
            
            // 과제 및 수업 정보 처리
            int imported = 0;
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
                    
                    String courseId = digest(item.getCourseName());
                    Course course = courseRepository.findById(courseId).orElse(null);
                    if (course == null) {
                        course = new Course();
                        course.setId(courseId);
                        course.setName(item.getCourseName());
                        course = courseRepository.save(course);
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
                    
                        // ID 생성 시 URL이 null인 경우 처리 개선
                        String urlPart = item.getUrl() != null ? item.getUrl() : "NO_URL_" + item.getTitle();
                        String id = digest(item.getTitle() + "|" + urlPart + "|" + 
                                         courseId + "|" + studentId + "|" + 
                                         (item.getDue() != null ? item.getDue() : "NO_DUE"));
                    
                        if ("assignment".equalsIgnoreCase(item.getType())) {
                            // 기존 항목이 있는지 확인
                            if (!assignmentRepository.existsById(id)) {
                                Assignment assignment = new Assignment();
                                assignment.setId(id);
                                assignment.setCourse(course);
                                assignment.setTitle(item.getTitle());
                                assignment.setUrl(item.getUrl());
                                assignment.setDueAt(dueAt);
                                assignment.setStudent(student);
                                assignmentRepository.save(assignment);
                                imported++;
                            } else {
                                System.out.println("Assignment already exists: " + item.getTitle());
                            }
                        } else if ("class".equalsIgnoreCase(item.getType()) || "lecture".equalsIgnoreCase(item.getType())) {
                            // 기존 항목이 있는지 확인
                            if (!lectureRepository.existsById(id)) {
                                Lecture lecture = new Lecture();
                                lecture.setId(id);
                                lecture.setCourse(course);
                                lecture.setTitle(item.getTitle());
                                lecture.setUrl(item.getUrl());
                                lecture.setDueAt(dueAt);
                                lecture.setStudent(student);
                                lectureRepository.save(lecture);
                                imported++;
                            } else {
                                System.out.println("Lecture already exists: " + item.getTitle());
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

