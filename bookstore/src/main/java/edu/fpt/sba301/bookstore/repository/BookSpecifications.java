package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Book;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Book> titleOrAuthorContains(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern));
        };
    }

    public static Specification<Book> authorContains(String author) {
        return (root, query, cb) -> {
            String pattern = "%" + author.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("author")), pattern);
        };
    }

    public static Specification<Book> categoryIdEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Book> priceAtLeast(Long minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Book> priceAtMost(Long maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
