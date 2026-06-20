package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record PointTransactionResponse(
        Long id,
        Long delta,
        String reason,
        Long orderId,
        OffsetDateTime createdAt
) {
}
