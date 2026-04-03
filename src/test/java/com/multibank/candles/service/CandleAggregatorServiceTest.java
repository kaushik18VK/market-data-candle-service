package com.multibank.candles.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.multibank.candles.model.BidAskEvent;
import com.multibank.candles.model.Candle;
import com.multibank.candles.model.Interval;
import com.multibank.candles.storage.InMemoryCandleHistoryStore;
import java.util.List;
import org.junit.jupiter.api.Test;

class CandleAggregatorServiceTest {

    private final CandleAggregatorService service = new CandleAggregatorService(new InMemoryCandleHistoryStore());

    @Test
    void shouldAggregateWithinSameBucket() {
        service.onEvent(new BidAskEvent("BTC-USD", 100, 102, 61));
        service.onEvent(new BidAskEvent("BTC-USD", 103, 105, 62));
        service.onEvent(new BidAskEvent("BTC-USD", 98, 100, 63));

        List<Candle> candles = service.history("BTC-USD", Interval.ONE_MINUTE, 60, 60);
        Candle candle = candles.get(0);
        assertEquals(101.0, candle.open());
        assertEquals(104.0, candle.high());
        assertEquals(99.0, candle.low());
        assertEquals(99.0, candle.close());
        assertEquals(3, candle.volume());
    }

    @Test
    void shouldFinalizePreviousBucketWhenTimeAdvances() {
        service.onEvent(new BidAskEvent("ETH-USD", 200, 202, 10));
        service.onEvent(new BidAskEvent("ETH-USD", 204, 206, 11));
        service.onEvent(new BidAskEvent("ETH-USD", 300, 302, 12));

        List<Candle> oneSecond = service.history("ETH-USD", Interval.ONE_SECOND, 10, 12);
        assertEquals(3, oneSecond.size());
        assertEquals(201.0, oneSecond.get(0).open());
        assertEquals(205.0, oneSecond.get(1).close());
        assertEquals(301.0, oneSecond.get(2).open());
    }

    @Test
    void shouldMergeDelayedEventsIntoHistoricalBucket() {
        service.onEvent(new BidAskEvent("SOL-USD", 50, 52, 100));
        service.onEvent(new BidAskEvent("SOL-USD", 60, 62, 101));
        service.onEvent(new BidAskEvent("SOL-USD", 55, 57, 100)); // delayed

        List<Candle> oneSecond = service.history("SOL-USD", Interval.ONE_SECOND, 100, 101);
        assertEquals(2, oneSecond.size());
        Candle first = oneSecond.get(0);
        assertEquals(51.0, first.open());
        assertEquals(56.0, first.close());
        assertEquals(2, first.volume());
    }
}
