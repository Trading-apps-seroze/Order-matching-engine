package com.orderbookengine.api.dto;

import java.util.List;

/**
 * A single frame pushed over the market-data WebSocket: the current aggregated
 * book plus any trades produced by the event that triggered this frame (empty on
 * the initial snapshot, a cancel, or an in-place modify).
 */
public class MarketDataMessage {

    private final OrderBookResponse book;
    private final List<TradeResponse> trades;

    public MarketDataMessage(OrderBookResponse book, List<TradeResponse> trades) {
        this.book = book;
        this.trades = trades;
    }

    public OrderBookResponse getBook() {
        return book;
    }

    public List<TradeResponse> getTrades() {
        return trades;
    }
}
