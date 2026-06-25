package com.orderbookengine.model;

public class Trade {

    private final long tradeId;
    private final long buyOrderId;
    private final long sellOrderId;
    private final long quantity;
    private final long price;

    public Trade(long tradeId, long buyOrderId, long sellOrderId, long quantity, long price) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.quantity = quantity;
        this.price = price;
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
