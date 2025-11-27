# 🛒 Ecommerce Kafka - Microservicios con Spring Boot y Kafka

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.5.0-blue)](https://kafka.apache.org)
[![Docker](https://img.shields.io/badge/Docker-Enabled-success)](https://docker.com)
[![Microservices](https://img.shields.io/badge/Architecture-Microservices-orange)](https://microservices.io)

Sistema de ecommerce completo implementado con arquitectura de microservicios, utilizando Apache Kafka para comunicación asíncrona y el patrón Saga para gestionar transacciones distribuidas.

## 🏗️ Arquitectura del Sistema

### 📊 Diagrama de Arquitectura

```text
┌─────────────┐    OrderCreatedEvent    ┌──────────────┐
│   Order     │ ──────────────────────> │   Product    │
│   Service   │                         │   Service    │
│             │ <────────────────────── │              │
│             │   InventoryUpdatedEvent │              │
└─────────────┘                         └──────────────┘
       ʌ                                        │
       │                                        │
       │ PaymentProcessedEvent           PaymentProcessedEvent
       │                                        │
       │                                        ˅
┌─────────────┐    PaymentProcessedEvent ┌──────────────┐
│   Order     │ <─────────────────────── │   Payment    │  
│   Service   │                         │   Service    │
│ (Consumer)  │                         │ (Consumer)   │
└─────────────┘                         └──────────────┘
```

### 🔄 Flujo de la Saga

1. **Order Service** recibe petición y publica `OrderCreatedEvent`
2. **Product Service** consume evento y verifica inventario, publica `InventoryUpdatedEvent`
3. **Payment Service** consume evento y procesa pago, publica `PaymentProcessedEvent`
4. **Order Service** consume evento y actualiza estado de la orden a `COMPLETED`

## 🛠️ Tecnologías Utilizadas

- **Java 17** + **Spring Boot 3.2.0**
- **Apache Kafka** - Mensajería asíncrona
- **PostgreSQL** - Base de datos por servicio
- **Docker** + **Docker Compose** - Contenerización
- **Spring Cloud** - Eureka Discovery, API Gateway
- **Lombok** - Reducción de código boilerplate
- **Maven** - Gestión de dependencias

## 📦 Microservicios

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| Order Service | 8084 | Gestión de órdenes, producer/consumer de Kafka |
| Product Service | 8083 | Gestión de inventario, consumer de Kafka |
| Payment Service | 8085 | Procesamiento de pagos, consumer de Kafka |
| API Gateway | 8082 | Gateway único para APIs |
| Discovery Service | 8761 | Service registry (Eureka) |
| Kafka UI | 8080 | Interfaz web para monitorear Kafka |

## 🚀 Instalación y Ejecución

### Prerrequisitos
- Docker y Docker Compose
- Java 17
- Maven 3.6+

### 1. Clonar el repositorio
```bash
git clone https://github.com/ccrrmmrr/ecommerce-kafka.git
cd ecommerce-kafka
```

### 2. Ejecutar la infraestructura
```bash
# Iniciar Kafka, PostgreSQL, Zookeeper, Eureka
docker-compose up -d zookeeper kafka postgres eureka-server

# Esperar que los servicios estén listos
sleep 30
```

### 3. Compilar y ejecutar los microservicios
```bash
# Compilar todos los servicios
./mvnw clean package -DskipTests

# Ejecutar todos los servicios
docker-compose up -d --build
```

### 4. Verificar que todo esté funcionando
```bash
# Ver servicios en Eureka
curl http://localhost:8761

# Ver health checks
curl http://localhost:8084/health  # Order Service
curl http://localhost:8083/health  # Product Service
curl http://localhost:8085/health  # Payment Service

# Ver Kafka UI en http://localhost:8080
```

## 🧪 Uso del Sistema

### Crear una nueva orden
```bash
curl -X POST http://localhost:8084/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 2,
        "price": 1500.00
      },
      {
        "productId": "PROD-002",
        "quantity": 1,
        "price": 120.00
      }
    ]
  }'
```
### Consultar órdenes
```bash
# Todas las órdenes
curl http://localhost:8084/api/orders

# Orden específica
curl http://localhost:8084/api/orders/ORD-ABC123
```

### Monitorear eventos Kafka
```bash
# Ver eventos de órdenes
docker exec -it ecommerce-kafka-kafka-1 kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events --from-beginning

# Ver eventos de inventario
docker exec -it ecommerce-kafka-kafka-1 kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic inventory-events --from-beginning

# Ver eventos de pagos
docker exec -it ecommerce-kafka-kafka-1 kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic payment-events --from-beginning
```

## 📊 Estructura del Proyecto
```bash
ecommerce-kafka/
├── order-service/          # Microservicio de órdenes
├── product-service/        # Microservicio de productos  
├── payment-service/        # Microservicio de pagos
├── discovery-service/      # Eureka server
├── gateway-service/        # API Gateway
├── docker-compose.yml      # Orquestación de contenedores
└── README.md              # Este archivo
```

## 🔧 Configuración de Kafka

### Topics configurados:
```bash
order-events - Eventos de creación de órdenes
inventory-events - Eventos de actualización de inventario
payment-events - Eventos de procesamiento de pagos
```

### Consumer Groups:
```bash
product-service-group - Product Service
payment-service-group - Payment Service
order-service-group - Order Service
```

## 🧪 Datos de Prueba
```text
El sistema incluye datos de prueba automáticos:

Productos pre-cargados:

PROD-001: Laptop Gaming ($1500.00) - Stock: 10
PROD-002: Teclado Mecánico ($120.00) - Stock: 25
PROD-003: Mouse Inalámbrico ($80.00) - Stock: 30
PROD-004: Monitor 4K ($400.00) - Stock: 15
```

## 🐛 Troubleshooting

### Problemas comunes:
#### Kafka no conecta:
```bash
# Verificar que Kafka esté corriendo
docker-compose logs kafka

# Ver topics
docker exec -it ecommerce-kafka-kafka-1 kafka-topics.sh --list --bootstrap-server localhost:9092
```
#### Servicios no se registran con Eureka:
```bash
# Verificar Eureka
curl http://localhost:8761/eureka/apps

# Reiniciar servicios
docker-compose restart order-service product-service payment-service
```

## 🤝 Contribución
```text
1. Fork el proyecto
2. Crea una rama para tu feature (git checkout -b feature/AmazingFeature)
3. Commit tus cambios (git commit -m 'Add some AmazingFeature')
4. Push a la rama (git push origin feature/AmazingFeature)
5. Abre un Pull Request
```

## 👨‍💻 Autor

Carlos Roberto Martinez Rivadeneira - ccrrmmrr

## 🎯 Próximas Mejoras

- Implementar Circuit Breaker con Resilience4j
- Agregar métricas con Micrometer y Prometheus
- Implementar tracing distribuido con Zipkin
- Agregar autenticación JWT
- Implementar dead letter queues para mensajes fallidos
- Agregar tests de integración
- Implementar CI/CD con GitHub Actions





