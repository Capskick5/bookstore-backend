package edu.fpt.sba301.bookstore.dto.response;

import java.util.List;

public record ChatResponse(
        Long id,
        Long conversationId,
        String content,
        List<SourceResponse> sources,
        List<BookRecommendationResponse> recommendations
) {
}
