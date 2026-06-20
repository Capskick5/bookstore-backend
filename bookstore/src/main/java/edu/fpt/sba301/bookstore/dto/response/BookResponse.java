package edu.fpt.sba301.bookstore.dto.response;

import java.math.BigDecimal;

public record BookResponse(
        Long id,
        String title,
        String author,
        CategoryResponse category,
        Long price,
        Long originalPrice,
        Integer stock,
        String description,
        String coverUrl,
        BigDecimal ratingAvg,
        Integer soldCount
) {
}
