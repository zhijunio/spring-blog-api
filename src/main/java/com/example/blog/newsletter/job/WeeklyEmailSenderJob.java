package com.example.blog.newsletter.job;

import com.example.blog.ApplicationProperties;
import com.example.blog.notification.EmailService;
import com.example.blog.post.PostAPI;
import com.example.blog.post.domain.model.PostDto;
import com.example.blog.user.UserAPI;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class WeeklyEmailSenderJob {
    private static final Logger LOG = LoggerFactory.getLogger(WeeklyEmailSenderJob.class);
    private final PostAPI postAPI;
    private final UserAPI userAPI;
    private final EmailService emailService;
    private final ApplicationProperties properties;
    private final Clock clock;
    private final Counter sentCounter;
    private final Counter skippedCounter;
    private final Counter failedCounter;
    private final Timer durationTimer;

    WeeklyEmailSenderJob(
            PostAPI postAPI,
            UserAPI userAPI,
            EmailService emailService,
            ApplicationProperties properties,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.postAPI = postAPI;
        this.userAPI = userAPI;
        this.emailService = emailService;
        this.properties = properties;
        this.clock = clock;
        this.sentCounter = Counter.builder("blog.newsletter.sent")
                .description("Weekly newsletters sent")
                .register(meterRegistry);
        this.skippedCounter = Counter.builder("blog.newsletter.skipped")
                .description("Weekly newsletters skipped because there was no content or recipient")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("blog.newsletter.failed")
                .description("Weekly newsletter executions that failed")
                .register(meterRegistry);
        this.durationTimer = Timer.builder("blog.newsletter.duration")
                .description("Weekly newsletter execution duration")
                .register(meterRegistry);
    }

    @Scheduled(cron = "${app.newsletter-job-cron}", zone = "${app.time-zone}")
    @SchedulerLock(name = "weeklyEmailSenderJob", lockAtMostFor = "2h", lockAtLeastFor = "30s")
    void sendNewsLetter() {
        Timer.Sample sample = Timer.start();
        try {
            LOG.info("Sending newsletter at {}", clock.instant());
            Instant end = clock.instant();
            Instant startOfWeek = LocalDate.now(clock)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay(clock.getZone())
                    .toInstant();
            String newsLetterContent = createNewsLetterContent(startOfWeek, end);
            if (newsLetterContent.isEmpty()) {
                skippedCounter.increment();
                LOG.info("No post found for this week. Skipping newsletter");
                return;
            }

            int recipients = sendToUsers(newsLetterContent);
            if (recipients == 0) {
                skippedCounter.increment();
                LOG.info("No user found for this week. Skipping newsletter");
                return;
            }
            sentCounter.increment();
            LOG.info("Sent newsletter at {} to {} users", clock.instant(), recipients);
        } catch (RuntimeException failure) {
            failedCounter.increment();
            throw failure;
        } finally {
            sample.stop(durationTimer);
        }
    }

    private String createNewsLetterContent(Instant start, Instant end) {
        StringBuilder emailContent = new StringBuilder();
        int page = 0;
        List<PostDto> posts;
        do {
            var pageable = PageRequest.of(
                    page++,
                    properties.newsletterBatchSize(),
                    Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id")));
            posts = postAPI.findPostsCreatedBetween(start, end, pageable);
            for (PostDto post : posts) {
                String postUrl = properties.publicBaseUrl() + "/api/posts/" + post.slug();
                var fragment = """
                        <h2><a href="%s">%s</a></h2>
                        <p>%s</p>
                        """.formatted(postUrl, post.title(), post.content());
                emailContent.append(fragment);
            }
        } while (posts.size() == properties.newsletterBatchSize());
        return emailContent.toString();
    }

    private int sendToUsers(String content) {
        int page = 0;
        int recipients = 0;
        List<String> userEmails;
        do {
            var pageable = PageRequest.of(page++, properties.newsletterBatchSize());
            userEmails = userAPI.findEmailAddresses(pageable);
            if (!userEmails.isEmpty()) {
                emailService.send(userEmails, "Weekly Newsletter", content);
                recipients += userEmails.size();
            }
        } while (userEmails.size() == properties.newsletterBatchSize());
        return recipients;
    }
}
