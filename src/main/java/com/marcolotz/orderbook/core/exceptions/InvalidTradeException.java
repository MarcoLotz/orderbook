package com.marcolotz.orderbook.core.exceptions;

public class InvalidTradeException extends RuntimeException {

    public InvalidTradeException(final String s) {
        super(s);
    }
}
