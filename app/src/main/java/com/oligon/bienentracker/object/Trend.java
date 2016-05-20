package com.oligon.bienentracker.object;

import java.util.TreeMap;

public class Trend {

    private TreeMap<String, Double> mTrendYear;

    public Trend() {
        mTrendYear = new TreeMap<>();
    }

    public void addHoney(String year, double value) {
        if (mTrendYear.containsKey(year)) {
            Double previous = mTrendYear.get(year);
            mTrendYear.remove(year);
            mTrendYear.put(year, previous + value);
        } else {
            mTrendYear.put(year, value);
        }
    }

    public TreeMap<String, Double> getTrendYear() {
        return mTrendYear;
    }

}
