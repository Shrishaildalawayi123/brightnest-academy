package com.shrishailacademy.service;

import com.shrishailacademy.dto.BlogPostCreateRequest;
import com.shrishailacademy.dto.BlogPostUpdateRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.BlogPost;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.BlogPostRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final String TENANT_KEY = "default";

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private BlogService blogService;

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID, TENANT_KEY);
        lenient().when(tenantService.requireCurrentTenant())
                .thenReturn(new Tenant(TENANT_ID, TENANT_KEY, "Default Tenant"));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    // ===== CREATE =====

    @Test
    void createPostShouldSaveWithGeneratedSlug() {
        BlogPostCreateRequest request = new BlogPostCreateRequest();
        request.setTitle("My First Blog Post");
        request.setContent("Some content");
        request.setCategory(BlogPost.Category.ACADEMY_NEWS);
        request.setPublished(false);

        when(blogPostRepository.existsByTenantIdAndSlug(eq(TENANT_ID), anyString())).thenReturn(false);
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> {
            BlogPost p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        BlogPost saved = blogService.createPost(request);

        assertEquals("my-first-blog-post", saved.getSlug());
        assertFalse(saved.isPublished());
        assertNull(saved.getPublishedAt());
        verify(blogPostRepository).save(any(BlogPost.class));
    }

    @Test
    void createPostShouldUseCustomSlugWhenProvided() {
        BlogPostCreateRequest request = new BlogPostCreateRequest();
        request.setTitle("My Post");
        request.setSlug("custom-slug");

        when(blogPostRepository.existsByTenantIdAndSlug(TENANT_ID, "custom-slug")).thenReturn(false);
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> {
            BlogPost p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        BlogPost saved = blogService.createPost(request);

        assertEquals("custom-slug", saved.getSlug());
    }

    @Test
    void createPostShouldThrowWhenSlugIsDuplicate() {
        BlogPostCreateRequest request = new BlogPostCreateRequest();
        request.setTitle("Duplicate Slug");

        when(blogPostRepository.existsByTenantIdAndSlug(eq(TENANT_ID), eq("duplicate-slug"))).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> blogService.createPost(request));
        verify(blogPostRepository, never()).save(any());
    }

    @Test
    void createPostShouldSetPublishedAtWhenPublished() {
        BlogPostCreateRequest request = new BlogPostCreateRequest();
        request.setTitle("Published Post");
        request.setPublished(true);

        when(blogPostRepository.existsByTenantIdAndSlug(eq(TENANT_ID), anyString())).thenReturn(false);
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> {
            BlogPost p = inv.getArgument(0);
            p.setId(3L);
            return p;
        });

        BlogPost saved = blogService.createPost(request);

        assertTrue(saved.isPublished());
        assertNotNull(saved.getPublishedAt());
    }

    @Test
    void createPostShouldSanitizeTitle() {
        BlogPostCreateRequest request = new BlogPostCreateRequest();
        request.setTitle("<script>alert(1)</script> Hello");

        when(blogPostRepository.existsByTenantIdAndSlug(eq(TENANT_ID), anyString())).thenReturn(false);
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> {
            BlogPost p = inv.getArgument(0);
            p.setId(4L);
            return p;
        });

        BlogPost saved = blogService.createPost(request);

        assertFalse(saved.getTitle().contains("<script>"));
    }

    // ===== UPDATE =====

    @Test
    void updatePostShouldPatchOnlyProvidedFields() {
        BlogPost existing = new BlogPost();
        existing.setId(1L);
        existing.setTitle("Old Title");
        existing.setSlug("old-title");
        existing.setExcerpt("Old excerpt");
        existing.setContent("Old content");
        existing.setCategory(BlogPost.Category.ACADEMY_NEWS);
        existing.setPublished(false);

        BlogPostUpdateRequest patch = new BlogPostUpdateRequest();
        patch.setTitle("New Title");

        when(blogPostRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(blogPostRepository.existsByTenantIdAndSlugAndIdNot(eq(TENANT_ID), eq("new-title"), eq(1L)))
                .thenReturn(false);
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost updated = blogService.updatePost(1L, patch);

        assertEquals("New Title", updated.getTitle());
        assertEquals("new-title", updated.getSlug());
        assertEquals("Old excerpt", updated.getExcerpt());
        assertEquals("Old content", updated.getContent());
    }

    @Test
    void updatePostShouldThrowWhenNotFound() {
        when(blogPostRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> blogService.updatePost(99L, new BlogPostUpdateRequest()));
    }

    @Test
    void updatePostShouldSetPublishedAtOnFirstPublish() {
        BlogPost existing = new BlogPost();
        existing.setId(1L);
        existing.setTitle("Draft");
        existing.setSlug("draft");
        existing.setPublished(false);
        existing.setPublishedAt(null);

        BlogPostUpdateRequest patch = new BlogPostUpdateRequest();
        patch.setPublished(true);

        when(blogPostRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        BlogPost updated = blogService.updatePost(1L, patch);

        assertTrue(updated.isPublished());
        assertNotNull(updated.getPublishedAt());
    }

    // ===== TOGGLE PUBLISH =====

    @Test
    void togglePublishShouldFlipState() {
        BlogPost post = new BlogPost();
        post.setId(1L);
        post.setPublished(false);

        when(blogPostRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(post));
        when(blogPostRepository.save(any(BlogPost.class))).thenAnswer(inv -> inv.getArgument(0));

        blogService.togglePublish(1L);

        assertTrue(post.isPublished());
        assertNotNull(post.getPublishedAt());
    }

    @Test
    void togglePublishShouldThrowWhenNotFound() {
        when(blogPostRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> blogService.togglePublish(99L));
    }

    // ===== DELETE =====

    @Test
    void deletePostShouldDeleteExistingPost() {
        BlogPost post = new BlogPost();
        post.setId(1L);

        when(blogPostRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(post));

        blogService.deletePost(1L);

        verify(blogPostRepository).deleteByIdAndTenantId(1L, TENANT_ID);
    }

    @Test
    void deletePostShouldThrowWhenNotFound() {
        when(blogPostRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> blogService.deletePost(99L));
    }

    // ===== READ =====

    @Test
    void getPublishedPostsShouldFilterByTenant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPost> page = new PageImpl<>(List.of(new BlogPost()));

        when(blogPostRepository.findByTenantIdAndPublishedTrue(TENANT_ID, pageable)).thenReturn(page);

        Page<BlogPost> result = blogService.getPublishedPosts(null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(blogPostRepository).findByTenantIdAndPublishedTrue(TENANT_ID, pageable);
    }

    @Test
    void getPublishedPostsShouldFilterByCategory() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPost> page = new PageImpl<>(List.of());

        when(blogPostRepository.findByTenantIdAndPublishedTrueAndCategory(
                eq(TENANT_ID), eq(BlogPost.Category.LEARNING_TIPS), eq(pageable))).thenReturn(page);

        Page<BlogPost> result = blogService.getPublishedPosts("LEARNING_TIPS", pageable);

        assertNotNull(result);
        verify(blogPostRepository).findByTenantIdAndPublishedTrueAndCategory(
                TENANT_ID, BlogPost.Category.LEARNING_TIPS, pageable);
    }

    @Test
    void getPublishedPostsShouldThrowOnInvalidCategory() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(BusinessException.class,
                () -> blogService.getPublishedPosts("INVALID_CAT", pageable));
    }

    @Test
    void getPublishedPostBySlugShouldReturnPost() {
        BlogPost post = new BlogPost();
        post.setSlug("test-slug");

        when(blogPostRepository.findByTenantIdAndSlugAndPublishedTrue(TENANT_ID, "test-slug"))
                .thenReturn(Optional.of(post));

        BlogPost result = blogService.getPublishedPostBySlug("test-slug");

        assertEquals("test-slug", result.getSlug());
    }

    @Test
    void getPublishedPostBySlugShouldThrowWhenNotFound() {
        when(blogPostRepository.findByTenantIdAndSlugAndPublishedTrue(TENANT_ID, "missing"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> blogService.getPublishedPostBySlug("missing"));
    }

    @Test
    void getCategoriesShouldReturnAllCategories() {
        var categories = blogService.getCategories();

        assertEquals(BlogPost.Category.values().length, categories.size());
        assertTrue(categories.stream().anyMatch(c -> c.get("value").equals("ACADEMY_NEWS")));
    }

    @Test
    void getAllPostsShouldFilterByTenant() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPost> page = new PageImpl<>(List.of(new BlogPost(), new BlogPost()));

        when(blogPostRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

        Page<BlogPost> result = blogService.getAllPosts(pageable);

        assertEquals(2, result.getTotalElements());
    }
}
