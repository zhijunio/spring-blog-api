package com.example.blog.auth.web;

import com.example.blog.auth.domain.AuthService;
import com.example.blog.auth.domain.model.LoginCmd;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api")
@Tag(name = "Auth API")
class AuthController {
    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user and returns a JWT access token.
     *
     * @return the access token and authenticated user information
     */
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication succeeded"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    LoginResponse login(@RequestBody @Valid LoginRequest req) {
        LOG.info("Login request for email: {}", req.email());
        var request = new LoginCmd(req.email(), req.password());
        var authResponse = authService.authenticate(request);
        return new LoginResponse(
                authResponse.accessToken(),
                authResponse.expiresAt(),
                authResponse.userId(),
                authResponse.name(),
                authResponse.email(),
                authResponse.role());
    }
}
