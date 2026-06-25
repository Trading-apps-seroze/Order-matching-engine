package com.orderbookengine.model;

/**
 * Lifecycle state of an order as it is processed by the matching engine.
 */
public enum OrderStatus {
    /** Accepted but not yet matched against anything. */
    NEW,
    /** Some quantity has traded; the remainder is still live. */
    PARTIALLY_FILLED,
    /** The entire quantity has traded. */
    FILLED,
    /** Removed from the book before being fully filled. */
    CANCELLED
}
