package com.shrishailacademy.service;

import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.BlogPost;
import com.shrishailacademy.repository.BlogPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogService.class);

    private final BlogPostRepository blogPostRepository;

    public BlogService(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    // ========== PUBLIC METHODS ==========

    public List<BlogPost> getPublishedPosts(String category) {
        if (category != null && !category.isEmpty()) {
            try {
                BlogPost.Category cat = BlogPost.Category.valueOf(category.toUpperCase());
                return blogPostRepository.findByPublishedTrueAndCategoryOrderByPublishedAtDesc(cat);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid category: " + category, "INVALID_CATEGORY");
            }
        }
        return blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc();
    }

    public Page<BlogPost> getPublishedPosts(String category, Pageable pageable) {
        if (category != null && !category.isEmpty()) {
            try {
                BlogPost.Category cat = BlogPost.Category.valueOf(category.toUpperCase());
                return blogPostRepository.findByPublishedTrueAndCategory(cat, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid category: " + category, "INVALID_CATEGORY");
            }
        }
        return blogPostRepository.findByPublishedTrue(pageable);
    }

    public BlogPost getPublishedPostBySlug(String slug) {
        return blogPostRepository.findBySlugAndPublishedTrue(slug)
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

    public List<BlogPost> getAllPosts() {
        return blogPostRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<BlogPost> getAllPosts(Pageable pageable) {
        return blogPostRepository.findAll(pageable);
    }

    @Transactional
    public BlogPost createPost(BlogPost post) {
        if (post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(generateSlug(post.getTitle()));
        }
        if (post.isPublished() && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        BlogPost saved = blogPostRepository.save(post);
        log.info("BLOG_POST_CREATED: id={}, title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public BlogPost updatePost(Long id, BlogPost updated) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", id));

        post.setTitle(updated.getTitle());
        post.setSlug(updated.getSlug() != null ? updated.getSlug() : generateSlug(updated.getTitle()));
        post.setExcerpt(updated.getExcerpt());
        post.setContent(updated.getContent());
        post.setCategory(updated.getCategory());
        post.setCoverImageUrl(updated.getCoverImageUrl());
        post.setAuthor(updated.getAuthor());

        boolean wasPublished = post.isPublished();
        post.setPublished(updated.isPublished());
        if (!wasPublished && updated.isPublished()) {
            post.setPublishedAt(LocalDateTime.now());
        }

        BlogPost saved = blogPostRepository.save(post);
        log.info("BLOG_POST_UPDATED: id={}", id);
        return saved;
    }

    @Transactional
    public void togglePublish(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BlogPost", "id", id));

        post.setPublished(!post.isPublished());
        if (post.isPublished() && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        blogPostRepository.save(post);
        log.info("BLOG_POST_TOGGLED: id={}, published={}", id, post.isPublished());
    }

    @Transactional
    public void deletePost(Long id) {
        if (!blogPostRepository.existsById(id)) {
            throw new ResourceNotFoundException("BlogPost", "id", id);
        }
        blogPostRepository.deleteById(id);
        log.info("BLOG_POST_DELETED: id={}", id);
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
