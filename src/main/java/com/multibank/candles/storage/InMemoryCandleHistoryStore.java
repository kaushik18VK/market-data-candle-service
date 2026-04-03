package com.multibank.candles.storage;

import com.multibank.candles.model.Candle;
import com.multibank.candles.model.Interval;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class InMemoryCandleHistoryStore implements CandleHistoryStore {

    private final ConcurrentHashMap<String, ConcurrentSkipListMap<Long, Candle>> historical = new ConcurrentHashMap<>();

    @Override
    public void upsert(String symbol, Interval interval, Candle candle) {
        map(symbol, interval).put(candle.time(), candle);
    }

    @Override
    public void mergeLateTick(String symbol, Interval interval, long bucketStart, double price) {
        map(symbol, interval).compute(bucketStart, (ts, existing) -> {
            if (existing == null) {
                return new Candle(bucketStart, price, price, price, price, 1);
            }
            return new Candle(
                existing.time(),
                existing.open(),
                Math.max(existing.high(), price),
                Math.min(existing.low(), price),
                price,
                existing.volume() + 1
            );
        });
    }

    @Override
    public Optional<Candle> findAt(String symbol, Interval interval, long time) {
        return Optional.ofNullable(map(symbol, interval).get(time));
    }

    @Override
    public List<Candle> findRange(String symbol, Interval interval, long fromEpochSecond, long toEpochSecond) {
        NavigableMap<Long, Candle> subMap = map(symbol, interval).subMap(fromEpochSecond, true, toEpochSecond, true);
        return new ArrayList<>(subMap.values());
    }

    private ConcurrentSkipListMap<Long, Candle> map(String symbol, Interval interval) {
        return historical.computeIfAbsent(key(symbol, interval), unused -> new ConcurrentSkipListMap<>());
    }

    private String key(String symbol, Interval interval) {
        return symbol + "|" + interval.code();
    }
}
