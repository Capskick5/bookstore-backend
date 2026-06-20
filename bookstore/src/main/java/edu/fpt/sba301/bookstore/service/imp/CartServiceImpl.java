package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.request.GuestCartItemRequest;
import edu.fpt.sba301.bookstore.dto.response.CartItemResponse;
import edu.fpt.sba301.bookstore.dto.response.CartResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Cart;
import edu.fpt.sba301.bookstore.entity.CartItem;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.CartItemRepository;
import edu.fpt.sba301.bookstore.repository.CartRepository;
import edu.fpt.sba301.bookstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        Cart cart = getOrCreateCart(user);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(User user, Long bookId, int quantity) {
        Book book = findActiveBook(bookId);
        validateQuantity(book, quantity);
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartAndBookId(cart, bookId).orElse(null);
        int newQty = quantity;
        if (item != null) {
            newQty = item.getQuantity() + quantity;
        }
        validateQuantity(book, newQty);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setBook(book);
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        }
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(User user, Long bookId, int quantity) {
        Book book = findActiveBook(bookId);
        validateQuantity(book, quantity);
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartAndBookId(cart, bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(User user, Long bookId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartAndBookId(cart, bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public void mergeGuestCart(User user, List<GuestCartItemRequest> guestItems) {
        if (guestItems == null || guestItems.isEmpty()) {
            return;
        }
        for (GuestCartItemRequest guestItem : guestItems) {
            Book book = bookRepository.findById(guestItem.bookId()).orElse(null);
            if (book == null || Boolean.FALSE.equals(book.getActive())) {
                continue;
            }
            Cart cart = getOrCreateCart(user);
            CartItem existing = cartItemRepository.findByCartAndBookId(cart, guestItem.bookId()).orElse(null);
            int mergedQty = guestItem.quantity();
            if (existing != null) {
                mergedQty += existing.getQuantity();
            }
            mergedQty = Math.min(mergedQty, book.getStock());
            if (mergedQty <= 0) {
                continue;
            }
            if (existing == null) {
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setBook(book);
                item.setQuantity(mergedQty);
                cartItemRepository.save(item);
            } else {
                existing.setQuantity(mergedQty);
                cartItemRepository.save(existing);
            }
        }
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        cartRepository.findByUser(user).ifPresent(cart -> {
            cartItemRepository.deleteByCart(cart);
        });
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private Book findActiveBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
        if (Boolean.FALSE.equals(book.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book is inactive");
        }
        return book;
    }

    private void validateQuantity(Book book, int quantity) {
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
        }
        if (quantity > book.getStock()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock");
        }
    }

    CartResponse toResponse(Cart cart) {
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
                    unitPrice,
                    item.getQuantity(),
                    lineTotal,
                    book.getActive()
            ));
        }
        return new CartResponse(responses, subtotal);
    }
}
