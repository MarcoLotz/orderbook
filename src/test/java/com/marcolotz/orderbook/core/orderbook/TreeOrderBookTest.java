package com.marcolotz.orderbook.core.orderbook;

import com.marcolotz.orderbook.port.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

@DisplayName("When testing a tree based order book")
public class TreeOrderBookTest extends BaseOrderBookTest {

  @BeforeEach
  void setUp() {
    // BID tree -> highest order first
    orderBook = new TreeOrderBook(Side.BID);
  }

}
