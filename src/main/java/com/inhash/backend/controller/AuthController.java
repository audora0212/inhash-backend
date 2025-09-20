package com.inhash.backend.controller;

import com.inhash.backend.domain.Student;
import com.inhash.backend.service.AuthService;
import com.inhash.backend.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    public AuthController(AuthService authService, RegistrationService registrationService) {
        this.authService = authService;
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        // email, name, password, lmsUsername, lmsPassword를 함께 받는다
        Student s = registrationService.registerWithLms(
                body.get("email"),
                body.get("name"),
                body.get("password"),
                body.get("lmsUsername"),
                body.get("lmsPassword")
        );
        return ResponseEntity.ok(Map.of("id", s.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        Optional<Student> s = authService.login(body.get("email"), body.get("password"));
        return s.<ResponseEntity<?>>map(student -> ResponseEntity.ok(Map.of("id", student.getId()))).orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "invalid")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> body) {
        Long id = Long.parseLong(body.get("id"));
        authService.resetPassword(id, body.get("newPassword"));
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}


