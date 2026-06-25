package com.orderbookengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.orderbookengine.engine.MatchingEngine;
import com.orderbookengine.engine.OrderBook;
import com.orderbookengine.model.Order;
import com.orderbookengine.model.OrderStatus;
import com.orderbookengine.model.OrderType;
import com.orderbookengine.model.Side;
import com.orderbookengine.service.OrderResult;
import com.orderbookengine.service.OrderService;

import org.junit.Before;
import org.junit.Test;

public class OrderServiceTest {

    private OrderBook book;
    private OrderService service;

    @Before
    public void setUp() {
        book = new OrderBook();
        service = new OrderService(new MatchingEngine(book));
    }

    private OrderResult submitLimit(Side side, long price, long qty) {
        return service.submit(side, OrderType.LIMIT, price, qty);
    }

    // --- submit ---

    @Test
    public void submitRestsUnmatchedOrderAndIndexesIt() {
        OrderResult result = submitLimit(Side.BUY, 100, 50);

        assertTrue(result.getTrades().isEmpty());
        assertEquals(OrderStatus.NEW, result.getOrder().getStatus());
        assertEquals(result.getOrder(), book.getOrder(result.getOrder().getOrderId()));
    }

    @Test
    public void submitMatchesAgainstRestingLiquidity() {
        submitLimit(Side.SELL, 100, 50);

        OrderResult result = submitLimit(Side.BUY, 100, 50);

        assertEquals(1, result.getTrades().size());
        assertEquals(OrderStatus.FILLED, result.getOrder().getStatus());
        assertTrue(book.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void submitRejectsNonPositiveQuantity() {
        submitLimit(Side.BUY, 100, 0);
    }

    // --- cancel ---

    @Test
    public void cancelRemovesRestingOrder() {
        long id = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();

        boolean cancelled = service.cancel(id);

        assertTrue(cancelled);
        assertNull(book.getOrder(id));
        assertNull(book.bestBid());
    }

    @Test
    public void cancelUnknownOrderReturnsFalse() {
        assertFalse(service.cancel(999));
    }

    @Test
    public void cancelAlreadyFilledOrderReturnsFalse() {
        submitLimit(Side.SELL, 100, 50);
        long takerId = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();

        // Taker fully filled, so it never rested and cannot be cancelled.
        assertFalse(service.cancel(takerId));
    }

    // --- modify ---

    @Test
    public void modifyQuantityDownInPlaceKeepsTimePriority() {
        long firstId = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();
        Order second = submitLimit(Side.BUY, 100, 40).getOrder();

        // Shrink the first (older) order; it should keep its place at the head.
        OrderResult result = service.modify(firstId, 30, 100);

        assertTrue("in-place edit produces no trade", result.getTrades().isEmpty());
        assertEquals(firstId, result.getOrder().getOrderId());
        assertEquals(30, book.getOrder(firstId).getRemainingQty());

        // A SELL 30 @100 should hit the still-first order, proving priority held.
        OrderResult sell = service.submit(Side.SELL, OrderType.LIMIT, 100, 30);
        assertEquals(1, sell.getTrades().size());
        assertEquals(firstId, sell.getTrades().get(0).getBuyOrderId());
        assertEquals(second, book.bestBid());
    }

    @Test
    public void modifyPriceLosesTimePriority() {
        long firstId = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();
        long secondId = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();

        // Re-price the first order up then back to 100: it goes to the back.
        service.modify(firstId, 50, 101);
        service.modify(firstId, 50, 100);

        // The second order is now ahead at price 100.
        OrderResult sell = service.submit(Side.SELL, OrderType.LIMIT, 100, 50);
        assertEquals(secondId, sell.getTrades().get(0).getBuyOrderId());
    }

    @Test
    public void modifyQuantityUpLosesTimePriority() {
        long firstId = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();
        long secondId = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();

        // Increasing size sends the first order to the back of the queue.
        service.modify(firstId, 80, 100);

        OrderResult sell = service.submit(Side.SELL, OrderType.LIMIT, 100, 50);
        assertEquals(secondId, sell.getTrades().get(0).getBuyOrderId());
    }

    @Test
    public void modifyRepricedOrderCanCrossAndTrade() {
        submitLimit(Side.SELL, 101, 50);
        long buyId = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();

        // Lift the bid to 101 so it now crosses the resting ask.
        OrderResult result = service.modify(buyId, 50, 101);

        assertEquals(1, result.getTrades().size());
        assertEquals(101, result.getTrades().get(0).getPrice());
        assertEquals(OrderStatus.FILLED, result.getOrder().getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void modifyUnknownOrderThrows() {
        service.modify(999, 10, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void modifyRejectsNonPositiveQuantity() {
        long id = submitLimit(Side.BUY, 100, 50).getOrder().getOrderId();
        service.modify(id, 0, 100);
    }
}
