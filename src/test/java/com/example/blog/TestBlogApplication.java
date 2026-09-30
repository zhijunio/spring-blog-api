package com.example.blog;

import org.springframework.boot.SpringApplication;

public class TestBlogApplication {

    static void main(String[] args) {
        System.setProperty("spring.docker.compose.enabled", "false");
        SpringApplication.from(BlogApplication::main)
                .with(TestcontainersConfig.class)
                .run(args);
    }
}
