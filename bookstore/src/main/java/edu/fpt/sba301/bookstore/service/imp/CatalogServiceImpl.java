package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.response.BookResponse;
import edu.fpt.sba301.bookstore.dto.response.CategoryResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.BookSpecifications;
import edu.fpt.sba301.bookstore.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final BookRepository bookRepository;

    @Override
    public PageResponse<BookResponse> searchBooks(
            String q,
            Long categoryId,
            String author,
            Long minPrice,
            Long maxPrice,
            String sort,
            int page,
            int size) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice cannot exceed maxPrice");
        }

        String keyword = normalize(q);
        String authorFilter = normalize(author);
        Sort sortOrder = resolveSort(sort);

        Specification<Book> spec = BookSpecifications.activeOnly();
        if (keyword != null) {
            spec = spec.and(BookSpecifications.titleOrAuthorContains(keyword));
        }
        if (authorFilter != null) {
            spec = spec.and(BookSpecifications.authorContains(authorFilter));
        }
        if (categoryId != null) {
            spec = spec.and(BookSpecifications.categoryIdEquals(categoryId));
        }
        if (minPrice != null) {
            spec = spec.and(BookSpecifications.priceAtLeast(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(BookSpecifications.priceAtMost(maxPrice));
        }

        Page<Book> books = bookRepository.findAll(spec, PageRequest.of(page, size, sortOrder));

        return PageResponse.from(books.map(this::mapToResponse));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by("id").descending();
        }
        return switch (sort) {
            case "title_asc" -> Sort.by("title").ascending();
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "sold_desc" -> Sort.by("soldCount").descending();
            case "rating_desc" -> Sort.by("ratingAvg").descending();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort parameter");
        };
    }

    private BookResponse mapToResponse(Book book) {
        CategoryResponse category = book.getCategory() == null
                ? null
                : new CategoryResponse(
                        book.getCategory().getId(),
                        book.getCategory().getName(),
                        book.getCategory().getSlug());

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                category,
                book.getPrice(),
                book.getOriginalPrice(),
                book.getStock(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getRatingAvg(),
                book.getSoldCount());
    }
}
