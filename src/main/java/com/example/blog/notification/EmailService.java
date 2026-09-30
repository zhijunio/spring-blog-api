package com.example.blog.notification;

import java.util.List;
import java.util.UUID;

public interface EmailService {

    void send(String to, String subject, String content);

    void send(List<String> to, String subject, String content);

    default void send(UUID idempotencyKey, String to, String subject, String content) {
        send(to, subject, content);
    }

    default void send(UUID idempotencyKey, List<String> to, String subject, String content) {
        send(to, subject, content);
    }
}
