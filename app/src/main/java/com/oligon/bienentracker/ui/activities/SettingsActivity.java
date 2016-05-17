package com.oligon.bienentracker.ui.activities;


import android.Manifest;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.oligon.bienentracker.BeeApplication;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.util.AppCompatPreferenceActivity;
import com.oligon.bienentracker.util.DriveHandler;
import com.oligon.bienentracker.util.HiveDB;
import com.oligon.bienentracker.util.adapter.RestoreListAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class SettingsActivity extends AppCompatPreferenceActivity {

    private static Context context;
    private static final int FILE_WRITE_PERMISSION_EXPORT = 214739;
    private static final int FILE_WRITE_PERMISSION_BACKUP = 324123;
    private static FragmentManager fm;

    private static SharedPreferences prefs;

    private static boolean isPremiumUser, isStatisticsUser;
    private static boolean openFile;
    private static String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_settings);
        setupActionBar();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isPremiumUser = prefs.getBoolean("premium_user", false);
        isStatisticsUser = prefs.getBoolean("statistics_package", false);

        fm = getFragmentManager();
        getFragmentManager().beginTransaction()
                .replace(R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (openFile)
            openFile(filePath);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BeeApplication.getApiClient((HomeActivity) HomeActivity.context).isConnected()) {
            Log.d(BeeApplication.TAG, "Synching Preferences");
            DriveHandler.getInstance((HomeActivity) HomeActivity.context).createPreferencesFile();
        }
    }

    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

        EditTextPreference mName, mId, mMail;
        public static Preference export, backup;
        public static Preference premium, statistics;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);

            export = findPreference("pref_export_excel");
            export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showExportDialog();
                    return true;
                }
            });
            backup = findPreference("pref_export_backup");
            backup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showBackupDialog();
                    return true;
                }
            });

            premium = findPreference("pref_premium_purchase");
            premium.setOnPreferenceClickListener(this);
            statistics = findPreference("pref_premium_statistics");
            statistics.setOnPreferenceClickListener(this);
            findPreference("pref_premium_donate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new DonateDialogFragment().show(fm, "DonateDialog");
                    return false;
                }
            });


            mName = (EditTextPreference) findPreference("pref_user_name");
            mId = (EditTextPreference) findPreference("pref_user_id");
            mMail = (EditTextPreference) findPreference("pref_user_mail");

            mMail.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mMail.setSummary((String) newValue);
                    return true;
                }
            });
            mId.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mId.setSummary((String) newValue);
                    return true;
                }
            });
            mName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mName.setSummary((String) newValue);
                    return true;
                }
            });

            bindSummary(mName);
            bindSummary(mId);
            bindSummary(mMail);

            if (isPremiumUser) {
                SettingsFragment.export.setEnabled(true);
                SettingsFragment.backup.setEnabled(true);
                SettingsFragment.premium.setEnabled(false);
                SettingsFragment.premium.setTitle(R.string.prefs_premium_purchased);
                SettingsFragment.premium.setSummary(R.string.prefs_premium_purchased_summary);
            } else {
                SettingsFragment.export.setEnabled(false);
                SettingsFragment.backup.setEnabled(false);
                SettingsFragment.premium.setEnabled(true);
            }

            if (isStatisticsUser) {
                SettingsFragment.statistics.setEnabled(false);
            } else {
                SettingsFragment.statistics.setEnabled(true);
            }
