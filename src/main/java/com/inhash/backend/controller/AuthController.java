package com.inhash.backend.controller;

import com.inhash.backend.domain.Student;
import com.inhash.backend.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final StudentRepository studentRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public AuthController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
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
            student.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            
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
            if (!passwordEncoder.matches(request.getPassword(), student.getPasswordHash())) {
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
}