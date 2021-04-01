package com.marcolotz.orderbook.port;

import java.math.BigDecimal;

public interface Level2Metrics {

    long getSizeForPriceLevel(final Side side, final BigDecimal price); // total quantity of existing orders on this price level

    long getBookDepth(final Side side); // get the number of price levels on the specified side

    BigDecimal getTopOfBook(final Side side); // get highest bid or lowest ask, resp.

}
