
CONNECT_URL="http://localhost:8083"
CONNECTOR_NAME="postgres-cdc-connector"

echo "🔍 Checking if connector [$CONNECTOR_NAME] is already registered..."

if curl -s "$CONNECT_URL/connectors/$CONNECTOR_NAME" | grep -q "$CONNECTOR_NAME"; then
  echo "✅ Connector [$CONNECTOR_NAME] already exists. Skipping registration."
else
  echo "📡 Registering connector [$CONNECTOR_NAME]..."
  curl -s -X POST "$CONNECT_URL/connectors" \
    -H "Content-Type: application/json" \
    -d @postgres-connector.json
  echo "✅ Connector [$CONNECTOR_NAME] registered."
fi