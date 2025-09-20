package com.inhash.backend.service;

import com.inhash.backend.domain.Student;
import com.inhash.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class AuthService {

    private final StudentRepository studentRepository;

    public AuthService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student register(String email, String name, String rawPassword) {
        Student s = new Student();
        s.setEmail(email);
        s.setName(name);
        s.setPasswordHash(hash(rawPassword));
        return studentRepository.save(s);
    }

    public Optional<Student> login(String email, String rawPassword) {
        return studentRepository.findAll().stream()
                .filter(s -> email.equalsIgnoreCase(s.getEmail()))
                .filter(s -> s.getPasswordHash() != null && s.getPasswordHash().equals(hash(rawPassword)))
                .findFirst();
    }

    public void resetPassword(Long studentId, String newPassword) {
        studentRepository.findById(studentId).ifPresent(s -> {
            s.setPasswordHash(hash(newPassword));
            studentRepository.save(s);
        });
    }

    private static String hash(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}


