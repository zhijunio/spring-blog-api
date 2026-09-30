package com.example.blog.post.domain;

import com.example.blog.post.domain.model.PostDto;
import java.time.Instant;

interface PostSearchProjection {
    Long getId();

    String getTitle();

    String getSlug();

    String getContent();

    String getCategorySlug();

    String getCategoryName();

    Long getAuthorId();

    String getAuthorName();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    default PostDto toPostDto() {
        return new PostDto(
                getId(),
                getTitle(),
                getSlug(),
                getContent(),
                getCategorySlug(),
                getCategoryName(),
                getAuthorId(),
                getAuthorName(),
                getCreatedAt(),
                getUpdatedAt());
    }
}
