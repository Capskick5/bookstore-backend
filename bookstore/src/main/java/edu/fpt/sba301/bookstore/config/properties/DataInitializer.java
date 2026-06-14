package edu.fpt.sba301.bookstore.config.properties;

import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.enums.Role;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // test user credentials
        String email = "test@example.com";
        String rawPassword = "password";

        // only create if not exists
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName("Test User");
            // choose a Role present in your enum: ADMIN, CUSTOMER, or STAFF
            user.setRole(Role.CUSTOMER.name());
            user.setEnabled(true);
            user.setPoints(0L);
            user.setTier("SILVER");
            user.setCreatedAt(OffsetDateTime.now());

            userRepository.save(user);
            System.out.println("Created test user: " + email + " / " + rawPassword + " (role=" + user.getRole() + ")");
        } else {
            System.out.println("Test user already exists: " + email);
        }
    }
}