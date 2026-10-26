package com.schedulex.controller;

import com.schedulex.dto.CreateOrderRequest;
import com.schedulex.model.Order;
import com.schedulex.model.PaymentTransaction;
import com.schedulex.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create new order (first payment attempt happens immediately)
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return new ResponseEntity<>(orderService.createOrder(request), HttpStatus.CREATED);
    }

    /**
     * Get all orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    /**
     * Get payment transactions for an order
     */
    @GetMapping("/{orderId}/transactions")
    public ResponseEntity<List<PaymentTransaction>> getOrderTransactions(
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderTransactions(orderId));
    }

    /**
     * Manually retry payment for a failed order (409 if it is paid, in progress or cancelled)
     */
    @PostMapping("/{orderId}/retry")
    public ResponseEntity<Order> retryPayment(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.retryPayment(orderId));
    }

    /**
     * Get order statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<OrderService.OrderStats> getStats() {
        return ResponseEntity.ok(orderService.getStats());
    }
}
