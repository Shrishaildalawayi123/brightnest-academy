package com.shrishailacademy.controller;

import com.shrishailacademy.dto.response.BlogPostResponse;
import com.shrishailacademy.model.BlogPost;
import com.shrishailacademy.service.BlogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Blog CMS Controller
 * Public: list published posts, read single post, get categories
 * Admin: full CRUD, publish/unpublish
 */
@RestController
@RequestMapping("/api/blog")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    // ========== PUBLIC ENDPOINTS ==========

    @GetMapping
    public ResponseEntity<Page<BlogPostResponse>> getPublishedPosts(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "publishedAt") Pageable pageable) {
        Page<BlogPostResponse> posts = blogService.getPublishedPosts(category, pageable)
                .map(BlogPostResponse::fromEntity);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, String>>> getCategories() {
        return ResponseEntity.ok(blogService.getCategories());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BlogPostResponse> getPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(BlogPostResponse.fromEntity(blogService.getPublishedPostBySlug(slug)));
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BlogPostResponse>> getAllPosts(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<BlogPostResponse> posts = blogService.getAllPosts(pageable)
                .map(BlogPostResponse::fromEntity);
        return ResponseEntity.ok(posts);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createPost(@Valid @RequestBody BlogPost post) {
        BlogPost saved = blogService.createPost(post);
        return ResponseEntity.ok(Map.of(
                "message", "Blog post created successfully",
                "post", BlogPostResponse.fromEntity(saved)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePost(@PathVariable Long id, @Valid @RequestBody BlogPost updated) {
        BlogPost saved = blogService.updatePost(id, updated);
        return ResponseEntity.ok(Map.of(
                "message", "Blog post updated",
                "post", BlogPostResponse.fromEntity(saved)));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> togglePublish(@PathVariable Long id) {
        blogService.togglePublish(id);
        return ResponseEntity.ok(Map.of("message", "Post publish status toggled successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deletePost(@PathVariable Long id) {
        blogService.deletePost(id);
        return ResponseEntity.ok(Map.of("message", "Blog post deleted successfully"));
    }
}
