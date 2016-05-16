package com.oligon.bienentracker.object;

import java.util.LinkedHashMap;
import java.util.TreeMap;

public class Trend {

    private TreeMap<String, Double> mTrendYear;
    private TreeMap<String, LinkedHashMap<String, Double>> mTrendMonth;

    public Trend() {
        mTrendYear = new TreeMap<>();
        mTrendMonth = new TreeMap<>();
    }

    public void addHoney(String year, String month, double value) {
        if (mTrendYear.containsKey(year)) {
            Double previous = mTrendYear.get(year);
            mTrendYear.remove(year);
            mTrendYear.put(year, previous + value);
        } else {
            mTrendYear.put(year, value);
        }
        if (mTrendMonth.containsKey(year)) {
            if (mTrendMonth.get(year).containsKey(month)) {
                Double data = mTrendMonth.get(year).get(month);
                mTrendMonth.get(year).remove(month);
                mTrendMonth.get(year).put(month, data + value);
            } else {
                mTrendMonth.get(year).put(month, value);
            }
        } else {
            LinkedHashMap<String, Double> data = new LinkedHashMap<>();
            data.put(month, value);
            mTrendMonth.put(year, data);
        }
    }

    public TreeMap<String, Double> getTrendYear() {
        return mTrendYear;
    }

    public TreeMap<String, LinkedHashMap<String, Double>> getTrendMonth() {
        return mTrendMonth;
    }

}
