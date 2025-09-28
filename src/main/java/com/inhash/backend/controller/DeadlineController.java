package com.inhash.backend.controller;

import com.inhash.backend.domain.Assignment;
import com.inhash.backend.domain.Lecture;
import com.inhash.backend.domain.Student;
import com.inhash.backend.repository.AssignmentRepository;
import com.inhash.backend.repository.LectureRepository;
import com.inhash.backend.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 과제/수업 마감 기한 조회 API
 */
@RestController
@RequestMapping("/api/deadlines")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://127.0.0.1:3000", "http://127.0.0.1:3001"})
public class DeadlineController {
    
    private final StudentRepository studentRepository;
    private final AssignmentRepository assignmentRepository;
    private final LectureRepository lectureRepository;
    
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            .withZone(ZoneId.of("Asia/Seoul"));
    
    public DeadlineController(StudentRepository studentRepository, 
                             AssignmentRepository assignmentRepository,
                             LectureRepository lectureRepository) {
        this.studentRepository = studentRepository;
        this.assignmentRepository = assignmentRepository;
        this.lectureRepository = lectureRepository;
    }
    
    /**
     * 학생의 모든 과제/수업 조회
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>> getDeadlines(@PathVariable Long studentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                response.put("success", false);
                response.put("error", "학생을 찾을 수 없습니다");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 현재 시간
            Instant now = Instant.now();
            
            // 과제 조회 (마감되지 않은 것만)
            List<Map<String, Object>> assignments = assignmentRepository.findByStudent(student)
                    .stream()
                    .filter(a -> a.getDueAt() == null || a.getDueAt().isAfter(now))
                    .map(this::assignmentToMap)
                    .collect(Collectors.toList());
            
            // 수업 조회 (마감되지 않은 것만)
            List<Map<String, Object>> lectures = lectureRepository.findByStudent(student)
                    .stream()
                    .filter(l -> l.getDueAt() == null || l.getDueAt().isAfter(now))
                    .map(this::lectureToMap)
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("assignments", assignments);
            response.put("lectures", lectures);
            
            System.out.println("Returning " + assignments.size() + " assignments and " + 
                             lectures.size() + " lectures for student " + studentId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "데이터 조회 중 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 학생의 모든 과제/수업 조회 (마감된 것 포함)
     */
    @GetMapping("/{studentId}/all")
    public ResponseEntity<Map<String, Object>> getAllDeadlines(@PathVariable Long studentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                response.put("success", false);
                response.put("error", "학생을 찾을 수 없습니다");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 모든 과제 조회
            List<Map<String, Object>> assignments = assignmentRepository.findByStudent(student)
                    .stream()
                    .map(this::assignmentToMap)
                    .collect(Collectors.toList());
            
            // 모든 수업 조회
            List<Map<String, Object>> lectures = lectureRepository.findByStudent(student)
                    .stream()
                    .map(this::lectureToMap)
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("assignments", assignments);
            response.put("lectures", lectures);
            
            System.out.println("Returning all " + assignments.size() + " assignments and " + 
                             lectures.size() + " lectures for student " + studentId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "데이터 조회 중 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 오늘 마감인 항목 조회
     */
    @GetMapping("/{studentId}/today")
    public ResponseEntity<Map<String, Object>> getTodayDeadlines(@PathVariable Long studentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                response.put("success", false);
                response.put("error", "학생을 찾을 수 없습니다");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 오늘 시작과 끝
            Instant todayStart = Instant.now().atZone(ZoneId.of("Asia/Seoul"))
                    .toLocalDate().atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
            Instant todayEnd = todayStart.plusSeconds(86399); // 23:59:59
            
            // 오늘 마감 과제
            List<Map<String, Object>> todayAssignments = assignmentRepository.findByStudent(student)
                    .stream()
                    .filter(a -> a.getDueAt() != null && 
                            a.getDueAt().isAfter(todayStart) && 
                            a.getDueAt().isBefore(todayEnd))
                    .map(this::assignmentToMap)
                    .collect(Collectors.toList());
            
            // 오늘 마감 수업
            List<Map<String, Object>> todayLectures = lectureRepository.findByStudent(student)
                    .stream()
                    .filter(l -> l.getDueAt() != null && 
                            l.getDueAt().isAfter(todayStart) && 
                            l.getDueAt().isBefore(todayEnd))
                    .map(this::lectureToMap)
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("assignments", todayAssignments);
            response.put("lectures", todayLectures);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "데이터 조회 중 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    private Map<String, Object> assignmentToMap(Assignment assignment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", assignment.getId());
        map.put("title", assignment.getTitle());
        map.put("courseName", assignment.getCourseName() != null ? assignment.getCourseName() : "");
        map.put("url", assignment.getUrl());
        map.put("completed", assignment.getCompleted() != null ? assignment.getCompleted() : false);
        map.put("type", "assignment");
        
        if (assignment.getDueAt() != null) {
            map.put("dueAt", formatter.format(assignment.getDueAt()));
            map.put("dueDate", formatter.format(assignment.getDueAt())); // 캘린더용
            
            // 남은 일수 계산
            long remainingSeconds = assignment.getDueAt().getEpochSecond() - Instant.now().getEpochSecond();
            long remainingDays = remainingSeconds / 86400;
            map.put("remainingDays", Math.max(0, remainingDays));
        } else {
            map.put("dueAt", null);
            map.put("dueDate", null);
            map.put("remainingDays", null);
        }
        
        return map;
    }
    
    private Map<String, Object> lectureToMap(Lecture lecture) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", lecture.getId());
        map.put("title", lecture.getTitle());
        map.put("courseName", lecture.getCourseName() != null ? lecture.getCourseName() : "");
        map.put("url", lecture.getUrl());
        map.put("completed", lecture.getCompleted() != null ? lecture.getCompleted() : false);
        map.put("type", "lecture");
        
        if (lecture.getDueAt() != null) {
            map.put("dueAt", formatter.format(lecture.getDueAt()));
            map.put("lectureDate", formatter.format(lecture.getDueAt())); // 캘린더용
            
            // 남은 일수 계산
            long remainingSeconds = lecture.getDueAt().getEpochSecond() - Instant.now().getEpochSecond();
            long remainingDays = remainingSeconds / 86400;
            map.put("remainingDays", Math.max(0, remainingDays));
        } else {
            map.put("dueAt", null);
            map.put("lectureDate", null);
            map.put("remainingDays", null);
        }
        
        return map;
    }
}
