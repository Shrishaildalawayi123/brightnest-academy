package com.shrishailacademy.dto.response;

import com.shrishailacademy.model.BlogPost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for BlogPost responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostResponse {

    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String category;
    private String coverImageUrl;
    private String author;
    private boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BlogPostResponse fromEntity(BlogPost post) {
        if (post == null)
            return null;
        return BlogPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .category(post.getCategory() != null ? post.getCategory().name() : null)
                .coverImageUrl(post.getCoverImageUrl())
                .author(post.getAuthor())
                .published(post.isPublished())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
