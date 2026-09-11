package com.bookstore.backend.payment;

import com.bookstore.backend.exception.PaymentDeclinedException;
import com.bookstore.backend.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditCardPaymentTest {

    private final CreditCardPayment payment = new CreditCardPayment();

    @Test
    void getType_isCreditCard() {
        assertThat(payment.getType()).isEqualTo(PaymentType.CREDIT_CARD);
    }

    @Test
    void process_wellFormedCardNumber_completesImmediately() {
        PaymentResult result = payment.process(new PaymentRequest(new BigDecimal("35.99"), "4111111111111111", "12/28"));

        assertThat(result.resultingStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.reference()).isNotBlank();
    }

    @Test
    void process_missingCardNumber_declines() {
        assertThatThrownBy(() -> payment.process(new PaymentRequest(new BigDecimal("35.99"), null, "12/28")))
                .isInstanceOf(PaymentDeclinedException.class);
    }

    @Test
    void process_cardNumberWrongLength_declines() {
        assertThatThrownBy(() -> payment.process(new PaymentRequest(new BigDecimal("35.99"), "1234", "12/28")))
                .isInstanceOf(PaymentDeclinedException.class);
    }

    @Test
    void process_cardNumberWithNonDigits_declines() {
        assertThatThrownBy(() -> payment.process(new PaymentRequest(new BigDecimal("35.99"), "411111111111111a", "12/28")))
                .isInstanceOf(PaymentDeclinedException.class);
    }
}
