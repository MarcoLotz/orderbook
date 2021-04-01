package com.marcolotz.orderbook.core.orderbook;

import com.marcolotz.orderbook.core.model.Order;
import com.marcolotz.orderbook.port.OrderBook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When adding orders to order book")
abstract class BaseOrderBookTest {

    OrderBook orderBook;

    @Test
    @DisplayName("Then orders can be added to the book")
    void addOrder() {
        // Given
        Order order = new Order(0, 10, new BigDecimal(0));

        // When
        orderBook.addOrder(order);

        // Then
        assertTrue(orderBook.containsOrder(order.getId()));
        assertEquals(order, orderBook.getTopOrder());
        assertEquals(1, orderBook.getBookDepth());
        assertEquals(order, orderBook.getOrderById(order.getId()));
        assertEquals(1, orderBook.getSizeForPriceLevel(order.getPrice()));
    }

    @Test
    @DisplayName("Then higher price orders can be added to the book")
    void addHigherPriceOrder() {
        // Given
        Order order1 = new Order(0, 10, new BigDecimal(0));
        Order order2 = new Order(1, 10, new BigDecimal(1));
        orderBook.addOrder(order1);

        // When
        orderBook.addOrder(order2);

        // Then
        assertTrue(orderBook.containsOrder(order2.getId()));
        assertEquals(order2, orderBook.getTopOrder());
        assertEquals(2, orderBook.getBookDepth());
        assertEquals(order2, orderBook.getOrderById(order2.getId()));
        assertEquals(order1, orderBook.getOrderById(order1.getId()));
        assertEquals(1, orderBook.getSizeForPriceLevel(order2.getPrice()));
    }

    @Test
    @DisplayName("Then orders without the top price won't change top price")
    void addNonTopPrice() {
        // Given
        Order order1 = new Order(0, 10, new BigDecimal(0));
        Order order2 = new Order(1, 10, new BigDecimal(1));
        Order order3 = new Order(2, 10, new BigDecimal(0));

        orderBook.addOrder(order1);
        orderBook.addOrder(order2);

        // When
        orderBook.addOrder(order3);

        // Then
        assertTrue(orderBook.containsOrder(order3.getId()));
        assertEquals(order2, orderBook.getTopOrder());
        assertEquals(2, orderBook.getBookDepth());
        assertEquals(order3, orderBook.getOrderById(order3.getId()));
        assertEquals(2, orderBook.getSizeForPriceLevel(order3.getPrice()));
    }

    @Test
    @DisplayName("Then no mutation is performed for non-existing order")
    void whenRemovingNotFoundOrderThenNoMutation() {
        // Given
        Order order = new Order(0, 10, new BigDecimal(0));
        orderBook.addOrder(order);
        // When
        assertDoesNotThrow(() -> orderBook.removeOrder(1));

        // Expect
        assertTrue(orderBook.containsOrder(order.getId()));
        assertEquals(order, orderBook.getTopOrder());
        assertEquals(1, orderBook.getBookDepth());
        assertEquals(order, orderBook.getOrderById(order.getId()));
        assertEquals(1, orderBook.getSizeForPriceLevel(order.getPrice()));
    }

    @Test
    @DisplayName("Then can empty a tree with a single order")
    void canRemoveSingleOrder() {
        // Given
        Order order = new Order(0, 10, new BigDecimal(0));
        orderBook.addOrder(order);

        // When
        orderBook.removeOrder(0);

        // Expect
        assertFalse(orderBook.containsOrder(order.getId()));
        assertNull(orderBook.getTopOrder());
        assertEquals(0, orderBook.getBookDepth());
        assertNull(orderBook.getOrderById(order.getId()));
        assertEquals(0, orderBook.getSizeForPriceLevel(order.getPrice()));
    }

    @Test
    @DisplayName("Then can remove order in the same price")
    void canRemoveSamePrice() {
        // Given
        Order order1 = new Order(0, 10, new BigDecimal(0));
        Order order2 = new Order(1, 10, new BigDecimal(0));
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);

        // When
        orderBook.removeOrder(1);

        // Expect
        assertTrue(orderBook.containsOrder(order1.getId()));
        assertFalse(orderBook.containsOrder(order2.getId()));
        assertEquals(order1, orderBook.getTopOrder());
        assertEquals(1, orderBook.getBookDepth());
        assertEquals(order1, orderBook.getOrderById(order1.getId()));
        assertNull(orderBook.getOrderById(order2.getId()));
        assertEquals(1, orderBook.getSizeForPriceLevel(order1.getPrice()));
    }

    @Test
    @DisplayName("Then can remove multiple orders with different prices")
    void canRemoveMultipleOrders() {
        // Given
        Order order1 = new Order(1, 10, new BigDecimal(0));
        Order order2 = new Order(2, 10, new BigDecimal(0));
        Order order3 = new Order(3, 10, new BigDecimal(2));
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);
        orderBook.addOrder(order3);

        // When
        orderBook.removeOrder(3);

        // Expect
        assertTrue(orderBook.containsOrder(order1.getId()));
        assertTrue(orderBook.containsOrder(order2.getId()));
        assertFalse(orderBook.containsOrder(order3.getId()));
        assertEquals(order1, orderBook.getTopOrder());
        assertEquals(1, orderBook.getBookDepth());
        assertEquals(order1, orderBook.getOrderById(order1.getId()));
        assertNull(orderBook.getOrderById(order3.getId()));
        assertEquals(2, orderBook.getSizeForPriceLevel(order1.getPrice()));
    }

    @Test
    @DisplayName("Then orders can be replaced")
    void canReplaceOrders() {
        // Given
        Order order = new Order(0, 10, new BigDecimal(0));
        Order order2 = new Order(order.getId(), order.getQuantity() + 10, order.getPrice().plus());
        orderBook.addOrder(order);

        // When
        orderBook.replaceOrder(order2);

        // Then
        assertTrue(orderBook.containsOrder(order2.getId()));
        assertEquals(order2, orderBook.getTopOrder());
        assertEquals(1, orderBook.getBookDepth());
        assertEquals(order2, orderBook.getOrderById(order.getId()));
        assertEquals(1, orderBook.getSizeForPriceLevel(order.getPrice()));
    }
}