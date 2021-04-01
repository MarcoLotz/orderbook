package com.marcolotz.orderbook.core.exceptions;

public class EmptyOrderBookException extends RuntimeException {

    public EmptyOrderBookException(final String s) {
        super(s);
    }
}
