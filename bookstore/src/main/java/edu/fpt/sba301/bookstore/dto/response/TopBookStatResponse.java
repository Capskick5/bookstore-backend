package edu.fpt.sba301.bookstore.dto.response;

public record TopBookStatResponse(
        Long id,
        String title,
        long soldCount
) {
}
