package com.bookstore.backend.service;

import com.bookstore.backend.dto.CheckoutRequest;
import com.bookstore.backend.dto.OrderResponse;
import com.bookstore.backend.exception.EmptyCartException;
import com.bookstore.backend.exception.OrderNotFoundException;
import com.bookstore.backend.exception.UnsupportedPaymentTypeException;
import com.bookstore.backend.model.Cart;
import com.bookstore.backend.model.Order;
import com.bookstore.backend.model.User;
import com.bookstore.backend.payment.PaymentMethod;
import com.bookstore.backend.payment.PaymentRequest;
import com.bookstore.backend.payment.PaymentResult;
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
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final List<PaymentMethod> paymentMethods;
    private final OrderMapper mapper;

    public OrderService(CartRepository cartRepository,
                         OrderRepository orderRepository,
                         UserRepository userRepository,
                         List<PaymentMethod> paymentMethods,
                         OrderMapper mapper) {
        this.cartRepository = cartRepository;
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

        // Checked here, before payment runs — not left to Order.createFrom()
        // alone. Order.createFrom() still checks this too (it must: nothing
        // stops a future caller from reaching it some other way), but that
        // check happening AFTER paymentMethod.process() below would mean an
        // empty-cart checkout attempts to charge a payment method before
        // the request is even validated as meaningful. 
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }

        PaymentMethod paymentMethod = resolvePaymentMethod(request.getPaymentType());
        PaymentResult paymentResult = paymentMethod.process(
                new PaymentRequest(cart.getTotalAmount(), request.getCardNumber(), request.getCardExpiry()));

        Order order = Order.createFrom(cart, user, request.getPaymentType(), paymentResult);

        // No explicit bookRepository.save() here, deliberately — each
        // Book was loaded through cart.getItems() within this same
        // transaction, so it's already a managed entity in Hibernate's
        // persistence context. Book.decrementStock(), called inside
        // Order.createFrom() above, mutated that managed instance
        // directly; Hibernate's own dirty-checking flushes that change
        // automatically at commit. An explicit save() here would be a
        // redundant merge() call on an object already identical to what's
        // being tracked

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
