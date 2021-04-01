package com.marcolotz.orderbook.core.model;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class Order {

    long id;
    long quantity;
    BigDecimal price;
}
