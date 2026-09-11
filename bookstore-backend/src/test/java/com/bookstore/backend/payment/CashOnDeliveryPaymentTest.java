package com.bookstore.backend.payment;

import com.bookstore.backend.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CashOnDeliveryPaymentTest {

    private final CashOnDeliveryPayment payment = new CashOnDeliveryPayment();

    @Test
    void getType_isCashOnDelivery() {
        assertThat(payment.getType()).isEqualTo(PaymentType.CASH_ON_DELIVERY);
    }

    @Test
    void process_neverCollectsPaymentUpfront() {
        PaymentResult result = payment.process(new PaymentRequest(new BigDecimal("35.99"), null, null));

        assertThat(result.resultingStatus()).isEqualTo(OrderStatus.CREATED);
    }
}
