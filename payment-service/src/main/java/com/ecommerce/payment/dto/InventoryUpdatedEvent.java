package com.ecommerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdatedEvent {
    private String orderId;
    private Map<String, Boolean> inventoryStatus;
    private Boolean allProductsAvailable;
    private String errorMessage;
    private LocalDateTime timestamp;
}
