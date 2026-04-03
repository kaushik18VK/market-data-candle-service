package com.multibank.candles.service;

import com.multibank.candles.model.BidAskEvent;
import com.multibank.candles.model.Candle;
import com.multibank.candles.model.Interval;
import com.multibank.candles.storage.CandleHistoryStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CandleAggregatorService {
    private static final Logger log = LoggerFactory.getLogger(CandleAggregatorService.class);

    private final Set<Interval> configuredIntervals = EnumSet.allOf(Interval.class);
    private final Map<SymbolIntervalKey, MutableCandle> activeCandles = new ConcurrentHashMap<>();
    private final CandleHistoryStore historyStore;

    @Autowired
    public CandleAggregatorService(CandleHistoryStore historyStore) {
        this.historyStore = historyStore;
    }

    public void onEvent(BidAskEvent event) {
        validate(event);
        double midPrice = (event.bid() + event.ask()) / 2.0d;
        long epochSecond = event.timestamp();

        for (Interval interval : configuredIntervals) {
            updateInterval(event.symbol(), interval, epochSecond, midPrice);
        }
    }

    public List<Candle> history(String symbol, Interval interval, long fromEpochSecond, long toEpochSecond) {
        SymbolIntervalKey key = new SymbolIntervalKey(symbol, interval);
        List<Candle> result = new ArrayList<>(historyStore.findRange(symbol, interval, fromEpochSecond, toEpochSecond));

        MutableCandle active = activeCandles.get(key);
        if (active != null
            && active.time >= fromEpochSecond
            && active.time <= toEpochSecond
            && result.stream().noneMatch(candle -> candle.time() == active.time)) {
            result.add(active.toImmutable());
            result.sort((a, b) -> Long.compare(a.time(), b.time()));
        }

        return result;
    }

    private void updateInterval(String symbol, Interval interval, long epochSecond, double price) {
        SymbolIntervalKey key = new SymbolIntervalKey(symbol, interval);
        long bucketStart = interval.alignEpochSecond(epochSecond);

        activeCandles.compute(key, (k, current) -> {
            if (current == null) {
                MutableCandle created = new MutableCandle(bucketStart, price);
                historyStore.upsert(k.symbol(), k.interval(), created.toImmutable());
                return created;
            }

            if (bucketStart == current.time) {
                current.update(price);
                historyStore.upsert(k.symbol(), k.interval(), current.toImmutable());
                return current;
            }

            if (bucketStart > current.time) {
                persist(k, current.toImmutable());
                MutableCandle created = new MutableCandle(bucketStart, price);
                historyStore.upsert(k.symbol(), k.interval(), created.toImmutable());
                return created;
            }

            // Slightly delayed event routed to historical bucket.
            mergeLateEvent(k, bucketStart, price);
            return current;
        });
    }

    private void mergeLateEvent(SymbolIntervalKey key, long bucketStart, double price) {
        Candle merged = historyStore.findAt(key.symbol(), key.interval(), bucketStart)
            .map(existing -> new Candle(
                existing.time(),
                existing.open(),
                Math.max(existing.high(), price),
                Math.min(existing.low(), price),
                price,
                existing.volume() + 1
            ))
            .orElseGet(() -> new Candle(bucketStart, price, price, price, price, 1));

        historyStore.upsert(key.symbol(), key.interval(), merged);
    }

    private void persist(SymbolIntervalKey key, Candle candle) {
        historyStore.upsert(key.symbol(), key.interval(), candle);
        if (log.isDebugEnabled()) {
            log.debug("Finalized candle symbol={} interval={} time={} ohlc={}/{}/{}/{} v={}",
                key.symbol(), key.interval().code(), Instant.ofEpochSecond(candle.time()),
                candle.open(), candle.high(), candle.low(), candle.close(), candle.volume());
        }
    }

    private void validate(BidAskEvent event) {
        Objects.requireNonNull(event.symbol(), "symbol cannot be null");
        if (event.ask() < event.bid()) {
            throw new IllegalArgumentException("ask must be >= bid");
        }
        if (event.timestamp() < 0) {
            throw new IllegalArgumentException("timestamp must be unix seconds");
        }
    }

    record SymbolIntervalKey(String symbol, Interval interval) {
    }

    static final class MutableCandle {
        private final long time;
        private final double open;
        private double high;
        private double low;
        private double close;
        private long volume;

        MutableCandle(long time, double price) {
            this.time = time;
            this.open = price;
            this.high = price;
            this.low = price;
            this.close = price;
            this.volume = 1;
        }

        void update(double price) {
            high = Math.max(high, price);
            low = Math.min(low, price);
            close = price;
            volume++;
        }

        Candle toImmutable() {
            return new Candle(time, open, high, low, close, volume);
        }
    }
}
