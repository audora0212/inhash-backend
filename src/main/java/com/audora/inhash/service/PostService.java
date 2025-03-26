package com.audora.inhash.service;

import com.audora.inhash.model.Post;
import com.audora.inhash.repository.PostRepository;
import lombok.RequiredArgsConstructor;
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

    public Post getPostById(Long id) {
        return postRepository.findById(id).map(post -> {
            // 조회 시 viewCount 증가
            post.setViewCount(post.getViewCount() + 1);
            return postRepository.save(post);
        }).orElse(null);
    }

    public Post createPost(Post post) {
        post.setCreatedDate(LocalDateTime.now());
        post.setUpdatedDate(LocalDateTime.now());
        return postRepository.save(post);
    }

    public Post updatePost(Long id, Post updatedPost) {
        return postRepository.findById(id).map(post -> {
            post.setTitle(updatedPost.getTitle());
            post.setContent(updatedPost.getContent());
            post.setAuthor(updatedPost.getAuthor());
            post.setUpdatedDate(LocalDateTime.now());
            return postRepository.save(post);
        }).orElse(null);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public Post likePost(Long id) {
        return postRepository.findById(id).map(post -> {
            post.setLikeCount(post.getLikeCount() + 1);
            return postRepository.save(post);
        }).orElse(null);
    }
}
