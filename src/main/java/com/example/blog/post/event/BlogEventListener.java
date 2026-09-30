package com.example.blog.post.event;

import com.example.blog.post.domain.PostPublishedEventDeliveryService;
import com.example.blog.post.domain.model.PostPublishedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
class BlogEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(BlogEventListener.class);
    private final PostPublishedEventKafkaPublisher kafkaPublisher;
    private final PostPublishedEventDeliveryService deliveryService;
    private final Counter failureCounter;

    BlogEventListener(
            PostPublishedEventKafkaPublisher kafkaPublisher,
            PostPublishedEventDeliveryService deliveryService,
            MeterRegistry meterRegistry) {
        this.kafkaPublisher = kafkaPublisher;
        this.deliveryService = deliveryService;
        this.failureCounter = Counter.builder("blog.events.failed")
                .description("Post published events moved to the dead-letter store")
                .tag("event", "post-published")
                .register(meterRegistry);
    }

    @Async
    @ApplicationModuleListener
    void handle(PostPublishedEvent event) {
        try {
            kafkaPublisher.publish(event);
        } catch (RuntimeException failure) {
            deliveryService.moveToDeadLetter(event, PostPublishedEventKafkaPublisher.MAX_ATTEMPTS, failure);
            failureCounter.increment();
            LOG.error("PostPublishedEvent moved to dead letter: eventId={}", event.eventId(), failure);
        }
    }
}
