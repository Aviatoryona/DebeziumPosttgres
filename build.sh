set -eo

export JAVA_HOME=$JAVA_22

mvn -T 2C clean compile package -DskipTests

if [ $? -ne 0 ]; then
  echo "Maven build failed"
  exit 1
fi
echo "Maven build succeeded"

# build the Docker image
docker build --no-cache -f Dockerfile -t debezium-demo:latest .

# starting db, kafka and debezium
echo "Starting Docker Compose services..."
docker container stop debezium-demo-container || true
docker compose down --remove-orphans
docker compose up -d

# wait for the services to be up
echo "Waiting for services to start..."
sleep 10

docker container stop debezium-demo-container || true
docker container rm debezium-demo-container || true
docker run  -d --env-file .env -p 8088:8081 --name debezium-demo-container debezium-demo:latest

docker logs -f debezium-demo-container
