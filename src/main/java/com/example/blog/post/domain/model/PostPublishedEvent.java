package com.example.blog.post.domain.model;

import java.util.UUID;

public record PostPublishedEvent(UUID eventId, String title, String slug, String content) {}
