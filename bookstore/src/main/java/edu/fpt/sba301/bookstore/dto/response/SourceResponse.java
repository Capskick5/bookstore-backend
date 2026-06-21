package edu.fpt.sba301.bookstore.dto.response;

public record SourceResponse(
        String title,
        String documentName,
        Integer page,
        Double score
) {
}
