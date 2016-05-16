package com.oligon.bienentracker.object;

import java.util.TreeMap;

public class StatisticsEarnings {

    private double honeySum;
    private int combSum;
    private TreeMap<String, Double> honeyStats;
    private TreeMap<String, Integer> combStats;

    private TreeMap<String, Double> honeyStatsGroup;
    private TreeMap<String, TreeMap<String, Double>> honeyStatsGroupDetail;

    public StatisticsEarnings() {
        honeyStats = new TreeMap<>();
        combStats = new TreeMap<>();
        honeyStatsGroup = new TreeMap<>();
        honeyStatsGroupDetail = new TreeMap<>();
    }

    public void addHoney(String hive, String group, double weight, double combs) {
        if (hive == null) return;
        if (honeyStats.containsKey(hive)) {
            Double previous = honeyStats.get(hive);
            honeyStats.remove(hive);
            honeyStats.put(hive, previous + weight);
        } else {
            honeyStats.put(hive, weight);
        }
        if (group == null || group.isEmpty()) group = "Keine Gruppe";
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

    public TreeMap<String, Double> getHoneyStats() {
        return honeyStats;
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
