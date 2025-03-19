package com.audora.inhash.service;

import com.audora.inhash.model.Comment;
import com.audora.inhash.model.Post;
import com.audora.inhash.repository.CommentRepository;
import com.audora.inhash.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    public Comment addComment(Long postId, Comment comment) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return null;
        }
        comment.setPost(post);
        comment.setCreatedDate(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public void deleteComment(Long id) {
        commentRepository.deleteById(id);
    }
}
