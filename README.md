# Candle Aggregation Service (Java)

Backend Java service that ingests bid/ask events, aggregates OHLC candles for multiple symbols and intervals, and exposes historical candles via REST for charting clients.

## Features
- Real-time simulated bid/ask ingestion (`BTC-USD`, `ETH-USD`, `SOL-USD`)
- Thread-safe OHLC aggregation for:
  - `1s`, `5s`, `1m`, `15m`, `1h`
- History endpoint compatible with charting libraries:
  - `GET /history?symbol=BTC-USD&interval=1m&from=1620000000&to=1620000600`
- PostgreSQL storage for candles (`candles` table)
- In-memory active-candle cache for current open buckets
- Health endpoint via Spring Boot Actuator (`/actuator/health`)
- Unit tests for interval parsing and aggregation behavior

## Tech Stack
- Java 17
- Spring Boot 3 (Web + Actuator)
- PostgreSQL + Spring JDBC
- JUnit 5

## Architecture
- `MarketDataSimulator` generates ticks on a schedule.
- `CandleAggregatorService` is the core domain service:
  - Maintains active candles per `(symbol, interval)`
  - Finalizes and stores completed candles in historical storage
  - Supports slightly delayed events by merging into historical buckets
- `HistoryController` returns TradingView-style arrays:
  - `s`, `t`, `o`, `h`, `l`, `c`, `v`

## Run Locally
```bash
docker compose up -d
mvn spring-boot:run
```

Service starts on `http://localhost:8080`.

## View Database (PostgreSQL)
- Host: `localhost`
- Port: `5432`
- DB: `candlesdb`
- User: `postgres`
- Password: `postgres`

Try:
```sql
SELECT * FROM candles ORDER BY time DESC LIMIT 20;
```

Using `psql`:
```bash
psql -h localhost -U postgres -d candlesdb -c "SELECT * FROM candles ORDER BY time DESC LIMIT 20;"
```

## Test
```bash
mvn test
```

## Example API Call
```bash
curl "http://localhost:8080/history?symbol=BTC-USD&interval=1m&from=1712120000&to=1712120600"
```

Example response:
```json
{
  "s": "ok",
  "t": [1712120040, 1712120100],
  "o": [65000.2, 65004.1],
  "h": [65008.0, 65010.2],
  "l": [64998.7, 65001.4],
  "c": [65004.1, 65008.8],
  "v": [15, 14]
}
```

## Assumptions / Trade-offs
- Event timestamp is in UNIX seconds.
- Mid price `((bid + ask) / 2)` is used as candle price.
- Volume is synthetic tick count.
- Active in-progress candles stay in memory and are continuously upserted into PostgreSQL.
- Delayed event handling is supported with merge logic, but exact historical open/close ordering for very late out-of-order events is best-effort unless upstream ordering guarantees are provided.

## Scaling Notes (Bonus Discussion)
- Swap in-memory store with PostgreSQL/TimescaleDB for durable history and partitioned time-series queries.
- Add Redis as read-through cache for popular `history` ranges.
- Use Kafka/WebSocket source and consumer groups for horizontal scaling.
- Introduce periodic flush/checkpoint and replay for resilience.
