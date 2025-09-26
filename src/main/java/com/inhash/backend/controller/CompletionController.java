package com.inhash.backend.controller;

import com.inhash.backend.repository.AssignmentRepository;
import com.inhash.backend.repository.LectureRepository;
import com.inhash.backend.repository.StudentRepository;
import com.inhash.backend.domain.Assignment;
import com.inhash.backend.domain.Lecture;
import com.inhash.backend.domain.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/completion")
@CrossOrigin(origins = "*")
public class CompletionController {
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private LectureRepository lectureRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    /**
     * 과제 완료 상태 토글
     */
    @PostMapping("/assignment/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAssignmentCompletion(
            @PathVariable String id,
            @RequestParam String studentId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 학생 확인
            Student student = studentRepository.findById(Long.parseLong(studentId))
                .orElse(null);
            
            if (student == null) {
                response.put("success", false);
                response.put("error", "Student not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 과제 찾기
            Assignment assignment = assignmentRepository.findById(id)
                .orElse(null);
            
            if (assignment == null) {
                response.put("success", false);
                response.put("error", "Assignment not found");
                return ResponseEntity.notFound().build();
            }
            
            // 해당 학생의 과제인지 확인
            if (!assignment.getStudent().getId().equals(student.getId())) {
                response.put("success", false);
                response.put("error", "Assignment does not belong to this student");
                return ResponseEntity.status(403).body(response);
            }
            
            // 완료 상태 토글
            Boolean currentStatus = assignment.getCompleted();
            assignment.setCompleted(!currentStatus);
            assignmentRepository.save(assignment);
            
            response.put("success", true);
            response.put("completed", assignment.getCompleted());
            response.put("message", assignment.getCompleted() ? "Assignment marked as completed" : "Assignment marked as incomplete");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 수업 완료 상태 토글
     */
    @PostMapping("/lecture/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleLectureCompletion(
            @PathVariable String id,
            @RequestParam String studentId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 학생 확인
            Student student = studentRepository.findById(Long.parseLong(studentId))
                .orElse(null);
            
            if (student == null) {
                response.put("success", false);
                response.put("error", "Student not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 수업 찾기
            Lecture lecture = lectureRepository.findById(id)
                .orElse(null);
            
            if (lecture == null) {
                response.put("success", false);
                response.put("error", "Lecture not found");
                return ResponseEntity.notFound().build();
            }
            
            // 해당 학생의 수업인지 확인
            if (!lecture.getStudent().getId().equals(student.getId())) {
                response.put("success", false);
                response.put("error", "Lecture does not belong to this student");
                return ResponseEntity.status(403).body(response);
            }
            
            // 완료 상태 토글
            Boolean currentStatus = lecture.getCompleted();
            lecture.setCompleted(!currentStatus);
            lectureRepository.save(lecture);
            
            response.put("success", true);
            response.put("completed", lecture.getCompleted());
            response.put("message", lecture.getCompleted() ? "Lecture marked as completed" : "Lecture marked as incomplete");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
