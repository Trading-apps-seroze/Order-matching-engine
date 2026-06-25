package com.orderbookengine.api;

import com.orderbookengine.api.dto.ModifyOrderRequest;
import com.orderbookengine.api.dto.OrderResponse;
import com.orderbookengine.api.dto.SubmitOrderRequest;
import com.orderbookengine.api.dto.SubmitOrderResponse;
import com.orderbookengine.service.OrderResult;
import com.orderbookengine.service.OrderService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order lifecycle endpoints. The controller only translates JSON DTOs to and
 * from {@link OrderService} calls — all logic lives in the service and engine.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitOrderResponse submit(@RequestBody SubmitOrderRequest request) {
        OrderResult result = orderService.submit(
                request.getSide(), request.getType(), request.getPrice(), request.getQuantity());
        return SubmitOrderResponse.from(result);
    }

    @PutMapping("/{id}")
    public SubmitOrderResponse modify(@PathVariable long id,
                                      @RequestBody ModifyOrderRequest request) {
        OrderResult result = orderService.modify(id, request.getQuantity(), request.getPrice());
        return SubmitOrderResponse.from(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable long id) {
        boolean cancelled = orderService.cancel(id);
        return cancelled ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping
    public List<OrderResponse> list() {
        return orderService.getAllOrders().stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }
}
