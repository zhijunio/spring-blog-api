package com.example.blog.auth.domain.model;

import java.time.Instant;

public record JwtToken(String token, Instant expiresAt) {}
