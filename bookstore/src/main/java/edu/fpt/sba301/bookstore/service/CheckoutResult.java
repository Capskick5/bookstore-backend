package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.response.OrderResponse;

public record CheckoutResult(OrderResponse order, boolean newlyCreated) {
}
