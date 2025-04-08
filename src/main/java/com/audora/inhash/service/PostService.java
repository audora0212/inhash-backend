package com.audora.inhash.service;

import com.audora.inhash.model.Post;
import com.audora.inhash.model.User;
import com.audora.inhash.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;  // UserService 주입

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
        // 현재 로그인한 사용자의 정보를 조회하여 id를 추출
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
            // 현재 글 작성자와 로그인한 사용자 id 비교
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
        // 현재 글 작성자와 로그인한 사용자 id 비교
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
}
