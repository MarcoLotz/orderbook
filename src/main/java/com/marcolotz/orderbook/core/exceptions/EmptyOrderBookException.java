package com.marcolotz.orderbook.core.exceptions;

public class EmptyOrderBookException extends RuntimeException {

  public EmptyOrderBookException(String s) {
    super(s);
  }
}
