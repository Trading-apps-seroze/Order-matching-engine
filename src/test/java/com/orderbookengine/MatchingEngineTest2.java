package com.orderbookengine;

import com.orderbookengine.engine.MatchingEngine;
import com.orderbookengine.engine.OrderBook;
import com.orderbookengine.model.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatchingEngineTest2 {

    private MatchingEngine engine;
    private OrderBook book;
    private final AtomicLong orderIdSeq = new AtomicLong(1);
    private final AtomicLong clock = new AtomicLong(1);

    @Before
    public void setUp() {
        book = new OrderBook();
        engine = new MatchingEngine(book);
    }

    // Creates a LIMIT order with auto-assigned id and increasing timestamp
    private Order limit(Side side, long price, long qty){
        return new Order(orderIdSeq.getAndIncrement(), side, OrderType.LIMIT,
                price, qty, clock.getAndIncrement());

    }

    // Creates a MARKET order with auto-assigned id and increasing timestamp
    private Order market(Side side, long qty){
        return new Order(orderIdSeq.getAndIncrement(), side, OrderType.MARKET,
                0, qty, clock.getAndIncrement());
    }

    private Order rest(Order order){
        // No counter-side liquidity, so procecss simply rests a LIMIT order.
        List<Trade> trades = engine.process(order);
        assertTrue("expected order to rest without trading", trades.isEmpty());
        return order;
    }

    @Test
    public void limitOrderWithNoMatchRestsOnBook(){
        Order buy = limit(Side.BUY, 100, 50);
        List<Trade> trades = engine.process(buy);

        assertTrue(trades.isEmpty());
        assertEquals(OrderStatus.NEW, buy.getStatus());
        assertEquals(50, buy.getRemainingQty());
        assertEquals(buy, book.bestBid());
    }

    @Test
    public void fullyCrossingLimitOrderTradesAtMakerPrice(){
        Order resting = rest(limit(Side.SELL, 100, 50));

        Order taker = limit(Side.BUY, 101, 50);
        List<Trade> trades = engine.process(taker);

        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(50, trade.getQuantity());
        assertEquals("trade executes at resting market price", 100, trade.getPrice());
        assertEquals(taker.getOrderId(), trade.getBuyOrderId());
        assertEquals(resting.getOrderId(), trade.getSellOrderId());

        assertEquals(OrderStatus.FILLED, taker.getStatus());
        assertEquals(OrderStatus.FILLED, resting.getStatus());
        assertTrue("filled maker leaves the book", book.isEmpty());
    }
}
