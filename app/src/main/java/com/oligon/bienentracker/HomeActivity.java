package com.oligon.bienentracker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.oligon.bienentracker.util.HiveDB;
import com.oligon.bienentracker.util.adapter.HiveListAdapter;
import com.oligon.bienentracker.util.object.Hive;
import com.oligon.bienentracker.util.object.LogEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, HiveDialogFragment.OnDialogFinishedListener,
        HiveSortDialogFragment.OnDialogFinishedListener, RateHiveDialogFragment.OnDialogFinishedListener {

    private static Context context;
    private static RecyclerView list;
    private static View empty_message;
    private static List<Hive> hives = new ArrayList<>();
    private static FragmentManager fm;

    public static HiveDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        context = HomeActivity.this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.pref_settings, false);
        loadDefaultValues();

        fm = getSupportFragmentManager();

        empty_message = findViewById(R.id.home_empty_message);
        empty_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new HiveDialogFragment().show(fm, "AddHive");
            }
        });

        list = (RecyclerView) findViewById(R.id.list_log);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(llm);
    }

    @Override
    protected void onResume() {
        super.onResume();
        db = HiveDB.getInstance(this);
        updateList();
    }

    private static void updateList() {
        hives = db.getAllHives();
        List<LogEntry> logs = new ArrayList<>();
        for (Hive hive : hives)
            logs.add(db.getLog(hive.getId()));
        HiveListAdapter mHiveListAdapter = new HiveListAdapter(hives, logs, new HiveListAdapter.LogClickListener() {
            @Override
            public void onClick(View caller, final Hive hive) {
                PopupMenu popup = new PopupMenu(context, caller);
                popup.getMenuInflater().inflate(R.menu.menu_hive_more, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_card_delete:
                                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
                                builder.setMessage(R.string.alert_delete_hive);
                                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        db.deleteHive(hive.getId());
                                        updateList();
                                        Snackbar.make(list, R.string.action_recover, Snackbar.LENGTH_LONG).setAction(android.R.string.ok, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                db.addHive(hive);
                                                updateList();
                                            }
                                        }).show();
                                    }
                                });
                                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                                builder.create().show();
                                break;
                            case R.id.menu_card_edit:
                                HiveDialogFragment dialog = new HiveDialogFragment();
                                Bundle args = new Bundle();
                                args.putInt("id", hive.getId());
                                args.putString("name", hive.getName());
                                args.putString("position", hive.getLocation());
                                args.putInt("year", hive.getYear());
                                args.putString("marker", hive.getMarker());
                                args.putBoolean("offspring", hive.isOffspring());
                                args.putString("info", hive.getInfo());
                                dialog.setArguments(args);
                                dialog.show(fm, "AddHive");
                                break;
                            case R.id.menu_card_rate:
                                RateHiveDialogFragment rate = new RateHiveDialogFragment();
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("hive", hive);
                                rate.setArguments(bundle);
                                rate.show(fm, "RateHive");
                                break;
                        }
                        return true;
                    }
                });
                popup.show();
            }

            @Override
            public void onAddLogClick(final Hive hive, View title) {

                Intent intent = new Intent(context, NewEntryActivity.class);
                intent.putExtra("HiveId", hive.getId());
                // TODO: make it look awesome
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptionsCompat options = ActivityOptionsCompat.
                            makeSceneTransitionAnimation((HomeActivity) context, title, "hivetitle");
                    context.startActivity(intent, options.toBundle());
                } else {*/
                context.startActivity(intent);
                //}
            }

            @Override
            public void onMoreLogClick(Hive hive) {
                Intent intent = new Intent(context, LogActivity.class);
                intent.putExtra("Hive", hive);
                context.startActivity(intent);
            }
        });
        list.setAdapter(mHiveListAdapter);

        if (hives.size() == 0) empty_message.setVisibility(View.VISIBLE);
        else empty_message.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_hive:
                new HiveDialogFragment().show(fm, "AddHive");
                return true;
            case R.id.action_sort_hives:
                new HiveSortDialogFragment().show(fm, "SortHives");
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onClick(View v) {
        Intent intent = new Intent(HomeActivity.this, NewEntryActivity.class);
        intent.putExtra("HiveId", hives.get(Integer.parseInt(v.getTag().toString())).getId());
        startActivity(intent);
    }

    @Override
    public void onDialogFinished() {
        updateList();
    }

    private void loadDefaultValues() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> set;
        if (sp.getStringSet("pref_list_food", null) == null) {
            set = new TreeSet<>();
            set.addAll(Arrays.asList(context.getResources().getStringArray(R.array.food)));
            sp.edit().putStringSet("pref_list_food", set).apply();
        } else if (sp.getStringSet("pref_list_food", null) == null) {
            set = new TreeSet<>();
            set.addAll(Arrays.asList(context.getResources().getStringArray(R.array.drugs)));
            sp.edit().putStringSet("pref_list_drug", set).apply();
        }
    }
}
