package com.multibank.candles.storage;

import com.multibank.candles.model.Candle;
import com.multibank.candles.model.Interval;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCandleHistoryStore implements CandleHistoryStore {

    private static final RowMapper<Candle> CANDLE_ROW_MAPPER = (rs, rowNum) ->
        new Candle(
            rs.getLong("time"),
            rs.getDouble("open"),
            rs.getDouble("high"),
            rs.getDouble("low"),
            rs.getDouble("close"),
            rs.getLong("volume")
        );

    private final JdbcTemplate jdbcTemplate;

    public JdbcCandleHistoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void upsert(String symbol, Interval interval, Candle candle) {
        jdbcTemplate.update("""
            MERGE INTO candles (symbol, interval_code, time, open, high, low, close, volume)
            KEY (symbol, interval_code, time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            symbol,
            interval.code(),
            candle.time(),
            candle.open(),
            candle.high(),
            candle.low(),
            candle.close(),
            candle.volume()
        );
    }

    @Override
    public Optional<Candle> findAt(String symbol, Interval interval, long time) {
        List<Candle> rows = jdbcTemplate.query("""
                SELECT time, open, high, low, close, volume
                FROM candles
                WHERE symbol = ? AND interval_code = ? AND time = ?
                """,
            CANDLE_ROW_MAPPER,
            symbol, interval.code(), time
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<Candle> findRange(String symbol, Interval interval, long fromEpochSecond, long toEpochSecond) {
        return jdbcTemplate.query("""
                SELECT time, open, high, low, close, volume
                FROM candles
                WHERE symbol = ? AND interval_code = ? AND time >= ? AND time <= ?
                ORDER BY time
                """,
            CANDLE_ROW_MAPPER,
            symbol, interval.code(), fromEpochSecond, toEpochSecond
        );
    }
}
