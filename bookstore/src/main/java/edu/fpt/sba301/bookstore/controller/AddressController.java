package edu.fpt.sba301.bookstore.controller;

import edu.fpt.sba301.bookstore.dto.request.AddressRequest;
import edu.fpt.sba301.bookstore.dto.response.AddressResponse;
import edu.fpt.sba301.bookstore.dto.response.ApiResponse;
import edu.fpt.sba301.bookstore.entity.Address;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.AddressRepository;
import edu.fpt.sba301.bookstore.support.ApiResponseSupport;
import edu.fpt.sba301.bookstore.support.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/auth/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(Principal principal) {
        User user = currentUserService.requireUser(principal);
        List<AddressResponse> data = addressRepository.findAllByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponseSupport.ok(data);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            Principal principal,
            @Valid @RequestBody AddressRequest request) {
        User user = currentUserService.requireUser(principal);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault())
                || addressRepository.findAllByUserId(user.getId()).isEmpty();

        if (makeDefault) {
            clearDefaultFlag(user.getId());
        }

        Address address = new Address();
        address.setUser(user);
        applyRequest(address, request, makeDefault);
        address = addressRepository.save(address);
        return ApiResponseSupport.created(toResponse(address));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        User user = currentUserService.requireUser(principal);
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            clearDefaultFlag(user.getId());
        }

        applyRequest(address, request, makeDefault || Boolean.TRUE.equals(address.getIsDefault()));
        address = addressRepository.save(address);
        return ApiResponseSupport.ok(toResponse(address));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteAddress(Principal principal, @PathVariable Long id) {
        User user = currentUserService.requireUser(principal);
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        addressRepository.delete(address);
        return ResponseEntity.noContent().build();
    }

    private void clearDefaultFlag(Long userId) {
        for (Address existing : addressRepository.findAllByUserId(userId)) {
            if (Boolean.TRUE.equals(existing.getIsDefault())) {
                existing.setIsDefault(false);
                addressRepository.save(existing);
            }
        }
    }

    private void applyRequest(Address address, AddressRequest request, boolean isDefault) {
        address.setRecipient(request.recipient());
        address.setPhone(request.phone());
        address.setLine(request.line());
        address.setCity(request.city());
        address.setIsDefault(isDefault);
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipient(),
                address.getPhone(),
                address.getLine(),
                address.getCity(),
                Boolean.TRUE.equals(address.getIsDefault()));
    }
}
