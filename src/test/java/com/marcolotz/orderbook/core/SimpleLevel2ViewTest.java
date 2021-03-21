package com.marcolotz.orderbook.core;

import static com.marcolotz.orderbook.port.Side.ASK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.marcolotz.orderbook.core.exceptions.EmptyOrderBookException;
import com.marcolotz.orderbook.core.exceptions.InvalidOrderException;
import com.marcolotz.orderbook.core.exceptions.InvalidTradeException;
import com.marcolotz.orderbook.core.model.Order;
import com.marcolotz.orderbook.port.OrderBook;
import com.marcolotz.orderbook.port.Side;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("When performing actions on a Level2View")
@ExtendWith(MockitoExtension.class)
class SimpleLevel2ViewTest {

  SimpleLevel2View level2View;

  @Mock
  OrderBook askBook;
  @Mock
  OrderBook bidBook;

  @BeforeEach
  void setUp() {
    level2View = new SimpleLevel2View(askBook, bidBook);
    reset(askBook, bidBook);
  }

  @DisplayName("Then new orders are forwarded to the correct book")
  @ParameterizedTest
  @EnumSource(value = Side.class)
  void onNewOrder(Side side) {
    // Given
    final BigDecimal price = new BigDecimal(10);
    final int quantity = 10;
    final long orderId = 100L;
    final OrderBook expectedOrderBook = getBookForEnum(side);
    doNothing().when(expectedOrderBook).addOrder(any());

    // When
    level2View.onNewOrder(side, price, quantity, orderId);

    // Then
    verify(expectedOrderBook).addOrder(any(Order.class));
  }

  @Test
  @DisplayName("Then non existing orders throw InvalidOrderIdException")
  void onCancelOrderNonExistingOrder() {
    // Given
    final long orderId = 100L;
    doReturn(false).when(askBook).containsOrder(orderId);
    doReturn(false).when(bidBook).containsOrder(orderId);

    // Expect
    assertThrows(InvalidOrderException.class, () -> level2View.onCancelOrder(orderId));
  }

  @Test
  @DisplayName("Then pre-existent orders can be cancelled")
  void cancelOrderWhenFound() {
    // Given
    final long orderId = 100L;
    doReturn(true).when(bidBook).containsOrder(orderId);

    // When
    level2View.onCancelOrder(orderId);

    // Expect
    verify(bidBook).removeOrder(orderId);
  }

  @Test
  @DisplayName("Then existing orders can be replaced")
  void existingOrdersCanBeReplaced() {
    // Given
    final BigDecimal originalPrice = new BigDecimal(0);
    final Order oldOrder = new Order(0, 0, originalPrice);
    final BigDecimal updatedPrice = new BigDecimal(0);
    final Order updateOrder = new Order(0, 1, updatedPrice);

    doReturn(true).when(askBook).containsOrder(oldOrder.getId());
    doNothing().when(askBook).replaceOrder(any(Order.class));

    // When
    level2View.onReplaceOrder(updateOrder.getPrice(), updateOrder.getQuantity(), updateOrder.getId());

    // Then
    verify(askBook).containsOrder(updateOrder.getId());
    verify(askBook).replaceOrder(any(Order.class));
  }

  @Test
  @DisplayName("Then non-existing orders will throw exception when replaced")
  void replacedNonExistingOrdersThrowException() {
    // Given
    final BigDecimal originalPrice = new BigDecimal(0);
    final Order oldOrder = new Order(0, 0, originalPrice);
    final BigDecimal updatedPrice = new BigDecimal(0);
    final Order updateOrder = new Order(0, 1, updatedPrice);

    doReturn(false).when(askBook).containsOrder(oldOrder.getId());
    doReturn(false).when(bidBook).containsOrder(oldOrder.getId());

    // Expect
    assertThrows(InvalidOrderException.class,
        () -> level2View.onReplaceOrder(updateOrder.getPrice(), updateOrder.getQuantity(), updateOrder.getId()));
  }

