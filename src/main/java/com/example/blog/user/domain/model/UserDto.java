package com.example.blog.user.domain.model;

public record UserDto(
        /** User identifier. */
        Long id,
        /** User display name. */
        String name,
        /** User email address. */
        String email,
        /** Encoded password; never expose this field in API responses. */
        String password,
        /** User role. */
        Role role) {}
