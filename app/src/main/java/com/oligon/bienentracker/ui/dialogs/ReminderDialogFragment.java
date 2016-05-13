package com.oligon.bienentracker.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.oligon.bienentracker.NotificationReceiver;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.Hive;
import com.oligon.bienentracker.object.Reminder;
import com.oligon.bienentracker.ui.activities.HomeActivity;
import com.oligon.bienentracker.util.InstantAutoCompleteTextView;
import com.oligon.bienentracker.util.OnDialogFinishedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class ReminderDialogFragment extends DialogFragment {

    private static Reminder mReminder = new Reminder();
    public static Calendar mCalendar;
    private static Context context;
    private static TextView mDate, mTime;
    private Hive mHive;
    private InstantAutoCompleteTextView mDescription;
    private TextInputLayout mDescriptionLabel;

    private OnDialogFinishedListener mListener;

    @Override
    public void onAttach(Activity activity) {
        mListener = (OnDialogFinishedListener) activity;
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        context = getContext();
        mCalendar = Calendar.getInstance();
        mCalendar.set(Calendar.SECOND, 0);

        Bundle args = getArguments();
        if (args != null && !args.isEmpty())
            mHive = (Hive) args.getSerializable("hive");
        else return super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.AlertDialogOrange);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_reminder, null);

        mDescription = (InstantAutoCompleteTextView) view.findViewById(R.id.et_reminder_description);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        List<String> list = new ArrayList<>(sp.getStringSet("pref_list_reminders", new HashSet<String>()));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, list);

        mDescription.setAdapter(adapter);
        mDescription.setThreshold(1);

        mDescription.addTextChangedListener(new DescriptionTextWatcher());
        mDescriptionLabel = (TextInputLayout) view.findViewById(R.id.label_reminder_description);

        mDate = (TextView) view.findViewById(R.id.tv_reminder_date);
        mTime = (TextView) view.findViewById(R.id.tv_reminder_time);
        mDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerFragment().show(getFragmentManager(), "EditDate");
            }
        });
        mTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerFragment().show(getFragmentManager(), "EditTime");
            }
        });

        if (mHive.hasReminder()) {
            mReminder = mHive.getReminder();
            mDescription.setText(mReminder.getDescription());
            mCalendar.setTime(mReminder.getTime());
        }
        updateDate();

        dialog.setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setNeutralButton(R.string.action_delete, null);
        return dialog.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String description = mDescription.getText().toString();
                    if (!validateDescription()) return;
                    mReminder.setDescription(description);
                    scheduleNotification(getNotification(mHive.getName(), description), mCalendar);
                    HomeActivity.db.updateHiveReminder(mHive.getId(), mReminder);
                    mListener.onDialogFinished();
                    d.dismiss();
                }
            });
            d.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HomeActivity.db.removeHiveReminder(mHive.getId());
                    mListener.onDialogFinished();
                    d.dismiss();
                }
            });
        }
    }

    private static void updateDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat(context.getString(R.string.time_format), Locale.getDefault());
        mDate.setText(sdf.format(mCalendar.getTime()));
        mTime.setText(sdfTime.format(mCalendar.getTime()));
    }

    private void scheduleNotification(Notification notification, Calendar c) {
        Intent notificationIntent = new Intent(getContext(), NotificationReceiver.class);
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION_ID, mHive.getId());
        notificationIntent.putExtra(NotificationReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), mHive.getId(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
    }

    private Notification getNotification(String title, String content) {
        Intent intent = new Intent(getContext(), HomeActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(getContext(), (int) System.currentTimeMillis(), intent, 0);

        Notification.Builder builder = new Notification.Builder(getActivity());
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setAutoCancel(true);
        builder.setContentIntent(pIntent);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setWhen(mCalendar.getTimeInMillis());
        builder.setDefaults(Notification.DEFAULT_ALL);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_ALARM);
            builder.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        }
        return builder.build();
    }

    private class DescriptionTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            validateDescription();
        }
    }

    private boolean validateDescription() {
        if (mDescription.getText().toString().trim().isEmpty()) {
            mDescriptionLabel.setError(getString(R.string.error_msg_name));
            mDescriptionLabel.setErrorEnabled(true);
            requestFocus(mDescription);
            return false;
        } else {
            mDescriptionLabel.setError(null);
            mDescriptionLabel.setErrorEnabled(false);
        }
        return true;
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int year = mCalendar.get(Calendar.YEAR);
            int month = mCalendar.get(Calendar.MONTH);
            int day = mCalendar.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            mReminder.setTime(mCalendar.getTime());
            updateDate();
        }
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new TimePickerDialog(getActivity(), R.style.AlertDialogOrange, this, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), true);
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            mCalendar.set(Calendar.MINUTE, minute);
            mReminder.setTime(mCalendar.getTime());
            updateDate();
        }
    }


}
