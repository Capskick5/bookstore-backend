package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.dto.request.GuestCartItemRequest;
import edu.fpt.sba301.bookstore.dto.response.CartResponse;
import edu.fpt.sba301.bookstore.entity.User;

import java.util.List;

public interface CartService {
    CartResponse getCart(User user);

    CartResponse addItem(User user, Long bookId, int quantity);

    CartResponse updateItem(User user, Long bookId, int quantity);

    CartResponse removeItem(User user, Long bookId);

    void mergeGuestCart(User user, List<GuestCartItemRequest> guestItems);

    void clearCart(User user);
}
