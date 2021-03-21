package com.marcolotz.orderbook.core.exceptions;

public class InvalidOrderException extends RuntimeException {

  public InvalidOrderException(String message) {
    super(message);
  }
}
