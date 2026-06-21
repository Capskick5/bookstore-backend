package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record VoucherRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Pattern(regexp = "FIXED|PERCENT|SHIP") String type,
        @NotNull @Min(0) Long value,
        @NotNull @Min(0) Long minOrder,
        @Min(0) Long maxDiscount,
        @Min(1) Integer usageLimit,
        @NotNull @Min(1) Integer perUserLimit,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Boolean active
) {
}
