package edu.fpt.sba301.bookstore.mapper;

import edu.fpt.sba301.bookstore.dto.response.AdminBookResponse;
import edu.fpt.sba301.bookstore.dto.response.BookResponse;
import edu.fpt.sba301.bookstore.dto.response.CategoryResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookResponse toBookResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                toCategoryResponse(book),
                book.getPrice(),
                book.getOriginalPrice(),
                book.getStock(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getIsbn(),
                book.getPublisher(),
                book.getPublishedYear(),
                book.getPageCount(),
                book.getLanguage(),
                book.getRatingAvg(),
                book.getSoldCount());
    }

    public AdminBookResponse toAdminBookResponse(Book book) {
        return new AdminBookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                toCategoryResponse(book),
                book.getPrice(),
                book.getOriginalPrice(),
                book.getStock(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getIsbn(),
                book.getPublisher(),
                book.getPublishedYear(),
                book.getPageCount(),
                book.getLanguage(),
                book.getRatingAvg(),
                book.getSoldCount(),
                book.getActive(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }

    private CategoryResponse toCategoryResponse(Book book) {
        if (book.getCategory() == null) {
            return null;
        }
        return new CategoryResponse(
                book.getCategory().getId(),
                book.getCategory().getName(),
                book.getCategory().getSlug());
    }
}
