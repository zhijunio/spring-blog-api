package com.example.blog.newsletter.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.blog.ApplicationProperties;
import com.example.blog.notification.EmailService;
import com.example.blog.post.PostAPI;
import com.example.blog.post.domain.model.PostDto;
import com.example.blog.user.UserAPI;
import com.example.blog.user.domain.model.UserDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class WeeklyEmailSenderJobTests {

    @Test
    void newsletterLinksUseThePublishedPostsApi() {
        CapturingEmailService emailService = new CapturingEmailService();
        PostAPI postAPI = (start, end, pageable) -> pageable.getPageNumber() == 0
                ? List.of(new PostDto(
                        1L,
                        "Title",
                        "post-slug",
                        "Content",
                        "general",
                        "General",
                        1L,
                        "Author",
                        Instant.parse("2026-09-28T00:00:00Z"),
                        null))
                : List.of();
        UserAPI userAPI = new UserAPI() {
            @Override
            public Optional<UserDto> findById(Long id) {
                return Optional.empty();
            }

            @Override
            public Optional<UserDto> findByEmailWithPassword(String email) {
                return Optional.empty();
            }

            @Override
            public Optional<UserDto> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public List<String> findEmailAddresses(Pageable pageable) {
                return pageable.getPageNumber() == 0 ? List.of("reader@example.com") : List.of();
            }
        };
        var job = new WeeklyEmailSenderJob(
                postAPI,
                userAPI,
                emailService,
                new ApplicationProperties(
                        "support@example.com",
                        "console",
                        "0 0 9 * * 6",
                        "Asia/Shanghai",
                        new ApplicationProperties.Kafka("blog.post-published"),
                        100,
                        "http://localhost:8080",
                        5),
                Clock.fixed(Instant.parse("2026-09-30T08:00:00Z"), ZoneId.of("Asia/Shanghai")),
                new SimpleMeterRegistry());

        job.sendNewsLetter();

        assertThat(emailService.content).contains("http://localhost:8080/api/posts/post-slug");
    }

    private static final class CapturingEmailService implements EmailService {
        private String content;

        @Override
        public void send(String to, String subject, String content) {
            this.content = content;
        }

        @Override
        public void send(List<String> to, String subject, String content) {
            this.content = content;
        }
    }
}
