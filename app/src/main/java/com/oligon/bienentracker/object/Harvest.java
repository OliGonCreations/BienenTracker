package com.oligon.bienentracker.object;

import java.text.DecimalFormat;

public class Harvest {
    public static Unit intToUnit(int i) {
        if (i == 0) return Unit.KG;
        else if (i == 1) return Unit.G;
        else return Unit.KG;
    }

    public enum Unit {KG, G}

    private int mCombCount;
    private double mWeight;
    private Unit mUnit = Unit.KG;

    public Harvest() {
    }

    public Harvest(int mCombCount) {
        this.mCombCount = mCombCount;
    }

    public int getCombCount() {
        return mCombCount;
    }

    public void setCombCount(int mCombCount) {
        this.mCombCount = mCombCount;
    }

    public double getWeight() {
        return mWeight;
    }

    public void setWeight(double mWeight) {
        this.mWeight = mWeight;
    }

    public void setUnit(Unit mUnit) {
        this.mUnit = mUnit;
    }

    private String getUnitAsString() {
        if (mUnit == Unit.KG) return "kg";
        return "g";
    }

    public String weightAsString() {
        DecimalFormat format = new DecimalFormat("0.###");
        return format.format(mWeight) + " " + getUnitAsString();
    }
}
