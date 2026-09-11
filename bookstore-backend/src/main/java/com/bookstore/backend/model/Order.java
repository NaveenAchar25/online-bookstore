package com.bookstore.backend.model;

import com.bookstore.backend.exception.EmptyCartException;
import com.bookstore.backend.payment.PaymentResult;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class Order extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_type", nullable = false, length = 30)
    private String paymentType;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public static Order createFrom(Cart cart, User user, String paymentType, PaymentResult paymentResult) {
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }

        Order order = Order.builder()
                .user(user)
                .status(paymentResult.resultingStatus())
                .totalAmount(cart.getTotalAmount())
                .paymentType(paymentType)
                .build();

        for (CartItem cartItem : cart.getItems()) {
            Book book = cartItem.getBook();
            // Re-validated and decremented here, at the actual moment of
            // purchase — not trusted from whatever check happened when the
            // item was added to the cart,
            // Throws InsufficientStockException internally
            // if stock no longer supports this quantity.
            book.decrementStock(cartItem.getQuantity());
            order.items.add(OrderItem.of(order, book, cartItem.getQuantity()));
        }

        return order;
    }
}
