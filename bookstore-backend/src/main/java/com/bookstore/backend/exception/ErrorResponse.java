package com.bookstore.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Every error response in this API uses this exact shape, so a frontend's
 * error-handling code has exactly one structure to deal with, regardless
 * of which endpoint or which failure produced it.
 */
@Getter
@Builder
@AllArgsConstructor
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /** Populated only for request-validation failures (field -> message). */
    private List<String> details;
}
