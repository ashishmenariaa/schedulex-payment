package com.schedulex.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Client input for a new order. Status, retry counters and timestamps are server-controlled,
 * so they are deliberately not accepted here.
 */
public record CreateOrderRequest(
    @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "may only contain letters, digits, _ and -")
    String orderId,

    @NotBlank @Size(max = 50) String customerId,
    @NotBlank @Size(max = 100) String customerName,
    @Email String customerEmail,
    @Pattern(regexp = "^$|^\\+?[0-9]{10,13}$", message = "must be 10-13 digits") String customerPhone,

    @NotNull @DecimalMin(value = "1.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,

    String orderItems
) {}
