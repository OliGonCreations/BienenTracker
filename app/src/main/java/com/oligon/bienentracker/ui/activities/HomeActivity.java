package com.oligon.bienentracker.ui.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveStatusCodes;
import com.oligon.bienentracker.BeeApplication;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.object.Hive;
import com.oligon.bienentracker.object.LogEntry;
import com.oligon.bienentracker.ui.dialogs.HiveDialogFragment;
import com.oligon.bienentracker.ui.dialogs.HiveSortDialogFragment;
import com.oligon.bienentracker.ui.dialogs.RateHiveDialogFragment;
import com.oligon.bienentracker.ui.dialogs.ReminderDialogFragment;
import com.oligon.bienentracker.util.DriveHandler;
import com.oligon.bienentracker.util.HiveDB;
import com.oligon.bienentracker.util.OnDialogFinishedListener;
import com.oligon.bienentracker.util.adapter.HiveListAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, OnDialogFinishedListener,
        HiveListAdapter.LogClickListener, NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, SwipeRefreshLayout.OnRefreshListener {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final int RC_SIGN_IN = 3494;
    private static final int ADD_GROUPS_ID = 5432;
    private static final long LIMIT_EXCEED_TIME = 1000 * 60 * 3;

    private static boolean isHome = true;
    private static String selectedGroup, toolbarTitle;
    private static int selectedItem;
    private static int selectedHive;

    protected static Context context;
    private static MenuItem mGroups;
    private static RecyclerView list;
    private static SwipeRefreshLayout mRefreshLayout;
    private static LinearLayoutManager llm;
    private static HiveListAdapter mHiveListAdapter;
    private static View empty_message;
    private static List<Hive> hives = new ArrayList<>();
    private static FragmentManager fm;

    public static HiveDB db;
    public static boolean dbChanged;

    private DrawerLayout drawer;
    private static NavigationView navigationView;
    private Toolbar toolbar;

    private static TextView mUserName;
    private static TextView mUserMail;
    private static ImageButton mUserMenu;

    private static SharedPreferences sp;

    private static Parcelable listSavedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        context = HomeActivity.this;

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mRefreshLayout.setOnRefreshListener(this);

        View headerView = navigationView.getHeaderView(0);
        mUserName = (TextView) headerView.findViewById(R.id.header_user_name);
        mUserMail = (TextView) headerView.findViewById(R.id.header_user_mail);

        mUserMenu = (ImageButton) headerView.findViewById(R.id.header_user_menu);
        mUserMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(context, v, Gravity.END);
                popup.getMenuInflater().inflate(R.menu.menu_user, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.menu_user_logout) {
                            signOut();
                            return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

        BeeApplication.getApiClient(this).connect();

        navigationView.getHeaderView(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sp.getBoolean("premium_user", false)) {
                    if (mUserMail.getText().toString().isEmpty())
                        signIn();
                }
            }
        });
        mGroups = navigationView.getMenu().findItem(R.id.nav_groups_title);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.pref_settings, false);
        loadDefaultValues();

        if (getApplicationContext().getPackageName().endsWith(".debug")) {
            sp.edit().putBoolean("premium_user", true).apply();
            sp.edit().putBoolean("statistics_package", true).apply();
        }

        fm = getSupportFragmentManager();

        empty_message = findViewById(R.id.home_empty_message);
        empty_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new HiveDialogFragment().show(fm, "AddHive");
            }
        });

        mHiveListAdapter = new HiveListAdapter(this);
        mHiveListAdapter.setHasStableIds(true);

        list = (RecyclerView) findViewById(R.id.list_log);

        llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(llm);
        list.setAdapter(mHiveListAdapter);

        db = HiveDB.getInstance(this);

        if (savedInstanceState != null) {
            selectedItem = savedInstanceState.getInt("nav_item", R.id.nav_all);
            toolbarTitle = savedInstanceState.getString("toolbar_title", getString(R.string.nav_all));
            isHome = savedInstanceState.getBoolean("is_home", true);
        } else goHome();

        if (sp.getBoolean("premium_user", false) && sp.getBoolean("swipe_tutorial", true)) {
            AlertDialog.Builder tutorialDialog = new AlertDialog.Builder(this, R.style.AlertDialogOrange);
            tutorialDialog.setMessage(R.string.tutorial_swipe);
            tutorialDialog.setTitle(R.string.tutorial_title);
            tutorialDialog.setPositiveButton(R.string.tutorial_acknowledged, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sp.edit().putBoolean("swipe_tutorial", false).apply();
                    dialog.dismiss();
                }
            });
            tutorialDialog.show();
        }
    }

    @Override
    public void onBackPressed() {
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START) || !isHome) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        if (!isHome) {
            goHome();
            updateList();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sp.getBoolean("premium_user", false)) {
            OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(BeeApplication.getApiClient(this));
            if (opr.isDone()) {
                Log.d(BeeApplication.TAG, "Got cached sign-in");
                GoogleSignInResult result = opr.get();
                handleSignInResult(result);
            } else {
                opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                    @Override
                    public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                        handleSignInResult(googleSignInResult);
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGroups();
        selectItem();

        if (!sp.getBoolean("premium_user", false)) {
            mUserName.setText(getString(R.string.header_login));
            mUserMail.setText(getString(R.string.header_premium));
            mUserMenu.setVisibility(View.INVISIBLE);
        }

        if (dbChanged) {
            syncData();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("nav_item", selectedItem);
        outState.putString("toolbar_title", toolbar.getTitle().toString());
        outState.putBoolean("is_home", isHome);

        outState.putInt("selected_item", selectedHive);

        outState.putParcelable("recycler_view", list.getLayoutManager().onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            selectedHive = savedInstanceState.getInt("selected_item");
            listSavedInstanceState = savedInstanceState.getParcelable("recycler_view");
        }
    }

    private static void restoreLayoutManagerPosition() {
        if (listSavedInstanceState != null) {
            list.getLayoutManager().onRestoreInstanceState(listSavedInstanceState);
        }
    }

    private void syncDatabase() {
        Log.d(BeeApplication.TAG, "Trying to sync database");
        if (BeeApplication.getApiClient(this).isConnected()) {
            Log.d(BeeApplication.TAG, "Syncing");
            DriveHandler.getInstance(this).syncDatabase();
        } else {
            Log.d(BeeApplication.TAG, "Not connected");
            BeeApplication.getApiClient(this).registerConnectionCallbacks(this);
            BeeApplication.getApiClient(this).connect();
        }
    }

    private void syncPreferences() {
        Log.d(BeeApplication.TAG, "Trying to sync preferences");
        if (BeeApplication.getApiClient(this).isConnected()) {
            Log.d(BeeApplication.TAG, "Syncing Preferences");
            DriveHandler.getInstance(this).syncPreferences();
        } else {
            Log.d(BeeApplication.TAG, "Not connected");
            BeeApplication.getApiClient(this).registerConnectionCallbacks(this);
            BeeApplication.getApiClient(this).connect();
        }

    }

    private void goHome() {
        isHome = true;
        selectedItem = R.id.nav_all;
        toolbarTitle = getString(R.string.nav_all);
        selectItem();
    }

    public static void updateList() {
        hives = isHome ? db.getAllHives() : db.getAllHivesByGroup(selectedGroup);
        List<LogEntry> logs = new ArrayList<>();
        for (Hive hive : hives) {
            logs.add(db.getLog(hive.getId()));
            if (hive.getId() == selectedHive)
                hive.setExpanded(true);
        }
        mHiveListAdapter.updateData(hives, logs);

        if (hives.size() == 0) empty_message.setVisibility(View.VISIBLE);
        else empty_message.setVisibility(View.GONE);

    }

    public static void updateGroups() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> set = sp.getStringSet("pref_list_groups", new HashSet<String>());
        List<String> groups = new ArrayList<>(set);
        Collections.sort(groups);
        SubMenu subMenu = mGroups.getSubMenu();
        subMenu.clear();
        if (groups.isEmpty()) {
            subMenu.add(0, ADD_GROUPS_ID, Menu.NONE, context.getString(R.string.nav_no_groups))
                    .setCheckable(false).setEnabled(false);
        }
        for (String s : groups) {
            subMenu.add(0, s.hashCode(), Menu.NONE, s).setCheckable(true);
        }
        navigationView.getMenu().findItem(R.id.nav_stats).setEnabled(sp.getBoolean("statistics_package", false));
        navigationView.invalidate();
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
        updateGroups();
        updateList();
        if (dbChanged) syncData();
    }

    private void loadDefaultValues() {
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


    @Override
    public void onClick(View caller, final Hive hive) {
        PopupMenu popup = new PopupMenu(context, caller);
        popup.getMenuInflater().inflate(R.menu.menu_hive_more, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_card_delete:
                        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
                        builder.setTitle(getString(R.string.alert_delete_hive));
                        builder.setMessage(R.string.alert_delete_hive_message);
                        builder.setPositiveButton(R.string.alert_delete_hive_positive, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                db.deleteHive(hive.getId());
                                updateList();
                                Snackbar.make(list, R.string.action_recover, Snackbar.LENGTH_LONG).setAction(android.R.string.ok, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        hive.setId(-1);
                                        db.editHive(hive);
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
                        builder.setNeutralButton(R.string.alert_delete_hive_neutral, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.deleteHive(hive.getId());
                                db.deleteLogsFromHive(hive.getId());
                                updateList();

                            }
                        });
                        builder.create().show();
                        break;
                    case R.id.menu_card_edit:
                        HiveDialogFragment dialog = new HiveDialogFragment();
                        Bundle args = new Bundle();
                        args.putSerializable("hive", hive);
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
    public void onAddLogClick(final Hive hive) {
        selectedHive = hive.getId();
        Intent intent = new Intent(context, NewEntryActivity.class);
        intent.putExtra("HiveId", hive.getId());
        context.startActivity(intent);
    }

    @Override
    public void onMoreLogClick(Hive hive) {
        selectedHive = hive.getId();
        Intent intent = new Intent(context, LogActivity.class);
        intent.putExtra("Hive", hive);
        context.startActivity(intent);
    }

    @Override
    public void onAddReminderClick(Hive hive) {
        ReminderDialogFragment reminder = new ReminderDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("hive", hive);
        reminder.setArguments(bundle);
        reminder.show(fm, "ReminderDialogFragment");
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.nav_all:
                goHome();
                selectedItem = R.id.nav_all;
                updateList();
                break;
            case R.id.nav_stats:
                intent = new Intent(this, StatisticsActivity.class);
                break;
            case R.id.nav_settings:
                intent = new Intent(this, SettingsActivity.class);
                break;
            case R.id.nav_about:
                intent = new Intent(this, AboutActivity.class);
                break;
            default:
                isHome = false;
                selectedGroup = item.getTitle().toString();
                selectedItem = item.getItemId();
                toolbarTitle = selectedGroup;
                selectItem();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        if (intent != null)
            startActivity(intent);
        return true;
    }

    private void selectItem() {
        toolbar.setTitle(toolbarTitle);
        toolbar.invalidate();
        navigationView.setCheckedItem(selectedItem);
        navigationView.invalidate();
        updateList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
            syncData();
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result != null && result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            if (acct != null) {
                sp.edit().putBoolean("is_logged_in", true).apply();
                mUserName.setText(acct.getDisplayName());
                mUserMail.setText(acct.getEmail());
                mUserMenu.setVisibility(View.VISIBLE);
            }
        } else {
            // Signed out, show unauthenticated UI.
            mUserName.setText(getString(R.string.header_login_msg));
            mUserMail.setText("");
            mUserMenu.setVisibility(View.INVISIBLE);
            sp.edit().putBoolean("is_logged_in", false).apply();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(BeeApplication.TAG, "Message: " + connectionResult.toString());
        Log.e(BeeApplication.TAG, "Connection Failed: " + connectionResult.getErrorMessage());
    }

    private void signIn() {
        if (!BeeApplication.getApiClient(this).isConnected()) return;
        if (sp.getBoolean("is_first_signin", true)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AlertDialogOrange);
            dialog.setTitle(getString(R.string.signin_dialog_title));
            dialog.setMessage(getString(R.string.signin_dialog_message));
            dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sp.edit().putBoolean("is_first_signin", false).apply();
                    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(BeeApplication.getApiClient(context));
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                }
            });
            dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.setCancelable(false);
            dialog.show();
        } else {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(BeeApplication.getApiClient(context));
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }
    }

    public void signOut() {
        if (BeeApplication.getApiClient(this).isConnected())
            Auth.GoogleSignInApi.signOut(BeeApplication.getApiClient(this)).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            handleSignInResult(null);
                        }
                    });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private synchronized void syncData() {
        dbChanged = false;
        if (!sp.getBoolean("is_logged_in", false) || !sp.getBoolean("premium_user", false))
            return;
        if (!BeeApplication.getApiClient(this).isConnected()) {
            Log.d(BeeApplication.TAG, "Client not connected");
            return;
        }
        long lastsync = sp.getLong("last_sync_request", 0);
        mRefreshLayout.setRefreshing(true);
        if (new Date().getTime() - lastsync > LIMIT_EXCEED_TIME) {
            Log.d(BeeApplication.TAG, "Last sync long ago, requesting sync");
            Drive.DriveApi.requestSync(BeeApplication.getApiClient(this)).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    mRefreshLayout.setRefreshing(false);
                    Log.d(BeeApplication.TAG, "Request Sync Message: " + status.getStatusMessage());
                    if (status.isSuccess()) {
                        sp.edit().putLong("last_sync_request", new Date().getTime()).apply();
                        Log.d(BeeApplication.TAG, "Request sync successfull");
                        syncDatabase();
                        syncPreferences();
                    } else if (status.getStatusCode() == DriveStatusCodes.DRIVE_RATE_LIMIT_EXCEEDED) {
                        sp.edit().putLong("last_sync_request", new Date().getTime()).apply();
                        syncDatabase();
                        syncPreferences();
                    }
                }
            });
        } else {
            Log.d(BeeApplication.TAG, "Last sync not so long ago; no request");
            syncDatabase();
            syncPreferences();
        }
    }

    @Override
    public void onRefresh() {
        if (sp.getBoolean("premium_user", false)) {
            syncData();
        }
    }

    public static void finishedRefreshing() {
        try {
            mRefreshLayout.setRefreshing(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
