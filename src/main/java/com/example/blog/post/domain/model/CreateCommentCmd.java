package com.example.blog.post.domain.model;

public record CreateCommentCmd(String name, String email, String content, Long postId) {}
