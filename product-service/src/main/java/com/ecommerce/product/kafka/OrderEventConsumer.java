package com.ecommerce.product.kafka;

import com.ecommerce.product.dto.OrderCreatedEvent;
import com.ecommerce.product.dto.InventoryUpdatedEvent;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "product-service-group")
    public void consumeOrderEvent(String message) {
        try {
            log.info("📥 Mensaje recibido en product-service: {}", message);
            
            OrderCreatedEvent orderEvent = objectMapper.readValue(message, OrderCreatedEvent.class);
            
            if ("ORDER_CREATED".equals(orderEvent.getEventType())) {
                log.info("🎯 Procesando ORDER_CREATED: {}", orderEvent.getOrderId());
                processInventoryCheck(orderEvent);
            } else {
                log.info("⏭️  Evento ignorado: {}", orderEvent.getEventType());
            }
            
        } catch (Exception e) {
            log.error("❌ Error procesando mensaje: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void processInventoryCheck(OrderCreatedEvent orderEvent) {
        Map<String, Boolean> inventoryStatus = new HashMap<>();
        boolean allProductsAvailable = true;
        String errorMessage = null;

        try {
            log.info("🔍 Verificando inventario para orden: {}", orderEvent.getOrderId());

            if (orderEvent.getItems() == null || orderEvent.getItems().isEmpty()) {
                throw new RuntimeException("La orden no contiene items");
            }

            // Verificar stock para cada item
            for (OrderCreatedEvent.OrderItem item : orderEvent.getItems()) {
                String productId = item.getProductId();
                Integer quantity = item.getQuantity();

                log.info("📦 Verificando Producto: {}, Cantidad: {}", productId, quantity);

                // Buscar producto por productId (asumiendo que productId es el SKU)
                Product product = productRepository.findBySku(productId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productId));
                
                boolean isAvailable = product.getStock() >= quantity;
                inventoryStatus.put(productId, isAvailable);
                
                if (!isAvailable) {
                    allProductsAvailable = false;
                    log.warn("⚠️ Stock insuficiente para Producto: {}. Stock actual: {}, Requerido: {}", 
                            productId, product.getStock(), quantity);
                } else {
                    log.info("✅ Stock suficiente para Producto: {} - Stock: {}", productId, product.getStock());
                }
            }

            // Crear y enviar evento de inventario
            InventoryUpdatedEvent inventoryEvent = InventoryUpdatedEvent.builder()
                    .orderId(orderEvent.getOrderId())
                    .inventoryStatus(inventoryStatus)
                    .allProductsAvailable(allProductsAvailable)
                    .errorMessage(errorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            sendInventoryEvent(inventoryEvent);

        } catch (Exception e) {
            log.error("❌ Error en verificación de inventario: {}", e.getMessage());
            errorMessage = e.getMessage();
            // Enviar evento de fallo
            sendFailedInventoryEvent(orderEvent, errorMessage);
        }
    }

    private void sendInventoryEvent(InventoryUpdatedEvent inventoryEvent) {
        try {
            String message = objectMapper.writeValueAsString(inventoryEvent);
            kafkaTemplate.send("inventory-events", message);
            log.info("📤 Enviado InventoryUpdatedEvent para orden: {} - Status: {}", 
                    inventoryEvent.getOrderId(), 
                    inventoryEvent.getAllProductsAvailable() ? "APPROVED" : "REJECTED");
                    
            log.info("📊 Resumen inventario: {}", inventoryEvent.getInventoryStatus());
        } catch (JsonProcessingException e) {
            log.error("❌ Error enviando InventoryUpdatedEvent: {}", e.getMessage());
        }
    }

    private void sendFailedInventoryEvent(OrderCreatedEvent orderEvent, String error) {
        try {
            InventoryUpdatedEvent failedEvent = InventoryUpdatedEvent.builder()
                    .orderId(orderEvent.getOrderId())
                    .inventoryStatus(new HashMap<>())
                    .allProductsAvailable(false)
                    .errorMessage(error)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            String message = objectMapper.writeValueAsString(failedEvent);
            kafkaTemplate.send("inventory-events", message);
            log.warn("📤 Enviado InventoryUpdatedEvent con error para orden: {}", orderEvent.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("❌ Error enviando evento de fallo: {}", e.getMessage());
        }
    }
}

