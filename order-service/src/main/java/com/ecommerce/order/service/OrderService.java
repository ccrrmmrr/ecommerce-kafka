package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderCreatedEvent;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // Generar número de orden único
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Calcular total
        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Crear orden
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customerId(request.getCustomerId())
                .totalAmount(totalAmount)
                .status("PENDING")
                .build();

        // Crear items
        List<OrderItem> orderItems = request.getItems().stream()
                .map(item -> OrderItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .order(order)
                        .build())
                .collect(Collectors.toList());

        order.setItems(orderItems);

        // Guardar orden
        Order savedOrder = orderRepository.save(order);
        
        log.info("💾 Orden guardada en BD: {} - Total: ${}", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());
        
        // Enviar evento a Kafka
        sendOrderCreatedEvent(savedOrder);
        
        return savedOrder;
    }

    private void sendOrderCreatedEvent(Order order) {
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_CREATED")
                    .orderId(order.getOrderNumber())
                    .customerId(order.getCustomerId())
                    .totalAmount(order.getTotalAmount())
                    .orderDate(LocalDateTime.now())
                    .items(order.getItems().stream()
                            .map(item -> OrderCreatedEvent.OrderItem.builder()
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order-events", order.getOrderNumber(), message);
            
            log.info("📤 Enviado OrderCreatedEvent para orden: {}", order.getOrderNumber());
            
        } catch (JsonProcessingException e) {
            log.error("❌ Error enviando OrderCreatedEvent: {}", e.getMessage());
        }
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }
}
