package com.orderbookengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.orderbookengine.engine.MatchingEngine;
import com.orderbookengine.engine.OrderBook;
import com.orderbookengine.model.Order;
import com.orderbookengine.model.OrderStatus;
import com.orderbookengine.model.OrderType;
import com.orderbookengine.model.Side;
import com.orderbookengine.model.Trade;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

public class MatchingEngineTest {

    private MatchingEngine engine;
    private OrderBook book;
    private final AtomicLong orderIdSeq = new AtomicLong(1);
    private final AtomicLong clock = new AtomicLong(1);

    @Before
    public void setUp() {
        book = new OrderBook();
        engine = new MatchingEngine(book);
    }

    /** Creates a LIMIT order with auto-assigned id and increasing timestamp. */
    private Order limit(Side side, long price, long qty) {
        return new Order(orderIdSeq.getAndIncrement(), side, OrderType.LIMIT, price, qty,
                clock.getAndIncrement());
    }

    /** Creates a MARKET order with auto-assigned id and increasing timestamp. */
    private Order market(Side side, long qty) {
        return new Order(orderIdSeq.getAndIncrement(), side, OrderType.MARKET, 0, qty,
                clock.getAndIncrement());
    }

    /** Creates a limit-priced IOC order. */
    private Order ioc(Side side, long price, long qty) {
        return new Order(orderIdSeq.getAndIncrement(), side, OrderType.IOC, price, qty,
                clock.getAndIncrement());
    }

    /** Creates a limit-priced FOK order. */
    private Order fok(Side side, long price, long qty) {
        return new Order(orderIdSeq.getAndIncrement(), side, OrderType.FOK, price, qty,
                clock.getAndIncrement());
    }

    private Order rest(Order order) {
        // No counter-side liquidity, so process simply rests a LIMIT order.
        List<Trade> trades = engine.process(order);
        assertTrue("expected order to rest without trading", trades.isEmpty());
        return order;
    }

    @Test
    public void limitOrderWithNoMatchRestsOnBook() {
        Order buy = limit(Side.BUY, 100, 50);

        List<Trade> trades = engine.process(buy);

        assertTrue(trades.isEmpty());
        assertEquals(OrderStatus.NEW, buy.getStatus());
        assertEquals(50, buy.getRemainingQty());
        assertEquals(buy, book.bestBid());
    }

    @Test
    public void fullyCrossingLimitOrderTradesAtMakerPrice() {
        Order resting = rest(limit(Side.SELL, 100, 50));

        Order taker = limit(Side.BUY, 101, 50);
        List<Trade> trades = engine.process(taker);

        assertEquals(1, trades.size());
        Trade trade = trades.get(0);
        assertEquals(50, trade.getQuantity());
        assertEquals("trade executes at resting maker price", 100, trade.getPrice());
        assertEquals(taker.getOrderId(), trade.getBuyOrderId());
        assertEquals(resting.getOrderId(), trade.getSellOrderId());

        assertEquals(OrderStatus.FILLED, taker.getStatus());
        assertEquals(OrderStatus.FILLED, resting.getStatus());
        assertTrue("filled maker leaves the book", book.isEmpty());
    }

    @Test
    public void partialFillRestsTakerRemainder() {
        rest(limit(Side.SELL, 100, 30));

        Order taker = limit(Side.BUY, 100, 50);
        List<Trade> trades = engine.process(taker);

        assertEquals(1, trades.size());
        assertEquals(30, trades.get(0).getQuantity());
        assertEquals(20, taker.getRemainingQty());
        assertEquals(OrderStatus.PARTIALLY_FILLED, taker.getStatus());
        assertEquals("remainder rests as a bid", taker, book.bestBid());
        assertNull(book.bestAsk());
    }

    @Test
    public void partialFillLeavesMakerRestingPartiallyFilled() {
        Order maker = rest(limit(Side.SELL, 100, 80));

        Order taker = limit(Side.BUY, 100, 30);
        engine.process(taker);

        assertEquals(50, maker.getRemainingQty());
        assertEquals(OrderStatus.PARTIALLY_FILLED, maker.getStatus());
        assertEquals("maker stays on the book", maker, book.bestAsk());
    }

