package com.oligon.bienentracker.util.object;

public class Inspection {

    private boolean mQueenless, mQueen, mBrood, mPins;
    private float mVarroa = 0, mWeight = 0;
    private String mNote = "";

    public Inspection() {
    }

    public boolean hasQueenless() {
        return mQueenless;
    }

    public void setQueenless(boolean mQueenless) {
        this.mQueenless = mQueenless;
    }

    public boolean hasQueen() {
        return mQueen;
    }

    public void setQueen(boolean mQueen) {
        this.mQueen = mQueen;
    }

    public boolean hasBrood() {
        return mBrood;
    }

    public void setBrood(boolean mBrood) {
        this.mBrood = mBrood;
    }

    public boolean hasPins() {
        return mPins;
    }

    public void setPins(boolean mPins) {
        this.mPins = mPins;
    }

    public float getVarroa() {
        return mVarroa;
    }

    public void setVarroa(float mVarroa) {
        this.mVarroa = mVarroa;
    }

    public float getWeight() {
        return mWeight;
    }

    public void setWeight(float mWeight) {
        this.mWeight = mWeight;
    }

    public String getNote() {
        return mNote;
    }

    public void setNote(String mNote) {
        this.mNote = mNote;
    }

    public String getInspectionText() {
        StringBuilder string = new StringBuilder();
        if (mQueenless | mQueen | mBrood | mPins) {
            string.append("Gesichtet: ");
            if (mQueenless)
                string.append("Weiselzelle");
            if (mQueen)
                string.append(string.length() > 14 ? ", Königin" : "Königin");
            if (mBrood)
                string.append(string.length() > 14 ? ", Brut" : "Brut");
            if (mPins)
                string.append(string.length() > 14 ? ", Stifte" : "Stifte");

            string.append("\n");
        }
        if (mVarroa != 0) {
            string.append("Varroabefall: ");
            string.append(mVarroa);
            string.append(" Milben / Tag\n");
        }
        if (mWeight != 0) {
            string.append("Gewicht: ");
            string.append(mWeight);
            string.append(" kg\n");
        }
        if (!mNote.isEmpty()) {
            string.append("Notiz: ");
            string.append(mNote);
        }
        return string.toString();
    }
}
