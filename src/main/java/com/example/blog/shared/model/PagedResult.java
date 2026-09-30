package com.example.blog.shared.model;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PagedResult<T>(
        List<T> data, int current, int totalPages, long totalElements, boolean hasNextPage, boolean hasPrevPage) {

    public static <T> PagedResult<T> from(Page<T> page) {
        return new PagedResult<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext(),
                page.hasPrevious());
    }

    public <R> PagedResult<R> map(Function<T, R> converter) {
        return new PagedResult<>(
                this.data.stream().map(converter).toList(),
                this.current,
                this.totalPages,
                this.totalElements,
                this.hasNextPage,
                this.hasPrevPage);
    }
}
