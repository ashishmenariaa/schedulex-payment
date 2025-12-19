package com.schedulex.repository;

import com.schedulex.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    
    List<PaymentTransaction> findByOrderIdOrderByAttemptedAtDesc(String orderId);
    
    long countByOrderId(String orderId);
}