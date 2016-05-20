package com.oligon.bienentracker.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.oligon.bienentracker.BeeApplication;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.Activities;
import com.oligon.bienentracker.object.Food;
import com.oligon.bienentracker.object.Harvest;
import com.oligon.bienentracker.object.Hive;
import com.oligon.bienentracker.object.Inspection;
import com.oligon.bienentracker.object.LogEntry;
import com.oligon.bienentracker.object.Reminder;
import com.oligon.bienentracker.object.StatisticsEarnings;
import com.oligon.bienentracker.object.StatisticsFood;
import com.oligon.bienentracker.object.Treatment;
import com.oligon.bienentracker.object.Trend;
import com.oligon.bienentracker.ui.activities.HomeActivity;

import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jxl.CellView;
import jxl.SheetSettings;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.write.Blank;
import jxl.write.DateFormat;
import jxl.write.DateTime;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

public class HiveDB extends SQLiteOpenHelper {

    private static Context mContext;
    public static HiveDB instance;

    private final static int DB_VERSION = 8;
    public final static String DB_NAME = "Hive.db";

    private final static String LOG_ID = "id";

    public final static String LOG_TABLE_NAME = "beeLog";
    private final static String LOG_DATE = "date";
    private final static String LOG_HIVE_ID = "hiveId";
    private final static String LOG_FOOD = "food";
    private final static String LOG_TREATMENT = "treatment";
    private final static String LOG_ACTIVITIES = "removal";
    private final static String LOG_HARVEST = "harvest";
    private final static String LOG_INSPECTION = "inspection";
    private final static String LOG_WEATHER = "weather";
    private final static String LOG_WEATHER_TEMP = "weatherTemp";

    private final static String LOG_HONEY_ROOM = "honeyRoom";
    private final static String LOG_BROOD = "broodFrame";
    private final static String LOG_DRONE = "droneFrame";
    private final static String LOG_EMPTY_FRAME = "emptyFrame";
    private final static String LOG_FOOD_FRAME = "foodFrame";
    private final static String LOG_MIDDLE = "middleFrame";
    private final static String LOG_BOX = "box";
    private final static String LOG_BEE_ESCAPE = "beeEscape";
    private final static String LOG_FENCE = "fence";
    private final static String LOG_DIAPER = "diaper";
    private final static String LOG_OTHER_ACT = "otherActivity";

    public final static String HIVE_TABLE_NAME = "hive";
    private final static String HIVE_ID = "hiveId";
    private final static String HIVE_NAME = "name";
    private final static String HIVE_POSITION = "position";
    private final static String HIVE_YEAR = "year";
    private final static String HIVE_MARKER = "marker";
    private final static String HIVE_OFFSPRING = "offspring";
    private final static String HIVE_SWARM = "swarm";
    private final static String HIVE_INFO = "info";
    private final static String HIVE_SORTER = "sorter";

    private final static String HIVE_GENTLE = "gentle";
    private final static String HIVE_ESCAPE = "escaping";
    private final static String HIVE_STRENGTH = "strength";
    private final static String HIVE_GROUP = "hivegroup";

    private final static String HIVE_REMINDER_TIME = "reminderTime";
    private final static String HIVE_REMINDER_DESCRIPTION = "reminderDescription";

    private final static String STING_TABLE_NAME = "stings";
    private final static String STING_ID = "stringId";
    private final static String STING_DATE = "date";
    private final static String STING_HIVE = "hive";
    private final static String STING_ALERGIC = "reaction";


    private final static String DB_CREATE_LOG = "CREATE TABLE " + LOG_TABLE_NAME + "("
            + LOG_ID + " INTEGER PRIMARY KEY, " + LOG_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + LOG_HIVE_ID + " INTEGER, " + LOG_FOOD + " TEXT, " + LOG_TREATMENT
            + " TEXT, "
            + LOG_DRONE + " INTEGER, "
            + LOG_BROOD + " INTEGER, "
            + LOG_EMPTY_FRAME + " INTEGER, "
            + LOG_FOOD_FRAME + " INTEGER, "
            + LOG_MIDDLE + " INTEGER, "
            + LOG_HONEY_ROOM + " INTEGER, "
            + LOG_BOX + " INTEGER, "
            + LOG_BEE_ESCAPE + " INTEGER, "
            + LOG_FENCE + " INTEGER, "
            + LOG_DIAPER + " INTEGER, "
            + LOG_OTHER_ACT + " TEXT, "
            + LOG_ACTIVITIES + " TEXT, " // REDUNDANCE
            + LOG_HARVEST + " TEXT, "
            + LOG_INSPECTION + " TEXT," + LOG_WEATHER + " INTEGER, " + LOG_WEATHER_TEMP + " REAL)";

    private final static String DB_CREATE_HIVE = "CREATE TABLE " + HIVE_TABLE_NAME + "("
            + HIVE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + HIVE_NAME + " TEXT, "
            + HIVE_POSITION + " TEXT, "
            + HIVE_YEAR + " INTEGER, "
            + HIVE_MARKER + " TEXT, "
            + HIVE_OFFSPRING + " INTEGER,"
            + HIVE_SWARM + " INTEGER,"
            + HIVE_INFO + " TEXT,"
            + HIVE_SORTER + " INTEGER, "
            + HIVE_GENTLE + " REAL, "
            + HIVE_ESCAPE + " REAL, "
            + HIVE_STRENGTH + " REAL,"
            + HIVE_REMINDER_TIME + " TEXT, "
            + HIVE_REMINDER_DESCRIPTION + " TEXT,"
            + HIVE_GROUP + " TEXT)";

    private final static String DB_CREATE_STING = "CREATE TABLE " + STING_TABLE_NAME + "("
            + STING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + STING_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
            + STING_HIVE + " TEXT, " + STING_ALERGIC + " TEXT)";

