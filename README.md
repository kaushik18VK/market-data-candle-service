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
export DB_URL=jdbc:postgresql://localhost:5432/candlesdb
export DB_USER=postgres
export DB_PASSWORD=<your_postgres_password>
mvn spring-boot:run
```

Prerequisite: PostgreSQL must be running locally and `candlesdb` database should exist.

Service starts on `http://localhost:8080` (native run).

## Run With Docker (App + DB)
Prerequisite: Docker Desktop installed and running.

```bash
docker compose up --build -d
```

This starts:
- App on `http://localhost:8081`
- PostgreSQL on `localhost:5433` (`candlesdb` / `postgres` / `postgres`)

Check logs:
```bash
docker compose logs -f app
```

Stop:
```bash
docker compose down
```

Stop and remove DB volume (fresh DB next run):
```bash
docker compose down -v
```

## View Database (PostgreSQL)
- Native run:
  - Host: `localhost`
  - Port: `5432`
  - DB: `candlesdb`
  - User: value of `DB_USER` (default `postgres`)
  - Password: value of `DB_PASSWORD` (default `postgres`)
- Docker run:
  - Host: `localhost`
  - Port: `5433`
  - DB: `candlesdb`
  - User: `postgres`
  - Password: `postgres`

Try:
```sql
SELECT * FROM candles ORDER BY time DESC LIMIT 20;
```

Using `psql`:
```bash
PGPASSWORD=<your_postgres_password> psql -h localhost -U postgres -d candlesdb -c "SELECT * FROM candles ORDER BY time DESC LIMIT 20;"
```

If running with Docker defaults, use:
```bash
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d candlesdb -c "SELECT * FROM candles ORDER BY time DESC LIMIT 20;"
```

## Test
```bash
mvn test
```

What these tests validate:
- Interval parsing and timestamp alignment behavior.
- Core OHLC aggregation inside the same bucket.
- Bucket rollover/finalization behavior when time advances.
- Delayed event merge behavior for historical buckets.

Expected result: Maven reports `BUILD SUCCESS` and all tests passing.

## Health Check
```bash
curl "http://localhost:8080/actuator/health"
```

If running in Docker mode:
```bash
curl "http://localhost:8081/actuator/health"
```

## Example API Call
- Native run:
```bash
curl "http://localhost:8080/history?symbol=BTC-USD&interval=1m&from=1712120000&to=1712120600"
```

- Docker run:
```bash
curl "http://localhost:8081/history?symbol=BTC-USD&interval=1m&from=1712120000&to=1712120600"
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

## Bonus Features
- Dockerized setup to run app + PostgreSQL via `docker compose`.
- Multi-symbol simulation (`BTC-USD`, `ETH-USD`, `SOL-USD`).
- Delayed event handling with merge logic for late ticks.
- Health endpoint via Spring Boot Actuator (`/actuator/health`).

Test results

1. API response
<img width="2997" height="1303" alt="image" src="https://github.com/user-attachments/assets/5f10208e-b036-4e7a-9b8f-4770719f869a" />

2. DB response
<img width="1349" height="727" alt="image" src="https://github.com/user-attachments/assets/4abf489c-ff8c-4967-80d5-e5a9f62febea" />
