package com.schedulex.controller;

import com.schedulex.model.Order;
import com.schedulex.model.PaymentTransaction;
import com.schedulex.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    
    /**
     * Create new order
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order createdOrder = orderService.createOrder(order);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
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
     * Manually retry payment for an order
     */
    @PostMapping("/{orderId}/retry")
    public ResponseEntity<String> retryPayment(@PathVariable String orderId) {
        Order order = orderService.getOrder(orderId);
        orderService.attemptPayment(order);
        return ResponseEntity.ok("Payment retry initiated for order: " + orderId);
    }
    
    /**
     * Get order statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<OrderService.OrderStats> getStats() {
        return ResponseEntity.ok(orderService.getStats());
    }
}