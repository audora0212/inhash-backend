package com.audora.inhash.service;

import com.audora.inhash.dto.CommentResponseDto;
import com.audora.inhash.model.Comment;
import com.audora.inhash.model.Post;
import com.audora.inhash.model.User;
import com.audora.inhash.repository.CommentRepository;
import com.audora.inhash.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    public Comment addComment(Long postId, Comment comment) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return null;
        }
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(currentUsername);
        comment.setAuthorId(currentUser.getId());
        comment.setPost(post);
        comment.setCreatedDate(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public void deleteComment(Long id) {
        commentRepository.deleteById(id);
    }

    // Comment 엔티티를 CommentResponseDto로 변환하여 반환
    public CommentResponseDto convertToCommentResponseDto(Comment comment) {
        User user = userService.findById(comment.getAuthorId());
        String username = (user != null) ? user.getUsername() : "Unknown";
        return new CommentResponseDto(comment.getId(), comment.getContent(), username, comment.getCreatedDate());
    }
}
