package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.AddressRequest;
import edu.fpt.sba301.bookstore.dto.response.AddressResponse;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.entity.Address;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.AddressRepository;
import edu.fpt.sba301.bookstore.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/auth/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<AddressResponse> data = addressRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        ApiResponse<List<AddressResponse>> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(data);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(Principal principal,
                                                                      @Valid @RequestBody AddressRequest request) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (Boolean.TRUE.equals(request.isDefault())) {
            unsetOtherDefaults(user.getId());
        }

        Address address = new Address();
        address.setUser(user);
        address.setRecipient(request.recipient());
        address.setPhone(request.phone());
        address.setLine(request.line());
        address.setCity(request.city());
        address.setIsDefault(request.isDefault());

        Address saved = addressRepository.save(address);

        ApiResponse<AddressResponse> response = new ApiResponse<>();
        response.setCode(201);
        response.setMessage("Created");
        response.setData(mapToResponse(saved));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(Principal principal,
                                                                      @PathVariable Long id,
                                                                      @Valid @RequestBody AddressRequest request) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (Boolean.TRUE.equals(request.isDefault())) {
            unsetOtherDefaults(user.getId());
        }

        address.setRecipient(request.recipient());
        address.setPhone(request.phone());
        address.setLine(request.line());
        address.setCity(request.city());
        address.setIsDefault(request.isDefault());

        Address saved = addressRepository.save(address);

        ApiResponse<AddressResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");
        response.setData(mapToResponse(saved));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteAddress(Principal principal, @PathVariable Long id) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        addressRepository.delete(address);

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("OK");

        return ResponseEntity.ok(response);
    }

    private void unsetOtherDefaults(Long userId) {
        List<Address> addresses = addressRepository.findAllByUserId(userId);
        for (Address addr : addresses) {
            if (Boolean.TRUE.equals(addr.getIsDefault())) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            }
        }
    }

    private AddressResponse mapToResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipient(),
                address.getPhone(),
                address.getLine(),
                address.getCity(),
                address.getIsDefault()
        );
    }
}
