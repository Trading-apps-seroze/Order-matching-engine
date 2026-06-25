package com.orderbookengine.model;

/**
 * The execution style of an order.
 *
 * <ul>
 *   <li>{@link #LIMIT} - only trades at the order's price or better and rests
 *       on the book if it cannot be fully filled.</li>
 *   <li>{@link #MARKET} - trades against the best available prices regardless of
 *       price and never rests on the book.</li>
 *   <li>{@link #IOC} - immediate-or-cancel: a limit-priced order that fills as
 *       much as possible immediately, then cancels any remainder. Never rests.</li>
 *   <li>{@link #FOK} - fill-or-kill: a limit-priced order that must fill in full
 *       immediately or not trade at all. Never rests.</li>
 * </ul>
 */
public enum OrderType {
    LIMIT,
    MARKET,
    IOC,
    FOK
}
