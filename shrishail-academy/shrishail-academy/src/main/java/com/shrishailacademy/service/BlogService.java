package com.shrishailacademy.service;

import com.shrishailacademy.dto.BlogPostCreateRequest;
import com.shrishailacademy.dto.BlogPostUpdateRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.BlogPost;
import com.shrishailacademy.repository.BlogPostRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogService.class);

    private final BlogPostRepository blogPostRepository;
    private final TenantService tenantService;

    public BlogService(BlogPostRepository blogPostRepository, TenantService tenantService) {
        this.blogPostRepository = blogPostRepository;
        this.tenantService = tenantService;
    }

    // ========== PUBLIC METHODS ==========

    public Page<BlogPost> getPublishedPosts(String category, Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        if (category != null && !category.isEmpty()) {
            try {
                BlogPost.Category cat = BlogPost.Category.valueOf(category.toUpperCase());
                return blogPostRepository.findByTenantIdAndPublishedTrueAndCategory(tenantId, cat, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid category: " + category, "INVALID_CATEGORY");
            }
        }
        return blogPostRepository.findByTenantIdAndPublishedTrue(tenantId, pageable);
    }

    public BlogPost getPublishedPostBySlug(String slug) {
        Long tenantId = TenantContext.requireTenantId();
        return blogPostRepository.findByTenantIdAndSlugAndPublishedTrue(tenantId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "slug", slug));
    }

    public List<Map<String, String>> getCategories() {
        return Arrays.stream(BlogPost.Category.values())
                .map(c -> Map.of(
                        "value", c.name(),
                        "label", formatCategoryLabel(c.name())))
                .collect(Collectors.toList());
    }

    // ========== ADMIN METHODS ==========

    public Page<BlogPost> getAllPosts(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        return blogPostRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional
    public BlogPost createPost(BlogPostCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        BlogPost post = new BlogPost();
        post.setTenant(tenantService.requireCurrentTenant());
        post.setTitle(InputSanitizer.sanitizeAndTruncate(request.getTitle(), 200));

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? generateSlug(InputSanitizer.sanitize(request.getSlug()))
                : generateSlug(request.getTitle());
        post.setSlug(slug);

        validateSlugUniqueness(tenantId, slug, null);

        post.setExcerpt(InputSanitizer.sanitizeAndTruncateNullable(request.getExcerpt(), 500));
        post.setContent(InputSanitizer.sanitizeNullable(request.getContent()));
        post.setCategory(request.getCategory() != null ? request.getCategory() : BlogPost.Category.ACADEMY_NEWS);
        post.setCoverImageUrl(InputSanitizer.sanitizeAndTruncateNullable(request.getCoverImageUrl(), 500));
        post.setAuthor(InputSanitizer.sanitizeAndTruncateNullable(request.getAuthor(), 100));
        post.setPublished(request.isPublished());

        if (post.isPublished()) {
            post.setPublishedAt(LocalDateTime.now());
        }

        BlogPost saved = blogPostRepository.save(post);
        log.info("BLOG_POST_CREATED: id={}, title='{}', tenantId={}", saved.getId(), saved.getTitle(), tenantId);
        return saved;
    }

    @Transactional
    public BlogPost updatePost(Long id, BlogPostUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        BlogPost post = blogPostRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", id));

        if (request.getTitle() != null) {
            post.setTitle(InputSanitizer.sanitizeAndTruncate(request.getTitle(), 200));
        }

        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String newSlug = generateSlug(InputSanitizer.sanitize(request.getSlug()));
            validateSlugUniqueness(tenantId, newSlug, id);
            post.setSlug(newSlug);
        } else if (request.getTitle() != null) {
            String newSlug = generateSlug(request.getTitle());
            validateSlugUniqueness(tenantId, newSlug, id);
            post.setSlug(newSlug);
        }

        if (request.getExcerpt() != null) {
            post.setExcerpt(InputSanitizer.sanitizeAndTruncateNullable(request.getExcerpt(), 500));
        }
        if (request.getContent() != null) {
            post.setContent(InputSanitizer.sanitizeNullable(request.getContent()));
        }
        if (request.getCategory() != null) {
            post.setCategory(request.getCategory());
        }
        if (request.getCoverImageUrl() != null) {
            post.setCoverImageUrl(InputSanitizer.sanitizeAndTruncateNullable(request.getCoverImageUrl(), 500));
        }
        if (request.getAuthor() != null) {
            post.setAuthor(InputSanitizer.sanitizeAndTruncateNullable(request.getAuthor(), 100));
        }

        if (request.getPublished() != null) {
            boolean wasPublished = post.isPublished();
            post.setPublished(request.getPublished());
            if (!wasPublished && request.getPublished()) {
                post.setPublishedAt(LocalDateTime.now());
            }
        }

        BlogPost saved = blogPostRepository.save(post);
        log.info("BLOG_POST_UPDATED: id={}, tenantId={}", id, tenantId);
        return saved;
    }

    @Transactional
    public void togglePublish(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        BlogPost post = blogPostRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", id));

        post.setPublished(!post.isPublished());
        if (post.isPublished() && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        blogPostRepository.save(post);
        log.info("BLOG_POST_TOGGLED: id={}, published={}, tenantId={}", id, post.isPublished(), tenantId);
    }

    @Transactional
    public void deletePost(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        if (blogPostRepository.findByIdAndTenantId(id, tenantId).isEmpty()) {
            throw new ResourceNotFoundException("BlogPost", "id", id);
        }
        blogPostRepository.deleteByIdAndTenantId(id, tenantId);
        log.info("BLOG_POST_DELETED: id={}, tenantId={}", id, tenantId);
    }

    // ========== HELPERS ==========

    private void validateSlugUniqueness(Long tenantId, String slug, Long excludeId) {
        boolean exists = (excludeId != null)
                ? blogPostRepository.existsByTenantIdAndSlugAndIdNot(tenantId, slug, excludeId)
                : blogPostRepository.existsByTenantIdAndSlug(tenantId, slug);
        if (exists) {
            throw new DuplicateResourceException("BlogPost", "slug", slug);
        }
    }

    private String generateSlug(String title) {
        if (title == null) {
            return null;
        }
        String slug = title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return slug.length() > 220 ? slug.substring(0, 220) : slug;
    }

    private String formatCategoryLabel(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
