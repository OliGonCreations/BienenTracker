package com.oligon.bienentracker.object;

import java.text.DecimalFormat;

public class Food {

    private String mFood;
    private double mAmount;
    private String mUnit;

    public Food() {
        mFood = "";
        mAmount = 0;
        mUnit = "l";
    }

    public String getFood() {
        return mFood;
    }

    public void setFood(String mType) {
        this.mFood = mType;
    }

    public double getAmount() {
        return mAmount;
    }

    public void setAmount(double mAmount) {
        this.mAmount = mAmount;
    }

    public String getUnit() {
        return mUnit;
    }

    public void setUnit(String mUnit) {
        this.mUnit = mUnit;
    }

    public String amountAsString() {
        DecimalFormat format = new DecimalFormat("0.###");
        return format.format(mAmount) + " " + getUnit();
    }
}
