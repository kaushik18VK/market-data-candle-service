package com.multibank.candles.model;

import java.util.Arrays;

public enum Interval {
    ONE_SECOND("1s", 1),
    FIVE_SECONDS("5s", 5),
    ONE_MINUTE("1m", 60),
    FIFTEEN_MINUTES("15m", 900),
    ONE_HOUR("1h", 3600);

    private final String code;
    private final long seconds;

    Interval(String code, long seconds) {
        this.code = code;
        this.seconds = seconds;
    }

    public String code() {
        return code;
    }

    public long seconds() {
        return seconds;
    }

    public long alignEpochSecond(long epochSecond) {
        return (epochSecond / seconds) * seconds;
    }

    public static Interval fromCode(String code) {
        return Arrays.stream(values())
            .filter(value -> value.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported interval: " + code));
    }
}
