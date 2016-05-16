package com.oligon.bienentracker.ui.activities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.Hive;
import com.oligon.bienentracker.object.LogEntry;
import com.oligon.bienentracker.ui.dialogs.HiveDialogFragment;
import com.oligon.bienentracker.util.HiveDB;
import com.oligon.bienentracker.util.OnDialogFinishedListener;
import com.oligon.bienentracker.util.adapter.LogListAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LogActivity extends AppCompatActivity implements OnDialogFinishedListener {

    private static Hive mHive;
    private static RecyclerView mList;
    public static HiveDB mDB;
    private static Context mContext;
    private static LogListAdapter mAdapter;
    private static FragmentManager fm;
    private static CharSequence mFilter = "";
    private static CharSequence mStandardFilter = "";
    private static MenuItem mFilterCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        mContext = this;
        fm = getSupportFragmentManager();

        mHive = (Hive) getIntent().getSerializableExtra("Hive");
        if (mHive == null)
            return;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(mHive.getName());

        mList = (RecyclerView) findViewById(R.id.log_list);
        assert mList != null;
        mList.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mList.setLayoutManager(llm);

        mDB = HiveDB.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
    }

    public static void updateList() {
        List<LogEntry> entries = mDB.getAllLogs(mHive.getId(), true, 50);
        if (entries.size() == 0) {
            TextView emptyText = new TextView(mContext);
            emptyText.setText(R.string.log_empty_message);
            emptyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setTypeface(null, Typeface.BOLD);
            ((LogActivity) mContext).setContentView(emptyText);
        } else {
            mAdapter = new LogListAdapter(entries);
            mList.setAdapter(mAdapter);
            mStandardFilter = entries.get(entries.size() - 1).getDate().getTime() +
                    ";" + entries.get(0).getDate().getTime();
            mFilter = mStandardFilter;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log, menu);
        mFilterCheck = menu.findItem(R.id.menu_filter);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit_hive:
                HiveDialogFragment dialog = new HiveDialogFragment();
                Bundle args = new Bundle();
                args.putSerializable("hive", mHive);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "EditHive");
                return true;
            case R.id.menu_filter:
                if (item.isChecked()) {
                    mFilter = mStandardFilter;
                    filterDate(mFilter);
                    item.setChecked(false);
                } else {
                    FilterDialogFragment filterDialog = new FilterDialogFragment();
                    if (mFilter.length() != 0) {
                        String[] dates = mFilter.toString().split(";");
                        Bundle filterArg = new Bundle();
                        filterArg.putLong("from", Long.parseLong(dates[0]));
                        filterArg.putLong("till", Long.parseLong(dates[1]));
                        filterDialog.setArguments(filterArg);
                    }
                    filterDialog.show(fm, "FilterDialog");
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDialogFinished() {
        updateHive();
    }

    private void updateHive() {
        mHive = mDB.getHive(mHive.getId());
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(mHive.getName());
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    private static void filterDate(CharSequence filter) {
        mFilter = filter;
        mAdapter.getFilter().filter(filter);
    }

    public static class FilterDialogFragment extends DialogFragment implements View.OnClickListener, DatePickerDialog.OnDateSetListener {

        private TextView tvFrom, tvTill;
        private Calendar mFrom, mTill, mMax;
        private boolean flag;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogOrange);
            LayoutInflater inflater = getActivity().getLayoutInflater();

            mFrom = Calendar.getInstance();
            mTill = Calendar.getInstance();
            mMax = Calendar.getInstance();

            Bundle args = getArguments();
            if (args != null) {
                mFrom.setTimeInMillis(args.getLong("from"));
                mTill.setTimeInMillis(args.getLong("till"));
                mMax.setTimeInMillis(args.getLong("till"));
            }

            View view = inflater.inflate(R.layout.dialog_filter_date, null);

            tvFrom = (TextView) view.findViewById(R.id.filter_date_from_text);
            tvTill = (TextView) view.findViewById(R.id.filter_date_till_text);

            tvFrom.setOnClickListener(this);
            tvTill.setOnClickListener(this);

            updateTextView();

            builder.setView(view)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            filterDate(mFrom.getTimeInMillis() + ";" + mTill.getTimeInMillis());
                            mFilterCheck.setChecked(true);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            FilterDialogFragment.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }

        private void updateTextView() {
            SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
            tvFrom.setText(sdf.format(mFrom.getTime()));
            tvTill.setText(sdf.format(mTill.getTime()));
        }

        @Override
        public void onClick(View v) {
            flag = v.getId() == R.id.filter_date_from_text;
            DatePickerFragment datePicker = new DatePickerFragment();
            datePicker.mListener = this;
            Bundle b = new Bundle();
            if (flag) {
                b.putLong("minDate", 0);
                b.putLong("curDate", mFrom.getTimeInMillis());
                b.putLong("maxDate", mTill.getTimeInMillis());
            } else {
                b.putLong("minDate", mFrom.getTimeInMillis());
                b.putLong("curDate", mTill.getTimeInMillis());
                b.putLong("maxDate", mMax.getTimeInMillis());
            }
            datePicker.setArguments(b);
            datePicker.show(fm, "Date" + flag);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar date = Calendar.getInstance();
            date.set(Calendar.YEAR, year);
            date.set(Calendar.MONTH, monthOfYear);
            date.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            if (flag) {
                mFrom = date;
            } else {
                mTill = date;
            }

            updateTextView();
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        public DatePickerDialog.OnDateSetListener mListener;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            long maxDate = c.getTimeInMillis(), curDate = c.getTimeInMillis(), minDate = 0;
            Bundle b = getArguments();
            if (b != null) {
                maxDate = b.getLong("maxDate", c.getTimeInMillis());
                minDate = b.getLong("minDate", 0);
                curDate = b.getLong("curDate", c.getTimeInMillis());
            }
            if (mListener == null) mListener = this;

            c.setTimeInMillis(curDate);
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog dialog = new DatePickerDialog(getActivity(), R.style.AlertDialogOrange, mListener, year, month, day);
            dialog.getDatePicker().setMaxDate(maxDate);
            dialog.getDatePicker().setMinDate(minDate);
            return dialog;
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
        }
    }

}