    @Test
    public void takerSweepsMultiplePriceLevels() {
        rest(limit(Side.SELL, 100, 30));
        rest(limit(Side.SELL, 101, 30));

        Order taker = limit(Side.BUY, 101, 50);
        List<Trade> trades = engine.process(taker);

        assertEquals(2, trades.size());
        // Best price filled first.
        assertEquals(100, trades.get(0).getPrice());
        assertEquals(30, trades.get(0).getQuantity());
        assertEquals(101, trades.get(1).getPrice());
        assertEquals(20, trades.get(1).getQuantity());
        assertEquals(OrderStatus.FILLED, taker.getStatus());
    }

    @Test
    public void timePriorityFillsOldestOrderFirstAtSamePrice() {
        Order older = rest(limit(Side.SELL, 100, 30));
        Order newer = rest(limit(Side.SELL, 100, 30));

        Order taker = limit(Side.BUY, 100, 30);
        List<Trade> trades = engine.process(taker);

        assertEquals(1, trades.size());
        assertEquals("older resting order trades first", older.getOrderId(),
                trades.get(0).getSellOrderId());
        assertEquals(OrderStatus.FILLED, older.getStatus());
        assertEquals(OrderStatus.NEW, newer.getStatus());
        assertEquals(newer, book.bestAsk());
    }

    @Test
    public void limitBuyDoesNotCrossAboveItsPrice() {
        rest(limit(Side.SELL, 101, 50));

        Order taker = limit(Side.BUY, 100, 50);
        List<Trade> trades = engine.process(taker);

        assertTrue("price does not cross, so no trade", trades.isEmpty());
        assertEquals(taker, book.bestBid());
        assertEquals(50, taker.getRemainingQty());
    }

    @Test
    public void marketBuyTradesAcrossLevelsAndDiscardsRemainder() {
        rest(limit(Side.SELL, 100, 20));
        rest(limit(Side.SELL, 105, 20));

        Order taker = market(Side.BUY, 60);
        List<Trade> trades = engine.process(taker);

        assertEquals(2, trades.size());
        assertEquals(100, trades.get(0).getPrice());
        assertEquals(105, trades.get(1).getPrice());
        // 20 unfilled, but a MARKET order never rests.
        assertEquals(20, taker.getRemainingQty());
        assertEquals(OrderStatus.PARTIALLY_FILLED, taker.getStatus());
        assertNull("market remainder is discarded, not rested", book.bestBid());
        assertTrue(book.isEmpty());
    }

    @Test
    public void marketSellIgnoresPriceAndHitsBestBid() {
        rest(limit(Side.BUY, 100, 40));
        rest(limit(Side.BUY, 99, 40));

        Order taker = market(Side.SELL, 50);
        List<Trade> trades = engine.process(taker);

        assertEquals(2, trades.size());
        assertEquals("best bid filled first", 100, trades.get(0).getPrice());
        assertEquals(40, trades.get(0).getQuantity());
        assertEquals(99, trades.get(1).getPrice());
        assertEquals(10, trades.get(1).getQuantity());
        assertEquals(taker.getOrderId(), trades.get(0).getSellOrderId());
        assertEquals(OrderStatus.FILLED, taker.getStatus());
    }

    @Test
    public void marketOrderWithEmptyBookProducesNoTradeAndDoesNotRest() {
        Order taker = market(Side.BUY, 50);
        List<Trade> trades = engine.process(taker);

        assertTrue(trades.isEmpty());
        assertEquals(50, taker.getRemainingQty());
        assertEquals(OrderStatus.NEW, taker.getStatus());
        assertTrue(book.isEmpty());
    }

