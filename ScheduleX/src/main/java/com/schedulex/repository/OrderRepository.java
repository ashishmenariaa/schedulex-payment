package com.schedulex.repository;

import com.schedulex.model.Order;
import com.schedulex.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderId(String orderId);
    
    List<Order> findByPaymentStatus(PaymentStatus status);
    
    // Count orders by payment status
    long countByPaymentStatus(PaymentStatus status);
    
    // Find recent orders
    List<Order> findTop10ByOrderByCreatedAtDesc();
}