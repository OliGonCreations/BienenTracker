package com.oligon.bienentracker.object;

import java.io.Serializable;
import java.util.Date;

public class Reminder implements Serializable {

    private String mDescription;
    private Date mTime;

    public Reminder() {}

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public Date getTime() {
        return mTime;
    }

    public void setTime(Date mTime) {
        this.mTime = mTime;
    }
}
