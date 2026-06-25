package com.orderbookengine.engine;

import com.orderbookengine.model.Order;
import com.orderbookengine.model.Side;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Holds the resting orders for a single instrument, split into two sides.
 *
 * <p>Bids are kept in descending price order and asks in ascending price order,
 * so in both maps the first entry is the most aggressive (best) price.
 */
public class OrderBook {

    private final NavigableMap<Long, PriceLevel> bids;
    private final NavigableMap<Long, PriceLevel> asks;

    /** Index of every currently resting order by id, for O(1) cancel/lookup. */
    private final Map<Long, Order> ordersById = new HashMap<>();

    /** Creates an empty book with the conventional price orderings. */
    public OrderBook() {
        this(new TreeMap<>(Comparator.reverseOrder()), new TreeMap<>());
    }

    public OrderBook(NavigableMap<Long, PriceLevel> bids, NavigableMap<Long, PriceLevel> asks) {
        this.bids = bids;
        this.asks = asks;
    }

    public void addOrder(Order order) {
        NavigableMap<Long, PriceLevel> book = bookFor(order.getSide());
        PriceLevel level = book.computeIfAbsent(order.getPrice(), PriceLevel::new);
        level.addOrder(order);
        ordersById.put(order.getOrderId(), order);
    }

    public void removeOrder(Order order) {
        ordersById.remove(order.getOrderId());
        NavigableMap<Long, PriceLevel> book = bookFor(order.getSide());
        PriceLevel level = book.get(order.getPrice());
        if (level == null) {
            return;
        }
        level.remove(order);
        if (level.isEmpty()) {
            book.remove(order.getPrice());
        }
    }

    /** The resting order with this id, or {@code null} if none is on the book. */
    public Order getOrder(long orderId) {
        return ordersById.get(orderId);
    }

    /** The best (highest priced, oldest) resting buy order, or {@code null}. */
    public Order bestBid() {
        PriceLevel level = bestBidLevel();
        return level == null ? null : level.peek();
    }

    /** The best (lowest priced, oldest) resting sell order, or {@code null}. */
    public Order bestAsk() {
        PriceLevel level = bestAskLevel();
        return level == null ? null : level.peek();
    }

    PriceLevel bestBidLevel() {
        Map.Entry<Long, PriceLevel> entry = bids.firstEntry();
        return entry == null ? null : entry.getValue();
    }

    PriceLevel bestAskLevel() {
        Map.Entry<Long, PriceLevel> entry = asks.firstEntry();
        return entry == null ? null : entry.getValue();
    }

    /**
     * Returns up to {@code depth} price levels for the given side, ordered from
     * the best price outward.
     */
    public List<PriceLevel> topLevels(Side side, int depth) {
        List<PriceLevel> levels = new ArrayList<>();
        for (PriceLevel level : bookFor(side).values()) {
            if (levels.size() >= depth) {
                break;
            }
            levels.add(level);
        }
        return levels;
    }

    public boolean isEmpty() {
        return bids.isEmpty() && asks.isEmpty();
    }

    private NavigableMap<Long, PriceLevel> bookFor(Side side) {
        return side == Side.BUY ? bids : asks;
    }
}
