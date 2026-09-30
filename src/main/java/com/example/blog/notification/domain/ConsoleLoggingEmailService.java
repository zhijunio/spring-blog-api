package com.example.blog.notification.domain;

import com.example.blog.ApplicationProperties;
import com.example.blog.notification.EmailService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email-service-type", havingValue = "console")
public class ConsoleLoggingEmailService implements EmailService {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleLoggingEmailService.class);
    private final ApplicationProperties properties;

    public ConsoleLoggingEmailService(ApplicationProperties properties) {
        this.properties = properties;
    }

    public void send(String to, String subject, String content) {
        this.send(List.of(to), subject, content);
    }

    public void send(List<String> to, String subject, String content) {
        String supportEmail = properties.supportEmail();
        String email = """
                ======================================================
                From: %s
                To: %s
                Subject: %s

                %s
                ======================================================
                """.formatted(supportEmail, to, subject, content);
        LOG.info(email);
    }

    @Override
    public void send(UUID idempotencyKey, String to, String subject, String content) {
        this.send(idempotencyKey, List.of(to), subject, content);
    }

    @Override
    public void send(UUID idempotencyKey, List<String> to, String subject, String content) {
        String supportEmail = properties.supportEmail();
        String email = """
                ======================================================
                Idempotency-Key: %s
                From: %s
                To: %s
                Subject: %s

                %s
                ======================================================
                """.formatted(idempotencyKey, supportEmail, to, subject, content);
        LOG.info(email);
    }
}
