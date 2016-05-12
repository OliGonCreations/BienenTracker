package com.oligon.bienentracker.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.oligon.bienentracker.NotificationReceiver;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.Hive;

import java.util.Calendar;

public class ReminderDialog extends DialogFragment {

    private Hive mHive;
    private Calendar mCalendar = Calendar.getInstance();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null && !args.isEmpty())
            mHive = (Hive) args.getSerializable("hive");
        else return super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogOrange);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_reminder, null);
        dialog.setView(view);
        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                scheduleNotification(getNotification(mHive.getName()), mCalendar);
            }
        });

        dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return dialog.create();
    }

    private void scheduleNotification(Notification notification, Calendar c) {
        Intent notificationIntent = new Intent(getContext(), NotificationReceiver.class);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION_ID, mHive.getId());
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Log.d("Test", "Scheduled for: " + c.getTimeInMillis());

        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

    private Notification getNotification(String content) {
        Notification.Builder builder = new Notification.Builder(getActivity());
        builder.setContentTitle("Scheduled Notification");
        builder.setContentText(content);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        return builder.build();
    }
}
