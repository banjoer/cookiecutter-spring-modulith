package com.example.springmodulithapplication.shared.error;

/**
 * Base type for expected business failures that should be translated to an HTTP error response.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
