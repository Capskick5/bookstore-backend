package edu.fpt.sba301.bookstore.dto.response;

public record BookRecommendationResponse(
        Long id,
        String title,
        String author,
        Long price,
        Integer stock
) {
}
