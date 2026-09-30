package com.example.blog.category.domain.model;

import java.time.Instant;

public record CategoryDto(
        /** Category identifier. */
        Long id,
        /** Category display name. */
        String name,
        /** URL-friendly category slug. */
        String slug,
        /** Creation timestamp in UTC. */
        Instant createdAt,
        /** Last update timestamp in UTC. */
        Instant updatedAt) {}
