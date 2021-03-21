package com.marcolotz.orderbook.core.orderbook;

import com.marcolotz.orderbook.core.model.Order;
import com.marcolotz.orderbook.core.model.PriceLevel;
import com.marcolotz.orderbook.core.util.RedBlackNode;
import com.marcolotz.orderbook.core.util.RedBlackTree;
import com.marcolotz.orderbook.port.OrderBook;
import com.marcolotz.orderbook.port.Side;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/***
 * Implementation of Order book using Red-Black trees.
 * This data structure was used because of its auto-balancing capabilities when doing the price levels tree.
 *
 * Notes:
 * - The order book can be parallelized on ISIN level. There are about 20M ISINS registered (from the top of my mind).
 * - I assumed that we could keep 20M orders in memory for this class - which seems a bit unrealistic for real use of this instance.
 * Even tho NASDAQ performs 200k operations per second on high peaks, they are on ALL possible ISINs. I may have miss-perceived it, but
 * a load balancer approach could split the load between multiple instances of this class - even on different JVMs and hosts, reducing the
 * memory pressure on a the single instance and garbage collection cycles on each JVM. This would be a method to scale-out the application
 * and assuming that memory is cheap. The only problem of doing this in a distributed system is that I/O is expensive - which implies that
 * the better routing protocols would have to be evaluated (e.g. FPGA routing, etc).
 */
public class TreeOrderBook implements OrderBook {

  private static final int START_NUMBER_OF_ORDERS = 20_000_000;
  private static final int START_NUMBER_OF_LEVELS = 10_000;

  // Red black tree are useful when we need insertion and deletion relatively frequent.
  // Red-black trees are self-balancing so these operations are guaranteed to be O(logn).
  private final RedBlackTree<BigDecimal> priceLevelTree;

  private final Map<Long, Order> orderMap;

  private final Map<BigDecimal, PriceLevel> priceLevelMap;
  private final Comparator<BigDecimal> priceLevelComparator;
  private RedBlackNode<BigDecimal> topOrderPrice;

  TreeOrderBook(Side side) {
    this.priceLevelComparator =  side.equals(Side.ASK)? Comparator.naturalOrder() : (Comparator<BigDecimal>) Comparator.naturalOrder().reversed();
    priceLevelTree = new RedBlackTree<>(priceLevelComparator);
    orderMap = new HashMap<>(START_NUMBER_OF_ORDERS);
    priceLevelMap = new HashMap<>(START_NUMBER_OF_LEVELS);
  }

  /**
   * Adds order on O(1) amortized.
   * <p>
   * The amortization is for when no orders on that price range were created yet. In that scenario,  it's O(logn) majored by the insertion
   * on a RB Tree. Since the number of orders >> number of price ranges in a time window, it's fine to assume O(1) amortized.
   *
   * @param order order to be added.
   */
  @Override
  public void addOrder(Order order) {
    // Get price level
    PriceLevel orderPriceLevel = priceLevelMap.get(order.getPrice()); // O(1)
    if (orderPriceLevel == null) {
      orderPriceLevel = new PriceLevel(order.getPrice());
      priceLevelMap.put(order.getPrice(), orderPriceLevel);
      RedBlackNode<BigDecimal> insertedNode = priceLevelTree.insert(orderPriceLevel.price); // O (log(n))
      if (topOrderPrice == null || priceLevelComparator.compare(topOrderPrice.value, order.getPrice()) > 0) {
        topOrderPrice = insertedNode;
      }
    }
    // Update orders on price level
    orderPriceLevel.orderSequence.put(order.getId(), order); // O(1)
    orderMap.put(order.getId(), order); // O(1)
  }

  /***
   *  Removes Order with O(1) amortized.
   *  Whenever the price level is empty, it needs to be removed from the R&B tree - which triggers a Tree search O(logn).
   *  It's possible to improve this to O(1) - by keeping track of the Tree Nodes and avoiding the search.
   *  I haven't implemented it due to time constraint.
   *
   * @param orderId order id
   */
  @Override
  public void removeOrder(long orderId) {
    // Remove order
    final Order removeOrder = orderMap.remove(orderId); // O(1)
    if (removeOrder != null) {
      // Remove order from price level
      final PriceLevel level = priceLevelMap.get(removeOrder.getPrice());
      level.orderSequence.remove(removeOrder.getId()); // O(1)

      // Cleanup if price level is empty
      if (level.orderSequence.isEmpty()) {
        priceLevelTree.remove(level.price); // O(logn)
        priceLevelMap.remove(level.price); // O(1)
        if (topOrderPrice.value.equals(level.price)) {
          updateTopPrice();
        }
      }
    }
  }

  private void updateTopPrice() {
    RedBlackNode<BigDecimal> parent = topOrderPrice.getParent();
    if (parent == null || parent.value == null) { // last order in the book
      topOrderPrice = null;
    } else {
      if (parent.getLeft() != null && parent.getLeft().value != null) {
        if (priceLevelComparator.compare(parent.value, parent.getLeft().value) > 0) {
          topOrderPrice = parent.getLeft();
        }
      } else if (parent.getRight() != null && parent.getRight().value != null) {
        if (priceLevelComparator.compare(parent.value, parent.getRight().value) > 0) {
          topOrderPrice = parent.getRight();
        }
      } else {
        topOrderPrice = parent;
      }
    }
  }

  /***
   * Replaces order with O(1).
   * @param order order to be replaced
   */
  @Override
  public void replaceOrder(final Order order) {
    Order oldOrder = orderMap.get(order.getId());
    if (oldOrder != null) {
      removeOrder(oldOrder.getId());
      addOrder(order);
    }
  }

  /***
   * Gets top order with O(1) time.
   * @return top order if any otherwise null if there's no orders in the book
   */
  @Override
  public Order getTopOrder() {
    return Optional.ofNullable(topOrderPrice)
        .map(order -> order.value)
        .map(priceLevelMap::get) // O(1)
        .map(e -> e.orderSequence).map(Map::values).map(Collection::iterator) // O(1)
        .map(Iterator::next)
        .orElse(null);
  }

  /***
   * Gets size for Price Level with O(1) time
   *
   * @param price price level
   * @return the number of orders in that price level
   */
  @Override
  public long getSizeForPriceLevel(BigDecimal price) {
    return Optional.ofNullable(priceLevelMap.get(price)) // O(1)
        .map(s -> s.orderSequence)
        .map(Map::size)
        .orElse(0);
  }

  @Override
  public long getBookDepth() {
    return priceLevelMap.size();
  }

  @Override
  public boolean containsOrder(long orderId) {
    return orderMap.containsKey(orderId);
  }

  @Override
  public Order getOrderById(long restingOrderId) {
    return orderMap.get(restingOrderId);
  }
}
