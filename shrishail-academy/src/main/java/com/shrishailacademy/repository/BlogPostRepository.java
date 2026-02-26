package com.shrishailacademy.repository;

import com.shrishailacademy.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    List<BlogPost> findByPublishedTrueOrderByPublishedAtDesc();

    List<BlogPost> findByPublishedTrueAndCategoryOrderByPublishedAtDesc(BlogPost.Category category);

    Optional<BlogPost> findBySlug(String slug);

    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);

    List<BlogPost> findAllByOrderByCreatedAtDesc();

    long countByPublished(boolean published);
}
