package com.example.blog.post.event;

import com.example.blog.post.domain.PostPublishedEventDeliveryService;
import com.example.blog.post.domain.model.PostPublishedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class PostPublishedEventKafkaListener {
    private static final Logger LOG = LoggerFactory.getLogger(PostPublishedEventKafkaListener.class);
    private final PostPublishedEventNotificationSender notificationSender;
    private final PostPublishedEventDeliveryService deliveryService;
    private final Counter failureCounter;

    PostPublishedEventKafkaListener(
            PostPublishedEventNotificationSender notificationSender,
            PostPublishedEventDeliveryService deliveryService,
            MeterRegistry meterRegistry) {
        this.notificationSender = notificationSender;
        this.deliveryService = deliveryService;
        this.failureCounter = Counter.builder("blog.events.failed")
                .description("Post published events moved to the dead-letter store")
                .tag("event", "post-published")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "${app.kafka.post-published-topic}", groupId = "${spring.kafka.consumer.group-id}")
    void handle(PostPublishedEvent event) {
        if (!deliveryService.tryStartProcessing(event)) {
            return;
        }
        try {
            notificationSender.send(event);
        } catch (RuntimeException failure) {
            deliveryService.moveToDeadLetter(event, PostPublishedEventNotificationSender.MAX_ATTEMPTS, failure);
            failureCounter.increment();
            LOG.error("PostPublishedEvent moved to dead letter: eventId={}", event.eventId(), failure);
        }
    }
}
