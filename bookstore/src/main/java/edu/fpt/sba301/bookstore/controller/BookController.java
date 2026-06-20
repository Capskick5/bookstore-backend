package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.BookResponse;
import edu.fpt.sba301.bookstore.dto.response.CategoryResponse;
import edu.fpt.sba301.bookstore.dto.response.PageResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Validated
public class BookController {

    private final BookRepository bookRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> getBooks(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        Page<BookResponse> books = bookRepository
                .findAllByActiveTrue(PageRequest.of(page, size, Sort.by("id").descending()))
                .map(this::mapToResponse);

        ApiResponse<PageResponse<BookResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(PageResponse.from(books));
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
