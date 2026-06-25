package com.orderbookengine.config;

import com.orderbookengine.engine.MatchingEngine;
import com.orderbookengine.engine.OrderBook;
import com.orderbookengine.service.OrderService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the single-instrument engine stack as Spring beans.
 *
 * <p>The same {@link OrderBook} instance backs the {@link MatchingEngine} and is
 * exposed on its own so read-only endpoints (e.g. the book snapshot) can query
 * it directly without going through the service.
 */
@Configuration
public class EngineConfig {

    @Bean
    public OrderBook orderBook() {
        return new OrderBook();
    }

    @Bean
    public MatchingEngine matchingEngine(OrderBook orderBook) {
        return new MatchingEngine(orderBook);
    }

    @Bean
    public OrderService orderService(MatchingEngine matchingEngine) {
        return new OrderService(matchingEngine);
    }
}
