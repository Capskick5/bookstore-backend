package edu.fpt.sba301.bookstore.dto.response;

import java.util.List;

public record PointsResponse(
        Long balance,
        List<PointTransactionResponse> transactions,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
