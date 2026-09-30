package com.example.blog.auth.domain;

import com.example.blog.auth.domain.model.LoginCmd;
import com.example.blog.auth.domain.model.LoginResult;
import com.example.blog.user.UserAPI;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final AuthenticationManager authManager;
    private final JwtTokenFactory tokenProvider;
    private final UserAPI userAPI;

    AuthService(AuthenticationManager authManager, JwtTokenFactory tokenProvider, UserAPI userAPI) {
        this.authManager = authManager;
        this.tokenProvider = tokenProvider;
        this.userAPI = userAPI;
    }

    public LoginResult authenticate(LoginCmd request) {
        var auth = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        authManager.authenticate(auth);

        var user = userAPI.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + request.email()));

        var authToken = tokenProvider.generateToken(user);
        return new LoginResult(
                authToken.token(),
                authToken.expiresAt(),
                user.id(),
                user.name(),
                user.email(),
                user.role().name());
    }
}
