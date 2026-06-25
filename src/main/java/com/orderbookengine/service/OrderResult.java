package com.orderbookengine.service;

import com.orderbookengine.model.Order;
import com.orderbookengine.model.Trade;

import java.util.List;

/**
 * The outcome of submitting or modifying an order: the resulting order (with its
 * assigned id and final status) and any trades it generated.
 */
public class OrderResult {

    private final Order order;
    private final List<Trade> trades;

    public OrderResult(Order order, List<Trade> trades) {
        this.order = order;
        this.trades = trades;
    }

    public Order getOrder() {
        return order;
    }

    public List<Trade> getTrades() {
        return trades;
    }
}
