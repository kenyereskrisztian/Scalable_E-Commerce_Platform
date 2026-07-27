package com.ecommerce.exception;

public class BadRequestException extends RuntimeException {
    private final String message;

    public BadRequestException(String message) {
        super(message);
        this.message = message;
    }

    public String getErrorMessage() {
        return message;
    }
}
