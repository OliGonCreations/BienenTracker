package com.oligon.bienentracker.util.object;

public class Job {

    public enum TYPE {
        COUNTABLE_FREE,
        COUNTABLE_LIMITED,
        CHECKABLE
    }

    private String mTitle;
    private TYPE mType;

    public Job(String title, TYPE type) {
        this.mTitle = title;
        this.mType = type;
    }

    public TYPE getType() {
        return mType;
    }

    public void setType(TYPE mType) {
        this.mType = mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }
}
