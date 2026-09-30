package com.example.blog.auth.domain.model;

import java.time.Instant;

public record LoginResult(String accessToken, Instant expiresAt, Long userId, String name, String email, String role) {}
