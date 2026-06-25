package com.orderbookengine.api.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderbookengine.api.dto.MarketDataMessage;
import com.orderbookengine.api.dto.OrderBookResponse;
import com.orderbookengine.api.dto.TradeResponse;
import com.orderbookengine.engine.OrderBook;
import com.orderbookengine.model.Trade;
import com.orderbookengine.service.MarketDataListener;
import com.orderbookengine.service.OrderService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Pushes the order book and trades to connected browsers.
 *
 * <p>It registers itself as a {@link MarketDataListener} on the
 * {@link OrderService}; every mutation triggers {@link #onMarketData} on the
 * matching thread, which serializes a frame and fans it out to all sessions. A
 * newly connected client first receives a full snapshot so it can render
 * immediately.
 */
@Component
public class MarketDataWebSocketHandler extends TextWebSocketHandler implements MarketDataListener {

    private static final Logger log = LoggerFactory.getLogger(MarketDataWebSocketHandler.class);
    private static final int DEPTH = 10;

    private final OrderBook orderBook;
    private final OrderService orderService;
    private final ObjectMapper mapper;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public MarketDataWebSocketHandler(OrderBook orderBook, OrderService orderService,
                                      ObjectMapper mapper) {
        this.orderBook = orderBook;
        this.orderService = orderService;
        this.mapper = mapper;
        orderService.addListener(this);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        // Initial snapshot: current book plus the full trade history so the
        // client starts in sync.
        send(session, buildMessage(orderService.getTrades()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    /** Called by the service after each mutation (on the matching thread). */
    @Override
    public void onMarketData(List<Trade> newTrades) {
        String frame = serialize(buildMessage(newTrades));
        if (frame == null) {
            return;
        }
        TextMessage message = new TextMessage(frame);
        for (WebSocketSession session : sessions) {
            send(session, message);
        }
    }

    private MarketDataMessage buildMessage(List<Trade> trades) {
        List<TradeResponse> tradeDtos = trades.stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
        return new MarketDataMessage(OrderBookResponse.snapshot(orderBook, DEPTH), tradeDtos);
    }

    private void send(WebSocketSession session, MarketDataMessage message) {
        String frame = serialize(message);
        if (frame != null) {
            send(session, new TextMessage(frame));
        }
    }

    private void send(WebSocketSession session, TextMessage message) {
        try {
            // WebSocketSession is not safe for concurrent sends, so guard per
            // session (the initial snapshot and a broadcast can race).
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.warn("dropping market-data session {}: {}", session.getId(), e.getMessage());
            sessions.remove(session);
        }
    }

    private String serialize(MarketDataMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("failed to serialize market-data frame", e);
            return null;
        }
    }
}
