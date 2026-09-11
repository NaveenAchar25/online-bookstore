package com.bookstore.backend.service;

import com.bookstore.backend.dto.CheckoutRequest;
import com.bookstore.backend.dto.OrderResponse;
import com.bookstore.backend.exception.EmptyCartException;
import com.bookstore.backend.exception.OrderNotFoundException;
import com.bookstore.backend.exception.UnsupportedPaymentTypeException;
import com.bookstore.backend.model.Book;
import com.bookstore.backend.model.Cart;
import com.bookstore.backend.model.CartItem;
import com.bookstore.backend.model.Order;
import com.bookstore.backend.model.User;
import com.bookstore.backend.payment.PaymentMethod;
import com.bookstore.backend.payment.PaymentRequest;
import com.bookstore.backend.payment.PaymentResult;
import com.bookstore.backend.repository.BookRepository;
import com.bookstore.backend.repository.CartRepository;
import com.bookstore.backend.repository.OrderRepository;
import com.bookstore.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
@Slf4j
public class OrderService {

    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final List<PaymentMethod> paymentMethods;
    private final OrderMapper mapper;

    public OrderService(CartRepository cartRepository,
                         BookRepository bookRepository,
                         OrderRepository orderRepository,
                         UserRepository userRepository,
                         List<PaymentMethod> paymentMethods,
                         OrderMapper mapper) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paymentMethods = paymentMethods;
        this.mapper = mapper;
    }

    public OrderResponse checkout(String userEmail, String guestToken, CheckoutRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated request carried an email with no matching user: " + userEmail));

        Cart cart = cartRepository.findByGuestToken(guestToken)
                .orElseThrow(EmptyCartException::new);

        PaymentMethod paymentMethod = resolvePaymentMethod(request.getPaymentType());
        PaymentResult paymentResult = paymentMethod.process(
                new PaymentRequest(cart.getTotalAmount(), request.getCardNumber(), request.getCardExpiry()));

        Order order = Order.createFrom(cart, user, request.getPaymentType(), paymentResult);

        // Book.decrementStock() already mutated each managed Book entity in
        // memory; these saves make the persistence intent explicit rather
        // than relying silently on Hibernate's dirty-checking flush, the
        // same convention used everywhere else in this codebase.
        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            bookRepository.save(book);
        }

        Order saved = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order {} placed by {}: {} item(s), total {}, payment {}",
                saved.getId(), userEmail, saved.getItems().size(), saved.getTotalAmount(), request.getPaymentType());

        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated request carried an email with no matching user: " + userEmail));

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String userEmail, Long orderId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated request carried an email with no matching user: " + userEmail));

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return mapper.toResponse(order);
    }

    private PaymentMethod resolvePaymentMethod(String paymentType) {
        return paymentMethods.stream()
                .filter(pm -> pm.getType().name().equalsIgnoreCase(paymentType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentTypeException(paymentType));
    }
}
