package com.oligon.bienentracker.ui.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, OnDialogFinishedListener,
        HiveListAdapter.LogClickListener, NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {


    private static final int RC_SIGN_IN = 3494;
    private static final int ADD_GROUPS_ID = 5432;

    private static boolean isHome = true;
    private static String selectedGroup;
    private static int selectedItem;

    private static Context context;
    private static MenuItem mGroups;
    private static RecyclerView list;
    private static View empty_message;
    private static List<Hive> hives = new ArrayList<>();
    private static FragmentManager fm;

    public static HiveDB db;

    private DrawerLayout drawer;
    private static NavigationView navigationView;
    private Toolbar toolbar;

    private ProgressDialog mProgressDialog;

    private static TextView mUserName;
    private static TextView mUserMail;
    private static ImageButton mUserMenu;

    private SharedPreferences sp;

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
        }

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

        db = HiveDB.getInstance(this);

        if (savedInstanceState != null) {
            selectedItem = savedInstanceState.getInt("nav_item");
            selectItem(savedInstanceState.getString("toolbar_title"));
            isHome = savedInstanceState.getBoolean("is_home");
        } else goHome();
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
                //showProgressDialog();
                opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                    @Override
                    public void onResult(GoogleSignInResult googleSignInResult) {
                        //hideProgressDialog();
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
        if (sp.getBoolean("database_old", false)) {
            DriveHandler.getInstance(this).getDBFromDrive();
        }

        if (!sp.getBoolean("premium_user", false)) {
            mUserName.setText(getString(R.string.header_login));
            mUserMail.setText(getString(R.string.header_premium));
            mUserMenu.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("nav_item", selectedItem);
        outState.putString("toolbar_title", selectedGroup);
        outState.putBoolean("is_home", isHome);
    }

    private void goHome() {
        isHome = true;
        toolbar.setTitle(getString(R.string.nav_all));
        navigationView.setCheckedItem(R.id.nav_all);
        updateList();
    }

    private static void updateList() {
        hives = isHome ? db.getAllHives() : db.getAllHivesByGroup(selectedGroup);
        List<LogEntry> logs = new ArrayList<>();
        for (Hive hive : hives)
            logs.add(db.getLog(hive.getId()));
        HiveListAdapter mHiveListAdapter = new HiveListAdapter(hives, logs, (HomeActivity) context);
        list.setAdapter(mHiveListAdapter);

        if (hives.size() == 0) empty_message.setVisibility(View.VISIBLE);
        else empty_message.setVisibility(View.GONE);
    }

    private static void updateGroups() {
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
            subMenu.add(0, 1234, Menu.NONE, s).setCheckable(true);
        }
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
        if (BeeApplication.getApiClient(this).isConnected())
            DriveHandler.getInstance(this).createDBFile();
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
                        // TODO: redo
                        HiveDialogFragment dialog = new HiveDialogFragment();
                        Bundle args = new Bundle();
                        args.putInt("id", hive.getId());
                        args.putString("name", hive.getName());
                        args.putString("position", hive.getLocation());
                        args.putInt("year", hive.getYear());
                        args.putString("marker", hive.getMarker());
                        args.putBoolean("offspring", hive.isOffspring());
                        args.putString("info", hive.getInfo());
                        args.putString("group", hive.getGroup());
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
        context.startActivity(intent);
    }

    @Override
    public void onMoreLogClick(Hive hive) {
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
        selectedItem = item.getItemId();
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.nav_all:
                goHome();
                break;
            case R.id.nav_stats:
                Toast.makeText(HomeActivity.this, "Bald verfügbar", Toast.LENGTH_SHORT).show();
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
                toolbar.setTitle(selectedGroup);
                updateList();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);

        if (intent != null)
            startActivity(intent);
        return true;
    }

    private void selectItem(String title) {
        toolbar.setTitle(title);
        navigationView.setCheckedItem(selectedItem);
        updateList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        hideProgressDialog();
        if (result != null && result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            mUserName.setText(acct.getDisplayName());
            mUserMail.setText(acct.getEmail());
            mUserMenu.setVisibility(View.VISIBLE);
        } else {
            // Signed out, show unauthenticated UI.
            mUserName.setText("Mit Google einloggen");
            mUserMail.setText("");
            mUserMenu.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(BeeApplication.TAG, "Message: " + connectionResult.toString());
        Log.e(BeeApplication.TAG, "Connection Failed: " + connectionResult.getErrorMessage());
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading");
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
        mProgressDialog = null;
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(BeeApplication.getApiClient(this));
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void signOut() {
        if (BeeApplication.getApiClient(this).isConnected())
            Auth.GoogleSignInApi.signOut(BeeApplication.getApiClient(this)).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            handleSignInResult(null);
                        }
                    });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        hideProgressDialog();
        DriveHandler.getInstance(this).addDBChangeListener();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
