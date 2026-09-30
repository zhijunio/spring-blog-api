package com.example.blog.category.domain;

import com.example.blog.category.CategoryAPI;
import com.example.blog.category.domain.model.CategoryDto;
import com.example.blog.category.domain.model.CreateCategoryCmd;
import com.example.blog.category.domain.model.UpdateCategoryCmd;
import com.example.blog.shared.exception.BadRequestException;
import com.example.blog.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService implements CategoryAPI {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Cacheable(cacheNames = "categories")
    public List<CategoryDto> findAllCategories() {
        return categoryRepository.findAllCategories();
    }

    @Cacheable(cacheNames = "categoriesBySlug", key = "#slug", unless = "#result == null")
    public Optional<CategoryDto> findBySlug(String slug) {
        return categoryRepository.findBySlug(slug);
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"categories", "categoriesBySlug"},
            allEntries = true)
    public CategoryDto createCategory(CreateCategoryCmd cmd) {
        var entity = new Category();
        entity.setName(cmd.name());
        entity.setSlug(cmd.slug());
        try {
            categoryRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Category with slug %s already exists".formatted(cmd.slug()), e);
        }
        return categoryMapper.toCategoryDto(entity);
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"categories", "categoriesBySlug"},
            allEntries = true)
    public CategoryDto updateCategory(UpdateCategoryCmd cmd) {
        var entity = categoryRepository
                .findEntityBySlug(cmd.slug())
                .orElseThrow(() -> new ResourceNotFoundException("Category with slug '" + cmd.slug() + "' not found"));

        entity.setName(cmd.newName());
        entity.setSlug(cmd.newSlug());
        try {
            categoryRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Category with slug %s already exists".formatted(cmd.newSlug()), e);
        }
        return categoryMapper.toCategoryDto(entity);
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"categories", "categoriesBySlug"},
            allEntries = true)
    public void deleteCategory(String slug) {
        var entity = categoryRepository
                .findEntityBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category with slug '" + slug + "' not found"));
        try {
            categoryRepository.delete(entity);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Category cannot be deleted because it is associated with one or more post");
        }
    }
}
