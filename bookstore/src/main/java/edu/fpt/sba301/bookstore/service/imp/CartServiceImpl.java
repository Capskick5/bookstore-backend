package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.dto.request.GuestCartItemRequest;
import edu.fpt.sba301.bookstore.dto.response.CartResponse;
import edu.fpt.sba301.bookstore.entity.Book;
import edu.fpt.sba301.bookstore.entity.Cart;
import edu.fpt.sba301.bookstore.entity.CartItem;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.mapper.CartMapper;
import edu.fpt.sba301.bookstore.repository.BookRepository;
import edu.fpt.sba301.bookstore.repository.CartItemRepository;
import edu.fpt.sba301.bookstore.repository.CartRepository;
import edu.fpt.sba301.bookstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    public CartResponse getCart(User user) {
        Cart cart = getOrCreateCart(user);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(User user, Long bookId, int quantity) {
        Book book = findActiveBook(bookId);
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartAndBookId(cart, bookId).orElse(null);
        int newQuantity = item == null ? quantity : item.getQuantity() + quantity;
        validateQuantity(book, newQuantity);
        upsertCartItem(cart, book, item, newQuantity);
        return cartMapper.toResponse(cart);
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
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(User user, Long bookId) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartAndBookId(cart, bookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        cartItemRepository.delete(item);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public void mergeGuestCart(User user, List<GuestCartItemRequest> guestItems) {
        if (guestItems == null || guestItems.isEmpty()) {
            return;
        }
        Cart cart = getOrCreateCart(user);
        for (GuestCartItemRequest guestItem : guestItems) {
            mergeGuestItem(cart, guestItem);
        }
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        cartRepository.findByUser(user).ifPresent(cartItemRepository::deleteByCart);
    }

    private void mergeGuestItem(Cart cart, GuestCartItemRequest guestItem) {
        Book book = bookRepository.findById(guestItem.bookId()).orElse(null);
        if (book == null || Boolean.FALSE.equals(book.getActive())) {
            return;
        }

        CartItem existing = cartItemRepository.findByCartAndBookId(cart, guestItem.bookId()).orElse(null);
        int mergedQuantity = guestItem.quantity() + (existing != null ? existing.getQuantity() : 0);
        mergedQuantity = Math.min(mergedQuantity, book.getStock());
        if (mergedQuantity <= 0) {
            return;
        }

        upsertCartItem(cart, book, existing, mergedQuantity);
    }

    private void upsertCartItem(Cart cart, Book book, CartItem existingItem, int quantity) {
        if (existingItem == null) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setBook(book);
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            return;
        }
        existingItem.setQuantity(quantity);
        cartItemRepository.save(existingItem);
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
}
