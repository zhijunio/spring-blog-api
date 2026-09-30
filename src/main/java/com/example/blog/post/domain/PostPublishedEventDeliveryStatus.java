package com.example.blog.post.domain;

enum PostPublishedEventDeliveryStatus {
    PROCESSING,
    PROCESSED,
    DEAD_LETTER
}
