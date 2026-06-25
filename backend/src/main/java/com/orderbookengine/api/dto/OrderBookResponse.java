package com.orderbookengine.api.dto;

import com.orderbookengine.engine.OrderBook;
import com.orderbookengine.engine.PriceLevel;
import com.orderbookengine.model.Side;

import java.util.List;
import java.util.stream.Collectors;

/** Aggregated snapshot of both sides of the book, best price first. */
public class OrderBookResponse {

    private final List<PriceLevelResponse> bids;
    private final List<PriceLevelResponse> asks;

    private OrderBookResponse(List<PriceLevelResponse> bids, List<PriceLevelResponse> asks) {
        this.bids = bids;
        this.asks = asks;
    }

    public static OrderBookResponse snapshot(OrderBook book, int depth) {
        return new OrderBookResponse(levels(book, Side.BUY, depth), levels(book, Side.SELL, depth));
    }

    private static List<PriceLevelResponse> levels(OrderBook book, Side side, int depth) {
        List<PriceLevel> levels = book.topLevels(side, depth);
        return levels.stream().map(PriceLevelResponse::from).collect(Collectors.toList());
    }

    public List<PriceLevelResponse> getBids() {
        return bids;
    }

    public List<PriceLevelResponse> getAsks() {
        return asks;
    }
}
