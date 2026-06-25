package com.orderbookengine.api;

import com.orderbookengine.api.dto.OrderBookResponse;
import com.orderbookengine.api.dto.TradeResponse;
import com.orderbookengine.engine.OrderBook;
import com.orderbookengine.service.OrderService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only market-data endpoints: the book snapshot and the trade log. */
@RestController
public class MarketDataController {

    private final OrderBook orderBook;
    private final OrderService orderService;

    public MarketDataController(OrderBook orderBook, OrderService orderService) {
        this.orderBook = orderBook;
        this.orderService = orderService;
    }

    @GetMapping("/orderbook")
    public OrderBookResponse orderBook(@RequestParam(defaultValue = "10") int depth) {
        return OrderBookResponse.snapshot(orderBook, depth);
    }

    @GetMapping("/trades")
    public List<TradeResponse> trades() {
        return orderService.getTrades().stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
    }
}
