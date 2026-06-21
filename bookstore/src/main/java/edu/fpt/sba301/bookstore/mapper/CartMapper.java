package edu.fpt.sba301.bookstore.mapper;

import edu.fpt.sba301.bookstore.dto.response.CartItemResponse;
import edu.fpt.sba301.bookstore.dto.response.CartResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Cart;
import edu.fpt.sba301.bookstore.entity.CartItem;
import edu.fpt.sba301.bookstore.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartMapper {

    private final CartItemRepository cartItemRepository;

    public CartResponse toResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCart(cart);
        List<CartItemResponse> responses = new ArrayList<>();
        long subtotal = 0L;
        for (CartItem item : items) {
            Book book = item.getBook();
            long unitPrice = book.getPrice();
            long lineTotal = unitPrice * item.getQuantity();
            subtotal += lineTotal;
            responses.add(new CartItemResponse(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getCoverUrl(),
                    unitPrice,
                    item.getQuantity(),
                    lineTotal,
                    book.getActive()
            ));
        }
        return new CartResponse(responses, subtotal);
    }
}
