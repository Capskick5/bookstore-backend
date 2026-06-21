package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    boolean existsByCategoryId(Long categoryId);

    Page<Book> findAllByActiveTrue(Pageable pageable);

    @Query("""
            SELECT b FROM Book b
            WHERE b.active = true AND b.stock > 0
              AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    List<Book> searchActiveInStock(@Param("keyword") String keyword, Pageable pageable);

    Optional<Book> findByIdAndActiveTrue(Long id);

    long countByActiveTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.stock = b.stock - :qty WHERE b.id = :id AND b.stock >= :qty")
    int reserveStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.stock = b.stock + :qty WHERE b.id = :id")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.soldCount = b.soldCount + :qty WHERE b.id = :id")
    int incrementSoldCount(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.soldCount = b.soldCount - :qty WHERE b.id = :id AND b.soldCount >= :qty")
    int decrementSoldCount(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Book b SET b.ratingAvg = (
                SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.book.id = :bookId
            ) WHERE b.id = :bookId
            """)
    void recomputeRatingAvg(@Param("bookId") Long bookId);
}
