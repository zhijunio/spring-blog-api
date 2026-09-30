package com.example.blog.post.domain;

import com.example.blog.ApplicationProperties;
import com.example.blog.category.CategoryAPI;
import com.example.blog.category.domain.model.CategoryDto;
import com.example.blog.post.PostAPI;
import com.example.blog.post.domain.model.*;
import com.example.blog.shared.exception.BadRequestException;
import com.example.blog.shared.exception.ResourceNotFoundException;
import com.example.blog.shared.model.PagedResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService implements PostAPI {
    private static final String DEFAULT_CATEGORY_SLUG = "general";

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CategoryAPI categoryAPI;
    private final PostMapper postMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationProperties properties;

    PostService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            CategoryAPI categoryAPI,
            PostMapper postMapper,
            ApplicationEventPublisher eventPublisher,
            ApplicationProperties properties) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.categoryAPI = categoryAPI;
        this.postMapper = postMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    public PagedResult<PostDto> findPosts(int pageNo) {
        Pageable pageable = this.getPageRequest(pageNo);
        Page<PostDto> posts = postRepository.findPosts(pageable);
        return PagedResult.from(posts);
    }

    public PagedResult<PostDto> searchPosts(String query, int pageNo) {
        Pageable pageable = PageRequest.of(Math.max(pageNo, 1) - 1, properties.pageSize());
        Page<PostDto> posts = postRepository.searchPosts(query.trim(), pageable).map(PostSearchProjection::toPostDto);
        return PagedResult.from(posts);
    }

    @Override
    public List<PostDto> findPostsCreatedBetween(Instant start, Instant end, Pageable pageable) {
        return postRepository.findByCreatedDate(start, end, pageable);
    }

    public Optional<PostDto> findPostBySlug(String slug) {
        return postRepository.findBySlug(slug);
    }

    @Transactional
    public void createPost(CreatePostCmd cmd) {
        var category = getCategoryBySlug(cmd.categorySlug());

        var entity = new Post();
        entity.setTitle(cmd.title());
        entity.setSlug(cmd.slug());
        entity.setContent(cmd.content());
        entity.setCategoryId(category.id());
        try {
            postRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Post with slug %s already exists".formatted(cmd.slug()), e);
        }

        var event = new PostPublishedEvent(UUID.randomUUID(), entity.getTitle(), entity.getSlug(), entity.getContent());
        eventPublisher.publishEvent(event);
    }

    @Transactional
    public void updatePost(UpdatePostCmd cmd) {
        var entity = postRepository
                .findEntityBySlug(cmd.slug())
                .orElseThrow(() -> new ResourceNotFoundException("Post with slug '" + cmd.slug() + "' not found"));

        if (!entity.getCreatedBy().equals(cmd.userId())) {
            throw new AccessDeniedException("Only the post owner can modify the post");
        }

        var category = getCategoryBySlug(cmd.categorySlug());

        entity.setTitle(cmd.newTitle());
        entity.setSlug(cmd.newSlug());
        entity.setContent(cmd.newContent());
        entity.setCategoryId(category.id());
        try {
            postRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Post with slug %s already exists".formatted(cmd.newSlug()), e);
        }
    }

    private CategoryDto getCategoryBySlug(String categorySlug) {
        var slug = (categorySlug == null || categorySlug.isBlank()) ? DEFAULT_CATEGORY_SLUG : categorySlug;
        return categoryAPI
                .findBySlug(slug)
                .orElseThrow(() -> new BadRequestException("Category with slug %s not found".formatted(slug)));
    }

    public PagedResult<CommentDto> getCommentsByPostId(Long postId, int pageNo) {
        Pageable pageable = this.getPageRequest(pageNo);
        var comments = commentRepository.findByPostId(postId, pageable).map(postMapper::toCommentDto);
        return PagedResult.from(comments);
    }

    @Transactional
    public CommentDto createComment(CreateCommentCmd cmd) {
        var post = postRepository.getReferenceById(cmd.postId());
        var entity = new Comment();
        entity.setName(cmd.name());
        entity.setEmail(cmd.email());
        entity.setContent(cmd.content());
        entity.setPost(post);
        commentRepository.save(entity);
        return postMapper.toCommentDto(entity);
    }

    public Optional<CommentDto> getCommentById(Long commentId) {
        return commentRepository.findById(commentId).map(postMapper::toCommentDto);
    }

    private Pageable getPageRequest(int pageNo) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        int pageSize = properties.pageSize();
        if (pageNo < 1) {
            pageNo = 1;
        }
        return PageRequest.of(pageNo - 1, pageSize, sort);
    }
}
