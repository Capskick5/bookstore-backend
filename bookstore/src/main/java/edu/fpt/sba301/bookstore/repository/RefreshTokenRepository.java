package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.RefreshToken;
import edu.fpt.sba301.bookstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findAllByUser(User user);
    void deleteAllByExpiresAtBefore(java.time.OffsetDateTime time);
}