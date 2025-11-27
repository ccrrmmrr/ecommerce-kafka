package com.ecommerce.payment.kafka;

import com.ecommerce.payment.dto.InventoryUpdatedEvent;
import com.ecommerce.payment.dto.PaymentProcessedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory-events", groupId = "payment-service-group")
    public void consumeInventoryEvent(String message) {
        try {
            log.info("💰 Recibido InventoryUpdatedEvent: {}", message);
            
            InventoryUpdatedEvent inventoryEvent = objectMapper.readValue(message, InventoryUpdatedEvent.class);
            
            // Solo procesar si el inventario está disponible
            if (Boolean.TRUE.equals(inventoryEvent.getAllProductsAvailable())) {
                log.info("🎯 Procesando pago para orden: {}", inventoryEvent.getOrderId());
                processPayment(inventoryEvent);
            } else {
                log.warn("⏭️  Inventario insuficiente, saltando procesamiento de pago para orden: {}", 
                        inventoryEvent.getOrderId());
            }
            
        } catch (Exception e) {
            log.error("❌ Error procesando mensaje de inventario: {}", e.getMessage());
        }
    }

    private void processPayment(InventoryUpdatedEvent inventoryEvent) {
        try {
            // Simular procesamiento de pago (80% aprobado, 20% rechazado)
            boolean isPaymentApproved = Math.random() > 0.2;
            String paymentStatus = isPaymentApproved ? "APPROVED" : "REJECTED";
            String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            log.info("💳 Procesando pago {} - Status: {}", paymentId, paymentStatus);

            // Simular delay de procesamiento
            Thread.sleep(1000);

            // Crear evento de pago procesado
            PaymentProcessedEvent paymentEvent = PaymentProcessedEvent.builder()
                    .orderId(inventoryEvent.getOrderId())
                    .paymentId(paymentId)
                    .paymentStatus(paymentStatus)
                    .paymentMethod("CREDIT_CARD")
                    .amount(calculateOrderAmount(inventoryEvent)) // En un caso real, esto vendría de la orden
                    .errorMessage(isPaymentApproved ? null : "Fondos insuficientes")
                    .timestamp(LocalDateTime.now())
                    .build();

            sendPaymentEvent(paymentEvent);

        } catch (Exception e) {
            log.error("❌ Error en procesamiento de pago: {}", e.getMessage());
            sendFailedPaymentEvent(inventoryEvent, e.getMessage());
        }
    }

    private Double calculateOrderAmount(InventoryUpdatedEvent inventoryEvent) {
        // En un caso real, esto vendría de la base de datos o del evento de orden
        // Por ahora simulamos un monto fijo basado en la orden
        return 100.0 + (Math.random() * 200); // Monto entre 100 y 300
    }

    private void sendPaymentEvent(PaymentProcessedEvent paymentEvent) {
        try {
            String message = objectMapper.writeValueAsString(paymentEvent);
            kafkaTemplate.send("payment-events", message);
            log.info("📤 Enviado PaymentProcessedEvent para orden: {} - Status: {}", 
                    paymentEvent.getOrderId(), paymentEvent.getPaymentStatus());
        } catch (JsonProcessingException e) {
            log.error("❌ Error enviando PaymentProcessedEvent: {}", e.getMessage());
        }
    }

    private void sendFailedPaymentEvent(InventoryUpdatedEvent inventoryEvent, String error) {
        try {
            PaymentProcessedEvent failedEvent = PaymentProcessedEvent.builder()
                    .orderId(inventoryEvent.getOrderId())
                    .paymentId("PAY-FAILED")
                    .paymentStatus("FAILED")
                    .paymentMethod("CREDIT_CARD")
                    .amount(0.0)
                    .errorMessage(error)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            String message = objectMapper.writeValueAsString(failedEvent);
            kafkaTemplate.send("payment-events", message);
            log.warn("📤 Enviado PaymentProcessedEvent con error para orden: {}", inventoryEvent.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("❌ Error enviando evento de pago fallido: {}", e.getMessage());
        }
    }
}
