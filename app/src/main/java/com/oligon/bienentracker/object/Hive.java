package com.oligon.bienentracker.object;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Hive implements Serializable {


    public enum Rating {
        GENTLENESS,
        ESCAPE,
        STRENGTH
    }

    private int mId = -1;
    private int mPosition = -1;
    private String mName = "", mLocation = "", mMarker = "", mInfo = "", mGroup = "";
    private int mYear = 0;
    private boolean isOffspring = false;
    private Map<Rating, Float> mRatings = new HashMap<>();
    private Reminder mReminder = null;

    public Hive(int mId) {
        this.mId = mId;
    }

    public int getId() {
        return mId;
    }

    public void setId(int _id) {
        this.mId = _id;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        this.mPosition = position;
    }

    public String getName() {
        return mName;
    }

    public void setName(String _name) {
        this.mName = _name;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String mPosition) {
        this.mLocation = mPosition;
    }

    public String getMarker() {
        return mMarker;
    }

    public void setMarker(String mMarker) {
        this.mMarker = mMarker;
    }

    public int getYear() {
        return mYear;
    }

    public void setYear(int _year) {
        this.mYear = _year;
    }

    public String getInfo() {
        return mInfo;
    }

    public void setInfo(String _info) {
        this.mInfo = _info;
    }

    public String getGroup() {
        return mGroup;
    }

    public void setGroup(String group) {
        this.mGroup = group;
    }

    public boolean isOffspring() {
        return isOffspring;
    }

    public void setType(boolean isOffspring) {
        this.isOffspring = isOffspring;
    }

    public void setRating(Rating rating, float value) {
        mRatings.put(rating, value);
    }

    public float getRating(Rating rating) {
        if (mRatings.containsKey(rating))
            return mRatings.get(rating);
        return 0;
    }

    public void setReminder(Reminder reminder) {
        this.mReminder = reminder;
    }

    public Reminder getReminder() {
        return mReminder;
    }

    public boolean hasReminder() {
        return mReminder != null && mReminder.getTime().getTime() != 0;
    }
}
