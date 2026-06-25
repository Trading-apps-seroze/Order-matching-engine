package com.orderbookengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boots the REST API in front of the matching engine.
 *
 * <p>Component scanning starts at this package, so the {@code api},
 * {@code service}, and {@code engine} packages are all picked up.
 */
@SpringBootApplication
public class OrderBookEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderBookEngineApplication.class, args);
    }
}
