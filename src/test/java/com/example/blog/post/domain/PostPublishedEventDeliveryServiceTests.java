package com.example.blog.post.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.blog.post.domain.model.PostPublishedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostPublishedEventDeliveryServiceTests {

    @Mock
    private PostPublishedEventDeliveryRepository repository;

    @Test
    void onlyOneConsumerCanClaimAnEventAtATime() {
        var event = new PostPublishedEvent(UUID.randomUUID(), "Title", "slug", "Content");
        var delivery = PostPublishedEventDelivery.forEvent(event);
        when(repository.findByEventIdForUpdate(event.eventId())).thenReturn(Optional.of(delivery));
        when(repository.saveAndFlush(any(PostPublishedEventDelivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var service = new PostPublishedEventDeliveryService(
                repository, Clock.fixed(Instant.parse("2026-09-30T00:00:00Z"), ZoneOffset.UTC));

        assertThat(service.tryStartProcessing(event)).isTrue();
        assertThat(service.tryStartProcessing(event)).isFalse();
    }
}
