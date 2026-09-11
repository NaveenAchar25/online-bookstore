package com.bookstore.backend.model;

import com.bookstore.backend.exception.CartItemNotFoundException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Root for a shopping cart. CartService never mutates a
 * CartItem directly — every change goes through a method here, so this
 * class is the single place that knows what "add a book," "change a
 * quantity," or "remove an item" actually means, including the stock validation 
 *
 * Identified by an opaque guest token rather than a user id — this cart
 * exists before any login system does, mirroring how a real storefront
 * lets you add items to a cart before creating an account.
 */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class Cart extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guest_token", nullable = false, unique = true)
    private String guestToken;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    public static Cart forGuestToken(String guestToken) {
        return Cart.builder().guestToken(guestToken).build();
    }

    /**
     * Adds a book to the cart, or increases its quantity if it's already
     * present — the caller doesn't need to know or care which case applies.
     */
    public void addOrIncreaseItem(Book book, int quantity) {
        findItemFor(book.getId())
                .ifPresentOrElse(
                        item -> item.increaseBy(quantity),
                        () -> {
                            CartItem newItem = CartItem.of(this, book, quantity);
                            items.add(newItem);
                        });
    }

    public void updateItemQuantity(Long bookId, int newQuantity) {
        requireItemFor(bookId).updateQuantity(newQuantity);
    }

    public void removeItem(Long bookId) {
        CartItem item = requireItemFor(bookId);
        items.remove(item);
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<CartItem> findItemFor(Long bookId) {
        return items.stream()
                .filter(item -> item.getBook().getId().equals(bookId))
                .findFirst();
    }

    private CartItem requireItemFor(Long bookId) {
        return findItemFor(bookId).orElseThrow(() -> new CartItemNotFoundException(bookId));
    }
}