/*
            if (context.getApplicationContext().getPackageName().endsWith(".debug")) {
                SettingsFragment.export.setEnabled(true);
                SettingsFragment.backup.setEnabled(true);
            }*/


            export.setEnabled(isPremiumUser);
            premium.setEnabled(!isPremiumUser);
        }


        private void bindSummary(Preference pref) {
            String s = prefs.getString(pref.getKey(), null);
            if (s != null) pref.setSummary(s);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference.getKey().equals("pref_premium_purchase")) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
                dialog.setMessage(R.string.dialog_premium_purchase_message);
                dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Bundle buyIntentBundle = BeeApplication.mService.getBuyIntent(3, context.getPackageName(),
                                    "premium_user", "inapp", "l-#dWQ8rbF#}&R4S$uH{gt&ESH#G14329112gqY5?&u=[+-6E9Hh3?mBHRe");
                            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                            if (pendingIntent != null)
                                getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                                        1001, new Intent(), 0, 0, 0);
                        } catch (RemoteException | IntentSender.SendIntentException e) {
                            e.printStackTrace();
                            BeeApplication.getInstance().trackException(e);
                        }
                        dialog.dismiss();
                    }
                });
                dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog.create().show();
            } else if (preference.getKey().equals("pref_premium_statistics")) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
                dialog.setMessage(R.string.dialog_premium_statistics_message);
                dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Bundle buyIntentBundle = BeeApplication.mService.getBuyIntent(3, context.getPackageName(),
                                    "statistics_package", "inapp", "K_9Iuu$Du}8g0j0$P2CP{Q,`8491P^%-IV)HfutXa|qOf?QT/9AggmoC27$aAv");
                            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                            if (pendingIntent != null)
                                getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(),
                                        1001, new Intent(), 0, 0, 0);
                        } catch (RemoteException | IntentSender.SendIntentException e) {
                            e.printStackTrace();
                            BeeApplication.getInstance().trackException(e);
                        }
                        dialog.dismiss();
                    }
                });
                dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                dialog.create().show();
            }
            return false;
        }
    }

    public static class ExportDialogFragment extends DialogFragment {

        CheckBox cbSendMail, cbOpenFile;
        EditText etAddress;
        EditText etFileName;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogOrange);

            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_export_xls, null);

            cbSendMail = (CheckBox) view.findViewById(R.id.cbExportSend);
            cbOpenFile = (CheckBox) view.findViewById(R.id.cbExportOpen);
            etAddress = (EditText) view.findViewById(R.id.etExportAddress);
            etFileName = (EditText) view.findViewById(R.id.etExportFilename);

            cbSendMail.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    etAddress.setEnabled(isChecked);
                }
            });

            etAddress.setText(prefs.getString("pref_user_mail", ""));


            builder.setView(view)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ExportDialogFragment.this.getDialog().cancel();
                        }
                    });
            return builder.create();
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
                        if (etFileName.getText().toString().isEmpty()) {
                            etFileName.setError(getString(R.string.error_et));
                            return;
                        }
                        if (cbSendMail.isChecked() && etAddress.getText().toString().isEmpty()) {
                            etAddress.setError(getString(R.string.error_et));
                            return;
                        }
                        d.dismiss();
                        exportToExcel(cbSendMail.isChecked(), cbOpenFile.isChecked(), etAddress.getText().toString(), etFileName.getText().toString());
                    }
                });
            }
        }
    }

    public static class BackupDialogFragment extends DialogFragment {

        private static RecyclerView mList;
        private static TextView mLastBackup;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogOrange);

            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_export_backup, null);

            Button done = (Button) view.findViewById(R.id.dialog_ok);
            done.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getDialog().dismiss();
                }
            });
            Button backup = (Button) view.findViewById(R.id.dialog_export_backup);
            backup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        backupSQL();
                        prefs.edit().putLong("last_backup_date", new Date().getTime()).apply();
                        updateUI();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), R.string.error_msg_backup_error, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mLastBackup = (TextView) view.findViewById(R.id.dialog_export_backup_last_backup);


            mList = (RecyclerView) view.findViewById(R.id.dialog_backup_list);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            mList.setLayoutManager(llm);
            mList.setHasFixedSize(true);

            updateUI();

            builder.setView(view);
            return builder.create();
        }

        public static void updateUI() {
            Map<String, File> elements = getAvailableDatabases();
            List<String> names = new ArrayList<>();
            List<File> files = new ArrayList<>();
            for (Map.Entry<String, File> e : elements.entrySet()) {
                names.add(e.getKey());
                files.add(e.getValue());
            }

            RestoreListAdapter adapter = new RestoreListAdapter(names, files);
            mList.setAdapter(adapter);

            long lastBackup = prefs.getLong("last_backup_date", 0);
            if (lastBackup != 0) {
                Date result = new Date(lastBackup);
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy kk:mm", Locale.getDefault());
                mLastBackup.setText(String.format(context.getString(R.string.last_backup), sdf.format(result)));
            } else {
                mLastBackup.setText(R.string.no_backup);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case FILE_WRITE_PERMISSION_EXPORT:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    showExportDialog();
                break;
            case FILE_WRITE_PERMISSION_BACKUP:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    showBackupDialog();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            if (resultCode == data.getIntExtra("RESPONSE_CODE", 0)) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    switch (sku) {
                        case "premium_user":
                            Toast.makeText(context, R.string.premium_purchased, Toast.LENGTH_LONG).show();
                            SettingsActivity.this.recreate();
                            break;
                        case "statistics_package":
                            Toast.makeText(context, R.string.statistics_purchased, Toast.LENGTH_LONG).show();
                            SettingsActivity.this.recreate();
                            break;
                        default:
                            BeeApplication.mService.consumePurchase(3, getPackageName(), jo.getString("purchaseToken"));
                            break;
                    }
                } catch (JSONException | RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static void showBackupDialog() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((SettingsActivity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    FILE_WRITE_PERMISSION_BACKUP);
        } else {
            new BackupDialogFragment().show(fm, "BackupDialog");
        }
    }

    private static void showExportDialog() {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((SettingsActivity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    FILE_WRITE_PERMISSION_EXPORT);
        } else {
            new ExportDialogFragment().show(fm, "ExportDialog");
        }
    }

    private static void backupSQL() throws IOException {
        File dbFile = context.getDatabasePath(HiveDB.DB_NAME);
        FileInputStream fis = new FileInputStream(dbFile);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.getDefault());

        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + "/Bienen Tracker");

        if (!directory.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }

        File outFile = new File(directory, "BienenTracker_"
                + sdf.format(new Date()) + ".db");

        OutputStream output = new FileOutputStream(outFile);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        output.flush();
        output.close();
        fis.close();
    }

    public static void restoreSQL(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        OutputStream output = new FileOutputStream(context.getDatabasePath(HiveDB.DB_NAME).getAbsolutePath());

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }
        inputStream.close();
        output.flush();
        output.close();
    }

    private static Map<String, File> getAvailableDatabases() {
        Map<String, File> files = new TreeMap<>(Collections.reverseOrder());

        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + "/Bienen Tracker");

        if (!directory.isDirectory()) {
            return files;
        }
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                String filename = file.getName();
                if (filename.endsWith(".db")) {
                    //String name = filename.replaceAll("(\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2})", "$1");
                    files.put(filename, file);
                }
            }
        }

        return files;
    }

    private static void exportToExcel(boolean send, boolean open, String address, String filename) {
        String path = HomeActivity.db.exportToExcel(filename + ".xls");
        if (send) {
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
            emailIntent.setType("text/plain");

            Uri uri = Uri.parse("file://" + path);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            context.startActivity(emailIntent);
        }
        if (open) {
            if (send) {
                openFile = true;
                filePath = "file://" + path;
            } else
                openFile("file://" + path);

        }
    }

    private static void openFile(String path) {
        openFile = false;
        Intent i = new Intent();
        i.setAction(android.content.Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse(path), "application/vnd.ms-excel");
        context.startActivity(i);
    }

    public static class DonateDialogFragment extends DialogFragment {

        RecyclerView list;
        ProgressBar progress;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogOrange);
            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_load_list, null);
            list = (RecyclerView) view.findViewById(R.id.load_list);
            progress = (ProgressBar) view.findViewById(R.id.progress);
            progress.setIndeterminate(true);

            LinearLayoutManager llm = new LinearLayoutManager(context);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            list.setLayoutManager(llm);
            list.setAdapter(null);

            new LoadSKUs().execute(context.getResources().getStringArray(R.array.donations_ids));

            return builder.setView(view)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
        }

        private class LoadSKUs extends AsyncTask<String, Void, DonateListAdapter> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            protected DonateListAdapter doInBackground(String... params) {
                Bundle querySkus = new Bundle();
                querySkus.putStringArrayList("ITEM_ID_LIST",
                        new ArrayList<>(Arrays.asList(params)));
                Log.d("BienenTracker", querySkus.toString());
                try {
                    Bundle skuDetails = BeeApplication.mService.getSkuDetails(3,
                            context.getPackageName(), "inapp", querySkus);
                    int response = skuDetails.getInt("RESPONSE_CODE");
                    if (response == 0) {
                        ArrayList<String> responseList
                                = skuDetails.getStringArrayList("DETAILS_LIST");
                        List<String> sku = new ArrayList<>();
                        List<String> title = new ArrayList<>();
                        List<String> summary = new ArrayList<>();
                        List<String> price = new ArrayList<>();
                        if (responseList != null) {
                            for (String thisResponse : responseList) {
                                JSONObject object = new JSONObject(thisResponse);
                                sku.add(object.getString("productId"));
                                String s = object.getString("title");
                                s = s.substring(0, s.length() - 17);
                                title.add(s);
                                summary.add(object.getString("description"));
                                price.add(object.getString("price"));
                            }
                        }

                        return new DonateListAdapter(sku, title, summary, price);
                    }
                } catch (RemoteException | JSONException e) {
                    e.printStackTrace();
                    Log.d("BienenTracker", e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(DonateListAdapter donateListAdapter) {
                super.onPostExecute(donateListAdapter);
                list.setAdapter(donateListAdapter);
                progress.setVisibility(View.GONE);
            }
        }

        private static class DonateListAdapter extends RecyclerView.Adapter<DonateListAdapter.DonateViewHolder> {

            private static List<String> skus;
            private List<String> titles;
            private List<String> summaries;
            private List<String> prices;

            public DonateListAdapter(List<String> skus, List<String> titles, List<String> summaries, List<String> prices) {
                DonateListAdapter.skus = skus;
                this.titles = titles;
                this.summaries = summaries;
                this.prices = prices;
            }

            @Override
            public DonateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(context)
                        .inflate(R.layout.list_donate_item, parent, false);
                return new DonateViewHolder(v);
            }

            @Override
            public void onBindViewHolder(DonateViewHolder holder, int position) {
                holder.mTitle.setText(titles.get(position));
                holder.mSummary.setText(summaries.get(position));
                holder.mPrice.setText(prices.get(position));
            }

            @Override
            public int getItemCount() {
                return titles.size();
            }

            public static class DonateViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

                public TextView mTitle, mSummary, mPrice;
                public View item;

                public DonateViewHolder(View v) {
                    super(v);
                    item = v;
                    mTitle = (TextView) v.findViewById(R.id.donate_title);
                    mSummary = (TextView) v.findViewById(R.id.donate_summary);
                    mPrice = (TextView) v.findViewById(R.id.donate_price);
                    item.setOnClickListener(this);
                }

                @Override
                public void onClick(View v) {
                    donate(skus.get(getAdapterPosition()));
                }
            }
        }
    }

    private static void donate(String id) {
        try {
            Bundle buyIntentBundle = BeeApplication.mService.getBuyIntent(3, context.getPackageName(),
                    id, "inapp", "l-#dWasdf&ES2g3414329112gqY5sdf9Hh3?mBHRe");
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent != null)
                ((SettingsActivity) context).startIntentSenderForResult(pendingIntent.getIntentSender(),
                        1001, new Intent(), 0, 0, 0);
        } catch (RemoteException | IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

}
