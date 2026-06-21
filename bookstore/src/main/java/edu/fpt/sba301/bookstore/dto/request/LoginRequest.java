package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        List<GuestCartItemRequest> guestCartItems
) {
    public LoginRequest(String email, String password) {
        this(email, password, null);
    }
}
