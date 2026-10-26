package com.schedulex.controller;

import com.schedulex.exception.PaymentConflictException;
import com.schedulex.exception.ResourceNotFoundException;
import com.schedulex.model.Order;
import com.schedulex.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private OrderService orderService;

    @Test
    void createOrderReturns201() throws Exception {
        Order order = new Order("ORD_1", "C1", "Asha", "a@example.com", "9876543210", new BigDecimal("10.00"), "[]");
        when(orderService.createOrder(any())).thenReturn(order);

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerId":"C1","customerName":"Asha","customerEmail":"a@example.com",
                 "customerPhone":"9876543210","amount":10.00}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value("ORD_1"));
    }

    @Test
    void invalidOrderReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerId":"","customerName":"Asha","customerEmail":"not-an-email","amount":-5}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.customerId").exists())
            .andExpect(jsonPath("$.fieldErrors.customerEmail").exists())
            .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        when(orderService.getOrder("NOPE")).thenThrow(new ResourceNotFoundException("Order not found: NOPE"));

        mvc.perform(get("/api/orders/NOPE"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Order not found: NOPE"));
    }

    @Test
    void retryingPaidOrderReturns409() throws Exception {
        when(orderService.retryPayment("ORD_PAID"))
            .thenThrow(new PaymentConflictException("Order ORD_PAID cannot be charged in status SUCCESS"));

        mvc.perform(post("/api/orders/ORD_PAID/retry"))
            .andExpect(status().isConflict());
    }
}
