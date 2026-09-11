package com.bookstore.backend.repository;

import com.bookstore.backend.model.Cart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@ActiveProfiles({"dev", "test"})
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Test
    void findByGuestToken_existingCart_returnsIt() {
        cartRepository.save(Cart.forGuestToken("guest-xyz"));

        Optional<Cart> found = cartRepository.findByGuestToken("guest-xyz");

        assertThat(found).isPresent();
    }

    @Test
    void findByGuestToken_unknownToken_returnsEmpty() {
        assertThat(cartRepository.findByGuestToken("does-not-exist")).isEmpty();
    }

    @Test
    void guestToken_mustBeUnique() {
        cartRepository.saveAndFlush(Cart.forGuestToken("duplicate-token"));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> cartRepository.saveAndFlush(Cart.forGuestToken("duplicate-token")));
    }
}
