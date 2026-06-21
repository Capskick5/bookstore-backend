package edu.fpt.sba301.bookstore.dto.response;

import java.time.OffsetDateTime;

public record VoucherResponse(
        Long id,
        String code,
        String type,
        Long value,
        Long minOrder,
        Long maxDiscount,
        Integer usageLimit,
        Integer usedCount,
        Integer perUserLimit,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Boolean active
) {
}
