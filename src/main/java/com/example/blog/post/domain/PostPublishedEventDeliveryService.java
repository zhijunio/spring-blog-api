package com.example.blog.post.domain;

import com.example.blog.post.domain.model.PostPublishedEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostPublishedEventDeliveryService {
    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(15);
    private final PostPublishedEventDeliveryRepository repository;
    private final Clock clock;

    PostPublishedEventDeliveryService(PostPublishedEventDeliveryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public boolean tryStartProcessing(PostPublishedEvent event) {
        Instant now = clock.instant();
        var delivery = repository
                .findByEventIdForUpdate(event.eventId())
                .orElseGet(() -> PostPublishedEventDelivery.forEvent(event));
        if (delivery.isProcessed() || delivery.isDeadLettered()) {
            return false;
        }
        if (delivery.isProcessing() && !delivery.isProcessingExpired(now, PROCESSING_LEASE)) {
            return false;
        }
        delivery.markProcessing(now);
        repository.saveAndFlush(delivery);
        return true;
    }

    @Transactional
    public void markProcessed(PostPublishedEvent event) {
        var delivery = repository
                .findByEventIdForUpdate(event.eventId())
                .orElseGet(() -> PostPublishedEventDelivery.forEvent(event));
        delivery.markProcessed(clock.instant());
        repository.save(delivery);
    }

    @Transactional
    public void moveToDeadLetter(PostPublishedEvent event, int attempts, Throwable failure) {
        var delivery = repository
                .findByEventIdForUpdate(event.eventId())
                .orElseGet(() -> PostPublishedEventDelivery.forEvent(event));
        if (!delivery.isProcessed()) {
            delivery.markDeadLetter(attempts, failure, clock.instant());
            repository.save(delivery);
        }
    }
}
