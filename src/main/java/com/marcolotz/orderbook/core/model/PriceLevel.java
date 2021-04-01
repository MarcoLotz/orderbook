package com.marcolotz.orderbook.core.model;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class PriceLevel {

    public final BigDecimal price;
    // assuming number of price Limits (generally << N the number of orders)
    public final Map<Long, Order> orderSequence = new LinkedHashMap<>(100);
}
