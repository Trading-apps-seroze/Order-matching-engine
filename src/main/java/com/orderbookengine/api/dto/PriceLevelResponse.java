package com.orderbookengine.api.dto;

import com.orderbookengine.engine.PriceLevel;

/** One aggregated price level in the book snapshot: price plus total resting size. */
public class PriceLevelResponse {

    private final long price;
    private final long quantity;

    private PriceLevelResponse(PriceLevel level) {
        this.price = level.getPrice();
        this.quantity = level.totalQuantity();
    }

    public static PriceLevelResponse from(PriceLevel level) {
        return new PriceLevelResponse(level);
    }

    public long getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }
}
