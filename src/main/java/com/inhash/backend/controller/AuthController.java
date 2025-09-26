package com.inhash.backend.controller;

import com.inhash.backend.domain.Student;
import com.inhash.backend.repository.StudentRepository;
import com.inhash.backend.service.ClientCrawlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final StudentRepository studentRepository;
    private final ClientCrawlService clientCrawlService;
    
    public AuthController(StudentRepository studentRepository, ClientCrawlService clientCrawlService) {
        this.studentRepository = studentRepository;
        this.clientCrawlService = clientCrawlService;
    }
    
    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody SignupRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 이메일 중복 확인
            if (studentRepository.findByEmail(request.getEmail()).isPresent()) {
                response.put("success", false);
                response.put("error", "이미 사용중인 이메일입니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 새 학생 생성
            Student student = new Student();
            student.setEmail(request.getEmail());
            student.setName(request.getName());
            student.setPasswordHash(hashPassword(request.getPassword()));
            
            student = studentRepository.save(student);
            
            // 간단한 토큰 생성 (실제 환경에서는 JWT 사용 권장)
            String token = UUID.randomUUID().toString();
            
            response.put("success", true);
            response.put("studentId", student.getId());
            response.put("token", token);
            response.put("email", student.getEmail());
            response.put("name", student.getName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "회원가입 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 이메일로 학생 찾기
            Student student = studentRepository.findByEmail(request.getEmail())
                    .orElse(null);
            
            if (student == null) {
                response.put("success", false);
                response.put("error", "이메일 또는 비밀번호가 일치하지 않습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 비밀번호 확인
            if (!verifyPassword(request.getPassword(), student.getPasswordHash())) {
                response.put("success", false);
                response.put("error", "이메일 또는 비밀번호가 일치하지 않습니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 간단한 토큰 생성 (실제 환경에서는 JWT 사용 권장)
            String token = UUID.randomUUID().toString();
            
            response.put("success", true);
            response.put("studentId", student.getId());
            response.put("token", token);
            response.put("email", student.getEmail());
            response.put("name", student.getName());
            
            // LMS 연결 상태 확인 (과제/수업 데이터가 있는지)
            boolean hasData = studentRepository.existsById(student.getId());
            response.put("lmsLinked", hasData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "로그인 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 회원가입 요청 DTO
     */
    public static class SignupRequest {
        private String email;
        private String password;
        private String name;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    /**
     * 로그인 요청 DTO
     */
    public static class LoginRequest {
        private String email;
        private String password;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    /**
     * 비밀번호 해시 생성 (SHA-256 + Base64)
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
    
    /**
     * 비밀번호 검증
     */
    private boolean verifyPassword(String password, String hashedPassword) {
        String hashed = hashPassword(password);
        return hashed.equals(hashedPassword);
    }
    
    /**
     * 계정 삭제
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long studentId = Long.parseLong(request.get("studentId").toString());
            
            // 학생 찾기
            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Student not found");
                return ResponseEntity.notFound().build();
            }
            
            // 1. 먼저 관련 데이터 모두 삭제 (과제, 수업)
            boolean dataDeleted = clientCrawlService.deleteStudentData(studentId);
            if (!dataDeleted) {
                response.put("success", false);
                response.put("error", "Failed to delete student data");
                return ResponseEntity.internalServerError().body(response);
            }
            
            // 2. 학생 계정 삭제
            Student student = studentOpt.get();
            studentRepository.delete(student);
            
            response.put("success", true);
            response.put("message", "Account and all related data deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to delete account: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}