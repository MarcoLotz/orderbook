package com.marcolotz.orderbook.core.model;

import java.math.BigDecimal;
import lombok.Value;

@Value
public class Order {

  long id;
  long quantity;
  BigDecimal price;
}
