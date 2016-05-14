package com.oligon.bienentracker.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.DriveEventService;

public class DriveService extends DriveEventService {

    @Override
    public void onChange(ChangeEvent event) {
        Log.d("DriveService", event.toString());
        if (event.hasContentChanged()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            sp.edit().putBoolean("database_old", true).apply();
        }
    }

}
