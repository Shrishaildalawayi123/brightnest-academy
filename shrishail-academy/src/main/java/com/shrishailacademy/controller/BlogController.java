package com.shrishailacademy.controller;

import com.shrishailacademy.model.BlogPost;
import com.shrishailacademy.repository.BlogPostRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
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

    @Autowired
    private BlogPostRepository blogPostRepository;

    // ========== PUBLIC ENDPOINTS ==========

    /**
     * GET /api/blog - List published blog posts, optionally filtered by category
     */
    @GetMapping
    public ResponseEntity<?> getPublishedPosts(@RequestParam(required = false) String category) {
        List<BlogPost> posts;
        if (category != null && !category.isEmpty()) {
            try {
                BlogPost.Category cat = BlogPost.Category.valueOf(category.toUpperCase());
                posts = blogPostRepository.findByPublishedTrueAndCategoryOrderByPublishedAtDesc(cat);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid category: " + category));
            }
        } else {
            posts = blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc();
        }
        return ResponseEntity.ok(posts);
    }

    /**
     * GET /api/blog/categories - List available categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        List<Map<String, String>> categories = Arrays.stream(BlogPost.Category.values())
                .map(c -> Map.of(
                        "value", c.name(),
                        "label", formatCategoryLabel(c.name())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    /**
     * GET /api/blog/{slug} - Read a single published blog post by slug
     */
    @GetMapping("/{slug}")
    public ResponseEntity<?> getPostBySlug(@PathVariable String slug) {
        return blogPostRepository.findBySlugAndPublishedTrue(slug)
                .map(post -> ResponseEntity.ok((Object) post))
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== ADMIN ENDPOINTS ==========

    /**
     * GET /api/blog/all - List all posts (including drafts) for admin
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPosts() {
        return ResponseEntity.ok(blogPostRepository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * POST /api/blog - Create a new blog post
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPost(@Valid @RequestBody BlogPost post) {
        // Auto-generate slug from title if not provided
        if (post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(generateSlug(post.getTitle()));
        }
        // If publishing now, set publishedAt
        if (post.isPublished() && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        BlogPost saved = blogPostRepository.save(post);
        return ResponseEntity.ok(Map.of("message", "Blog post created successfully", "post", saved));
    }

    /**
     * PUT /api/blog/{id} - Update a blog post
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @Valid @RequestBody BlogPost updated) {
        return blogPostRepository.findById(id)
                .map(post -> {
                    post.setTitle(updated.getTitle());
                    post.setSlug(updated.getSlug() != null ? updated.getSlug() : generateSlug(updated.getTitle()));
                    post.setExcerpt(updated.getExcerpt());
                    post.setContent(updated.getContent());
                    post.setCategory(updated.getCategory());
                    post.setCoverImageUrl(updated.getCoverImageUrl());
                    post.setAuthor(updated.getAuthor());

                    // Handle publish state change
                    boolean wasPublished = post.isPublished();
                    post.setPublished(updated.isPublished());
                    if (!wasPublished && updated.isPublished()) {
                        post.setPublishedAt(LocalDateTime.now());
                    }

                    BlogPost saved = blogPostRepository.save(post);
                    return ResponseEntity.ok(Map.of("message", "Blog post updated", "post", saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/blog/{id}/publish - Toggle publish status
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> togglePublish(@PathVariable Long id) {
        return blogPostRepository.findById(id)
                .map(post -> {
                    post.setPublished(!post.isPublished());
                    if (post.isPublished() && post.getPublishedAt() == null) {
                        post.setPublishedAt(LocalDateTime.now());
                    }
                    blogPostRepository.save(post);
                    String action = post.isPublished() ? "published" : "unpublished";
                    return ResponseEntity.ok(Map.of("message", "Post " + action + " successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/blog/{id} - Delete a blog post
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        if (!blogPostRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        blogPostRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Blog post deleted successfully"));
    }

    // ========== HELPERS ==========

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String formatCategoryLabel(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
