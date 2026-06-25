package com.orderbookengine.api.dto;

import com.orderbookengine.service.OrderResult;

import java.util.List;
import java.util.stream.Collectors;

/** Response for {@code POST /orders} and {@code PUT /orders/{id}}. */
public class SubmitOrderResponse {

    private final OrderResponse order;
    private final List<TradeResponse> trades;

    private SubmitOrderResponse(OrderResult result) {
        this.order = OrderResponse.from(result.getOrder());
        this.trades = result.getTrades().stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
    }

    public static SubmitOrderResponse from(OrderResult result) {
        return new SubmitOrderResponse(result);
    }

    public OrderResponse getOrder() {
        return order;
    }

    public List<TradeResponse> getTrades() {
        return trades;
    }
}
