package com.oligon.bienentracker;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.oligon.bienentracker.ui.dialogs.CommonDialogFragment;
import com.oligon.bienentracker.util.Circle;
import com.oligon.bienentracker.util.HiveDB;
import com.oligon.bienentracker.util.object.Food;
import com.oligon.bienentracker.util.object.Harvest;
import com.oligon.bienentracker.util.object.Hive;
import com.oligon.bienentracker.util.object.Inspection;
import com.oligon.bienentracker.util.object.LogEntry;
import com.oligon.bienentracker.util.object.Treatment;
import com.oligon.bienentracker.weather.WeatherTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class NewEntryActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static Context context;
    private static HiveDB db;
    public static LogEntry mLogEntry;
    private static CoordinatorLayout coordinator;
    private static LinearLayout mViewTreatment, mViewFood, mViewHarvest, mViewActivity, mViewInspection;
    private static TextView tvTreatment, tvFood, tvHarvest, tvActivity, tvInspection, tvTextWeather, tvTextTemp, textDate;
    private static ImageView mWeatherIcon;


    private static boolean isTempSet = false, isWeatherEditing = false;

    private static SharedPreferences sp;

    private enum ActivityType {
        TREATMENT,
        FOOD,
        HARVEST
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_entry);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeAsUpIndicator(R.drawable.ic_action_close);
        }

        context = this;
        db = HiveDB.getInstance(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int id = extras.getInt("HiveId");
            if (extras.getBoolean("edit")) {
                isTempSet = true;
                mLogEntry = db.getLogById(extras.getInt("LogEntry"));
                mLogEntry.setHive(db.getHive(mLogEntry.getHive().getId()));
            } else {
                isTempSet = false;
                mLogEntry = new LogEntry(db.getHive(id));
                mLogEntry.setDate(new Date());
            }
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
            this.finish();
        }

        coordinator = (CoordinatorLayout) findViewById(R.id.snackbar);
        FloatingActionMenu fab = (FloatingActionMenu) findViewById(R.id.menu_add_entity);

        assert fab != null;
        fab.setClosedOnTouchOutside(true);

        FloatingActionButton addFood = (FloatingActionButton) findViewById(R.id.menu_add_food);
        FloatingActionButton addTreatment = (FloatingActionButton) findViewById(R.id.menu_add_treatment);
        FloatingActionButton addCommon = (FloatingActionButton) findViewById(R.id.menu_add_activity);
        FloatingActionButton addHarvest = (FloatingActionButton) findViewById(R.id.menu_add_harvest);
        FloatingActionButton addInspection = (FloatingActionButton) findViewById(R.id.menu_add_inspection);

        assert addFood != null;
        addFood.setOnClickListener(this);
        assert addTreatment != null;
        addTreatment.setOnClickListener(this);
        assert addCommon != null;
        addCommon.setOnClickListener(this);
        assert addHarvest != null;
        addHarvest.setOnClickListener(this);
        assert addInspection != null;
        addInspection.setOnClickListener(this);

        textDate = (TextView) findViewById(R.id.text_date);
        assert textDate != null;
        textDate.setOnClickListener(this);
        TextView textHive = (TextView) findViewById(R.id.text_hive);
        TextView textYear = (TextView) findViewById(R.id.text_year);
        TextView textLocation = (TextView) findViewById(R.id.text_location);
        Circle circleQueen = (Circle) findViewById(R.id.hive_circle);

        tvTextWeather = (TextView) findViewById(R.id.text_weather);
        tvTextTemp = (TextView) findViewById(R.id.text_weather_temp);

        //noinspection ConstantConditions
        findViewById(R.id.weather_temp_edit).setOnClickListener(this);

        updateDate();

        Hive hive = mLogEntry.getHive();

        assert textHive != null;
        textHive.setText(mLogEntry.getHive().getName());
        assert textYear != null;
        assert circleQueen != null;
        if (hive.getYear() != 0) {
            textYear.setText(String.valueOf(hive.getYear()));
            circleQueen.setYear(hive.getYear());
            circleQueen.setLabel(hive.getMarker());
        } else {
            textYear.setVisibility(View.INVISIBLE);
            circleQueen.setVisibility(View.INVISIBLE);
        }
        assert textLocation != null;
        textLocation.setText(hive.getLocation().length() > 0 ?
                hive.getLocation() : " - ");

        mViewTreatment = (LinearLayout) findViewById(R.id.view_treatment);
        mViewFood = (LinearLayout) findViewById(R.id.view_food);
        mViewHarvest = (LinearLayout) findViewById(R.id.view_harvest);
        mViewActivity = (LinearLayout) findViewById(R.id.view_activity);
        mViewInspection = (LinearLayout) findViewById(R.id.view_inspection);

        tvTreatment = (TextView) findViewById(R.id.treatment_text);
        tvFood = (TextView) findViewById(R.id.food_text);
        tvHarvest = (TextView) findViewById(R.id.harvest_text);
        tvActivity = (TextView) findViewById(R.id.activity_text);
        tvInspection = (TextView) findViewById(R.id.inspection_text);

        mWeatherIcon = (ImageView) findViewById(R.id.weather_img);

        assert mWeatherIcon != null;
        mWeatherIcon.setOnClickListener(this);

        mViewTreatment.setOnLongClickListener(this);
        mViewFood.setOnLongClickListener(this);
        mViewHarvest.setOnLongClickListener(this);
        mViewActivity.setOnLongClickListener(this);
        mViewInspection.setOnLongClickListener(this);

        if (!isTempSet)
            requestWeather();

        sp = PreferenceManager.getDefaultSharedPreferences(this);


        final View content = findViewById(R.id.hive_description);
        assert content != null;
        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                enterReveal(content);
                content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLayout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
                db.addLog(mLogEntry);
                this.finish();
                BeeApplication.getInstance().trackEvent("Log", "Add", "Log Entry added");
                return true;
            case android.R.id.home:
                finish();
                //NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this, R.style.AlertDialogGreen)
                .setMessage(R.string.alert_exit_message)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NewEntryActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    private void requestWeather() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(coordinator, R.string.permission_location_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(NewEntryActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    0);
                        }
                    }).show();
        } else {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            List<String> providers = lm.getProviders(true);

            Location l = null;

            for (int i = providers.size() - 1; i >= 0; i--) {
                l = lm.getLastKnownLocation(providers.get(i));
                if (l != null) break;
            }
            if (l == null) {

            }

            Double[] gps = new Double[2];
            if (l != null) {
                gps[0] = l.getLatitude();
                gps[1] = l.getLongitude();

                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    try {
                        new WeatherTask().execute(gps);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d("BienenTracker", "No internet connection");
                }
            }
        }
    }

    public static void onWeatherSet(float temp, int code) {
        if (!isWeatherEditing) {
            BeeApplication.getInstance().trackEvent("Log", "Weather", "Weather successfully requested");
            mLogEntry.setWeatherCode(code);
            mLogEntry.setTemp(temp);
            isTempSet = true;
            updateLayout();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestWeather();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.weather_img) {
            isTempSet = false;
            requestWeather();
            return;
        } else if (v.getId() == R.id.text_date) {
            new DatePickerFragment().show(getSupportFragmentManager(), "EditDate");
            return;
        }
        showEntityDialog(v.getId());
    }

    @Override
    public boolean onLongClick(final View v) {
        PopupMenu popup = new PopupMenu(context, v, Gravity.END);
        popup.getMenuInflater().inflate(R.menu.menu_card_more, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_card_delete) {
                    switch (v.getId()) {
                        case R.id.view_treatment:
                            mLogEntry.setTreatment(null);
                            break;
                        case R.id.view_food:
                            mLogEntry.setFood(null);
                            break;
                        case R.id.view_harvest:
                            mLogEntry.setHarvest(null);
                            break;
                        case R.id.view_inspection:
                            mLogEntry.setInspection(null);
                            break;
                        case R.id.view_activity:
                            mLogEntry.setCommonActivities(null);
                            break;
                    }
                    updateLayout();
                    return true;
                } else if (item.getItemId() == R.id.menu_card_edit) {
                    switch (v.getId()) {
                        case R.id.view_treatment:
                            showEntityDialog(R.id.menu_add_treatment);
                            break;
                        case R.id.view_food:
                            showEntityDialog(R.id.menu_add_food);
                            break;
                        case R.id.view_harvest:
                            showEntityDialog(R.id.menu_add_harvest);
                            break;
                        case R.id.view_inspection:
                            showEntityDialog(R.id.menu_add_inspection);
                            break;
                        case R.id.view_activity:
                            showEntityDialog(R.id.menu_add_activity);
                            break;
                    }
                    return true;
                }
                return false;
            }
        });
        popup.show();
        return false;
    }

    private void enterReveal(View view) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int cx = view.getMeasuredWidth() / 2;
            int cy = view.getMeasuredHeight() / 2;

            int finalRadius = Math.max(view.getWidth(), view.getHeight()) / 2;

            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);

            view.setVisibility(View.VISIBLE);
            anim.start();
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    private static void exitReveal(final View view) {
        if (view.getVisibility() != View.GONE) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                int cx = view.getMeasuredWidth() / 2;
                int cy = view.getMeasuredHeight() / 2;

                int initialRadius = view.getWidth() / 2;

                Animator anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);

                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        view.setVisibility(View.INVISIBLE);
                        view.setVisibility(View.GONE);
                    }
                });
                anim.start();
            } else {
                view.setVisibility(View.GONE);
            }
        }
    }

    public static void dataSetChanged() {
        updateLayout();
    }

    private static void updateDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault());
        assert textDate != null;
        textDate.setText(sdf.format(mLogEntry.getDate()));
    }

    private static void updateLayout() {
        if (mLogEntry.hasTreatment()) {
            mViewTreatment.setVisibility(View.VISIBLE);
            tvTreatment.setText(mLogEntry.getTreatmentText());
        } else mViewTreatment.setVisibility(View.GONE);

        if (mLogEntry.hasFood()) {
            mViewFood.setVisibility(View.VISIBLE);
            tvFood.setText(mLogEntry.getFoodText());
        } else mViewFood.setVisibility(View.GONE);

        if (mLogEntry.hasHarvest()) {
            mViewHarvest.setVisibility(View.VISIBLE);
            tvHarvest.setText(mLogEntry.getHarvestText(context));
        } else mViewHarvest.setVisibility(View.GONE);

        if (mLogEntry.hasActivities()) {
            mViewActivity.setVisibility(View.VISIBLE);
            tvActivity.setText(mLogEntry.getActivitiesText(context));
        } else mViewActivity.setVisibility(View.GONE);

        if (mLogEntry.hasInspection()) {
            tvInspection.setText(mLogEntry.getInspectionText());
            mViewInspection.setVisibility(View.VISIBLE);
        } else mViewInspection.setVisibility(View.GONE);

        if (isTempSet) {
            if (mLogEntry.getWeatherCode() != -1) {
                tvTextTemp.setText(String.format(Locale.getDefault(), "%.1f °C", mLogEntry.getTemp()));
                tvTextWeather.setText(mLogEntry.getWeatherString(context));
                //noinspection ResourceType
                mWeatherIcon.setImageResource(mLogEntry.getWeatherImageId(context));
            }
        }
    }

    private void showEntityDialog(int res) {
        Bundle args = new Bundle();
        ActionDialogFragment dialog = new ActionDialogFragment();
        switch (res) {
            case R.id.menu_add_food:
                args.putSerializable("type", ActivityType.FOOD);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "ActionFood");
                break;
            case R.id.menu_add_treatment:
                args.putSerializable("type", ActivityType.TREATMENT);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "ActionTreatment");
                break;
            case R.id.menu_add_harvest:
                args.putSerializable("type", ActivityType.HARVEST);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "ActionTreatment");
                break;
            case R.id.menu_add_activity:
                args.putSerializable("activities", mLogEntry.getCommonActivities());
                CommonDialogFragment d = new CommonDialogFragment();
                d.setArguments(args);
                d.show(getSupportFragmentManager(), "CommonDialog");
                break;
            case R.id.menu_add_inspection:
                new InspectionDialogFragment().show(getSupportFragmentManager(), "InspectionDialog");
                break;
            case R.id.weather_temp_edit:
                isWeatherEditing = true;
                new TempEditDialogFragment().show(getSupportFragmentManager(), "TempEditDialog");
                break;
        }
    }

    public static class ActionDialogFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {

        private TextView mAction, mTypeTitle, mAmount;
        private Spinner mType, mUnit;
        private EditText etAmount, etHarvest;
        private ActivityType mActivity;

        private ArrayAdapter<String> aType = null;
        private ArrayAdapter<String> aUnit = null;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogGreen);
            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_action, null);
            mAction = (TextView) view.findViewById(R.id.action_title);
            mTypeTitle = (TextView) view.findViewById(R.id.action_type);
            mAmount = (TextView) view.findViewById(R.id.action_amount);
            mType = (Spinner) view.findViewById(R.id.spType);
            mUnit = (Spinner) view.findViewById(R.id.spUnit);
            etAmount = (EditText) view.findViewById(R.id.etAmount);
            etHarvest = (EditText) view.findViewById(R.id.etHarvest);

            mType.setOnItemSelectedListener(this);
            mUnit.setOnItemSelectedListener(this);


            Bundle args = getArguments();
            if (args != null && !args.isEmpty()) {
                mActivity = (ActivityType) args.getSerializable("type");

                String actionText = "", titleText = "", amountText = "";
                List<String> list = new ArrayList<>();
                List<String> content = new ArrayList<>();
                Set<String> set;
                Resources res = getResources();
                switch (mActivity) {
                    case FOOD:
                        actionText = res.getString(R.string.action_food);
                        titleText = res.getString(R.string.action_food_title);
                        amountText = res.getString(R.string.action_amount);

                        set = sp.getStringSet("pref_list_food", new HashSet<String>());
                        content.addAll(set);
                        list.add("l");
                        list.add("kg");
                        break;
                    case TREATMENT:
                        actionText = res.getString(R.string.action_treatment);
                        titleText = res.getString(R.string.action_treatment_title);
                        amountText = res.getString(R.string.action_amount);
                        set = sp.getStringSet("pref_list_treatment", new HashSet<String>());
                        content.addAll(set);
                        list.add("ml");
                        list.add("min");
                        list.add("h");
                        break;
                    case HARVEST:
                        actionText = res.getString(R.string.action_harvest);
                        titleText = res.getString(R.string.action_harvest_title);
                        amountText = res.getString(R.string.action_amount);
                        mType.setVisibility(View.INVISIBLE);
                        etHarvest.setVisibility(View.VISIBLE);
                        list.add("kg");
                        break;
                }

                mAction.setText(actionText);
                mTypeTitle.setText(titleText);
                mAmount.setText(amountText);


                Collections.sort(content);
                aType = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_spinner_item, content);
                aUnit = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_spinner_item, list);
                aType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                aUnit.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mType.setAdapter(aType);
                mUnit.setAdapter(aUnit);
                mType.setSelection(0);
                mUnit.setSelection(0);
            }


            builder.setView(view)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActionDialogFragment.this.getDialog().cancel();
                        }
                    });
            Dialog dialog = builder.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            return dialog;
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
                        switch (mActivity) {
                            case FOOD:
                                if (etAmount.getText().length() == 0) {
                                    etAmount.setError(getString(R.string.error_et));
                                    return;
                                }
                                Food mFood = new Food();
                                mFood.setAmount(Double.valueOf(etAmount.getText().toString()));
                                mFood.setUnit(aUnit.getItem(mUnit.getSelectedItemPosition()));
                                mFood.setFood(aType.getItem(mType.getSelectedItemPosition()));
                                mLogEntry.setFood(mFood);
                                break;
                            case TREATMENT:
                                if (etAmount.getText().length() == 0) {
                                    etAmount.setError(getString(R.string.error_et));
                                    return;
                                }
                                Treatment mTreatment = new Treatment(Double.valueOf(etAmount.getText().toString()),
                                        aType.getItem(mType.getSelectedItemPosition()),
                                        aUnit.getItem(mUnit.getSelectedItemPosition()));
                                mLogEntry.setTreatment(mTreatment);
                                break;
                            case HARVEST:
                                if (etHarvest.getText().length() == 0) {
                                    etHarvest.setError(getString(R.string.error_et));
                                    return;
                                } else if (etAmount.getText().length() == 0) {
                                    etAmount.setError(getString(R.string.error_et));
                                    return;
                                }
                                Harvest mHarvest = new Harvest(Integer.valueOf(etHarvest.getText().toString()));
                                mHarvest.setWeight(Double.valueOf(etAmount.getText().toString()));
                                mHarvest.setUnit(Harvest.intToUnit(mUnit.getSelectedItemPosition()));
                                mLogEntry.setHarvest(mHarvest);
                                break;
                        }
                        d.dismiss();
                        dataSetChanged();
                    }
                });
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public static class InspectionDialogFragment extends DialogFragment {

        private CheckBox mQueenless, mQueen, mBrood, mPins;
        private EditText mVarroa, mWeight, mNote;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogGreen);

            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_inspection, null);

            mQueenless = (CheckBox) view.findViewById(R.id.inspection_queenless);
            mQueen = (CheckBox) view.findViewById(R.id.inspection_queen);
            mBrood = (CheckBox) view.findViewById(R.id.inspection_brood);
            mPins = (CheckBox) view.findViewById(R.id.inspection_pins);
            mVarroa = (EditText) view.findViewById(R.id.inspection_varroa);
            mWeight = (EditText) view.findViewById(R.id.inspection_weight);
            mNote = (EditText) view.findViewById(R.id.inspection_note);

            builder.setView(view)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Inspection inspection = new Inspection();
                            inspection.setQueenless(mQueenless.isChecked());
                            inspection.setQueen(mQueen.isChecked());
                            inspection.setBrood(mBrood.isChecked());
                            inspection.setPins(mPins.isChecked());
                            String f = mVarroa.getText().toString();
                            if (f.isEmpty())
                                inspection.setVarroa(0);
                            else
                                inspection.setVarroa(Float.valueOf(f));
                            f = mWeight.getText().toString();
                            if (f.isEmpty())
                                inspection.setWeight(0);
                            else
                                inspection.setWeight(Float.valueOf(f));
                            inspection.setNote(mNote.getText().toString());
                            mLogEntry.setInspection(inspection);
                            updateLayout();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            InspectionDialogFragment.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }

    }

    public static class TempEditDialogFragment extends DialogFragment {

        EditText etTemp;
        Spinner spConditions;
        ImageView imgCondition;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogGreen);

            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_temp, null);
            etTemp = (EditText) view.findViewById(R.id.et_temp);
            spConditions = (Spinner) view.findViewById(R.id.sp_conditions);
            imgCondition = (ImageView) view.findViewById(R.id.img_condition);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.weather_conditions_reduced, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spConditions.setAdapter(adapter);
            spConditions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    TypedArray icons = context.getResources().obtainTypedArray(R.array.weather_icons);
                    //noinspection ResourceType
                    imgCondition.setImageResource(icons.getResourceId(parseReducedToWeatherCode(pos), R.drawable.ic_weather_load));
                    icons.recycle();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            spConditions.setSelection(parseWeatherCodeToReduced(mLogEntry.getWeatherCode()));

            builder.setView(view)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            TempEditDialogFragment.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }

        private int parseWeatherCodeToReduced(int weatherCode) {
            if (weatherCode == -1) return 1;
            int[] codes = context.getResources().getIntArray(R.array.weather_reduced_codes);
            for (int i = 0; i < codes.length; i++) {
                if (weatherCode == codes[i]) return i;
            }
            return 1;
        }

        private int parseReducedToWeatherCode(int pos) {
            return context.getResources().getIntArray(R.array.weather_reduced_codes)[pos];
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
                        if (etTemp.getText().toString().isEmpty()) {
                            etTemp.setError(getString(R.string.error_et));
                            return;
                        } else {
                            try {
                                mLogEntry.setTemp(Float.parseFloat(etTemp.getText().toString()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mLogEntry.setWeatherCode(parseReducedToWeatherCode(spConditions.getSelectedItemPosition()));
                            isTempSet = true;
                        }
                        d.dismiss();
                        dataSetChanged();
                        isWeatherEditing = false;
                    }
                });
            }
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, day);
            mLogEntry.setDate(c.getTime());
            updateDate();
        }
    }
}
