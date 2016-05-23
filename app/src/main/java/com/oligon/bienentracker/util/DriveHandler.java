package com.oligon.bienentracker.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.oligon.bienentracker.BeeApplication;
import com.oligon.bienentracker.R;
import com.oligon.bienentracker.ui.activities.HomeActivity;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DriveHandler {

    private Context context;
    private static DriveHandler instance;
    private static GoogleApiClient mApiClient;
    private AlertDialog mUpgradeDialog;

    public static synchronized DriveHandler getInstance(FragmentActivity context) {
        if (instance == null) {
            instance = new DriveHandler(context);
        }
        return instance;
    }

    public DriveHandler(FragmentActivity context) {
        this.context = context;
        mApiClient = BeeApplication.getApiClient(context);
        mUpgradeDialog = null;
    }

    public void syncDatabase() {
        final long lastUpload = PreferenceManager.getDefaultSharedPreferences(context).getLong("database_timestamp", 0);
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.contains(
                        SearchableField.TITLE, "Hive"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Cannot query directory");
                        } else {
                            boolean fileExists = false;
                            for (Metadata m : result.getMetadataBuffer()) {
                                if (m.getTitle().contains("Hive")) {
                                    String[] fileTitle = m.getTitle().split("_");
                                    if (fileTitle.length > 2) {
                                        final long timestamp = Long.parseLong(fileTitle[1]);
                                        int dbVersion = HiveDB.getInstance(context).getVersion();
                                        fileExists = true;
                                        final DriveFile file = m.getDriveId().asDriveFile();

                                        if (Integer.parseInt(fileTitle[2]) > dbVersion) {
                                            AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
                                            dialog.setMessage(context.getString(R.string.sync_dialog_error_message));
                                            dialog.setTitle(context.getString(R.string.sync_dialog_error_title));
                                            dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                            dialog.setCancelable(false);
                                            dialog.show();
                                            break;
                                        }

                                        Log.d(BeeApplication.TAG, "Found file with timestamp: " + timestamp);
                                        if (timestamp > lastUpload) { // remote database newer
                                            Log.d(BeeApplication.TAG, "File exists and newer");
                                            if (mUpgradeDialog != null) {
                                                try {
                                                    if (mUpgradeDialog.isShowing())
                                                        mUpgradeDialog.dismiss();
                                                } finally {
                                                    mUpgradeDialog = null;
                                                }
                                            }
                                            AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
                                            dialog.setMessage(context.getString(R.string.sync_dialog_message));
                                            dialog.setTitle(context.getString(R.string.sync_dialog_title));
                                            dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    new FetchDBAsyncTask(timestamp).execute(file);
                                                }
                                            });
                                            dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                            dialog.setCancelable(false);
                                            mUpgradeDialog = dialog.create();
                                            mUpgradeDialog.show();
                                        } else { // local database newer; upload local
                                            long newTimestamp = new Date().getTime();
                                            PreferenceManager.getDefaultSharedPreferences(context)
                                                    .edit()
                                                    .putLong("database_timestamp", newTimestamp)
                                                    .putInt("database_version", dbVersion)
                                                    .apply();
                                            Log.d(BeeApplication.TAG, "Updated local timestamp to " + newTimestamp);
                                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                    .setTitle("Hive_" + newTimestamp + "_" + dbVersion).build();
                                            file.updateMetadata(mApiClient, changeSet).setResultCallback(metadataUpdatedCallback);
                                        }
                                    }
                                    break;
                                }
                            }
                            if (!fileExists) { // No database yet
                                Log.d(BeeApplication.TAG, "File not found; creating it.");
                                Drive.DriveApi.newDriveContents(mApiClient)
                                        .setResultCallback(createRemoteDBCallback);
                            }
                            result.release();
                        }
                    }
                });
    }

    final private ResultCallback<DriveApi.DriveContentsResult> createRemoteDBCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while trying to create new file contents");
                        return;
                    }

                    long newTimestamp = new Date().getTime();
                    int dbVersion = HiveDB.getInstance(context).getVersion();
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putLong("database_timestamp", newTimestamp)
                            .putInt("database_version", dbVersion)
                            .apply();
                    Log.d(BeeApplication.TAG, "Updated local timestamp to " + newTimestamp);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("Hive_" + newTimestamp + "_" + dbVersion)
                            .setMimeType("text/plain")
                            .build();
                    Drive.DriveApi.getAppFolder(mApiClient)
                            .createFile(mApiClient, changeSet, result.getDriveContents())
                            .setResultCallback(fileCreatedCallback);
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCreatedCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while trying to create the file");
                        return;
                    }
                    Log.d(BeeApplication.TAG, "Created a file in App Folder: "
                            + result.getDriveFile().getDriveId());
                    new UploadDBAsyncTask().execute(result.getDriveFile());
                }
            };

    final ResultCallback<DriveResource.MetadataResult> metadataUpdatedCallback = new
            ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(@NonNull DriveResource.MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while updating file title");
                        return;
                    }
                    Log.d(BeeApplication.TAG, "File title updated to " + result.getMetadata().getTitle());
                    new UploadDBAsyncTask().execute(result.getMetadata().getDriveId().asDriveFile());
                }
            };

    public class UploadDBAsyncTask extends AsyncTask<DriveFile, Void, Boolean> {

        @Override
        protected Boolean doInBackground(DriveFile... args) {
            DriveFile file = args[0];
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        mApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    Log.d(BeeApplication.TAG, "Status error");
                    return false;
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();

                File dbFile = context.getDatabasePath(HiveDB.DB_NAME);
                FileInputStream fis = new FileInputStream(dbFile);

                OutputStream outputStream = driveContents.getOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
                outputStream.close();
                fis.close();
                com.google.android.gms.common.api.Status status =
                        driveContents.commit(mApiClient, null).await();
                return status.getStatus().isSuccess();
            } catch (IOException e) {
                Log.e(BeeApplication.TAG, "IOException while appending to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result == null || !result) {
                Log.d(BeeApplication.TAG, "Error while uploading db");
                return;
            }
            Log.d(BeeApplication.TAG, "Successfully uploaded db");
            HomeActivity.finishedRefreshing();
        }
    }

    public class FetchDBAsyncTask extends AsyncTask<DriveFile, Void, Boolean> {

        private long timestamp;

        public FetchDBAsyncTask(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        protected Boolean doInBackground(DriveFile... args) {
            DriveFile file = args[0];
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        mApiClient, DriveFile.MODE_READ_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    Log.d(BeeApplication.TAG, "Status error");
                    return false;
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();
                InputStream inputStream = driveContents.getInputStream();
                OutputStream output = new FileOutputStream(context.getDatabasePath(HiveDB.DB_NAME).getAbsolutePath());

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
                inputStream.close();
                output.flush();
                output.close();
                return true;
            } catch (IOException e) {
                Log.e(BeeApplication.TAG, "IOException while appending to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result == null || !result) {
                Log.d(BeeApplication.TAG, "Error while fetching database");
                return;
            }
            Log.d(BeeApplication.TAG, "Successfully fetched database");
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putLong("database_timestamp", timestamp).apply();
            HiveDB.forceUpgrade();
            HomeActivity.updateList();
            HomeActivity.updateGroups();
            HomeActivity.finishedRefreshing();
        }
    }

    public void syncPreferences() {
        final long lastUpload = PreferenceManager.getDefaultSharedPreferences(context).getLong("preferences_timestamp", 0);
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.contains(
                        SearchableField.TITLE, "Preferences"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Cannot query directory");
                        } else {
                            boolean fileExists = false;
                            for (Metadata m : result.getMetadataBuffer()) {
                                if (m.getTitle().contains("Preferences")) {
                                    String[] fileTitle = m.getTitle().split("_");
                                    if (fileTitle.length > 1) {
                                        final long timestamp = Long.parseLong(fileTitle[1]);
                                        final DriveFile file = m.getDriveId().asDriveFile();
                                        fileExists = true;
                                        Log.d(BeeApplication.TAG, "Found file with timestamp: " + timestamp);

                                        if (timestamp > lastUpload) { // remote database newer
                                            Log.d(BeeApplication.TAG, "File exists and newer");
                                            new FetchPreferencesAsyncTask(timestamp).execute(file);
                                        } else { // local database newer; upload local
                                            long newTimestamp = new Date().getTime();
                                            PreferenceManager.getDefaultSharedPreferences(context)
                                                    .edit()
                                                    .putLong("preferences_timestamp", newTimestamp)
                                                    .apply();
                                            Log.d(BeeApplication.TAG, "Updated local timestamp to " + newTimestamp);
                                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                    .setTitle("Preferences_" + newTimestamp).build();
                                            file.updateMetadata(mApiClient, changeSet).setResultCallback(metadataUpdatedCallbackPreferences);
                                        }
                                    }
                                    break;
                                }
                            }
                            if (!fileExists) { // No database yet
                                Log.d(BeeApplication.TAG, "File not found; creating it.");
                                Drive.DriveApi.newDriveContents(mApiClient)
                                        .setResultCallback(createRemoteDBCallbackPreferences);
                            }
                            result.release();
                        }
                    }
                });
    }

    final private ResultCallback<DriveApi.DriveContentsResult> createRemoteDBCallbackPreferences =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while trying to create new file contents");
                        return;
                    }

                    long newTimestamp = new Date().getTime();
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putLong("preferences_timestamp", newTimestamp)
                            .apply();
                    Log.d(BeeApplication.TAG, "Updated local timestamp to " + newTimestamp);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("Preferences_" + newTimestamp)
                            .setMimeType("text/plain")
                            .build();
                    Drive.DriveApi.getAppFolder(mApiClient)
                            .createFile(mApiClient, changeSet, result.getDriveContents())
                            .setResultCallback(fileCreatedCallbackPreferences);
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCreatedCallbackPreferences = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while trying to create the file");
                        return;
                    }
                    Log.d(BeeApplication.TAG, "Created a file in App Folder: "
                            + result.getDriveFile().getDriveId());
                    new UploadPreferencesAsyncTask().execute(result.getDriveFile());
                }
            };

    final ResultCallback<DriveResource.MetadataResult> metadataUpdatedCallbackPreferences = new
            ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(@NonNull DriveResource.MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while updating file title");
                        return;
                    }
                    Log.d(BeeApplication.TAG, "File title updated to " + result.getMetadata().getTitle());
                    new UploadPreferencesAsyncTask().execute(result.getMetadata().getDriveId().asDriveFile());
                }
            };

    public class UploadPreferencesAsyncTask extends AsyncTask<DriveFile, Void, Boolean> {

        @Override
        protected Boolean doInBackground(DriveFile... args) {
            DriveFile file = args[0];
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        mApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    Log.d(BeeApplication.TAG, "Status error");
                    return false;
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();

                File preferenceFile = new File(context.getApplicationInfo().dataDir + "/shared_prefs/"
                        + context.getPackageName() + "_preferences.xml");
                FileInputStream fis = new FileInputStream(preferenceFile);
                Log.d(BeeApplication.TAG, context.getApplicationInfo().dataDir
                        + "/shared_prefs/" + context.getPackageName() + "_preferences.xml");

                OutputStream outputStream = driveContents.getOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
                outputStream.close();
                fis.close();
                com.google.android.gms.common.api.Status status =
                        driveContents.commit(mApiClient, null).await();
                return status.getStatus().isSuccess();
            } catch (IOException e) {
                Log.e(BeeApplication.TAG, "IOException while appending to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result == null || !result) {
                Log.d(BeeApplication.TAG, "Error while uploading");
                return;
            }
            Log.d(BeeApplication.TAG, "Successfully uploaded preferences");
        }
    }

    public class FetchPreferencesAsyncTask extends AsyncTask<DriveFile, Void, Boolean> {


        private long timestamp;

        public FetchPreferencesAsyncTask(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        protected Boolean doInBackground(DriveFile... args) {
            DriveFile file = args[0];
            try {
                DriveApi.DriveContentsResult driveContentsResult = file.open(
                        mApiClient, DriveFile.MODE_READ_ONLY, null).await();
                if (!driveContentsResult.getStatus().isSuccess()) {
                    Log.d(BeeApplication.TAG, "Status error");
                    return false;
                }
                DriveContents driveContents = driveContentsResult.getDriveContents();
                InputStream inputStream = driveContents.getInputStream();

                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(context);

                SharedPreferences.Editor editor = sharedPreferences.edit();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.parse(inputStream);
                Element root = doc.getDocumentElement();

                Node child = root.getFirstChild();
                while (child != null) {
                    if (child.getNodeType() == Node.ELEMENT_NODE) {

                        Element element = (Element) child;

                        String type = element.getNodeName();
                        String name = element.getAttribute("name");

                        if (name.contains("database")) {
                            child = child.getNextSibling();
                            continue;
                        }

                        switch (type) {
                            case "string": {
                                String value = element.getTextContent();
                                editor.putString(name, value);
                                break;
                            }
                            case "boolean": {
                                String value = element.getAttribute("value");
                                editor.putBoolean(name, value.equals("true"));
                                break;
                            }
                            case "set": {
                                Set<String> set = new HashSet<>();
                                NodeList strings = element.getElementsByTagName("string");
                                for (int i = 0; i < strings.getLength(); i++) {
                                    set.add(strings.item(i).getTextContent());
                                }
                                editor.putStringSet(name, set);
                                break;
                            }
                            case "int": {
                                String value = element.getAttribute("value");
                                editor.putInt(name, Integer.valueOf(value));
                                break;
                            }
                        }
                    }
                    child = child.getNextSibling();

                }
                editor.apply();
                return true;
            } catch (IOException | ParserConfigurationException | SAXException e) {
                Log.e(BeeApplication.TAG, "Exception while appending to the output stream", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result == null || !result) {
                Log.d(BeeApplication.TAG, "Error while fetching Preferences");
                return;
            }
            Log.d(BeeApplication.TAG, "Successfully fetched Preferences");
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putLong("preferences_timestamp", timestamp).apply();
        }
    }

}
