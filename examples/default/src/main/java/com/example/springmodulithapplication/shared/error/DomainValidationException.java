package com.example.springmodulithapplication.shared.error;

/**
 * Raised when input is syntactically valid but violates a business rule.
 */
public class DomainValidationException extends DomainException {

    public DomainValidationException(String message) {
        super(message);
    }
}
