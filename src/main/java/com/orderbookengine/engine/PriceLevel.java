package com.orderbookengine.engine;

import com.orderbookengine.model.Order;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * All resting orders sitting at a single price, held in a FIFO queue so that
 * matching respects price-time priority (oldest order at the head trades first).
 */
public class PriceLevel {

    private final long price;
    private final Deque<Order> orders;

    public PriceLevel(long price) {
        this(price, new ArrayDeque<>());
    }

    public PriceLevel(long price, Deque<Order> orders) {
        this.price = price;
        this.orders = orders;
    }

    void addOrder(Order order) {
        orders.addLast(order);
    }

    Order peek() {
        return orders.peek();
    }

    Order poll() {
        return orders.poll();
    }

    boolean remove(Order order) {
        return orders.remove(order);
    }

    boolean isEmpty() {
        return orders.isEmpty();
    }

    int size() {
        return orders.size();
    }

    /** Sum of the remaining (unfilled) quantity of every order at this level. */
    long totalQuantity() {
        long total = 0;
        for (Order order : orders) {
            total += order.getRemainingQty();
        }
        return total;
    }

    public long getPrice() {
        return price;
    }
}
