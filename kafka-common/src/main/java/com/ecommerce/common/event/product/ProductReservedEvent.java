package com.ecommerce.common.event.product;

import com.ecommerce.common.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductReservedEvent extends BaseEvent {
    private String orderId;
    private String productId;
    private Integer quantity;
    private boolean success;
    private String message;
    
    public ProductReservedEvent() {
        super("PRODUCT_RESERVED");
    }
}
