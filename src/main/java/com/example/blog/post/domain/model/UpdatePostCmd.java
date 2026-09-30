package com.example.blog.post.domain.model;

public record UpdatePostCmd(
        String slug, String newTitle, String newSlug, String newContent, String categorySlug, Long userId) {}
