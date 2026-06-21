package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.BookRequest;
import edu.fpt.sba301.bookstore.dto.response.AdminBookResponse;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.CategoryResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Category;
import edu.fpt.sba301.bookstore.event.BookChangedEvent;
import edu.fpt.sba301.bookstore.mapper.BookMapper;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.CartItemRepository;
import edu.fpt.sba301.bookstore.repository.CategoryRepository;
import edu.fpt.sba301.bookstore.repository.OrderItemRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static edu.fpt.sba301.bookstore.config.SwaggerConfig.BEARER_AUTH;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/books")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Catalog", description = "Admin book management")
@SecurityRequirement(name = BEARER_AUTH)
public class AdminBookController {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BookMapper bookMapper;

    @Operation(summary = "List all books for admin management")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBookResponse>>> getBooks() {
        List<AdminBookResponse> data = bookRepository.findAll().stream()
                .map(bookMapper::toAdminBookResponse)
                .collect(Collectors.toList());

        ApiResponse<List<AdminBookResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<AdminBookResponse>> createBook(@Valid @RequestBody BookRequest request) {
        // Validation: price = 0 and stock = 0 is not permitted
        if (request.price() == 0 && request.stock() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either price or stock must be greater than zero");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        Book book = new Book();
        applyBookRequest(book, request);
        book.setCategory(category);
        book.setRatingAvg(BigDecimal.ZERO);
        book.setSoldCount(0);
        book.setActive(request.active() != null ? request.active() : true);
        book.setCreatedAt(OffsetDateTime.now());
        book.setUpdatedAt(OffsetDateTime.now());

        Book saved = bookRepository.save(book);

        // Publish event asynchronously
        eventPublisher.publishEvent(new BookChangedEvent(saved.getId(), "CREATE"));

        ApiResponse<AdminBookResponse> response = new ApiResponse<>();
        response.setCode(201);
        response.setMessage("Created");
        response.setData(bookMapper.toAdminBookResponse(saved));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<AdminBookResponse>> updateBook(@PathVariable Long id,
            @Valid @RequestBody BookRequest request) {
        // Validation: price = 0 and stock = 0 is not permitted
        if (request.price() == 0 && request.stock() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Either price or stock must be greater than zero");
        }

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        book.setCategory(category);
        applyBookRequest(book, request);
        if (request.active() != null) {
            book.setActive(request.active());
        }
        book.setUpdatedAt(OffsetDateTime.now());

        Book saved = bookRepository.save(book);

        // Publish event asynchronously
        eventPublisher.publishEvent(new BookChangedEvent(saved.getId(), "UPDATE"));

        ApiResponse<AdminBookResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(bookMapper.toAdminBookResponse(saved));

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Soft-delete or hard-delete a book")
    @DeleteMapping("/{id}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        if (orderItemRepository.existsByBookId(id)) {
            // Soft-delete: remove from carts, then deactivate
            cartItemRepository.deleteAllByBookId(id);
            book.setActive(false);
            book.setUpdatedAt(OffsetDateTime.now());
            bookRepository.save(book);

            // Publish event asynchronously (as an update)
            eventPublisher.publishEvent(new BookChangedEvent(id, "UPDATE"));
        } else {
            // Hard-delete: delete from cart_items and books
            cartItemRepository.deleteAllByBookId(id);
            bookRepository.delete(book);

            // Publish event asynchronously (as a delete)
            eventPublisher.publishEvent(new BookChangedEvent(id, "DELETE"));
        }

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");

        return ResponseEntity.ok(response);
    }

    private void applyBookRequest(Book book, BookRequest request) {
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setPrice(request.price());
        book.setOriginalPrice(request.originalPrice());
        book.setStock(request.stock());
        book.setDescription(request.description());
        book.setCoverUrl(request.coverUrl());
        book.setIsbn(normalizeOptional(request.isbn()));
        book.setPublisher(normalizeOptional(request.publisher()));
        book.setPublishedYear(request.publishedYear());
        book.setPageCount(request.pageCount());
        book.setLanguage(normalizeOptional(request.language()));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
