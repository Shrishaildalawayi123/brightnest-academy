package com.shrishailacademy.dto;

import com.shrishailacademy.model.BlogPost;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostUpdateRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 220, message = "Slug must not exceed 220 characters")
    private String slug;

    @Size(max = 500, message = "Excerpt must not exceed 500 characters")
    private String excerpt;

    private String content;

    private BlogPost.Category category;

    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    private String coverImageUrl;

    @Size(max = 100, message = "Author must not exceed 100 characters")
    private String author;

    private Boolean published;
}
