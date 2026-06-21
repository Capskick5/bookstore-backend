package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Author is required")
        @Size(max = 255, message = "Author must not exceed 255 characters")
        String author,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price must be non-negative")
        Long price,

        @Min(value = 0, message = "Original price must be non-negative")
        Long originalPrice,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be non-negative")
        Integer stock,

        String description,

        @Size(max = 500, message = "Cover URL must not exceed 500 characters")
        String coverUrl,

        @Size(max = 20, message = "ISBN must not exceed 20 characters")
        String isbn,

        @Size(max = 255, message = "Publisher must not exceed 255 characters")
        String publisher,

        @Min(value = 1000, message = "Published year must be 1000 or later")
        Integer publishedYear,

        @Min(value = 1, message = "Page count must be at least 1")
        Integer pageCount,

        @Size(max = 50, message = "Language must not exceed 50 characters")
        String language,

        Boolean active
) {}
