package com.oligon.bienentracker.object;

import android.content.Context;

import com.oligon.bienentracker.R;

import java.util.TreeMap;

public class StatisticsEarnings {

    private Context context;
    private double honeySum;

    private TreeMap<String, Double> honeyStatsGroup;
    private TreeMap<String, TreeMap<String, Double>> honeyStatsGroupDetail;

    public StatisticsEarnings(Context context) {
        this.context = context;
        honeyStatsGroup = new TreeMap<>();
        honeyStatsGroupDetail = new TreeMap<>();
    }

    public void addHoney(String hive, String group, double weight) {
        if (hive == null) return;
        if (group == null || group.isEmpty()) group = context.getString(R.string.no_group);
        if (honeyStatsGroup.containsKey(group)) {
            Double previous = honeyStatsGroup.get(group);
            honeyStatsGroup.remove(group);
            honeyStatsGroup.put(group, previous + weight);
        } else {
            honeyStatsGroup.put(group, weight);
        }
        if (honeyStatsGroupDetail.containsKey(group)) {
            if (honeyStatsGroupDetail.get(group).containsKey(hive)) {
                Double data = honeyStatsGroupDetail.get(group).get(hive);
                honeyStatsGroupDetail.get(group).remove(hive);
                honeyStatsGroupDetail.get(group).put(hive, data + weight);
            } else {
                honeyStatsGroupDetail.get(group).put(hive, weight);
            }
        } else {
            TreeMap<String, Double> data = new TreeMap<>();
            data.put(hive, weight);
            honeyStatsGroupDetail.put(group, data);
        }
        honeySum += weight;
    }

    public TreeMap<String, Double> getGroupStats() {
        return honeyStatsGroup;
    }

    public TreeMap<String, TreeMap<String, Double>> getGroupDetailStats() {
        return honeyStatsGroupDetail;
    }

    public double getHoneySum() {
        return honeySum;
    }
}
