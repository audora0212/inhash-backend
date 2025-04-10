package com.audora.inhash.controller;

import com.audora.inhash.dto.AuthResponse;
import com.audora.inhash.dto.UserResponse;
import com.audora.inhash.model.User;
import com.audora.inhash.security.JwtUtil;
import com.audora.inhash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            logger.info("회원가입 성공: {}", registeredUser.getUsername());
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("회원가입 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        try {
            logger.info("로그인 시도: username={}", loginRequest.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            logger.info("인증 성공: {}", authentication.getName());
            User user = userService.findByUsername(loginRequest.getUsername());
            // 비밀번호 정보 제거
            user.setPassword(null);
            String token = jwtUtil.generateToken(loginRequest.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, new UserResponse(user.getId(), user.getUsername())));
        } catch (Exception e) {
            logger.error("로그인 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>("잘못된 아이디 또는 비밀번호입니다.", HttpStatus.UNAUTHORIZED);
        }
    }
}
