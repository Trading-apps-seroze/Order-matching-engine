package com.orderbookengine.api.dto;

import com.orderbookengine.model.OrderType;
import com.orderbookengine.model.Side;

/**
 * Incoming JSON body for {@code POST /orders}. Jackson maps the {@code side} and
 * {@code type} strings straight onto the model enums.
 */
public class SubmitOrderRequest {

    private Side side;
    private OrderType type;
    private long price;
    private long quantity;

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }
}
