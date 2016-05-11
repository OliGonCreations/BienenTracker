package com.oligon.bienentracker.object;

import java.text.DecimalFormat;


public class Treatment {

    private double mWeight;
    private String mTreatment, mUnit = "ml";


    public Treatment(double mWeight, String mTreatment, String mUnit) {
        this.mWeight = mWeight;
        this.mTreatment = mTreatment;
        this.mUnit = mUnit;
    }

    public String getUnit() {
        return mUnit;
    }

    public double getWeight() {
        return mWeight;
    }

    public String getTreatment() {
        return mTreatment;
    }

    public String amountAsString() {
        DecimalFormat format = new DecimalFormat("0.###");
        return format.format(mWeight) + " " + mUnit;
    }
}
