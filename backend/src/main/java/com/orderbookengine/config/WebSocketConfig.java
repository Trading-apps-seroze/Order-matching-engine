package com.orderbookengine.config;

import com.orderbookengine.api.ws.MarketDataWebSocketHandler;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Exposes the market-data feed at {@code /ws/marketdata}. */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MarketDataWebSocketHandler marketDataHandler;

    public WebSocketConfig(MarketDataWebSocketHandler marketDataHandler) {
        this.marketDataHandler = marketDataHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketDataHandler, "/ws/marketdata")
                .setAllowedOriginPatterns("*");
    }
}
