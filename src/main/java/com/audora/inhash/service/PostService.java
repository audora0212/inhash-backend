package com.audora.inhash.service;

import com.audora.inhash.model.Post;
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

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    // 단순 조회: viewCount 증가는 하지 않음.
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    // 별도의 엔드포인트에서 호출하여 조회수를 업데이트
    public Post incrementViewCount(Long id) {
        return postRepository.findById(id).map(post -> {
            Integer currentCount = post.getViewCount() != null ? post.getViewCount() : 0;
            post.setViewCount(currentCount + 1);
            return postRepository.save(post);
        }).orElse(null);
    }

    public Post createPost(Post post) {
        // 현재 인증된 사용자 이름을 글 작성자로 설정
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        post.setAuthor(currentUsername);
        post.setCreatedDate(LocalDateTime.now());
        post.setUpdatedDate(LocalDateTime.now());
        return postRepository.save(post);
    }

    public Post updatePost(Long id, Post updatedPost) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return postRepository.findById(id).map(post -> {
            if (!post.getAuthor().equals(currentUsername)) {
                throw new AccessDeniedException("You are not authorized to update this post");
            }
            post.setTitle(updatedPost.getTitle());
            post.setContent(updatedPost.getContent());
            post.setUpdatedDate(LocalDateTime.now());
            return postRepository.save(post);
        }).orElse(null);
    }

    public void deletePost(Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getAuthor().equals(currentUsername)) {
            throw new AccessDeniedException("You are not authorized to delete this post");
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
