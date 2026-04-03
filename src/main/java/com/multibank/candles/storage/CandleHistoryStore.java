package com.multibank.candles.storage;

import com.multibank.candles.model.Candle;
import com.multibank.candles.model.Interval;
import java.util.List;
import java.util.Optional;

public interface CandleHistoryStore {

    void upsert(String symbol, Interval interval, Candle candle);

    void mergeLateTick(String symbol, Interval interval, long bucketStart, double price);

    Optional<Candle> findAt(String symbol, Interval interval, long time);

    List<Candle> findRange(String symbol, Interval interval, long fromEpochSecond, long toEpochSecond);
}
