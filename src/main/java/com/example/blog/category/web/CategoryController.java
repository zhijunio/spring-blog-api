package com.example.blog.category.web;

import com.example.blog.category.domain.CategoryService;
import com.example.blog.category.domain.model.CategoryDto;
import com.example.blog.category.domain.model.CreateCategoryCmd;
import com.example.blog.category.domain.model.UpdateCategoryCmd;
import com.example.blog.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/api")
class CategoryController {
    private static final Logger LOG = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryService categoryService;

    CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Returns all categories.
     *
     * @return the available categories
     */
    @GetMapping("/categories")
    List<CategoryDto> findCategories() {
        LOG.info("Get all category");
        return categoryService.findAllCategories();
    }

    /**
     * Returns a category identified by its slug.
     *
     * @return the requested category
     */
    @GetMapping("/categories/{slug}")
    ResponseEntity<CategoryDto> getCategoryBySlug(@PathVariable String slug) {
        LOG.info("Get category by slug='{}'", slug);
        var category = categoryService
                .findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category with slug '" + slug + "' not found"));
        return ResponseEntity.ok(category);
    }

    /**
     * Creates a category.
     *
     * @return the created category
     */
    @PostMapping(value = "/categories", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CreateCategoryRequest payload) {
        LOG.info("Creating a new category with slug: '{}'", payload.slug());
        var cmd = new CreateCategoryCmd(payload.name(), payload.slug());
        var category = categoryService.createCategory(cmd);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath(null)
                .path("/api/categories/{slug}")
                .buildAndExpand(category.slug())
                .toUri();
        return ResponseEntity.created(location).body(category);
    }

    /**
     * Updates a category.
     *
     * @return the updated category
     */
    @PutMapping(value = "/categories/{slug}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CategoryDto> updateCategory(
            @PathVariable String slug, @Valid @RequestBody UpdateCategoryRequest payload) {
        LOG.info("Updating category with slug: '{}'", slug);
        var cmd = new UpdateCategoryCmd(slug, payload.name(), payload.slug());
        var category = categoryService.updateCategory(cmd);
        return ResponseEntity.status(HttpStatus.OK).body(category);
    }

    /**
     * Deletes a category.
     */
    @DeleteMapping("/categories/{slug}")
    ResponseEntity<Void> deleteCategory(@PathVariable String slug) {
        LOG.info("Deleting category with slug: '{}'", slug);
        categoryService.deleteCategory(slug);
        return ResponseEntity.noContent().build();
    }

    record CreateCategoryRequest(
            @NotBlank(message = "{validation.name.required}") String name,

            @NotBlank(message = "{validation.slug.required}") String slug) {}

    record UpdateCategoryRequest(
            @NotBlank(message = "{validation.name.required}") String name,

            @NotBlank(message = "{validation.slug.required}") String slug) {}
}
