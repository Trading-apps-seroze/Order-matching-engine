package com.orderbookengine.api.dto;

import com.orderbookengine.model.Order;
import com.orderbookengine.model.OrderStatus;
import com.orderbookengine.model.OrderType;
import com.orderbookengine.model.Side;

/** Outgoing view of an {@link Order}. */
public class OrderResponse {

    private final long orderId;
    private final Side side;
    private final OrderType type;
    private final long price;
    private final long originalQuantity;
    private final long remainingQuantity;
    private final OrderStatus status;

    private OrderResponse(Order order) {
        this.orderId = order.getOrderId();
        this.side = order.getSide();
        this.type = order.getType();
        this.price = order.getPrice();
        this.originalQuantity = order.getOriginalQty();
        this.remainingQuantity = order.getRemainingQty();
        this.status = order.getStatus();
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(order);
    }

    public long getOrderId() {
        return orderId;
    }

    public Side getSide() {
        return side;
    }

    public OrderType getType() {
        return type;
    }

    public long getPrice() {
        return price;
    }

    public long getOriginalQuantity() {
        return originalQuantity;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
