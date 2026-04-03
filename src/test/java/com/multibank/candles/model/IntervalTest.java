package com.multibank.candles.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IntervalTest {

    @Test
    void shouldParseIntervalCode() {
        assertEquals(Interval.ONE_MINUTE, Interval.fromCode("1m"));
        assertEquals(Interval.ONE_HOUR, Interval.fromCode("1H"));
    }

    @Test
    void shouldAlignTimestamps() {
        assertEquals(120, Interval.ONE_MINUTE.alignEpochSecond(179));
        assertEquals(175, Interval.FIVE_SECONDS.alignEpochSecond(179));
    }

    @Test
    void shouldRejectUnsupportedInterval() {
        assertThrows(IllegalArgumentException.class, () -> Interval.fromCode("3m"));
    }
}
