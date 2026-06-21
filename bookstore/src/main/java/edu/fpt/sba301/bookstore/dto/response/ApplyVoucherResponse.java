package edu.fpt.sba301.bookstore.dto.response;

public record ApplyVoucherResponse(
        String code,
        String type,
        Long discount,
        boolean freeShipping,
        Long estimatedShippingFee,
        Long estimatedTotal,
        String description
) {
}
