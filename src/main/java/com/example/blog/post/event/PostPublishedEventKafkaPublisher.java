package com.example.blog.post.event;

import com.example.blog.ApplicationProperties;
import com.example.blog.post.domain.model.PostPublishedEvent;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
class PostPublishedEventKafkaPublisher {
    static final int MAX_RETRIES = 2;
    static final int MAX_ATTEMPTS = MAX_RETRIES + 1;
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, PostPublishedEvent> kafkaTemplate;
    private final ApplicationProperties properties;

    PostPublishedEventKafkaPublisher(
            KafkaTemplate<String, PostPublishedEvent> kafkaTemplate, ApplicationProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Retryable(
            includes = RuntimeException.class,
            maxRetries = MAX_RETRIES,
            delay = 200,
            multiplier = 2,
            maxDelay = 2_000)
    void publish(PostPublishedEvent event) {
        try {
            kafkaTemplate
                    .send(
                            properties.kafka().postPublishedTopic(),
                            event.eventId().toString(),
                            event)
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish interrupted", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            throw new IllegalStateException("Kafka publish failed", failure);
        }
    }
}
