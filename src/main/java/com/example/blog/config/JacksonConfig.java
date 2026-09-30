package com.example.blog.config;

import com.example.blog.ApplicationProperties;
import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class JacksonConfig {

    @Bean
    ZoneId applicationZoneId(ApplicationProperties properties) {
        return ZoneId.of(properties.timeZone());
    }

    @Bean
    Clock applicationClock(ZoneId applicationZoneId) {
        return Clock.system(applicationZoneId);
    }

    @Bean
    JsonMapperBuilderCustomizer jsonMapperTimeZoneCustomizer(ZoneId applicationZoneId) {
        return builder -> builder.defaultTimeZone(TimeZone.getTimeZone(applicationZoneId));
    }
}
