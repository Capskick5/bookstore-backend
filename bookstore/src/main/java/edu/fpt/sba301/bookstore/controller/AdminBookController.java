package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.BookRequest;
import edu.fpt.sba301.bookstore.dto.response.AdminBookResponse;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.CategoryResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Category;
import edu.fpt.sba301.bookstore.event.BookChangedEvent;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/books")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBookResponse>>> getBooks() {
        List<AdminBookResponse> data = bookRepository.findAll().stream()
                .map(this::mapToResponse)
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
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setCategory(category);
        book.setPrice(request.price());
        book.setOriginalPrice(request.originalPrice());
        book.setStock(request.stock());
        book.setDescription(request.description());
        book.setCoverUrl(request.coverUrl());
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
        response.setData(mapToResponse(saved));

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

        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setCategory(category);
        book.setPrice(request.price());
        book.setOriginalPrice(request.originalPrice());
        book.setStock(request.stock());
        book.setDescription(request.description());
        book.setCoverUrl(request.coverUrl());
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
        response.setData(mapToResponse(saved));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<ApiResponse<Void>> deleteBook(@PathVariable Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));

        if (orderItemRepository.existsByBookId(id)) {
            // Soft-delete: active = false
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

    private AdminBookResponse mapToResponse(Book book) {
        CategoryResponse categoryResponse = null;
        if (book.getCategory() != null) {
            categoryResponse = new CategoryResponse(
                    book.getCategory().getId(),
                    book.getCategory().getName(),
                    book.getCategory().getSlug());
        }
        return new AdminBookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                categoryResponse,
                book.getPrice(),
                book.getOriginalPrice(),
                book.getStock(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getRatingAvg(),
                book.getSoldCount(),
                book.getActive(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }
}
