package com.example.blog.post.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostPublishedEventDeliveryRepository extends JpaRepository<PostPublishedEventDelivery, Long> {
    Optional<PostPublishedEventDelivery> findByEventId(UUID eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from PostPublishedEventDelivery d where d.eventId = :eventId")
    Optional<PostPublishedEventDelivery> findByEventIdForUpdate(@Param("eventId") UUID eventId);
}
