package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    boolean existsByCategoryId(Long categoryId);

    Page<Book> findAllByActiveTrue(Pageable pageable);

    Optional<Book> findByIdAndActiveTrue(Long id);
}
