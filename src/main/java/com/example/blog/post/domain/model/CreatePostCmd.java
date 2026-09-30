package com.example.blog.post.domain.model;

public record CreatePostCmd(String title, String slug, String content, String categorySlug) {}
