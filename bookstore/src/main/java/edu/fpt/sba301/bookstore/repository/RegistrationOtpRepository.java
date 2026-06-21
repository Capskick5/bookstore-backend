package edu.fpt.sba301.bookstore.repository;

import edu.fpt.sba301.bookstore.entity.RegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, Long> {
    Optional<RegistrationOtp> findFirstByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);
}
