// src/main/java/com/audora/inhash/controller/UserController.java
package com.audora.inhash.controller;

import com.audora.inhash.dto.*;
import com.audora.inhash.model.User;
import com.audora.inhash.service.CommentService;
import com.audora.inhash.service.PostService;
import com.audora.inhash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        UserResponse dto = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getJoinDate()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<List<PostResponseDto>> getUserPosts(@PathVariable Long id) {
        List<PostResponseDto> dtos = postService.getAllPosts().stream()
                .filter(p -> p.getAuthorId().equals(id))
                .map(postService::convertToPostResponseDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponseDto>> getUserComments(@PathVariable Long id) {
        List<CommentResponseDto> dtos = commentService.getCommentsByAuthorId(id);
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest req) {
        try {
            User updated = userService.updateProfile(id, req.getEmail(), req.getUsername());
            UserResponse dto = new UserResponse(
                    updated.getId(),
                    updated.getUsername(),
                    updated.getEmail(),
                    updated.getJoinDate()
            );
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest req) {
        try {
            userService.changePassword(id, req.getCurrentPassword(), req.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
