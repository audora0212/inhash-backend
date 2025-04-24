package com.audora.inhash.service;

import com.audora.inhash.dto.CommentResponseDto;
import com.audora.inhash.dto.PostResponseDto;
import com.audora.inhash.model.Post;
import com.audora.inhash.model.User;
import com.audora.inhash.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    public Post incrementViewCount(Long id) {
        return postRepository.findById(id).map(post -> {
            Integer currentCount = post.getViewCount() != null ? post.getViewCount() : 0;
            post.setViewCount(currentCount + 1);
            return postRepository.save(post);
        }).orElse(null);
    }

    public Post createPost(Post post) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(currentUsername);
        post.setAuthorId(currentUser.getId());
        post.setCreatedDate(LocalDateTime.now());
        post.setUpdatedDate(LocalDateTime.now());
        return postRepository.save(post);
    }

    public Post updatePost(Long id, Post updatedPost) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(currentUsername);
        return postRepository.findById(id).map(post -> {
            if (!post.getAuthorId().equals(currentUser.getId())) {
                throw new AccessDeniedException("본인이 작성한 글만 수정할 수 있습니다.");
            }
            post.setTitle(updatedPost.getTitle());
            post.setContent(updatedPost.getContent());
            post.setUpdatedDate(LocalDateTime.now());
            return postRepository.save(post);
        }).orElse(null);
    }

    public void deletePost(Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByUsername(currentUsername);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getAuthorId().equals(currentUser.getId())) {
            throw new AccessDeniedException("본인이 작성한 글만 삭제할 수 있습니다.");
        }
        postRepository.deleteById(id);
    }

    public Post likePost(Long id) {
        return postRepository.findById(id).map(post -> {
            Integer currentLikes = post.getLikeCount() != null ? post.getLikeCount() : 0;
            post.setLikeCount(currentLikes + 1);
            return postRepository.save(post);
        }).orElse(null);
    }

    public PostResponseDto convertToPostResponseDto(Post post) {
        User user = userService.findById(post.getAuthorId());
        String username = (user != null) ? user.getUsername() : "Unknown";

        // 수정: 댓글 DTO 생성 시에도 postId 포함
        List<CommentResponseDto> commentDtos = post.getComments().stream().map(comment -> {
            User commentUser = userService.findById(comment.getAuthorId());
            String commentUsername = (commentUser != null) ? commentUser.getUsername() : "Unknown";
            return new CommentResponseDto(
                    comment.getId(),
                    comment.getContent(),
                    commentUsername,
                    comment.getCreatedDate(),
                    comment.getPost().getId()
            );
        }).collect(Collectors.toList());

        return new PostResponseDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                username,
                post.getCreatedDate(),
                post.getUpdatedDate(),
                post.getLikeCount(),
                post.getViewCount(),
                commentDtos
        );
    }
}
