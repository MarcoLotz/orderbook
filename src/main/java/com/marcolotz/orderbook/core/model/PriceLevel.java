package com.marcolotz.orderbook.core.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PriceLevel {

  public final BigDecimal price;
  // assuming number of price Limits (generally << N the number of orders)
  public final Map<Long, Order> orderSequence = new LinkedHashMap<>(100);
}
