package edu.fpt.sba301.bookstore.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminBookResponse(
        Long id,
        String title,
        String author,
        CategoryResponse category,
        Long price,
        Long originalPrice,
        Integer stock,
        String description,
        String coverUrl,
        String isbn,
        String publisher,
        Integer publishedYear,
        Integer pageCount,
        String language,
        BigDecimal ratingAvg,
        Integer soldCount,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
