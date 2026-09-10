package com.bookstore.backend.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private final Role customerRole = Role.builder().id(1L).name("ROLE_CUSTOMER").build();

    private User newUser() {
        return User.newCustomer("jane@example.com", "hashed", "Jane", "Doe", customerRole);
    }

    @Test
    void newUser_startsUnlockedWithZeroFailedAttempts() {
        User user = newUser();

        assertThat(user.isAccountLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void registerFailedLogin_incrementsCount() {
        User user = newUser();

        user.registerFailedLogin();

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    void registerFailedLogin_fifthConsecutiveFailure_locksTheAccount() {
        User user = newUser();

        for (int i = 0; i < 5; i++) {
            user.registerFailedLogin();
        }

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isAccountLocked()).isTrue();
    }

    @Test
    void registerFailedLogin_belowThreshold_doesNotLock() {
        User user = newUser();

        for (int i = 0; i < 4; i++) {
            user.registerFailedLogin();
        }

        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    void recordSuccessfulLogin_resetsFailedAttemptsAndUnlocks() {
        User user = newUser();
        for (int i = 0; i < 5; i++) {
            user.registerFailedLogin();
        }
        assertThat(user.isAccountLocked()).isTrue(); // sanity check on the setup

        user.recordSuccessfulLogin();

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.isAccountLocked()).isFalse();
    }
}
