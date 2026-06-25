package com.orderbookengine.engine;

import com.orderbookengine.model.Order;
import com.orderbookengine.model.OrderStatus;
import com.orderbookengine.model.OrderType;
import com.orderbookengine.model.Side;
import com.orderbookengine.model.Trade;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Drives matching against an {@link OrderBook} using price-time priority.
 *
 * <p>An incoming (taker) order is matched against the best prices on the
 * opposite side. Trades execute at the resting (maker) order's price. Any
 * unfilled remainder of a {@link OrderType#LIMIT} order rests on the book;
 * a {@link OrderType#MARKET} order's remainder is discarded.
 */
public class MatchingEngine {

    private final OrderBook orderBook;
    private final AtomicLong tradeIdSeq = new AtomicLong(1);

    public MatchingEngine() {
        this(new OrderBook());
    }

    public MatchingEngine(OrderBook orderBook) {
        this.orderBook = orderBook;
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

    /**
     * Processes an incoming order, generating any trades and resting the
     * remainder if appropriate.
     *
     * @return the trades produced, in execution order (possibly empty)
     */
    public List<Trade> process(Order taker){
        List<Trade> trades = new ArrayList<>();

        // FOK is all-or-nothing: if the book cannot fill it in full right now,
        // kill the whole order without trading.
        if (taker.getType() == OrderType.FOK && !canFullyFill(taker)) {
            taker.setStatus(OrderStatus.CANCELLED);
            return trades;
        }

        while (taker.getRemainingQty()>0){
            PriceLevel bestLevel = taker.getSide() == Side.BUY
                    ? orderBook.bestAskLevel()
                    : orderBook.bestBidLevel();

            if (bestLevel == null || !crosses(taker, bestLevel.getPrice())) break;

            Order maker = bestLevel.peek();
            long fillQty = Math.min(taker.getRemainingQty(), maker.getRemainingQty());

            taker.setRemainingQty(taker.getRemainingQty() - fillQty);
            maker.setRemainingQty(maker.getRemainingQty() - fillQty);

            trades.add(buildTrade(taker, maker, fillQty, maker.getPrice()));
            updateStatus(maker);
            if (maker.isFilled()) orderBook.removeOrder(maker);
        }

        updateStatus(taker);
        if (taker.getRemainingQty()>0) {
            if (taker.getType() == OrderType.LIMIT) {
                // Only a LIMIT order rests; every other type discards its
                // remainder.
                orderBook.addOrder(taker);
            } else if (taker.getType() == OrderType.IOC
                    && taker.getRemainingQty() == taker.getOriginalQty()) {
                // An IOC that matched nothing is cancelled outright; a partially
                // filled IOC keeps its PARTIALLY_FILLED status from updateStatus.
                taker.setStatus(OrderStatus.CANCELLED);
            }
        }
        return trades;
    }

    /**
     * Whether the opposite side holds enough liquidity, at prices this taker
     * crosses, to fill it completely. Used to decide an FOK order up front
     * without mutating the book.
     */
    private boolean canFullyFill(Order taker) {
        Side oppositeSide = taker.getSide() == Side.BUY ? Side.SELL : Side.BUY;
        long needed = taker.getRemainingQty();
        long available = 0;
        for (PriceLevel level : orderBook.topLevels(oppositeSide, Integer.MAX_VALUE)) {
            if (!crosses(taker, level.getPrice())) {
                break;
            }
            available += level.totalQuantity();
            if (available >= needed) {
                return true;
            }
        }
        return false;
    }

    /** Whether a taker is willing to trade against a resting price. */
    private boolean crosses(Order taker, long restingPrice) {
        if (taker.getType() == OrderType.MARKET) {
            return true;
        }
        return taker.getSide() == Side.BUY
                ? taker.getPrice() >= restingPrice
                : taker.getPrice() <= restingPrice;
    }

    private Trade buildTrade(Order taker, Order maker, long quantity, long price) {
        long buyOrderId = taker.getSide() == Side.BUY ? taker.getOrderId() : maker.getOrderId();
        long sellOrderId = taker.getSide() == Side.BUY ? maker.getOrderId() : taker.getOrderId();
        return new Trade(tradeIdSeq.getAndIncrement(), buyOrderId, sellOrderId, quantity, price);
    }

    private void updateStatus(Order order) {
        if (order.isFilled()) {
            order.setStatus(OrderStatus.FILLED);
        } else if (order.getRemainingQty() < order.getOriginalQty()) {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }
}
