package com.example.blog.category.domain.model;

public record UpdateCategoryCmd(String slug, String newName, String newSlug) {}
