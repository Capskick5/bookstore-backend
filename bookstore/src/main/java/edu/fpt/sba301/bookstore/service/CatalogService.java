package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.response.BookResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;

public interface CatalogService {
    PageResponse<BookResponse> searchBooks(
            String q,
            Long categoryId,
            String author,
            Long minPrice,
            Long maxPrice,
            String sort,
            int page,
            int size);
}
