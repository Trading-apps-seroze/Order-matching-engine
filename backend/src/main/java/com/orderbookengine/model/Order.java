package com.orderbookengine.model;

// These should be dumb data holders
public class Order {
    private final long orderId;
    private final Side side;
    private final OrderType type;
    private final long price;
    private final long originalQty;
    private long remainingQty;
    private OrderStatus status;
    private final long timestamp;

    public Order(long orderId, Side side, OrderType type, long price, long originalQty, long timestamp) {
        this.orderId = orderId;
        this.side = side;
        this.type = type;
        this.price = price;
        this.originalQty = originalQty;
        this.remainingQty = originalQty;
        this.status = OrderStatus.NEW;
        this.timestamp = timestamp;
    }

    /** Convenience constructor defaulting to a {@link OrderType#LIMIT} order. */
    public Order(long orderId, Side side, long price, long originalQty, long timestamp) {
        this(orderId, side, OrderType.LIMIT, price, originalQty, timestamp);
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

    public long getOrderId() {
        return orderId;
    }

    public long getOriginalQty() {
        return originalQty;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getRemainingQty() {
        return remainingQty;
    }

    public void setRemainingQty(long remainingQty) {
        this.remainingQty = remainingQty;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public boolean isFilled() {
        return remainingQty == 0;
    }

    @Override
    public String toString() {
        return "Order{id=" + orderId + ", side=" + side + ", type=" + type
                + ", price=" + price + ", remaining=" + remainingQty
                + "/" + originalQty + ", status=" + status + "}";
    }
}
