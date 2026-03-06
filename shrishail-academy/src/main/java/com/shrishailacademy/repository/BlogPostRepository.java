package com.shrishailacademy.repository;

import com.shrishailacademy.model.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    // ========== TENANT-AWARE QUERIES ==========

    Page<BlogPost> findByTenantIdAndPublishedTrue(Long tenantId, Pageable pageable);

    Page<BlogPost> findByTenantIdAndPublishedTrueAndCategory(Long tenantId, BlogPost.Category category,
            Pageable pageable);

    Optional<BlogPost> findByTenantIdAndSlugAndPublishedTrue(Long tenantId, String slug);

    Page<BlogPost> findByTenantId(Long tenantId, Pageable pageable);

    Optional<BlogPost> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndSlug(Long tenantId, String slug);

    boolean existsByTenantIdAndSlugAndIdNot(Long tenantId, String slug, Long id);

    void deleteByIdAndTenantId(Long id, Long tenantId);

    long countByTenantIdAndPublished(Long tenantId, boolean published);

    // ========== LEGACY (non-tenant) — retained for backward compatibility
    // ==========

    List<BlogPost> findByPublishedTrueOrderByPublishedAtDesc();

    Page<BlogPost> findByPublishedTrue(Pageable pageable);

    List<BlogPost> findByPublishedTrueAndCategoryOrderByPublishedAtDesc(BlogPost.Category category);

    Page<BlogPost> findByPublishedTrueAndCategory(BlogPost.Category category, Pageable pageable);

    Optional<BlogPost> findBySlug(String slug);

    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);

    List<BlogPost> findAllByOrderByCreatedAtDesc();

    long countByPublished(boolean published);
}