    private final static String ALTER_SORTER = "ALTER TABLE " + HIVE_TABLE_NAME + " ADD COLUMN "
            + HIVE_SORTER + " INTEGER";

    private final static String ALTER_GENTLE = "ALTER TABLE " + HIVE_TABLE_NAME + " ADD COLUMN "
            + HIVE_GENTLE + " REAL";
    private final static String ALTER_ESCAPE = "ALTER TABLE " + HIVE_TABLE_NAME + " ADD COLUMN "
            + HIVE_ESCAPE + " REAL";
    private final static String ALTER_STRENGTH = "ALTER TABLE " + HIVE_TABLE_NAME + " ADD COLUMN "
            + HIVE_STRENGTH + " REAL";
    private final static String ALTER_GROUP = "ALTER TABLE " + HIVE_TABLE_NAME + " ADD COLUMN "
            + HIVE_GROUP + " TEXT";

    private final static String ALTER_REMINDER_TIME = "ALTER TABLE " + HIVE_TABLE_NAME + " ADD COLUMN "
            + HIVE_REMINDER_TIME + " TEXT";
    private final static String ALTER_REMINDER_DESCR = "ALTER TABLE " + HIVE_TABLE_NAME + " ADD COLUMN "
            + HIVE_REMINDER_DESCRIPTION + " TEXT";

    private final static String ALTER_ACT_HONEY_ROOM = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_HONEY_ROOM + " INTEGER";
    private final static String ALTER_ACT_BROOD = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_BROOD + " INTEGER";
    private final static String ALTER_ACT_DRONE = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_DRONE + " INTEGER";
    private final static String ALTER_ACT_EMPTY = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_EMPTY_FRAME + " INTEGER";
    private final static String ALTER_ACT_FOOD = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_FOOD_FRAME + " INTEGER";
    private final static String ALTER_ACT_MIDDLE = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_MIDDLE + " INTEGER";
    private final static String ALTER_ACT_BOX = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_BOX + " INTEGER";
    private final static String ALTER_ACT_ESCAPE = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_BEE_ESCAPE + " INTEGER";
    private final static String ALTER_ACT_FENCE = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_FENCE + " INTEGER";
    private final static String ALTER_ACT_DIAPER = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_DIAPER + " INTEGER";
    private final static String ALTER_ACT_OTHER = "ALTER TABLE " + LOG_TABLE_NAME + " ADD COLUMN "
            + LOG_OTHER_ACT + " TEXT";

    private final static String ALTER_SWARM = "ALTER TABLE " + HIVE_TABLE_NAME + " ADD COLUMN "
            + HIVE_SWARM + " INTEGER";

    public static synchronized HiveDB getInstance(Context context) {
        if (instance == null) {
            instance = new HiveDB(context.getApplicationContext());
        }
        return instance;
    }

