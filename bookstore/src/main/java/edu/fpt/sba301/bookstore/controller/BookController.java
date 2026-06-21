package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.BookResponse;
import edu.fpt.sba301.bookstore.dto.response.CategoryResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.service.CatalogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Validated
@Tag(name = "Catalog", description = "Public book catalog")
public class BookController {

    private final BookRepository bookRepository;
    private final CatalogService catalogService;

    @Operation(summary = "Search and list active books")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> getBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false, defaultValue = "title_asc") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        PageResponse<BookResponse> data = catalogService.searchBooks(
                q, categoryId, author, minPrice, maxPrice, sort, page, size);

        ApiResponse<PageResponse<BookResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookResponse>> getBook(@PathVariable Long id) {
        return bookRepository.findByIdAndActiveTrue(id)
                .map(book -> {
                    ApiResponse<BookResponse> response = new ApiResponse<>();
                    response.setCode(200);
                    response.setMessage("OK");
                    response.setData(mapToResponse(book));
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
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
