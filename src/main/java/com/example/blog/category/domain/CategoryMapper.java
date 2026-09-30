package com.example.blog.category.domain;

import com.example.blog.category.domain.model.CategoryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface CategoryMapper {
    CategoryDto toCategoryDto(Category category);
}
