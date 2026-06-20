package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.points = u.points + :delta WHERE u.id = :id AND u.points + :delta >= 0")
    int adjustPoints(@Param("id") Long id, @Param("delta") long delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.lifetimePoints = u.lifetimePoints + :delta WHERE u.id = :id AND u.lifetimePoints + :delta >= 0")
    int adjustLifetimePoints(@Param("id") Long id, @Param("delta") long delta);
}
