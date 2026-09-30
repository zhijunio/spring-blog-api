package com.example.blog.config;

import com.example.blog.shared.security.SecurityUtils;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
class PersistenceConfig {

    @Bean
    AuditorAware<Long> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    static class SpringSecurityAuditorAware implements AuditorAware<Long> {

        @Override
        public @NonNull Optional<Long> getCurrentAuditor() {
            return Optional.ofNullable(SecurityUtils.getCurrentUserId());
        }
    }
}
