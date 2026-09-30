package com.example.blog.post.web;

import com.example.blog.post.domain.PostService;
import com.example.blog.post.domain.model.*;
import com.example.blog.shared.exception.ResourceNotFoundException;
import com.example.blog.shared.model.PagedResult;
import com.example.blog.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/api")
class PostController {
    private static final Logger LOG = LoggerFactory.getLogger(PostController.class);
    private final PostService postService;

    PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Returns a paginated list of posts, optionally filtered by a search query.
     *
     * @return the requested page of posts
     */
    @GetMapping("/posts")
    PagedResult<PostDto> findPosts(
            @RequestParam(value = "query", defaultValue = "") String query,
            @RequestParam(value = "page", defaultValue = "1") int page) {
        LOG.info("Get post by page='{}' and query='{}'", page, query);
        if (query == null || query.trim().isEmpty()) {
            return postService.findPosts(page);
        }
        return postService.searchPosts(query, page);
    }

    /**
     * Returns a post identified by its slug.
     *
     * @return the requested post
     */
    @GetMapping("/posts/{slug}")
    ResponseEntity<PostDto> getPostBySlug(@PathVariable String slug) {
        LOG.info("Get post by slug='{}'", slug);
        var post = postService
                .findPostBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post with slug '" + slug + "' not found"));
        return ResponseEntity.ok(post);
    }

    /**
     * Creates a post.
     *
     * @return an empty response with the location of the created post
     */
    @PostMapping(value = "/posts", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> createPost(@Valid @RequestBody PostRequest postRequest) {
        var slug = postRequest.slug();
        LOG.info("Creating a new post with slug: '{}'", slug);
        var cmd = new CreatePostCmd(postRequest.title(), slug, postRequest.content(), postRequest.categorySlug());
        this.postService.createPost(cmd);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath(null)
                .path("/api/posts/{slug}")
                .buildAndExpand(slug)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * Updates a post owned by the authenticated user.
     *
     * @return an empty response with the location of the updated post
     */
    @PutMapping(value = "/posts/{slug}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> updatePost(@PathVariable String slug, @Valid @RequestBody PostRequest postRequest) {
        LOG.info("Updating post with slug: '{}'", slug);
        var loginUserId = SecurityUtils.getCurrentUserIdOrThrow();

        var cmd = new UpdatePostCmd(
                slug,
                postRequest.title(),
                postRequest.slug(),
                postRequest.content(),
                postRequest.categorySlug(),
                loginUserId);
        this.postService.updatePost(cmd);

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath(null)
                .path("/api/posts/{slug}")
                .buildAndExpand(postRequest.slug())
                .toUri();
        return ResponseEntity.status(HttpStatus.OK).location(location).build();
    }

    /**
     * Returns the comments belonging to a post.
     *
     * @return the requested page of comments
     */
    @GetMapping("/posts/{slug}/comments")
    PagedResult<CommentDto> getPostComments(
            @PathVariable String slug, @RequestParam(value = "page", defaultValue = "1") int page) {
        LOG.info("Get post comments by slug='{}'", slug);
        PostDto postDto = postService
                .findPostBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post with slug '" + slug + "' not found"));
        return postService.getCommentsByPostId(postDto.id(), page);
    }

    /**
     * Adds a comment to a post.
     *
     * @return an empty response with the location of the created comment
     */
    @PostMapping(value = "/posts/{slug}/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> createComment(@PathVariable String slug, @Valid @RequestBody CreateCommentRequest payload) {
        LOG.info("Create comment for post with slug: '{}'", slug);
        PostDto postDto = postService
                .findPostBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post with slug '" + slug + "' not found"));
        var createdCommentCmd = new CreateCommentCmd(payload.name(), payload.email(), payload.content(), postDto.id());
        CommentDto comment = postService.createComment(createdCommentCmd);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath(null)
                .path("/api/comments/{commentId}")
                .buildAndExpand(comment.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * Returns a comment by its identifier.
     *
     * @return the requested comment
     */
    @GetMapping("/comments/{commentId}")
    ResponseEntity<CommentDto> getCommentById(@PathVariable Long commentId) {
        LOG.info("Get comment by commentId={}", commentId);
        CommentDto commentDto = postService
                .getCommentById(commentId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Comment with commentId '" + commentId + "' not found"));
        return ResponseEntity.ok(commentDto);
    }

    record CreateCommentRequest(
            @NotBlank(message = "{validation.name.required}") String name,

            @NotBlank(message = "{validation.email.required}") @Email(message = "{validation.email.invalid}") String email,

            @NotBlank(message = "{validation.content.required}") String content) {}

    record PostRequest(
            @NotBlank(message = "{validation.title.required}") String title,

            @NotBlank(message = "{validation.slug.required}") String slug,

            @NotBlank(message = "{validation.content.required}") String content,

            String categorySlug) {}
}
