package com.orderbookengine.service;

import com.orderbookengine.model.Trade;

import java.util.List;

/**
 * Notified after every state-changing operation on the {@link OrderService} so
 * downstream consumers (e.g. a WebSocket feed) can push updates.
 *
 * <p>Keeping this as a plain interface lets the service layer stay free of any
 * web/Spring dependency — the API layer registers itself as a listener.
 */
public interface MarketDataListener {

    /**
     * @param newTrades trades produced by the operation that just completed
     *                  (empty for a cancel or an in-place modify)
     */
    void onMarketData(List<Trade> newTrades);
}
