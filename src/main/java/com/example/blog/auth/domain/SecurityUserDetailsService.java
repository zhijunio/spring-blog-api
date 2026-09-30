package com.example.blog.auth.domain;

import com.example.blog.user.UserAPI;
import com.example.blog.user.domain.model.UserDto;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
class SecurityUserDetailsService implements UserDetailsService {
    private final UserAPI userAPI;

    SecurityUserDetailsService(UserAPI userAPI) {
        this.userAPI = userAPI;
    }

    @Override
    @NonNull public UserDetails loadUserByUsername(@NonNull String userName) {
        return userAPI.findByEmailWithPassword(userName)
                .map(this::toSecurityUser)
                .orElseThrow(() -> new UsernameNotFoundException("Email " + userName + " not found"));
    }

    private SecurityUser toSecurityUser(UserDto user) {
        return new SecurityUser(user.id(), user.name(), user.email(), user.password(), user.role());
    }
}
