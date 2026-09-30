package com.example.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record ApplicationProperties(
        @NotBlank String supportEmail,
        @NotBlank String emailServiceType,
        @NotBlank String newsletterJobCron,
        @NotBlank @DefaultValue("Asia/Shanghai") String timeZone,
        Kafka kafka,
        @Positive @DefaultValue("100") int newsletterBatchSize,
        @DefaultValue("http://localhost:8080") String publicBaseUrl,
        @Positive @DefaultValue("10") int pageSize) {

    public record Kafka(
            @NotBlank @DefaultValue("blog.post-published") String postPublishedTopic) {}
}
