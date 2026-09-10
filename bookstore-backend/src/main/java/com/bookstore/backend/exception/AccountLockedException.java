package com.bookstore.backend.exception;

import java.time.LocalDateTime;

public class AccountLockedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("Account is locked until " + lockedUntil + " due to repeated failed login attempts");
    }
}
