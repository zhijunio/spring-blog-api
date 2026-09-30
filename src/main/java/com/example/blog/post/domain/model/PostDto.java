package com.example.blog.post.domain.model;

import java.time.Instant;

public record PostDto(
        /** Post identifier. */
        Long id,
        /** Post title. */
        String title,
        /** URL-friendly post slug. */
        String slug,
        /** Post body content. */
        String content,
        /** Category slug. */
        String categorySlug,
        /** Category display name. */
        String categoryName,
        /** Author identifier. */
        Long authorId,
        /** Author display name. */
        String authorName,
        /** Creation timestamp in UTC. */
        Instant createdAt,
        /** Last update timestamp in UTC. */
        Instant updatedAt) {}
