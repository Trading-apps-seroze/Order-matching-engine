package com.orderbookengine.api.dto;

/**
 * Incoming JSON body for {@code PUT /orders/{id}}. {@code quantity} is the new
 * remaining (leaves) quantity; {@code price} is the new price.
 */
public class ModifyOrderRequest {

    private long quantity;
    private long price;

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }
}
