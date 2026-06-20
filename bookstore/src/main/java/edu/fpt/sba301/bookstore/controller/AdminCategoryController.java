package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.CategoryRequest;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.dto.response.CategoryResponse;
import edu.fpt.sba301.bookstore.entity.Category;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> data = categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .collect(Collectors.toList());

        ApiResponse<List<CategoryResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        String slug = slugify(request.name());
        if (categoryRepository.findBySlug(slug).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
        }

        Category category = new Category();
        category.setName(request.name());
        category.setSlug(slug);

        Category saved = categoryRepository.save(category);

        ApiResponse<CategoryResponse> response = new ApiResponse<>();
        response.setCode(201);
        response.setMessage("Created");
        response.setData(new CategoryResponse(saved.getId(), saved.getName(), saved.getSlug()));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        String slug = slugify(request.name());
        Optional<Category> existingSlugCategory = categoryRepository.findBySlug(slug);
        if (existingSlugCategory.isPresent() && !existingSlugCategory.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
        }

        category.setName(request.name());
        category.setSlug(slug);

        Category saved = categoryRepository.save(category);

        ApiResponse<CategoryResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(new CategoryResponse(saved.getId(), saved.getName(), saved.getSlug()));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (bookRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category is in use by one or more books");
        }

        categoryRepository.delete(category);

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");

        return ResponseEntity.ok(response);
    }

    private String slugify(String name) {
        if (name == null)
            return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}
