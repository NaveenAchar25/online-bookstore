package com.bookstore.backend.model;

import com.bookstore.backend.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Owns the rules for how its own quantity may change so there is no public setter
 * for quantity. Every path that changes it goes through a method that
 * validates first.
 */
@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class CartItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * The only way to create a CartItem — validates stock before the object
     * exists at all, rather than constructing an invalid one and hoping
     * something downstream checks it later.
     */
    static CartItem of(Cart cart, Book book, int quantity) {
        requireSufficientStock(book, quantity);
        return CartItem.builder().cart(cart).book(book).quantity(quantity).build();
    }

    void increaseBy(int additionalQuantity) {
        int newQuantity = this.quantity + additionalQuantity;
        requireSufficientStock(book, newQuantity);
        this.quantity = newQuantity;
    }

    void updateQuantity(int newQuantity) {
        requireSufficientStock(book, newQuantity);
        this.quantity = newQuantity;
    }

    public BigDecimal getSubtotal() {
        return book.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    private static void requireSufficientStock(Book book, int requestedQuantity) {
        if (!book.hasSufficientStock(requestedQuantity)) {
            throw new InsufficientStockException(book.getTitle(), requestedQuantity, book.getStockQuantity());
        }
    }
}
