package com.example.blog.auth.web;

import java.time.Instant;

record LoginResponse(String token, Instant expiresAt, Long userId, String name, String email, String role) {}
