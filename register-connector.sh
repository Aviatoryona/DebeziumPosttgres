
CONNECT_URL="http://localhost:8083"
CONNECTOR_NAME="postgres-cdc-connector"

echo "📡 Registering connector [$CONNECTOR_NAME]..."
curl -s -X POST "$CONNECT_URL/connectors" \
    -H "Content-Type: application/json" \
    -d @postgres-connector.json
echo "✅ Connector [$CONNECTOR_NAME] registered."