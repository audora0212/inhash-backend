package com.audora.inhash.service;

import com.audora.inhash.model.User;
import com.audora.inhash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 처리
    @Transactional
    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("이미 등록된 아이디입니다.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // 사용자명(username)으로 조회
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    // ID로 조회
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // 프로필 정보 수정
    @Transactional
    public User updateProfile(Long id, String email, String username) {
        User user = findById(id);
        if (user == null) throw new RuntimeException("User not found");
        user.setEmail(email);
        user.setUsername(username);
        return userRepository.save(user);
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(Long id, String currentPwd, String newPwd) {
        User user = findById(id);
        if (user == null) throw new RuntimeException("User not found");
        if (!passwordEncoder.matches(currentPwd, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(user);
    }

    // 계정 삭제
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}