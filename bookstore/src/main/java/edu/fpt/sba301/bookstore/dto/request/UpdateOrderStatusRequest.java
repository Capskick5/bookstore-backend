package edu.fpt.sba301.bookstore.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(@NotBlank String status) {
}
