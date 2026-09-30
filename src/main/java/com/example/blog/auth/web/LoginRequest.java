package com.example.blog.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank(message = "{validation.email.required}") @Email(message = "{validation.email.invalid}") String email,

        @NotBlank(message = "{validation.password.required}") String password) {}
