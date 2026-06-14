package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // Add query methods later (search, filter) as needed
}