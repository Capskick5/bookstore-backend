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
        // test customer user
        String customerEmail = "test@example.com";
        String customerPassword = "password123";

        if (userRepository.findByEmail(customerEmail).isEmpty()) {
            User user = new User();
            user.setEmail(customerEmail);
            user.setPasswordHash(passwordEncoder.encode(customerPassword));
            user.setFullName("Test User");
            user.setRole(Role.CUSTOMER.name());
            user.setEnabled(true);
            user.setPoints(0L);
            user.setLifetimePoints(0L);
            user.setTier("SILVER");
            user.setCreatedAt(OffsetDateTime.now());

            userRepository.save(user);
            System.out.println("Created test customer user: " + customerEmail + " / " + customerPassword);
        }

        // test admin user
        String adminEmail = "admin@example.com";
        String adminPassword = "adminpassword123";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setFullName("Admin User");
            admin.setRole(Role.ADMIN.name());
            admin.setEnabled(true);
            admin.setPoints(0L);
            admin.setLifetimePoints(0L);
            admin.setTier("SILVER");
            admin.setCreatedAt(OffsetDateTime.now());

            userRepository.save(admin);
            System.out.println("Created test admin user: " + adminEmail + " / " + adminPassword);
        }
    }
}