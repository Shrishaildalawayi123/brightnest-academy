package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "blog_posts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_blog_tenant_slug", columnNames = { "tenant_id", "slug" })
}, indexes = {
        @Index(name = "idx_blog_published_published_at", columnList = "published,published_at"),
        @Index(name = "idx_blog_category_published", columnList = "category,published"),
        @Index(name = "idx_blog_tenant", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPost extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private Tenant tenant;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(max = 220)
    @Column(nullable = false, length = 220)
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

    public enum Category {
        LEARNING_TIPS,
        EXAM_STRATEGIES,
        LANGUAGE_INSIGHTS,
        ACADEMY_NEWS
    }
}
