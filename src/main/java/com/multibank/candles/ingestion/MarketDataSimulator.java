package com.multibank.candles.ingestion;

import com.multibank.candles.model.BidAskEvent;
import com.multibank.candles.service.CandleAggregatorService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketDataSimulator {
    private static final Logger log = LoggerFactory.getLogger(MarketDataSimulator.class);

    private static final List<String> SYMBOLS = List.of("BTC-USD", "ETH-USD", "SOL-USD");
    private final Map<String, Double> markPrices = new ConcurrentHashMap<>();
    private final CandleAggregatorService candleAggregatorService;

    public MarketDataSimulator(CandleAggregatorService candleAggregatorService) {
        this.candleAggregatorService = candleAggregatorService;
        markPrices.put("BTC-USD", 65000.0);
        markPrices.put("ETH-USD", 3500.0);
        markPrices.put("SOL-USD", 140.0);
    }

    @Scheduled(fixedRate = 200)
    public void emitEvents() {
        long now = Instant.now().getEpochSecond();
        for (String symbol : SYMBOLS) {
            double last = markPrices.get(symbol);
            double delta = ThreadLocalRandom.current().nextDouble(-1.5, 1.5);
            double next = Math.max(0.1, last + delta);
            markPrices.put(symbol, next);

            double spread = Math.max(0.01, next * 0.0001);
            BidAskEvent event = new BidAskEvent(symbol, next - spread, next + spread, now);
            candleAggregatorService.onEvent(event);
            log.trace("Tick symbol={} bid={} ask={} ts={}", symbol, event.bid(), event.ask(), event.timestamp());
        }
    }
}
