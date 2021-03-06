package com.marcolotz.orderbook.core;

import com.marcolotz.orderbook.core.exceptions.EmptyOrderBookException;
import com.marcolotz.orderbook.core.exceptions.InvalidOrderException;
import com.marcolotz.orderbook.core.exceptions.InvalidTradeException;
import com.marcolotz.orderbook.core.model.Order;
import com.marcolotz.orderbook.port.Level2View;
import com.marcolotz.orderbook.port.OrderBook;
import com.marcolotz.orderbook.port.Side;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Log4j2 // Nonblocking - Async logging
public class SimpleLevel2View implements Level2View {

    final Map<Side, OrderBook> orderServiceMap;

    SimpleLevel2View(final OrderBook askBook, final OrderBook bidBook) {
        orderServiceMap = Map.of(
            Side.ASK, askBook,
            Side.BID, bidBook);
    }

    @Override
    public void onNewOrder(final Side side, final BigDecimal price, final long quantity, final long orderId) {
        final Order order = new Order(orderId, quantity, price);
        orderServiceMap.get(side).addOrder(order);
    }

    @Override
    public void onCancelOrder(final long orderId) {
        final Optional<OrderBook> orderServiceOpt = orderServiceMap.values().stream()
            .filter(r -> r.containsOrder(orderId))
            .findAny();
        orderServiceOpt.ifPresentOrElse(r -> r.removeOrder(orderId), () -> handleInvalidOrderId(orderId));
    }

    @Override
    public void onReplaceOrder(final BigDecimal price, final long quantity, final long orderId) {
        final Order order = new Order(orderId, quantity, price);
        orderServiceMap.values().stream()
            .filter(r -> r.containsOrder(orderId))
            .findAny()
            .ifPresentOrElse(r -> r.replaceOrder(order), () -> handleInvalidOrderId(orderId));
    }

    @Override
    public void onTrade(final long quantity, final long restingOrderId) {
        final Optional<OrderBook> orderServiceOp = orderServiceMap.values().stream()
            .filter(r -> r.containsOrder(restingOrderId))
            .findAny();

        if (orderServiceOp.isPresent()) {
            final OrderBook orderBook = orderServiceOp.get();
            final Order restingOrder = orderBook.getOrderById(restingOrderId);
            final long leftOver = restingOrder.getQuantity() - quantity;
            if (leftOver < 0) {
                throw new InvalidTradeException(
                    "Could not perform a trade of " + quantity + " on a resting order with " + restingOrder.getQuantity());
            }
            if (leftOver > 0) {
                final Order updatedOrder = new Order(restingOrderId, leftOver, restingOrder.getPrice());
                orderBook.replaceOrder(updatedOrder);
            } else {
                orderBook.removeOrder(restingOrderId);
            }
        }
    }

    @Override
    public long getSizeForPriceLevel(final Side side, final BigDecimal price) {
        return orderServiceMap.get(side).getSizeForPriceLevel(price);
    }

    @Override
    public long getBookDepth(final Side side) {
        return orderServiceMap.get(side).getBookDepth();
    }

    @Override
    public BigDecimal getTopOfBook(final Side side) {
        return Optional.ofNullable(orderServiceMap.get(side))
            .map(OrderBook::getTopOrder)
            .map(Order::getPrice)
            .orElseThrow(() -> new EmptyOrderBookException("No orders are available in the book"));
    }

    private void handleInvalidOrderId(final long orderId) {
        log.error("Could not find order ID {}", orderId);
        throw new InvalidOrderException("Order not found: " + orderId);
    }
}
