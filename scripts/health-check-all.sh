#!/bin/bash

echo "🔍 Health Check Completo del Sistema"

services=(
  "http://localhost:8761/actuator/health"
  "http://localhost:8082/actuator/health"
  "http://localhost:8083/actuator/health"
  "http://localhost:8084/actuator/health"
)

for service in "${services[@]}"; do
  echo "Checking: $service"
  response=$(curl -s -o /dev/null -w "%{http_code}" "$service")
  if [ "$response" -eq 200 ]; then
    echo "✅ $service - HEALTHY"
  else
    echo "❌ $service - UNHEALTHY (HTTP $response)"
  fi
done

# Verificar servicios registrados en Eureka
echo ""
echo "📊 Servicios registrados en Eureka:"
curl -s http://localhost:8761/eureka/apps | grep -o '<name>[^<]*' | sed 's/<name>//' | sort | uniq

# Verificar Kafka
echo ""
echo "🔮 Estado de Kafka:"
docker-compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
if [ $? -eq 0 ]; then
  echo "✅ Kafka - HEALTHY"
else
  echo "❌ Kafka - UNHEALTHY"
fi

# Verificar contenedores
echo ""
echo "🐳 Contenedores ejecutándose:"
docker-compose ps --services
