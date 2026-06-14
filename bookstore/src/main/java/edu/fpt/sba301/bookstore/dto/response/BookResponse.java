package edu.fpt.sba301.bookstore.dto.response;

import java.math.BigDecimal;

public record BookResponse(
        Long id,
        String title,
        String author,
        Long price,
        Long originalPrice,
        Integer stock,
        String coverUrl,
        BigDecimal ratingAvg,
        Integer soldCount,
        Boolean active
) {}