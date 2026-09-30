package com.example.blog.config;

import com.example.blog.ApplicationProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
class KafkaConfig {

    @Bean
    NewTopic postPublishedTopic(ApplicationProperties properties) {
        return TopicBuilder.name(properties.kafka().postPublishedTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
