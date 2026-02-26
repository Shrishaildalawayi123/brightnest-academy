package com.shrishailacademy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * BlogPost - CMS blog articles for the academy website
 */
@Entity
@Table(name = "blog_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(max = 220)
    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Size(max = 500)
    @Column(length = 500)
    private String excerpt;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Category category = Category.ACADEMY_NEWS;

    @Size(max = 500)
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Size(max = 100)
    @Column(length = 100)
    private String author;

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Category {
        LEARNING_TIPS,
        EXAM_STRATEGIES,
        LANGUAGE_INSIGHTS,
        ACADEMY_NEWS
    }
}
