package com.ecommerce.order.kafka;

import com.ecommerce.order.dto.PaymentProcessedEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void consumePaymentEvent(String message) {
        try {
            log.info("💰 Recibido PaymentProcessedEvent: {}", message);
            
            PaymentProcessedEvent paymentEvent = objectMapper.readValue(message, PaymentProcessedEvent.class);
            
            log.info("🎯 Actualizando orden: {} con estado de pago: {}", 
                    paymentEvent.getOrderId(), paymentEvent.getPaymentStatus());
            
            updateOrderStatus(paymentEvent);
            
        } catch (Exception e) {
            log.error("❌ Error procesando mensaje de pago: {}", e.getMessage());
        }
    }

    private void updateOrderStatus(PaymentProcessedEvent paymentEvent) {
        try {
            // Buscar la orden por orderNumber (que es el orderId en el evento)
            Order order = orderRepository.findByOrderNumber(paymentEvent.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + paymentEvent.getOrderId()));

            // Actualizar información de pago
            order.setPaymentStatus(paymentEvent.getPaymentStatus());
            order.setPaymentId(paymentEvent.getPaymentId());
            
            // Determinar el estado final de la orden basado en el pago
            String finalStatus = determineFinalOrderStatus(order, paymentEvent);
            order.setStatus(finalStatus);
            
            // Guardar la orden actualizada
            orderRepository.save(order);
            
            log.info("✅ Orden actualizada: {} - Estado: {} - Pago: {}", 
                    order.getOrderNumber(), order.getStatus(), order.getPaymentStatus());
                    
        } catch (Exception e) {
            log.error("❌ Error actualizando estado de orden: {}", e.getMessage());
        }
    }

    private String determineFinalOrderStatus(Order order, PaymentProcessedEvent paymentEvent) {
        // Lógica para determinar el estado final basado en inventory y payment
        if ("APPROVED".equals(paymentEvent.getPaymentStatus())) {
            return "COMPLETED";
        } else if ("REJECTED".equals(paymentEvent.getPaymentStatus())) {
            return "CANCELLED";
        } else if ("FAILED".equals(paymentEvent.getPaymentStatus())) {
            return "FAILED";
        }
        
        return "PENDING";
    }
}
