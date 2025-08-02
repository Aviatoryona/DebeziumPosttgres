#!/bin/bash
set -eo

export JAVA_HOME=$JAVA_22

echo "Compiling with Maven..."
mvn -T 2C clean compile package -DskipTests

if [ $? -ne 0 ]; then
  echo "❌ Maven build failed"
  exit 1
fi
echo "✅ Maven build succeeded"

# Build the Docker image for your Spring Boot app
echo "🔧 Building Docker image..."
docker build --no-cache -f Dockerfile -t debezium-demo:latest .

# Stop and remove old container if exists
echo "🧹 Stopping old container (if exists)..."
docker container stop debezium-demo-container || true
docker container rm debezium-demo-container || true

# IMPORTANT: Don't destroy volumes unless you mean to reset offsets
echo "🔄 Restarting Kafka/Postgres/Debezium stack..."
docker compose down --remove-orphans
docker compose up -d

# Wait for Debezium Connect to be up
until curl -s localhost:8083/connectors &>/dev/null; do
  echo "⌛ Waiting for Debezium Connect to be ready..."
  sleep 5
done

# Register connector
bash ./register-connector.sh

# Run Spring Boot app container
echo "🚀 Starting Spring Boot app container..."
docker run -d --env-file .env -p 8088:8081 --name debezium-demo-container debezium-demo:latest

# Follow logs
docker logs -f debezium-demo-container
