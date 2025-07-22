# Debezium , Kafka Connect, Postgres

> Start DB

```bash
docker compose  -f db.yaml build --no-cache 
docker compose  -f db.yaml up
```

> Start Kafka

```bash
docker compose  build --no-cache 
docker compose up
```

> Start Debezium

```bash 
docker compose -f  debezium.yaml  build --no-cache
docker compose  -f debezium.yaml up
```

> Execute requests in dbz.http

> To ensure full table logs written to WAL
```sql
alter table table_name replica identity full
```

> Create tables and do some CRUD operations
> Connect to Kafka using any tool and plugin and explore the messages in consumers topics.