    @Test
    public void iocPartiallyFillsThenCancelsRemainder() {
        rest(limit(Side.SELL, 100, 30));

        Order taker = ioc(Side.BUY, 100, 50);
        List<Trade> trades = engine.process(taker);

        assertEquals(1, trades.size());
        assertEquals(30, trades.get(0).getQuantity());
        assertEquals(20, taker.getRemainingQty());
        assertEquals(OrderStatus.PARTIALLY_FILLED, taker.getStatus());
        assertNull("IOC remainder is cancelled, never rested", book.bestBid());
    }

    @Test
    public void iocFillsFullyWhenLiquidityAvailable() {
        rest(limit(Side.SELL, 100, 50));

        Order taker = ioc(Side.BUY, 100, 50);
        List<Trade> trades = engine.process(taker);

        assertEquals(1, trades.size());
        assertEquals(0, taker.getRemainingQty());
        assertEquals(OrderStatus.FILLED, taker.getStatus());
        assertTrue(book.isEmpty());
    }

    @Test
    public void iocRespectsLimitPriceAndCancelsWhenNoCross() {
        rest(limit(Side.SELL, 101, 50));

        Order taker = ioc(Side.BUY, 100, 50);
        List<Trade> trades = engine.process(taker);

        assertTrue("price does not cross, so nothing trades", trades.isEmpty());
        assertEquals(50, taker.getRemainingQty());
        assertEquals(OrderStatus.CANCELLED, taker.getStatus());
        assertNull("IOC never rests on the book", book.bestBid());
    }

    @Test
    public void fokFillsFullyWhenEnoughLiquidityAcrossLevels() {
        rest(limit(Side.SELL, 100, 30));
        rest(limit(Side.SELL, 101, 30));

        Order taker = fok(Side.BUY, 101, 50);
        List<Trade> trades = engine.process(taker);

        assertEquals(2, trades.size());
        assertEquals(0, taker.getRemainingQty());
        assertEquals(OrderStatus.FILLED, taker.getStatus());
        // 30 @100 fully consumed, 20 of 30 @101 consumed, 10 left resting.
        assertEquals(10, book.bestAsk().getRemainingQty());
    }

    @Test
    public void fokIsKilledWhenItCannotFullyFill() {
        Order maker = rest(limit(Side.SELL, 100, 30));

        Order taker = fok(Side.BUY, 100, 50);
        List<Trade> trades = engine.process(taker);

        assertTrue("FOK that cannot fully fill trades nothing", trades.isEmpty());
        assertEquals(50, taker.getRemainingQty());
        assertEquals(OrderStatus.CANCELLED, taker.getStatus());
        // The book is left completely untouched.
        assertEquals(30, maker.getRemainingQty());
        assertEquals(OrderStatus.NEW, maker.getStatus());
        assertEquals(maker, book.bestAsk());
    }

    @Test
    public void fokIgnoresLiquidityThatDoesNotCross() {
        // 30 crosses at 100, another 30 sits at 101 which the FOK won't pay.
        rest(limit(Side.SELL, 100, 30));
        rest(limit(Side.SELL, 101, 30));

        Order taker = fok(Side.BUY, 100, 50);
        List<Trade> trades = engine.process(taker);

        assertTrue("only 30 crosses, so 50 cannot fully fill", trades.isEmpty());
        assertEquals(OrderStatus.CANCELLED, taker.getStatus());
        assertEquals("crossing level untouched", 30, book.bestAsk().getRemainingQty());
    }

    @Test
    public void fokOnEmptyBookIsKilled() {
        Order taker = fok(Side.BUY, 100, 10);
        List<Trade> trades = engine.process(taker);

        assertTrue(trades.isEmpty());
        assertEquals(OrderStatus.CANCELLED, taker.getStatus());
        assertTrue(book.isEmpty());
    }

    @Test
    public void tradeIdsAreSequential() {
        rest(limit(Side.SELL, 100, 10));
        rest(limit(Side.SELL, 101, 10));

        Order taker = limit(Side.BUY, 101, 20);
        List<Trade> trades = engine.process(taker);

        assertEquals(2, trades.size());
        assertEquals(1, trades.get(0).getTradeId());
        assertEquals(2, trades.get(1).getTradeId());
    }
}
