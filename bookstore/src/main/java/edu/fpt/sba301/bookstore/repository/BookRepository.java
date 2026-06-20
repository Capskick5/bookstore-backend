package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    boolean existsByCategoryId(Long categoryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.stock = b.stock - :qty WHERE b.id = :id AND b.stock >= :qty")
    int reserveStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.stock = b.stock + :qty WHERE b.id = :id")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Book b SET b.soldCount = b.soldCount + :qty WHERE b.id = :id")
    int incrementSoldCount(@Param("id") Long id, @Param("qty") int qty);
}
