package com.audora.inhash.controller;

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
            // 인증 시도: 입력된 username과 password가 올바른지 확인
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            logger.info("인증 성공: {}", authentication.getName());
            // DB에서 사용자 정보 조회
            User user = userService.findByUsername(loginRequest.getUsername());
            // 비밀번호 제거
            user.setPassword(null);
            String token = jwtUtil.generateToken(loginRequest.getUsername());
            // DTO를 사용하여 사용자 정보를 클라이언트에 전송
            return ResponseEntity.ok(new AuthResponse(token, new UserResponse(user)));
        } catch (Exception e) {
            logger.error("로그인 실패: {}", e.getMessage(), e);
            return new ResponseEntity<>("잘못된 아이디 또는 비밀번호입니다.", HttpStatus.UNAUTHORIZED);
        }
    }

    // 응답 객체: 토큰과 사용자 정보를 함께 포함
    static class AuthResponse {
        private String token;
        private UserResponse user;

        public AuthResponse(String token, UserResponse user) {
            this.token = token;
            this.user = user;
        }

        public String getToken() {
            return token;
        }
        public void setToken(String token) {
            this.token = token;
        }
        public UserResponse getUser() {
            return user;
        }
        public void setUser(UserResponse user) {
            this.user = user;
        }
    }

    // 사용자 정보 DTO – 클라이언트에 필요한 필드만 포함
    static class UserResponse {
        private Long id;
        private String username;

        public UserResponse(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
        }
        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
    }
}