    public HiveDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CREATE_HIVE);
        db.execSQL(DB_CREATE_LOG);
        db.execSQL(DB_CREATE_STING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) { // Update to Version 2
            db.execSQL(ALTER_SORTER);
        }
        if (oldVersion < 3) { // Update to Version 3
            db.execSQL(ALTER_GENTLE);
            db.execSQL(ALTER_ESCAPE);
            db.execSQL(ALTER_STRENGTH);
        }
        if (oldVersion < 4) { // Update to Version 4
            db.execSQL(ALTER_ACT_DRONE);
            db.execSQL(ALTER_ACT_BROOD);
            db.execSQL(ALTER_ACT_EMPTY);
            db.execSQL(ALTER_ACT_FOOD);
            db.execSQL(ALTER_ACT_MIDDLE);
            db.execSQL(ALTER_ACT_HONEY_ROOM);
            db.execSQL(ALTER_ACT_BOX);
            db.execSQL(ALTER_ACT_ESCAPE);
            db.execSQL(ALTER_ACT_FENCE);
            db.execSQL(ALTER_ACT_DIAPER);
            db.execSQL(ALTER_ACT_OTHER);
            transferActivities(db);
        }
        if (oldVersion < 5) { // Update to Version 5
            db.execSQL(ALTER_REMINDER_TIME);
            db.execSQL(ALTER_REMINDER_DESCR);
        }
        if (oldVersion < 6) { // Update to Version 6
            db.execSQL(ALTER_GROUP);
        }
        if (oldVersion < 7) {
            db.execSQL(DB_CREATE_STING);
        }
        if (oldVersion < 8) { // Synchronisation Feature
            try {
                db.execSQL(ALTER_SWARM);
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
        Log.d(BeeApplication.TAG, "Downgrading SQL");
    }

    public int getVersion() {
        SQLiteDatabase db = this.getReadableDatabase();
        int version = db.getVersion();
        db.close();
        return version;
    }

    private void transferActivities(SQLiteDatabase db) {
        Cursor cur = db.rawQuery("SELECT * FROM " + LOG_TABLE_NAME, null);
        cur.moveToFirst();
        ContentValues values = new ContentValues();
        while (!cur.isAfterLast()) {
            String acts = cur.getString(cur.getColumnIndex(LOG_ACTIVITIES));
            String[] array = acts.split(";");
            values.put(LOG_DRONE, array[0]);
            values.put(LOG_EMPTY_FRAME, array[1]);
            values.put(LOG_FOOD_FRAME, array[2]);
            values.put(LOG_MIDDLE, array[3]);
            values.put(LOG_BOX, array[4]);
            values.put(LOG_BEE_ESCAPE, array[5]);
            values.put(LOG_FENCE, array[6]);
            values.put(LOG_DIAPER, array[7]);
            if (array.length > 8)
                values.put(LOG_OTHER_ACT, array[8]);

            db.update(LOG_TABLE_NAME, values, LOG_ID + " = ?",
                    new String[]{cur.getString(cur.getColumnIndex(LOG_ID))});
            values.clear();
            cur.moveToNext();
        }
        cur.close();
    }

    public void editHive(Hive hive) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HIVE_NAME, hive.getName());
        values.put(HIVE_POSITION, hive.getLocation());
        values.put(HIVE_YEAR, hive.getYear());
        values.put(HIVE_MARKER, hive.getMarker());
        values.put(HIVE_OFFSPRING, hive.isOffspring() ? 1 : 0);
        values.put(HIVE_SWARM, hive.isSwarm() ? 1 : 0);
        values.put(HIVE_INFO, hive.getInfo());
        values.put(HIVE_GROUP, hive.getGroup());

        values.put(HIVE_GENTLE, hive.getRating(Hive.Rating.GENTLENESS));
        values.put(HIVE_ESCAPE, hive.getRating(Hive.Rating.ESCAPE));
        values.put(HIVE_STRENGTH, hive.getRating(Hive.Rating.STRENGTH));

        if (hive.getId() != -1)
            db.update(HIVE_TABLE_NAME, values, HIVE_ID + " = ? ", new String[]{Integer.toString(hive.getId())});
        else
            db.insert(HIVE_TABLE_NAME, null, values);
        db.close();
        HomeActivity.dbChanged = true;
    }

    public void updateHivePosition(int id, int position) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HIVE_SORTER, position);
        db.update(HIVE_TABLE_NAME, values, HIVE_ID + " = ?", new String[]{Integer.toString(id)});
        db.close();
        HomeActivity.dbChanged = true;
    }

    public void deleteHive(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HIVE_TABLE_NAME,
                HIVE_ID + " = ? ",
                new String[]{Integer.toString(id)});
        db.close();
        HomeActivity.dbChanged = true;
    }

    public void updateHiveReminder(int id, Reminder reminder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HIVE_REMINDER_TIME, getDateTime(reminder.getTime()));
        values.put(HIVE_REMINDER_DESCRIPTION, reminder.getDescription());
        db.update(HIVE_TABLE_NAME, values, HIVE_ID + " = ?", new String[]{Integer.toString(id)});
        db.close();
        HomeActivity.dbChanged = true;
    }

    public void removeHiveReminder(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(HIVE_REMINDER_TIME, "");
        values.put(HIVE_REMINDER_DESCRIPTION, "");
        db.update(HIVE_TABLE_NAME, values, HIVE_ID + " = ?", new String[]{Integer.toString(id)});
        db.close();
        HomeActivity.dbChanged = true;
    }

    public ArrayList<Hive> getAllHives() {
        ArrayList<Hive> hives = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        try {
            res = db.rawQuery("SELECT * FROM " + HIVE_TABLE_NAME + " ORDER BY " + HIVE_SORTER, null);
        } catch (Exception e) {
            e.printStackTrace();
            return hives;
        }
        res.moveToFirst();

        while (!res.isAfterLast()) {
            Hive hive = new Hive(res.getInt(res.getColumnIndex(HIVE_ID)));
            hive.setName(res.getString(res.getColumnIndex(HIVE_NAME)));
            hive.setLocation(res.getString(res.getColumnIndex(HIVE_POSITION)));
            hive.setYear(res.getInt(res.getColumnIndex(HIVE_YEAR)));
            hive.setMarker(res.getString(res.getColumnIndex(HIVE_MARKER)));
            hive.setOffspring(res.getInt(res.getColumnIndex(HIVE_OFFSPRING)) == 1);
            hive.setSwarm(res.getInt(res.getColumnIndex(HIVE_SWARM)) == 1);
            hive.setInfo(res.getString(res.getColumnIndex(HIVE_INFO)));
            hive.setGroup(res.getString(res.getColumnIndex(HIVE_GROUP)));

            hive.setRating(Hive.Rating.GENTLENESS, res.getFloat(res.getColumnIndex(HIVE_GENTLE)));
            hive.setRating(Hive.Rating.ESCAPE, res.getFloat(res.getColumnIndex(HIVE_ESCAPE)));
            hive.setRating(Hive.Rating.STRENGTH, res.getFloat(res.getColumnIndex(HIVE_STRENGTH)));

            Reminder reminder = new Reminder();
            String s = res.getString(res.getColumnIndex(HIVE_REMINDER_TIME));
            if (s != null && !s.isEmpty()) {
                reminder.setTime(getDateFromString(s));
            } else {
                reminder.setTime(new Date(0));
            }
            reminder.setDescription(res.getString(res.getColumnIndex(HIVE_REMINDER_DESCRIPTION)));
            hive.setReminder(reminder);

            hives.add(hive);
            res.moveToNext();
        }
        res.close();
        db.close();
        return hives;
    }

    public ArrayList<Hive> getAllHivesByGroup(String group) {
        ArrayList<Hive> hives = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        try {
            res = db.rawQuery("SELECT * FROM " + HIVE_TABLE_NAME + " WHERE " + HIVE_GROUP + " = '" + group
                    + "' ORDER BY " + HIVE_SORTER, null);
        } catch (Exception e) {
            e.printStackTrace();
            return hives;
        }
        res.moveToFirst();

        while (!res.isAfterLast()) {
            Hive hive = new Hive(res.getInt(res.getColumnIndex(HIVE_ID)));
            hive.setName(res.getString(res.getColumnIndex(HIVE_NAME)));
            hive.setLocation(res.getString(res.getColumnIndex(HIVE_POSITION)));
            hive.setYear(res.getInt(res.getColumnIndex(HIVE_YEAR)));
            hive.setMarker(res.getString(res.getColumnIndex(HIVE_MARKER)));
            hive.setOffspring(res.getInt(res.getColumnIndex(HIVE_OFFSPRING)) == 1);
            hive.setSwarm(res.getInt(res.getColumnIndex(HIVE_SWARM)) == 1);
            hive.setInfo(res.getString(res.getColumnIndex(HIVE_INFO)));
            hive.setGroup(res.getString(res.getColumnIndex(HIVE_GROUP)));

            hive.setRating(Hive.Rating.GENTLENESS, res.getFloat(res.getColumnIndex(HIVE_GENTLE)));
            hive.setRating(Hive.Rating.ESCAPE, res.getFloat(res.getColumnIndex(HIVE_ESCAPE)));
            hive.setRating(Hive.Rating.STRENGTH, res.getFloat(res.getColumnIndex(HIVE_STRENGTH)));

            Reminder reminder = new Reminder();
            String s = res.getString(res.getColumnIndex(HIVE_REMINDER_TIME));
            if (s != null && !s.isEmpty()) {
                reminder.setTime(getDateFromString(s));
            } else {
                reminder.setTime(new Date(0));
            }
            reminder.setDescription(res.getString(res.getColumnIndex(HIVE_REMINDER_DESCRIPTION)));
            hive.setReminder(reminder);

            hives.add(hive);
            res.moveToNext();
        }
        res.close();
        db.close();
        return hives;
    }

    public Hive getHive(int id) {
        Hive hive = new Hive(id);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + HIVE_TABLE_NAME + " WHERE " + HIVE_ID + " = ?", new String[]{String.valueOf(id)});
        if (res.moveToFirst()) {
            hive.setName(res.getString(res.getColumnIndex(HIVE_NAME)));
            hive.setLocation(res.getString(res.getColumnIndex(HIVE_POSITION)));
            hive.setYear(res.getInt(res.getColumnIndex(HIVE_YEAR)));
            hive.setMarker(res.getString(res.getColumnIndex(HIVE_MARKER)));
            hive.setOffspring(res.getInt(res.getColumnIndex(HIVE_OFFSPRING)) == 1);
            hive.setSwarm(res.getInt(res.getColumnIndex(HIVE_SWARM)) == 1);
            hive.setInfo(res.getString(res.getColumnIndex(HIVE_INFO)));
            hive.setGroup(res.getString(res.getColumnIndex(HIVE_GROUP)));

            hive.setRating(Hive.Rating.GENTLENESS, res.getFloat(res.getColumnIndex(HIVE_GENTLE)));
            hive.setRating(Hive.Rating.ESCAPE, res.getFloat(res.getColumnIndex(HIVE_ESCAPE)));
            hive.setRating(Hive.Rating.STRENGTH, res.getFloat(res.getColumnIndex(HIVE_STRENGTH)));

            Reminder reminder = new Reminder();
            String s = res.getString(res.getColumnIndex(HIVE_REMINDER_TIME));
            if (s != null && !s.isEmpty()) {
                reminder.setTime(getDateFromString(s));
            } else {
                reminder.setTime(new Date(0));
            }
            reminder.setDescription(res.getString(res.getColumnIndex(HIVE_REMINDER_DESCRIPTION)));
            hive.setReminder(reminder);

            res.close();
            db.close();
            return hive;
        }
        res.close();
        db.close();
        return null;
    }

    public void addLog(LogEntry log) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LOG_DATE, getDateTime(log.getDate()));
        values.put(LOG_HIVE_ID, log.getHive().getId());
        values.put(LOG_FOOD, parseFood(log.getFood()));
        values.put(LOG_TREATMENT, parseTreatment(log.getTreatment()));
        values.put(LOG_HARVEST, parseHarvest(log.getHarvest()));
        values.put(LOG_INSPECTION, parseInspection(log.getInspection()));
        values.put(LOG_WEATHER, log.getWeatherCode());
        values.put(LOG_WEATHER_TEMP, log.getTemp());

        Activities mActivities = log.getCommonActivities();
        if (mActivities != null) {
            values.put(LOG_DRONE, mActivities.getDrones());
            values.put(LOG_BROOD, mActivities.getBrood());
            values.put(LOG_EMPTY_FRAME, mActivities.getEmpty());
            values.put(LOG_FOOD_FRAME, mActivities.getFood());
            values.put(LOG_MIDDLE, mActivities.getMiddle());
            values.put(LOG_HONEY_ROOM, mActivities.getHoneyRoom());
            values.put(LOG_BOX, mActivities.getBox());
            values.put(LOG_BEE_ESCAPE, mActivities.getEscape());
            values.put(LOG_FENCE, mActivities.getFence());
            values.put(LOG_DIAPER, mActivities.getDiaper());
            values.put(LOG_OTHER_ACT, mActivities.getOther());
        }

        if (log.getId() != -1) {
            db.update(LOG_TABLE_NAME, values, LOG_ID + " = ?",
                    new String[]{String.valueOf(log.getId())});
        } else
            db.insert(LOG_TABLE_NAME, null, values);
        db.close();
        HomeActivity.dbChanged = true;
    }

    public ArrayList<LogEntry> getAllLogs(int id, boolean reverse, int l) {
        String order = reverse ? " DESC" : " ASC";
        String limit = l == 0 ? "" : " LIMIT " + l;
        ArrayList<LogEntry> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + LOG_TABLE_NAME
                + " WHERE " + LOG_HIVE_ID + " = ? " + " ORDER BY " + LOG_DATE + order + limit, new String[]{String.valueOf(id)});
        cur.moveToFirst();

        while (!cur.isAfterLast()) {
            LogEntry log = new LogEntry(getHive(cur.getInt(cur.getColumnIndex(LOG_HIVE_ID))));
            log.setId(cur.getInt(cur.getColumnIndex(LOG_ID)));
            log.setDate(getDateFromString(cur.getString(cur.getColumnIndex(LOG_DATE))));
            log.setWeatherCode(cur.getInt(cur.getColumnIndex(LOG_WEATHER)));
            log.setTemp(cur.getFloat(cur.getColumnIndex(LOG_WEATHER_TEMP)));
            log.setTreatment(getTreatment(cur.getString(cur.getColumnIndex(LOG_TREATMENT))));
            log.setFood(getFood(cur.getString(cur.getColumnIndex(LOG_FOOD))));
            log.setHarvest(getHarvest(cur.getString(cur.getColumnIndex(LOG_HARVEST))));
            log.setCommonActivities(getActivities(cur));
            log.setInspection(getInspection(cur.getString(cur.getColumnIndex(LOG_INSPECTION))));
            logs.add(log);
            cur.moveToNext();
        }
        cur.close();
        db.close();
        return logs;
    }

    public LogEntry getLog(int id) {
        LogEntry log = new LogEntry(getHive(id));
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + LOG_TABLE_NAME + " WHERE " + LOG_HIVE_ID + " = ? " + " ORDER BY " + LOG_DATE + " DESC Limit 1 ",
                new String[]{String.valueOf(id)});
        if (cur.moveToFirst()) {
            log.setId(cur.getInt(cur.getColumnIndex(LOG_ID)));
            log.setDate(getDateFromString(cur.getString(cur.getColumnIndex(LOG_DATE))));
            log.setWeatherCode(cur.getInt(cur.getColumnIndex(LOG_WEATHER)));
            log.setTemp(cur.getFloat(cur.getColumnIndex(LOG_WEATHER_TEMP)));
            log.setTreatment(getTreatment(cur.getString(cur.getColumnIndex(LOG_TREATMENT))));
            log.setFood(getFood(cur.getString(cur.getColumnIndex(LOG_FOOD))));
            log.setHarvest(getHarvest(cur.getString(cur.getColumnIndex(LOG_HARVEST))));
            log.setCommonActivities(getActivities(cur));
            log.setInspection(getInspection(cur.getString(cur.getColumnIndex(LOG_INSPECTION))));
        } else return null;
        cur.close();
        db.close();
        return log;
    }

    public LogEntry getLogById(int logId) {
        LogEntry log = new LogEntry(null);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + LOG_TABLE_NAME + " WHERE " + LOG_ID + " = ? ",
                new String[]{String.valueOf(logId)});
        if (cur.moveToFirst()) {
            log.setId(cur.getInt(cur.getColumnIndex(LOG_ID)));
            log.setHive(new Hive(cur.getInt(cur.getColumnIndex(LOG_HIVE_ID))));
            log.setDate(getDateFromString(cur.getString(cur.getColumnIndex(LOG_DATE))));
            log.setWeatherCode(cur.getInt(cur.getColumnIndex(LOG_WEATHER)));
            log.setTemp(cur.getFloat(cur.getColumnIndex(LOG_WEATHER_TEMP)));
            log.setTreatment(getTreatment(cur.getString(cur.getColumnIndex(LOG_TREATMENT))));
            log.setFood(getFood(cur.getString(cur.getColumnIndex(LOG_FOOD))));
            log.setHarvest(getHarvest(cur.getString(cur.getColumnIndex(LOG_HARVEST))));
            log.setCommonActivities(getActivities(cur));
            log.setInspection(getInspection(cur.getString(cur.getColumnIndex(LOG_INSPECTION))));
            cur.close();
            db.close();
        } else return null;
        cur.close();
        db.close();
        return log;
    }

    public void deleteLog(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(LOG_TABLE_NAME,
                LOG_ID + " = ? ",
                new String[]{Integer.toString(id)});
        db.close();
        HomeActivity.dbChanged = true;
    }

    private String parseTreatment(Treatment t) {
        if (t != null)
            return t.getTreatment() + ";" + String.valueOf(t.getWeight()) + ";" + t.getUnit();
        return "";
    }

    private String parseFood(Food f) {
        if (f != null)
            return f.getFood() + ";" + String.valueOf(f.getAmount() + ";" + f.getUnit());
        return "";
    }

    private String parseHarvest(Harvest h) {
        if (h != null)
            return String.valueOf(h.getCombCount()) + ";" + String.valueOf(h.getWeight());
        return "";
    }

    private String parseInspection(Inspection i) {
        if (i != null)
            return String.valueOf(i.hasQueenless()) +
                    ";" +
                    i.hasQueen() +
                    ";" +
                    i.hasBrood() +
                    ";" +
                    i.getVarroa() +
                    ";" +
                    i.getWeight() +
                    ";" +
                    i.getNote() +
                    ";" +
                    i.hasPins();
        return "";
    }

    private Treatment getTreatment(String s) {
        if (!s.isEmpty()) {
            String[] array = s.split(";");
            String unit = "ml";
            if (array.length > 2) unit = array[2];
            return new Treatment(Double.parseDouble(array[1]), array[0], unit);
        }
        return null;
    }

    private Food getFood(String s) {
        if (!s.isEmpty()) {
            Food food = new Food();
            String[] array = s.split(";");
            food.setFood(array[0]);
            food.setAmount(Double.parseDouble(array[1]));
            food.setUnit(array[2]);
            return food;
        }
        return null;
    }

    private Harvest getHarvest(String s) {
        if (!s.isEmpty()) {
            String[] arr = s.split(";");
            Harvest h = new Harvest();
            h.setCombCount(Integer.valueOf(arr[0]));
            h.setWeight(Double.valueOf(arr[1]));
            return h;
        }
        return null;
    }

    private Activities getActivities(Cursor c) {
        Activities act = new Activities();
        act.setDrones(c.getInt(c.getColumnIndex(LOG_DRONE)));
        act.setBrood(c.getInt(c.getColumnIndex(LOG_BROOD)));
        act.setEmpty(c.getInt(c.getColumnIndex(LOG_EMPTY_FRAME)));
        act.setFood(c.getInt(c.getColumnIndex(LOG_FOOD_FRAME)));
        act.setMiddle(c.getInt(c.getColumnIndex(LOG_MIDDLE)));
        act.setHoneyRoom(c.getInt(c.getColumnIndex(LOG_HONEY_ROOM)));
        act.setBox(c.getInt(c.getColumnIndex(LOG_BOX)));
        act.setEscape(c.getInt(c.getColumnIndex(LOG_BEE_ESCAPE)));
        act.setFence(c.getInt(c.getColumnIndex(LOG_FENCE)));
        act.setDiaper(c.getInt(c.getColumnIndex(LOG_DIAPER)));
        act.setOther(c.getString(c.getColumnIndex(LOG_OTHER_ACT)));
        if (act.getOther() == null) act.setOther("");
        return act;
    }

    private Inspection getInspection(String s) {
        if (!s.isEmpty()) {
            Inspection inspection = new Inspection();
            String[] split = s.split(";");
            inspection.setQueenless(split[0].equals("true"));
            inspection.setQueen(split[1].equals("true"));
            inspection.setBrood(split[2].equals("true"));
            if (split.length > 3) {
                if (split[3].isEmpty())
                    inspection.setVarroa(0);
                else
                    inspection.setVarroa(Float.parseFloat(split[3]));
            }
            if (split.length > 4) {
                if (split[4].isEmpty())
                    inspection.setWeight(0);
                else
                    inspection.setWeight(Float.parseFloat(split[4]));
            }
            if (split.length > 5) inspection.setNote(split[5]);
            if (split.length > 6) inspection.setPins(split[6].equals("true"));
            return inspection;
        }
        return null;
    }

    private String getDateTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(date);
    }

    private Date getDateFromString(String s) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.parse(s, new ParsePosition(0));
    }

    /* Sting Statistic */

    public void addSting(Date date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(STING_DATE, getDateTime(date));
        values.put(STING_ALERGIC, "");
        values.put(STING_HIVE, "");
        db.insert(STING_TABLE_NAME, null, values);
        db.close();
    }

    public void removeLastSting() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(STING_TABLE_NAME, STING_ID + " = (SELECT MAX(" + STING_ID + ") FROM " + STING_TABLE_NAME + ")", null);
        db.close();
    }

    public int getStingCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = db.query(STING_TABLE_NAME, new String[]{STING_ID}, null, null, null, null, null).getCount();
        db.close();
        return count;
    }


    /* Export To Excel Operations*/
    @SuppressWarnings("ConstantConditions")
    public String exportToExcel(String filename) {
        // Data
        String[] activities = mContext.getResources().getStringArray(R.array.activities);
        String[] inspections = mContext.getResources().getStringArray(R.array.inspections);
        List<Hive> hives = getAllHives();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);

        //Saving file in external storage
        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + "/Bienen Tracker");

        //create directory if not exist
        if (!directory.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }

        //file path
        File file = new File(directory, filename);

        WorkbookSettings wbSettings = new WorkbookSettings();
        wbSettings.setLocale(Locale.getDefault());
        WritableWorkbook workbook;


        try {
            workbook = Workbook.createWorkbook(file, wbSettings);

            // Label formats
            WritableCellFormat arial12BoldFormat = new WritableCellFormat(new WritableFont(WritableFont.ARIAL, 12, WritableFont.BOLD));
            WritableCellFormat line = new WritableCellFormat();
            WritableCellFormat line_center = new WritableCellFormat();
            WritableCellFormat right = new WritableCellFormat();
            WritableCellFormat left = new WritableCellFormat();
            WritableCellFormat center = new WritableCellFormat();

            WritableCellFormat wcf = new WritableCellFormat(new WritableFont(WritableFont.ARIAL, 24, WritableFont.BOLD));

            try {
                arial12BoldFormat.setAlignment(Alignment.CENTRE);
                line.setBorder(Border.BOTTOM, BorderLineStyle.MEDIUM);
                line_center.setBorder(Border.BOTTOM, BorderLineStyle.MEDIUM);
                line.setAlignment(Alignment.LEFT);
                line_center.setAlignment(Alignment.CENTRE);
                right.setAlignment(Alignment.RIGHT);
                left.setAlignment(Alignment.LEFT);
                center.setAlignment(Alignment.CENTRE);
                wcf.setAlignment(Alignment.CENTRE);
            } catch (WriteException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < hives.size(); i++) {
                Hive hive = hives.get(i);
                List<LogEntry> logEntries = getAllLogs(hive.getId(), false, 0);
                WritableSheet sheet = workbook.createSheet(hive.getName(), i);
                SheetSettings settings = sheet.getSettings();
                settings.setFitWidth(1);
                try {

                    // Title Labels
                    sheet.addCell(new Label(0, 0, "Stockkarte", wcf));
                    sheet.mergeCells(0, 0, 16, 0);
                    sheet.addCell(new Label(0, 1, "Stock-Info", arial12BoldFormat));
                    sheet.addCell(new Label(3, 1, "Imker-Info", arial12BoldFormat));
                    sheet.addCell(new Label(0, 8, "Datum", arial12BoldFormat));
                    sheet.addCell(new Label(1, 8, "Wetterlage", arial12BoldFormat));
                    sheet.addCell(new Label(2, 8, "Temp.", arial12BoldFormat));
                    sheet.addCell(new Label(2, 9, "[°C]"));
                    sheet.addCell(new Label(3, 8, "Allgemeiner Befund", arial12BoldFormat));
                    sheet.addCell(new Label(9, 8, "Gegeben (+) , Entnommen (-)", arial12BoldFormat));
                    sheet.mergeCells(3, 8, 8, 8);
                    sheet.mergeCells(9, 8, 18, 8);

                    sheet.addCell(new Label(0, 2, "Name:", right));
                    sheet.addCell(new Label(0, 3, "Lage:", right));
                    sheet.addCell(new Label(0, 4, "Jahrgang:", right));
                    sheet.addCell(new Label(0, 5, "Zeichen:", right));
                    sheet.addCell(new Label(0, 6, "Bemerkung:", right));

                    // Beekeeper information
                    sheet.addCell(new Label(3, 2, "Name:", right));
                    sheet.addCell(new Label(3, 3, "Registrierungs-Id:", right));
                    sheet.addCell(new Label(9, 2, sp.getString("pref_user_name", "-"), left));
                    sheet.addCell(new Label(9, 3, sp.getString("pref_user_id", "-"), left));
                    sheet.mergeCells(3, 1, 8, 1);
                    sheet.mergeCells(3, 2, 8, 2);
                    sheet.mergeCells(3, 3, 8, 3);
                    sheet.mergeCells(9, 2, 18, 2);
                    sheet.mergeCells(9, 3, 18, 3);

                    // Hive description
                    sheet.addCell(new Label(1, 2, hive.getName(), right));
                    sheet.addCell(new Label(1, 3, hive.getLocation(), right));
                    sheet.addCell(new Label(1, 4, hive.getYear() != 0 ? String.valueOf(hive.getYear()) : "", right));
                    sheet.addCell(new Label(1, 5, hive.getMarker(), right));
                    sheet.addCell(new Label(1, 6, hive.getInfo(), right));
                    ArrayList<String> hiveType = new ArrayList<>();
                    if (hive.isSwarm())
                        hiveType.add(mContext.getString(R.string.hive_swarm));
                    if (hive.isOffspring())
                        hiveType.add(mContext.getString(R.string.hive_offspring));
                    sheet.addCell(new Label(1, 7, TextUtils.join(", ", hiveType.toArray()), right));

                    sheet.setRowView(0, 36 * 20);
                    sheet.setRowView(1, 20 * 20);
                    sheet.setRowView(8, 20 * 20);

                    sheet.addCell(new Blank(0, 9, line));
                    sheet.addCell(new Blank(1, 9, line));
                    sheet.addCell(new Label(2, 9, "[°C]", line_center));
                    sheet.setColumnView(0, 14);
                    sheet.setColumnView(1, 14);
                    sheet.setColumnView(2, 8);
                    sheet.setColumnView(19, 30);
                    for (int col = 0; col < inspections.length; col++) {
                        sheet.addCell(new Label(col + 3, 9, inspections[col], line));
                        sheet.setColumnView(col + 3, 4);
                    }

                    for (int col = 0; col < activities.length; col++) {
                        sheet.addCell(new Label(9 + col, 9, activities[col], line));
                        sheet.setColumnView(9 + col, 4);
                    }
                    sheet.addCell(new Label(19, 8, "Tätigkeit", arial12BoldFormat));
                    sheet.addCell(new Label(20, 8, "Notiz", arial12BoldFormat));
                    sheet.addCell(new Blank(19, 9, line));
                    sheet.addCell(new Blank(20, 9, line));

                    int row = 10;
                    for (LogEntry entry : logEntries) {
                        DateFormat customDateFormat = new DateFormat("dd.MM.yy");
                        WritableCellFormat dateFormat = new WritableCellFormat(customDateFormat);
                        sheet.addCell(new DateTime(0, row, entry.getDate(), dateFormat));
                        sheet.addCell(new Label(1, row, entry.getWeatherString(mContext), center));
                        sheet.addCell(new Label(2, row, entry.getWeatherCode() != -1 ?
                                String.format(Locale.getDefault(), "%.1f", entry.getTemp()) : "-", center));
                        Inspection ins = entry.getInspection();
                        if (ins != null) {
                            if (ins.hasQueenless()) sheet.addCell(new Label(3, row, "x", center));
                            if (ins.hasQueen()) sheet.addCell(new Label(4, row, "x", center));
                            if (ins.hasBrood()) sheet.addCell(new Label(5, row, "x", center));
                            if (ins.hasPins()) sheet.addCell(new Label(6, row, "x", center));
                            if (ins.getVarroa() != 0)
                                sheet.addCell(new Label(7, row, String.valueOf(ins.getVarroa()), center));
                            if (ins.getWeight() != 0)
                                sheet.addCell(new Label(8, row, String.valueOf(ins.getWeight()), center));
                            sheet.addCell(new Label(20, row, ins.getNote()));
                        }

                        Activities act = entry.getCommonActivities();
                        sheet.addCell(new Label(9, row, act.getDrones() == 0 ? "/" : String.valueOf(act.getDrones()), center));
                        sheet.addCell(new Label(10, row, act.getBrood() == 0 ? "/" : String.valueOf(act.getBrood()), center));
                        sheet.addCell(new Label(11, row, act.getEmpty() == 0 ? "/" : String.valueOf(act.getEmpty()), center));
                        sheet.addCell(new Label(12, row, act.getFood() == 0 ? "/" : String.valueOf(act.getFood()), center));
                        sheet.addCell(new Label(13, row, act.getMiddle() == 0 ? "/" : String.valueOf(act.getMiddle()), center));
                        sheet.addCell(new Label(14, row, act.getHoneyRoom() == 0 ? "/" : String.valueOf(act.getHoneyRoom()), center));
                        sheet.addCell(new Label(15, row, act.getBox() == 0 ? "/" : String.valueOf(act.getBox()), center));
                        sheet.addCell(new Label(16, row, act.getEscape() == 0 ? "/" : String.valueOf(act.getEscape()), center));
                        sheet.addCell(new Label(17, row, act.getFence() == 0 ? "/" : String.valueOf(act.getFence()), center));
                        sheet.addCell(new Label(18, row, act.getDiaper() == 0 ? "/" : String.valueOf(act.getDiaper()), center));

                        List<String> a = new ArrayList<>();
                        if (entry.hasFood())
                            a.add(mContext.getString(R.string.export_food, entry.getFoodText()));
                        if (entry.hasTreatment())
                            a.add(mContext.getString(R.string.export_treatment, entry.getTreatmentText()));
                        if (entry.hasHarvest())
                            a.add(mContext.getString(R.string.export_harvest, entry.getHarvestText(mContext)));
                        if (!entry.getCommonActivities().getOther().isEmpty())
                            a.add(entry.getCommonActivities().getOther());

                        sheet.addCell(new Label(19, row, TextUtils.join("\n", a.toArray())));

                        CellView cell = sheet.getRowView(row);
                        cell.setSize(cell.getSize() * Math.max(a.size(), 1));
                        sheet.setRowView(row, cell);
                        row++;
                    }
                } catch (WriteException e) {
                    e.printStackTrace();
                    BeeApplication.getInstance().trackException(e);
                }
            }
            try {
                workbook.write();
                workbook.close();
            } catch (WriteException e) {
                e.printStackTrace();
                BeeApplication.getInstance().trackException(e);
            }

        } catch (IOException e) {
            e.printStackTrace();
            BeeApplication.getInstance().trackException(e);
        }
        return file.getAbsolutePath();
    }

    public static void forceUpgrade() {
        instance.getWritableDatabase().close();
    }

    /* Statistics */
    public StatisticsEarnings getStatisticsEarnings(Calendar from, Calendar till) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<Integer, String> hives = new HashMap<>();
        Map<Integer, String> groups = new HashMap<>();
        Cursor curHives = db.query(HIVE_TABLE_NAME, new String[]{HIVE_ID, HIVE_NAME, HIVE_GROUP}, null, null, null, null, null);
        while (curHives.moveToNext()) {
            hives.put(curHives.getInt(curHives.getColumnIndex(HIVE_ID)), curHives.getString(curHives.getColumnIndex(HIVE_NAME)));
            groups.put(curHives.getInt(curHives.getColumnIndex(HIVE_ID)), curHives.getString(curHives.getColumnIndex(HIVE_GROUP)));
        }
        curHives.close();

        Cursor cur = db.query(LOG_TABLE_NAME, new String[]{LOG_HIVE_ID, LOG_HARVEST},
                LOG_HARVEST + " != '' AND " + LOG_DATE + " >= '" + getDateTime(from.getTime())
                        + "' AND " + LOG_DATE + " <= '" + getDateTime(till.getTime()) + "'",
                null, null, null, null);
        StatisticsEarnings stats = new StatisticsEarnings(mContext);
        while (cur.moveToNext()) {
            Harvest h = getHarvest(cur.getString(cur.getColumnIndex(LOG_HARVEST)));
            if (h != null) {
                stats.addHoney(hives.get(cur.getInt(cur.getColumnIndex(LOG_HIVE_ID))),
                        groups.get(cur.getInt(cur.getColumnIndex(LOG_HIVE_ID))),
                        h.getWeight());

            }
        }
        cur.close();
        db.close();
        return stats;
    }

    public StatisticsFood getStatisticsFood(Calendar from, Calendar till) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.query(LOG_TABLE_NAME, new String[]{LOG_FOOD},
                LOG_FOOD + " != '' AND " + LOG_DATE + " >= '" + getDateTime(from.getTime())
                        + "' AND " + LOG_DATE + " <= '" + getDateTime(till.getTime()) + "'",
                null, null, null, null);
        StatisticsFood stats = new StatisticsFood();
        while (cur.moveToNext()) {
            Food food = getFood(cur.getString(cur.getColumnIndex(LOG_FOOD)));
            if (food != null) {
                stats.addFood(food.getFood(), food.getAmount());
            }
        }
        cur.close();
        db.close();
        return stats;
    }

    public Trend getTrend() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.query(LOG_TABLE_NAME, new String[]{LOG_DATE, LOG_HARVEST}, null, null, null, null, LOG_DATE + " ASC");
        Trend trend = new Trend();
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        while (cur.moveToNext()) {
            Harvest h = getHarvest(cur.getString(cur.getColumnIndex(LOG_HARVEST)));
            if (h != null) {
                Date date = getDateFromString(cur.getString(cur.getColumnIndex(LOG_DATE)));
                trend.addHoney(yearFormat.format(date), monthFormat.format(date), h.getWeight());
            }
        }
        cur.close();
        db.close();
        return trend;
    }

    public Calendar getFirstEntryDate() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(LOG_TABLE_NAME, new String[]{LOG_DATE}, null, null, null, null, LOG_DATE + " ASC", "1");
        Calendar calendar = Calendar.getInstance();
        while (cursor.moveToNext()) {
            calendar.setTime(getDateFromString(cursor.getString(cursor.getColumnIndex(LOG_DATE))));
        }
        cursor.close();
        db.close();
        return calendar;
    }

    public Calendar getLastEntryDate() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(LOG_TABLE_NAME, new String[]{LOG_DATE}, null, null, null, null, LOG_DATE + " DESC", "1");
        Calendar calendar = Calendar.getInstance();
        while (cursor.moveToNext()) {
            calendar.setTime(getDateFromString(cursor.getString(cursor.getColumnIndex(LOG_DATE))));
        }
        cursor.close();
        db.close();
        return calendar;
    }

}
