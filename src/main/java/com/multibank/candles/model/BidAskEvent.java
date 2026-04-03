package com.multibank.candles.model;

public record BidAskEvent(String symbol, double bid, double ask, long timestamp) {
}
