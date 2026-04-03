package com.multibank.candles.api;

import com.multibank.candles.model.Candle;
import com.multibank.candles.model.Interval;
import com.multibank.candles.service.CandleAggregatorService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class HistoryController {

    private final CandleAggregatorService candleAggregatorService;

    public HistoryController(CandleAggregatorService candleAggregatorService) {
        this.candleAggregatorService = candleAggregatorService;
    }

    @GetMapping("/history")
    public HistoryResponse history(
        @RequestParam String symbol,
        @RequestParam String interval,
        @RequestParam long from,
        @RequestParam long to
    ) {
        if (from > to) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be <= 'to'");
        }

        Interval parsed;
        try {
            parsed = Interval.fromCode(interval);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        List<Candle> candles = candleAggregatorService.history(symbol, parsed, from, to);
        HistoryResponse response = HistoryResponse.ok();
        for (Candle candle : candles) {
            response.add(candle.time(), candle.open(), candle.high(), candle.low(), candle.close(), candle.volume());
        }
        return response;
    }
}
