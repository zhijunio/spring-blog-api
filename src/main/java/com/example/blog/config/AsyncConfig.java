package com.example.blog.config;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableAsync
@EnableResilientMethods
class AsyncConfig {

    @Bean
    TaskDecorator contextTaskDecorator() {
        return runnable -> {
            Map<String, String> capturedContext = MDC.getCopyOfContextMap();
            Authentication capturedAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();
            return () -> {
                Map<String, String> previousContext = MDC.getCopyOfContextMap();
                SecurityContext previousSecurityContext = SecurityContextHolder.getContext();
                try {
                    if (capturedContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(capturedContext);
                    }
                    SecurityContext childSecurityContext = SecurityContextHolder.createEmptyContext();
                    childSecurityContext.setAuthentication(capturedAuthentication);
                    SecurityContextHolder.setContext(childSecurityContext);
                    runnable.run();
                } finally {
                    SecurityContextHolder.setContext(previousSecurityContext);
                    if (previousContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previousContext);
                    }
                }
            };
        };
    }
}
