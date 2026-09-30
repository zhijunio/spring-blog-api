package com.example.blog.user.domain.model;

public record CreateUserCmd(String name, String email, String password, Role role) {}
