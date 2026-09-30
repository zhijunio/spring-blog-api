package com.example.blog.post;

import com.example.blog.post.domain.model.PostDto;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PostAPI {
    List<PostDto> findPostsCreatedBetween(Instant start, Instant end, Pageable pageable);
}
