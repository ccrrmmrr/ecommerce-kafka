#!/bin/bash

echo "🚀 Despliegue Completo del E-Commerce con Kafka"

# 1. Verificar estructura
echo "📁 Verificando estructura de archivos..."
find . -name "application.yml" | head -10

# 2. Construir microservicios
echo "📦 Construyendo microservicios..."
mvn clean package -DskipTests

# 3. Verificar que se crearon los JARs
echo "🔍 Verificando archivos JAR..."
find . -name "*.jar" | head -10

# 4. Parar servicios anteriores
echo "🛑 Deteniendo servicios anteriores..."
docker-compose down

# 5. Levantar infraestructura
echo "🏗️ Levantando infraestructura..."
docker-compose up -d zookeeper kafka postgres kafka-ui

# 6. Esperar a Kafka
echo "⏳ Esperando a Kafka (60 segundos)..."
sleep 60

# 7. Verificar Kafka
echo "🔍 Verificando Kafka..."
docker-compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# 8. Levantar microservicios
echo "🔨 Levantando microservicios..."
docker-compose up -d discovery-service
echo "⏳ Esperando Discovery Service (15 segundos)..."
sleep 15

docker-compose up -d gateway-service product-service order-service
echo "⏳ Esperando microservicios (30 segundos)..."
sleep 30

# 9. Verificar todo
echo "✅ Verificando despliegue completo..."
./scripts/health-check-all.sh

echo "🎉 ¡Despliegue completado!"
echo ""
echo "🌐 URLs de acceso:"
echo "   Eureka Server: http://localhost:8761"
echo "   API Gateway: http://localhost:8082"
echo "   Product Service: http://localhost:8083"
echo "   Order Service: http://localhost:8084"
echo "   Kafka UI: http://localhost:8080"
echo "   PostgreSQL: localhost:5432"
