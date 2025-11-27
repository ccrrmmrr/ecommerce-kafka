package com.ecommerce.common.event;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BaseEvent {
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    
    public BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
    
    public BaseEvent(String eventType) {
        this();
        this.eventType = eventType;
    }
}
