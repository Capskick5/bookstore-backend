package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.BookResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.mapper.BookMapper;
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

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Validated
@Tag(name = "Catalog", description = "Public book catalog")
public class BookController {

    private final BookRepository bookRepository;
    private final CatalogService catalogService;
    private final BookMapper bookMapper;

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
                    response.setData(bookMapper.toBookResponse(book));
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "List related books in the same category")
    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getRelatedBooks(@PathVariable Long id) {
        List<BookResponse> data = catalogService.getRelatedBooks(id);
        ApiResponse<List<BookResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);
        return ResponseEntity.ok(response);
    }
}
