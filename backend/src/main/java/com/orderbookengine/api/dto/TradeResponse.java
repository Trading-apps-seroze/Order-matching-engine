package com.orderbookengine.api.dto;

import com.orderbookengine.model.Trade;

/** Outgoing view of a {@link Trade}. */
public class TradeResponse {

    private final long tradeId;
    private final long buyOrderId;
    private final long sellOrderId;
    private final long quantity;
    private final long price;

    private TradeResponse(Trade trade) {
        this.tradeId = trade.getTradeId();
        this.buyOrderId = trade.getBuyOrderId();
        this.sellOrderId = trade.getSellOrderId();
        this.quantity = trade.getQuantity();
        this.price = trade.getPrice();
    }

    public static TradeResponse from(Trade trade) {
        return new TradeResponse(trade);
    }

    public long getTradeId() {
        return tradeId;
    }

    public long getBuyOrderId() {
        return buyOrderId;
    }

    public long getSellOrderId() {
        return sellOrderId;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getPrice() {
        return price;
    }
}
