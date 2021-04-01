package com.marcolotz.orderbook.core.exceptions;

public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(final String message) {
        super(message);
    }
}
