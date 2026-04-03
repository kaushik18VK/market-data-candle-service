CREATE TABLE IF NOT EXISTS candles (
    symbol VARCHAR(32) NOT NULL,
    interval_code VARCHAR(8) NOT NULL,
    time BIGINT NOT NULL,
    open DOUBLE PRECISION NOT NULL,
    high DOUBLE PRECISION NOT NULL,
    low DOUBLE PRECISION NOT NULL,
    close DOUBLE PRECISION NOT NULL,
    volume BIGINT NOT NULL,
    PRIMARY KEY (symbol, interval_code, time)
);

CREATE INDEX IF NOT EXISTS idx_candles_query
    ON candles (symbol, interval_code, time);
