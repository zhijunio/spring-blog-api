package com.example.blog.post.domain.model;

import java.time.Instant;

public record CommentDto(
        /** Comment identifier. */
        Long id,
        /** Comment author name. */
        String name,
        /** Comment author email. */
        String email,
        /** Comment body content. */
        String content,
        /** Creation timestamp in UTC. */
        Instant createdAt,
        /** Last update timestamp in UTC. */
        Instant updatedAt) {}
