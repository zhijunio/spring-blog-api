package com.example.blog.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskDecorator;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class AsyncConfigTests {

    private final TaskDecorator taskDecorator = new AsyncConfig().contextTaskDecorator();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void copiesAuthenticationToChildTaskAndRestoresWorkerContext() {
        Authentication authentication = new TestingAuthenticationToken("alice", "ignored", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        AtomicReference<Authentication> childAuthentication = new AtomicReference<>();

        Runnable decorated = taskDecorator.decorate(
                () -> childAuthentication.set(SecurityContextHolder.getContext().getAuthentication()));
        SecurityContextHolder.clearContext();

        decorated.run();

        assertThat(childAuthentication).hasValue(authentication);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
