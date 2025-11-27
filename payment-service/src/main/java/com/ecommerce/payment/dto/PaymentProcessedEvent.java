package com.ecommerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private String orderId;
    private String paymentId;
    private String paymentStatus; // APPROVED, REJECTED
    private String paymentMethod;
    private Double amount;
    private String errorMessage;
    private LocalDateTime timestamp;
}
