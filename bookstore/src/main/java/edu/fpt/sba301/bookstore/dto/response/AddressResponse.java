package edu.fpt.sba301.bookstore.dto.response;

public record AddressResponse(
        Long id,
        String recipient,
        String phone,
        String line,
        String city,
        boolean isDefault
) {
}
