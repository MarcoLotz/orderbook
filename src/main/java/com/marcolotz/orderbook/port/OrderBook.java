package com.marcolotz.orderbook.port;

import com.marcolotz.orderbook.core.model.Order;

import java.math.BigDecimal;

public interface OrderBook {

    void addOrder(Order order);

    void removeOrder(final long orderId);

    void replaceOrder(Order order);

    Order getTopOrder();

    long getSizeForPriceLevel(final BigDecimal price);

    long getBookDepth();

    boolean containsOrder(final long orderId);

    Order getOrderById(long restingOrderId);
}
