package com.marcolotz.orderbook.port;

import java.math.BigDecimal;

public interface Level2EventListener {

    void onNewOrder(final Side side, BigDecimal price, final long quantity, final long orderId);

    void onCancelOrder(final long orderId);

    void onReplaceOrder(final BigDecimal price, final long quantity, final long orderId);

    // When an aggressor order crosses the spread, it will be matched with an existing resting order, causing a trade.
    // The aggressor order will NOT cause an invocation of onNewOrder.
    void onTrade(final long quantity, final long restingOrderId);
}
