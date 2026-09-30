package com.example.blog.post.domain;

import com.example.blog.post.domain.model.PostPublishedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_published_event_delivery")
class PostPublishedEventDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(nullable = false, updatable = false)
    private String slug;

    @Column(nullable = false, columnDefinition = "text", updatable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostPublishedEventDeliveryStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "dead_lettered_at")
    private Instant deadLetteredAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_at")
    private Instant processingAt;

    protected PostPublishedEventDelivery() {}

    private PostPublishedEventDelivery(PostPublishedEvent event) {
        this.eventId = event.eventId();
        this.title = event.title();
        this.slug = event.slug();
        this.content = event.content();
        this.attempts = 0;
    }

    static PostPublishedEventDelivery forEvent(PostPublishedEvent event) {
        return new PostPublishedEventDelivery(event);
    }

    boolean isProcessed() {
        return status == PostPublishedEventDeliveryStatus.PROCESSED;
    }

    boolean isDeadLettered() {
        return status == PostPublishedEventDeliveryStatus.DEAD_LETTER;
    }

    boolean isProcessing() {
        return status == PostPublishedEventDeliveryStatus.PROCESSING;
    }

    boolean isProcessingExpired(Instant now, Duration lease) {
        return processingAt == null || processingAt.plus(lease).isBefore(now);
    }

    void markProcessing(Instant now) {
        this.status = PostPublishedEventDeliveryStatus.PROCESSING;
        this.attempts++;
        this.lastError = null;
        this.processingAt = now;
    }

    void markProcessed(Instant now) {
        this.status = PostPublishedEventDeliveryStatus.PROCESSED;
        this.lastError = null;
        this.processingAt = null;
        this.processedAt = now;
    }

    void markDeadLetter(int attempts, Throwable failure, Instant now) {
        this.status = PostPublishedEventDeliveryStatus.DEAD_LETTER;
        this.attempts = attempts;
        this.lastError = failure.getMessage();
        this.processingAt = null;
        this.deadLetteredAt = now;
    }
}
