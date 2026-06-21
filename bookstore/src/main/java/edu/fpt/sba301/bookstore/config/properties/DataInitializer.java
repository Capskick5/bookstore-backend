package edu.fpt.sba301.bookstore.config.properties;

import edu.fpt.sba301.bookstore.entity.Address;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.enums.Role;
import edu.fpt.sba301.bookstore.repository.AddressRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUser("admin@example.com", "adminpassword123", "Admin User", Role.ADMIN.name(), 0L, false);
        seedUser("test@example.com", "password123", "Test User", Role.CUSTOMER.name(), 50000L, true);
    }

    private void seedUser(String email, String rawPassword, String fullName, String role, long points, boolean withAddress) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        user.setEnabled(true);
        user.setPoints(points);
        user.setLifetimePoints(points);
        user.setTier("SILVER");
        user.setCreatedAt(OffsetDateTime.now());
        user = userRepository.save(user);

        if (withAddress) {
            Address address = new Address();
            address.setUser(user);
            address.setRecipient("Test User");
            address.setPhone("0901234567");
            address.setLine("123 Test Street");
            address.setCity("Hanoi");
            address.setIsDefault(true);
            addressRepository.save(address);
        }
    }
}
