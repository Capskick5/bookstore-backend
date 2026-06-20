package edu.fpt.sba301.bookstore.dto.response;

public record CategoryResponse(
        Long id,
        String name,
        String slug
) {}
