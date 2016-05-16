package com.oligon.bienentracker.object;

import java.util.TreeMap;

public class StatisticsFood {

    private TreeMap<String, Double> foodStats;

    public StatisticsFood() {
        foodStats = new TreeMap<>();
    }

    public void addFood(String food, double value) {
        if (food == null) return;
        if (foodStats.containsKey(food)) {
            Double previous = foodStats.get(food);
            foodStats.remove(food);
            foodStats.put(food, previous + value);
        } else {
            foodStats.put(food, value);
        }
    }

    public TreeMap<String, Double> getFoodStats() {
        return foodStats;
    }

}
