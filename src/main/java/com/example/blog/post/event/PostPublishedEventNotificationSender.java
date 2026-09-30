package com.example.blog.post.event;

import com.example.blog.ApplicationProperties;
import com.example.blog.notification.EmailService;
import com.example.blog.post.domain.PostPublishedEventDeliveryService;
import com.example.blog.post.domain.model.PostPublishedEvent;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
class PostPublishedEventNotificationSender {
    static final int MAX_RETRIES = 2;
    static final int MAX_ATTEMPTS = MAX_RETRIES + 1;

    private final EmailService emailService;
    private final ApplicationProperties properties;
    private final PostPublishedEventDeliveryService deliveryService;

    PostPublishedEventNotificationSender(
            EmailService emailService,
            ApplicationProperties properties,
            PostPublishedEventDeliveryService deliveryService) {
        this.emailService = emailService;
        this.properties = properties;
        this.deliveryService = deliveryService;
    }

    @Retryable(
            includes = RuntimeException.class,
            maxRetries = MAX_RETRIES,
            delay = 200,
            multiplier = 2,
            maxDelay = 2_000)
    void send(PostPublishedEvent event) {
        String subject = "New Post Published: " + event.title();
        String content = """
                New Post Published: <a href="%s">%s</a>
                %s
                """.formatted(event.slug(), event.title(), event.content());
        emailService.send(event.eventId(), properties.supportEmail(), subject, content);
        deliveryService.markProcessed(event);
    }
}
