package com.orderbookengine.service;

import com.orderbookengine.engine.MatchingEngine;
import com.orderbookengine.model.Order;
import com.orderbookengine.model.OrderStatus;
import com.orderbookengine.model.OrderType;
import com.orderbookengine.model.Side;
import com.orderbookengine.model.Trade;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application-level entry point for order operations, sitting between the
 * outward-facing API and the {@link MatchingEngine}.
 *
 * <p>The service owns order-id and timestamp generation so it is the single
 * authority on time priority: every order it creates gets a strictly increasing
 * timestamp, which is what makes "lose your place in the queue" meaningful.
 *
 * <p>Dependency direction: {@code api -> service -> engine -> model}.
 */
public class OrderService {

    private final MatchingEngine engine;
    private final AtomicLong orderIdSeq = new AtomicLong(1);
    private final AtomicLong clock = new AtomicLong(1);

    public OrderService(MatchingEngine engine) {
        this.engine = engine;
    }

    /**
     * Creates and processes a new order, matching it against the book and
     * resting any remainder if its type allows.
     */
    public OrderResult submit(Side side, OrderType type, long price, long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        Order order = new Order(orderIdSeq.getAndIncrement(), side, type, price, quantity,
                clock.getAndIncrement());
        List<Trade> trades = engine.process(order);
        return new OrderResult(order, trades);
    }

    /**
     * Cancels a resting order, removing it from the book.
     *
     * @return {@code true} if an order was resting and is now cancelled;
     *         {@code false} if no such order is on the book (already filled,
     *         already cancelled, or never rested)
     */
    public boolean cancel(long orderId) {
        Order resting = engine.getOrderBook().getOrder(orderId);
        if (resting == null) {
            return false;
        }
        engine.getOrderBook().removeOrder(resting);
        resting.setStatus(OrderStatus.CANCELLED);
        return true;
    }

    /**
     * Modifies a resting order. {@code newQuantity} is the desired remaining
     * (leaves) quantity.
     *
     * <p>Time priority is preserved only when the order is made strictly less
     * aggressive without changing price — i.e. a pure quantity reduction at the
     * same price is edited in place. Any price change, or any quantity increase,
     * is handled as cancel + new order and therefore loses queue position. See
     * {@code LEARNINGS.md} for the reasoning.
     *
     * @throws IllegalArgumentException if the order is not resting or the new
     *         quantity is not positive
     */
    public OrderResult modify(long orderId, long newQuantity, long newPrice) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException(
                    "new quantity must be positive (use cancel to remove): " + newQuantity);
        }
        Order existing = engine.getOrderBook().getOrder(orderId);
        if (existing == null) {
            throw new IllegalArgumentException("no resting order with id " + orderId);
        }

        boolean priceUnchanged = newPrice == existing.getPrice();
        boolean quantityReduced = newQuantity <= existing.getRemainingQty();
        if (priceUnchanged && quantityReduced) {
            // Strictly less aggressive at the same price: safe to edit in place,
            // keeping the order's place in the FIFO queue.
            existing.setRemainingQty(newQuantity);
            return new OrderResult(existing, Collections.emptyList());
        }

        // Price change or size increase: must go to the back of the queue. Reuse
        // the same id so callers keep addressing the order by its original id,
        // but stamp it with a fresh timestamp so it loses time priority.
        cancel(orderId);
        Order replacement = new Order(orderId, existing.getSide(), existing.getType(),
                newPrice, newQuantity, clock.getAndIncrement());
        List<Trade> trades = engine.process(replacement);
        return new OrderResult(replacement, trades);
    }
}