  @Test
  @DisplayName("Then a resting order with higher quantity than the matched order will remain will have its value updated")
  void whenRestingHasHigherQuantityUpdateValue() {
    // Given
    final BigDecimal restingOrderPrice = new BigDecimal(0);
    final int originalRestingOrderQuantity = 100;
    final int tradedQuantity = 50;
    final long restingOrderId = 0;
    final Order restingOrder = new Order(restingOrderId, originalRestingOrderQuantity, restingOrderPrice);

    doReturn(true).when(askBook).containsOrder(restingOrderId);
    doReturn(restingOrder).when(askBook).getOrderById(restingOrderId);
    doNothing().when(askBook).replaceOrder(any());

    // When
    level2View.onTrade(tradedQuantity, restingOrderId);

    // Then
    verify(askBook).replaceOrder(any());
  }

  @Test
  @DisplayName("Then a resting order with equal quantity than the matched order will be removed from order book")
  void whenRestingHasEqualQuantityThenRemove() {
    // Given
    final BigDecimal restingOrderPrice = new BigDecimal(0);
    final int originalRestingOrderQuantity = 100;
    final int tradedQuantity = 100;
    final long restingOrderId = 0;
    final Order restingOrder = new Order(restingOrderId, originalRestingOrderQuantity, restingOrderPrice);

    doReturn(true).when(askBook).containsOrder(restingOrderId);
    doReturn(restingOrder).when(askBook).getOrderById(restingOrderId);
    doNothing().when(askBook).removeOrder(restingOrderId);

    // When
    level2View.onTrade(tradedQuantity, restingOrderId);

    // Then
    verify(askBook).removeOrder(restingOrderId);
  }

  @Test
  @DisplayName("Then a resting order with lower quantity throws an exception")
  void whenRestingHasLowerQuantityThrowException() {
    // Given
    final BigDecimal restingOrderPrice = new BigDecimal(0);
    final int originalRestingOrderQuantity = 50;
    final int tradedQuantity = 100;
    final long restingOrderId = 0;
    final Order restingOrder = new Order(restingOrderId, originalRestingOrderQuantity, restingOrderPrice);

    doReturn(true).when(askBook).containsOrder(restingOrderId);
    doReturn(restingOrder).when(askBook).getOrderById(restingOrderId);

    // Expect
    assertThrows(InvalidTradeException.class, () -> level2View.onTrade(tradedQuantity, restingOrderId));
  }

  @ParameterizedTest
  @EnumSource(Side.class)
  @DisplayName("Then price size can be correctly recovered")
  void getSizeForPriceLevel(Side side) {
    // Given
    final OrderBook expectedBook = getBookForEnum(side);
    final long expectedPriceLevelSize = 10;
    doReturn(expectedPriceLevelSize).when(expectedBook).getSizeForPriceLevel(any());

    // When
    final long priceLevelSize = level2View.getSizeForPriceLevel(side, new BigDecimal(100));

    // Then
    assertEquals(expectedPriceLevelSize, priceLevelSize);
  }

  @ParameterizedTest
  @EnumSource(Side.class)
  @DisplayName("Then ask/bid depth information can be correctly recovered")
  void getBookDepth(Side side) {
    // Given
    final OrderBook expectedBook = getBookForEnum(side);
    final long expectedDepth = 10;
    doReturn(expectedDepth).when(expectedBook).getBookDepth();

    // When
    final long depth = level2View.getBookDepth(side);

    // Then
    assertEquals(expectedDepth, depth);
  }

  @DisplayName("Then top of book can be retrieved")
  @ParameterizedTest
  @EnumSource(Side.class)
  void getTopOfBookWhenFound(Side side) {
    // Given
    final OrderBook expectedBook = getBookForEnum(side);
    final BigDecimal expectedPrice = new BigDecimal(13);
    final Order expectedOrder = new Order(13, 0, expectedPrice);
    doReturn(expectedOrder).when(expectedBook).getTopOrder();

    // When
    BigDecimal topOrderPrice = level2View.getTopOfBook(side);

    // Then
    assertEquals(expectedPrice, topOrderPrice);
  }

  @DisplayName("Then empty books throw exception")
  @ParameterizedTest
  @EnumSource(Side.class)
  void getTopOfBookWhenEmptyThrowsException(Side side) {
    // Given
    final OrderBook expectedBook = getBookForEnum(side);
    doThrow(new EmptyOrderBookException("No order")).when(expectedBook).getTopOrder();

    // Expect
    assertThrows(EmptyOrderBookException.class, () -> level2View.getTopOfBook(side));
  }

  private OrderBook getBookForEnum(Side side) {
    return side.equals(ASK) ? askBook : bidBook;
  }
}