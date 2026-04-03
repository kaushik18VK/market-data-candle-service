package com.multibank.candles.api;

import java.util.ArrayList;
import java.util.List;

public class HistoryResponse {
    private String s;
    private final List<Long> t = new ArrayList<>();
    private final List<Double> o = new ArrayList<>();
    private final List<Double> h = new ArrayList<>();
    private final List<Double> l = new ArrayList<>();
    private final List<Double> c = new ArrayList<>();
    private final List<Long> v = new ArrayList<>();

    public static HistoryResponse ok() {
        HistoryResponse response = new HistoryResponse();
        response.s = "ok";
        return response;
    }

    public void add(long time, double open, double high, double low, double close, long volume) {
        t.add(time);
        o.add(open);
        h.add(high);
        l.add(low);
        c.add(close);
        v.add(volume);
    }

    public String getS() {
        return s;
    }

    public List<Long> getT() {
        return t;
    }

    public List<Double> getO() {
        return o;
    }

    public List<Double> getH() {
        return h;
    }

    public List<Double> getL() {
        return l;
    }

    public List<Double> getC() {
        return c;
    }

    public List<Long> getV() {
        return v;
    }
}
