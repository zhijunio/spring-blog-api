package com.example.blog.category;

import com.example.blog.category.domain.model.CategoryDto;
import java.util.Optional;

public interface CategoryAPI {
    Optional<CategoryDto> findBySlug(String slug);
}
