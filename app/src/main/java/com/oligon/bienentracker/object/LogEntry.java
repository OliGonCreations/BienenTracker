package com.oligon.bienentracker.object;

import android.content.Context;

import com.oligon.bienentracker.R;

import java.io.Serializable;
import java.util.Date;

public class LogEntry implements Serializable {

    private int mId = -1;
    private Hive mHive;
    private Activities mCommonActivities = new Activities();
    private Date mDate = new Date();
    private int mWeather = -1;
    private float mTemp;
    private Treatment mTreatment = null;
    private Food mFood = null;
    private Harvest mHarvest = null;
    private Inspection mInspection = null;


    public LogEntry(Hive mHive) {
        this.mHive = mHive;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public Hive getHive() {
        return mHive;
    }

    public void setHive(Hive mHive) {
        this.mHive = mHive;
    }

    public Activities getCommonActivities() {
        return mCommonActivities;
    }

    public void setCommonActivities(Activities mCommonActivities) {
        this.mCommonActivities = mCommonActivities;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date mDate) {
        this.mDate = mDate;
    }

    public int getWeatherCode() {
        return mWeather;
    }

    public void setWeatherCode(int mWeather) {
        this.mWeather = mWeather;
    }

    public float getTemp() {
        return mTemp;
    }

    public void setTemp(float mTemp) {
        this.mTemp = mTemp;
    }

    public Treatment getTreatment() {
        return mTreatment;
    }

    public void setTreatment(Treatment mTreatment) {
        this.mTreatment = mTreatment;
    }

    public Food getFood() {
        return mFood;
    }

    public void setFood(Food mFood) {
        this.mFood = mFood;
    }

    public Harvest getHarvest() {
        return mHarvest;
    }

    public void setHarvest(Harvest mHarvest) {
        this.mHarvest = mHarvest;
    }

    public Inspection getInspection() {
        return mInspection;
    }

    public void setInspection(Inspection mInspection) {
        this.mInspection = mInspection;
    }

    public boolean hasTreatment() {
        return mTreatment != null;
    }

    public boolean hasFood() {
        return mFood != null;
    }

    public boolean hasHarvest() {
        return mHarvest != null;
    }

    public boolean hasInspection() {
        return mInspection != null && (mInspection.getVarroa() != 0 || mInspection.getWeight() != 0
                || !mInspection.getNote().isEmpty() || mInspection.hasQueen()
                || mInspection.hasQueenless() || mInspection.hasBrood() || mInspection.hasPins());
    }

    public boolean hasActivities() {
        return mCommonActivities != null && (mCommonActivities.getDrones() != 0 ||
                mCommonActivities.getBrood() != 0 || mCommonActivities.getEmpty() != 0 ||
                mCommonActivities.getFood() != 0 || mCommonActivities.getMiddle() != 0 ||
                mCommonActivities.getHoneyRoom() != 0 || mCommonActivities.getBox() != 0 ||
                mCommonActivities.getEscape() != 0 || mCommonActivities.getFence() != 0 ||
                mCommonActivities.getDiaper() != 0 || !mCommonActivities.getOther().isEmpty());
    }

    public String getFoodText() {
        return mFood.amountAsString() + " " + mFood.getFood();
    }

    public String getTreatmentText() {
        return mTreatment.amountAsString() + " " + mTreatment.getTreatment();
    }

    public String getHarvestText(Context context) {
        return context.getResources().getQuantityString(R.plurals.harvest_text, mHarvest.getCombCount(), mHarvest.weightAsString(), mHarvest.getCombCount());
    }

    public String getActivitiesText(Context context) {
        StringBuilder builder = new StringBuilder();
        if (mCommonActivities.getDrones() != 0) {
            appendNumber(builder, mCommonActivities.getDrones());
            builder.append(context.getResources().getString(R.string.common_drone));
            appendEnding(builder, mCommonActivities.getDrones());
        }
        if (mCommonActivities.getBrood() != 0) {
            appendNumber(builder, mCommonActivities.getBrood());
            builder.append(context.getResources().getString(R.string.common_brood));
            appendEnding(builder, mCommonActivities.getBrood());
        }
        if (mCommonActivities.getEmpty() != 0) {
            appendNumber(builder, mCommonActivities.getEmpty());
            builder.append(context.getResources().getString(R.string.common_empty));
            appendEnding(builder, mCommonActivities.getEmpty());
        }
        if (mCommonActivities.getFood() != 0) {
            appendNumber(builder, mCommonActivities.getFood());
            builder.append(context.getResources().getString(R.string.common_food));
            appendEnding(builder, mCommonActivities.getFood());
        }
        if (mCommonActivities.getMiddle() != 0) {
            appendNumber(builder, mCommonActivities.getMiddle());
            builder.append(context.getResources().getString(R.string.common_middle));
            appendEnding(builder, mCommonActivities.getMiddle());
        }
        if (mCommonActivities.getHoneyRoom() != 0) {
            appendNumber(builder, mCommonActivities.getHoneyRoom());
            builder.append(context.getResources().getString(R.string.common_honey));
            appendEnding(builder, mCommonActivities.getHoneyRoom());
        }
        if (mCommonActivities.getBox() != 0) {
            appendNumber(builder, mCommonActivities.getBox());
            builder.append(context.getResources().getString(R.string.common_stock));
            if (Math.abs(mCommonActivities.getBox()) > 1)
                builder.append("n");
            appendEnding(builder, mCommonActivities.getBox());
        }
        if (mCommonActivities.getEscape() != 0) {
            builder.append(context.getResources().getString(R.string.common_escape));
            appendEnding(builder, mCommonActivities.getEscape());
        }
        if (mCommonActivities.getFence() != 0) {
            builder.append(context.getResources().getString(R.string.common_fence));
            appendEnding(builder, mCommonActivities.getFence());
        }
        if (mCommonActivities.getDiaper() != 0) {
            builder.append(context.getResources().getString(R.string.common_diaper));
            appendEnding(builder, mCommonActivities.getDiaper());
        }
        if (!mCommonActivities.getOther().isEmpty()) {
            builder.append(mCommonActivities.getOther());
        }
        if (builder.length() != 0) return builder.toString();
        else return null;
    }

    private void appendEnding(StringBuilder builder, int value) {
        builder.append(" ");
        builder = value > 0 ? builder.append("hinzugefügt") : builder.append("entfernt");
        builder.append("\n");
    }

    private void appendNumber(StringBuilder builder, int value) {
        builder.append(Math.abs(value));
        builder.append(" ");
    }

    public String getInspectionText() {
        return mInspection.getInspectionText();
    }

    public String getWeatherString(Context context) {
        if (mWeather < 0)
            return "-";
        else if (mWeather < 100)
            return context.getResources().getStringArray(R.array.weather_conditions)[mWeather];
        else {
            String[] conditions = context.getResources().getStringArray(R.array.weather_conditions_open);
            switch (mWeather) {
                case 200:
                    return conditions[0];
                case 201:
                    return conditions[1];
                case 202:
                    return conditions[2];
                case 210:
                    return conditions[3];
                case 211:
                    return conditions[4];
                case 212:
                    return conditions[5];
                case 221:
                    return conditions[6];
                case 230:
                    return conditions[7];
                case 231:
                    return conditions[8];
                case 232:
                    return conditions[9];
                case 300:
                    return conditions[10];
                case 301:
                    return conditions[11];
                case 302:
                    return conditions[12];
                case 310:
                    return conditions[13];
                case 311:
                    return conditions[14];
                case 312:
                    return conditions[15];
                case 321:
                    return conditions[16];
                case 500:
                    return conditions[17];
                case 501:
                    return conditions[18];
                case 502:
                    return conditions[19];
                case 503:
                case 504:
                    return conditions[20];
                case 511:
                    return conditions[21];
                case 520:
                    return conditions[22];
                case 521:
                    return conditions[23];
                case 522:
                case 531:
                    return conditions[24];
                case 600:
                    return conditions[25];
                case 601:
                    return conditions[26];
                case 602:
                    return conditions[27];
                case 611:
                case 612:
                case 615:
                case 616:
                    return conditions[28];
                case 620:
                case 621:
                case 622:
                    return conditions[29];
                case 701:
                    return conditions[30];
                case 711:
                    return conditions[31];
                case 721:
                    return conditions[32];
                case 731:
                    return conditions[33];
                case 741:
                    return conditions[34];
                case 751:
                    return conditions[33];
                case 761:
                case 762:
                case 771:
                    return conditions[34];
                case 781:
                    return conditions[35];
                case 800:
                    return conditions[36];
                case 801:
                    return conditions[37];
                case 802:
                    return conditions[38];
                case 803:
                    return conditions[39];
                case 804:
                    return conditions[40];
                case 900:
                    return conditions[41];
                case 901:
                    return conditions[42];
                case 902:
                    return conditions[43];
                case 903:
                    return conditions[44];
                case 904:
                    return conditions[45];
                case 905:
                    return conditions[46];
                case 906:
                    return conditions[47];
                case 951:
                    return conditions[48];
                case 952:
                    return conditions[49];
                case 953:
                    return conditions[50];
                case 954:
                    return conditions[51];
                case 955:
                    return conditions[52];
                case 956:
                    return conditions[53];
                case 957:
                    return conditions[54];
                case 958:
                    return conditions[55];
                case 959:
                    return conditions[56];
                case 960:
                    return conditions[57];
                case 961:
                    return conditions[58];
                case 962:
                    return conditions[59];
                default:
                    return "-";
            }
        }
    }

    public int getWeatherImageId(Context context) {
        if (mWeather < 0)
            return R.drawable.ic_weather_load;
        else if (mWeather < 46)
            return context.getResources().obtainTypedArray(R.array.weather_icons).getResourceId(mWeather, R.drawable.ic_weather_load);
        else if (mWeather < 300)
            return R.drawable.ic_weather_lightning;
        else if (mWeather < 400)
            return R.drawable.ic_weather_drizzle;
        else if (mWeather < 600)
            return R.drawable.ic_weather_rain;
        else if (mWeather < 700)
            return R.drawable.ic_weather_snow;
        else if (mWeather < 800)
            return R.drawable.ic_weather_fog;
        else if (mWeather == 800)
            return R.drawable.ic_weather_sun;
        else if (mWeather < 804)
            return R.drawable.ic_weather_cloud_sun;
        else if (mWeather < 900)
            return R.drawable.ic_weather_cloud;
        else if (mWeather < 950)
            return R.drawable.ic_weather_cloud_wind;
        else if (mWeather < 1000)
            return R.drawable.ic_weather_wind;
        return R.drawable.ic_weather_load;
    }
}
