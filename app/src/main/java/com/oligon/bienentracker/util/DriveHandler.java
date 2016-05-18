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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class DriveHandler {

    private Context context;
    private static DriveHandler instance;
    private static GoogleApiClient mApiClient;

    private static String FILE_ID, FILE_ID_PREFERENCES;

    public static synchronized DriveHandler getInstance(FragmentActivity context) {
        if (instance == null) {
            instance = new DriveHandler(context);
        }
        return instance;
    }

    public DriveHandler(FragmentActivity context) {
        this.context = context;
        mApiClient = BeeApplication.getApiClient(context);
    }

    private boolean isConnected() {
        return mApiClient.isConnected();
    }

    /*public void deleteDrive() {
        if (!isConnected()) return;
        Drive.DriveApi.getAppFolder(mApiClient)
                .listChildren(mApiClient)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Error while trying to create the folder");
                            return;
                        }
                        for (Metadata data : result.getMetadataBuffer()) {
                            Log.d(BeeApplication.TAG, "Title: " + data.getTitle());
                            data.getDriveId().asDriveResource().delete(mApiClient);
                        }
                    }
                });
    }*/

    public void addDBChangeListener() {
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(
                        SearchableField.TITLE, "Hive.db"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Cannot create file in the root.");
                        } else {
                            for (Metadata m : result.getMetadataBuffer()) {
                                if (m.getTitle().equals("Hive.db")) {
                                    Log.d(BeeApplication.TAG, "File exists");
                                    FILE_ID = m.getDriveId().getResourceId();
                                    m.getDriveId().asDriveFile().addChangeSubscription(mApiClient);
                                    break;
                                }
                            }
                            result.release();
                        }
                    }
                });
    }
/*
    public void getDBFromDrive() {
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(
                        SearchableField.TITLE, "Hive.db"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Cannot create file in the root.");
                        } else {
                            DriveFile file = null;
                            for (Metadata m : result.getMetadataBuffer()) {
                                if (m.getTitle().equals("Hive.db")) {
                                    Log.d(BeeApplication.TAG, "File exists");
                                    file = m.getDriveId().asDriveFile();
                                    break;
                                }
                            }
                            if (file != null) {
                                new FetchDBAsyncTask().execute(file);
                            }
                            result.release();
                        }
                    }
                });
    }*/
/*
    public void createDBFile() {
        HomeActivity.dbChanged = false;
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(
                        SearchableField.TITLE, "Hive"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mApiClient, query) // .getAppFolder(mApiClient).queryChildren
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Cannot create file in the root.");
                        } else {
                            boolean isFound = false;
                            for (Metadata m : result.getMetadataBuffer()) {
                                if (m.getTitle().equals("Hive.db")) {
                                    Log.d(BeeApplication.TAG, "File exists");
                                    FILE_ID = m.getDriveId().getResourceId();
                                    isFound = true;
                                    break;
                                }
                            }
                            if (!isFound) {
                                Log.d(BeeApplication.TAG, "File not found; creating it.");
                                Drive.DriveApi.newDriveContents(mApiClient)
                                        .setResultCallback(driveContentsCallback);
                            } else {
                                copyDBToDrive();
                            }
                        }
                        result.release();
                    }
                });
    }

    public void copyDBToDrive() {
        if (!isConnected()) return;

        final ResultCallback<DriveApi.DriveIdResult> idCallback = new ResultCallback<DriveApi.DriveIdResult>() {
            @Override
            public void onResult(@NonNull DriveApi.DriveIdResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d(BeeApplication.TAG, "Cannot find DriveId. Are you authorized to view this file?");
                    return;
                }
                new UploadDBAsyncTask().execute(result.getDriveId().asDriveFile());
            }
        };
        if (FILE_ID != null)
            Drive.DriveApi.fetchDriveId(mApiClient, FILE_ID)
                    .setResultCallback(idCallback);
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while trying to create new file contents");
                        return;
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("Hive.db")
                            .setMimeType("text/plain")
                            .build();
                    Drive.DriveApi.getAppFolder(mApiClient)
                            .createFile(mApiClient, changeSet, result.getDriveContents())
                            .setResultCallback(fileCallback);
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while trying to create the file");
                        return;
                    }
                    Log.d(BeeApplication.TAG, "Created a file in App Folder: "
                            + result.getDriveFile().getDriveId());
                    FILE_ID = result.getDriveFile().getDriveId().getResourceId();
                    if (FILE_ID != null)
                        copyDBToDrive();
                }
            };*/

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
                                    if (fileTitle.length > 1) {
                                        final long timestamp = Long.parseLong(fileTitle[1]);
                                        final DriveFile file = m.getDriveId().asDriveFile();
                                        fileExists = true;
                                        FILE_ID = m.getDriveId().getResourceId();
                                        Log.d(BeeApplication.TAG, "Found file with timestamp: " + timestamp);
                                        if (timestamp > lastUpload) { // remote database newer
                                            Log.d(BeeApplication.TAG, "File exists and newer");
                                            AlertDialog.Builder dialog = new AlertDialog.Builder(context, R.style.AlertDialogOrange);
                                            dialog.setMessage("Die lokale Datenbank wird mit einer neueren aus der Cloud überschrieben");
                                            dialog.setTitle("Synchronisation");
                                            dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    new FetchDBAsyncTask(timestamp).execute(file);
                                                }
                                            });
                                            dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });
                                            dialog.show();
                                        } else { // local database newer; upload local
                                            long newTimestamp = new Date().getTime();
                                            PreferenceManager.getDefaultSharedPreferences(context)
                                                    .edit()
                                                    .putLong("database_timestamp", newTimestamp)
                                                    .apply();
                                            Log.d(BeeApplication.TAG, "Updated local timestamp to " + newTimestamp);
                                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                                    .setTitle("Hive_" + newTimestamp).build();
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
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putLong("database_timestamp", newTimestamp)
                            .apply();
                    Log.d(BeeApplication.TAG, "Updated local timestamp to " + newTimestamp);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle("Hive_" + newTimestamp)
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
                public void onResult(DriveResource.MetadataResult result) {
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
                Log.d(BeeApplication.TAG, "Error while fetching");
                return;
            }
            Log.d(BeeApplication.TAG, "Successfully fetched");
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putLong("database_timestamp", timestamp).apply();
            HiveDB.forceUpgrade();
            HomeActivity.updateList();
            HomeActivity.updateGroups();
        }
    }

    // Shared Preferences
/*
    public void addPreferencesChangeListener() {
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(
                        SearchableField.TITLE, context.getPackageName() + "_preferences.xml"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Cannot create file in the root.");
                        } else {
                            for (Metadata m : result.getMetadataBuffer()) {
                                if (m.getTitle().equals(context.getPackageName() + "_preferences.xml")) {
                                    Log.d(BeeApplication.TAG, "File exists");
                                    FILE_ID_PREFERENCES = m.getDriveId().getResourceId();
                                    m.getDriveId().asDriveFile().addChangeSubscription(mApiClient);
                                    break;
                                }
                            }
                            result.release();
                        }
                    }
                });
    }

    public void getPreferencesFromDrive() {
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(
                        SearchableField.TITLE, context.getPackageName() + "_preferences.xml"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Cannot create file in the root.");
                        } else {
                            DriveFile file = null;
                            for (Metadata m : result.getMetadataBuffer()) {
                                if (m.getTitle().equals(context.getPackageName() + "_preferences.xml")) {
                                    Log.d(BeeApplication.TAG, "File exists");
                                    file = m.getDriveId().asDriveFile();
                                    break;
                                }
                            }
                            if (file != null) {
                                new FetchPreferencesAsyncTask().execute(file);
                            }
                            result.release();
                        }
                    }
                });
    }

    public void createPreferencesFile() {
        Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(
                        SearchableField.TITLE, context.getPackageName() + "_preferences.xml"),
                        Filters.eq(SearchableField.TRASHED, false)))
                .build();
        Drive.DriveApi.query(mApiClient, query)
                .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.MetadataBufferResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.d(BeeApplication.TAG, "Cannot create file in the root.");
                        } else {
                            boolean isFound = false;
                            for (Metadata m : result.getMetadataBuffer()) {
                                if (m.getTitle().equals(context.getPackageName() + "_preferences.xml")) {
                                    Log.d(BeeApplication.TAG, "File exists");
                                    FILE_ID_PREFERENCES = m.getDriveId().getResourceId();
                                    isFound = true;
                                    break;
                                }
                            }
                            if (!isFound) {
                                Log.d(BeeApplication.TAG, "File not found; creating it.");
                                Drive.DriveApi.newDriveContents(mApiClient)
                                        .setResultCallback(driveContentsCallbackPreferences);
                            } else {
                                copyPreferencesToDrive();
                            }
                        }
                        result.release();
                    }
                });
    }

    public void copyPreferencesToDrive() {
        if (!isConnected()) return;

        final ResultCallback<DriveApi.DriveIdResult> idCallback = new ResultCallback<DriveApi.DriveIdResult>() {
            @Override
            public void onResult(@NonNull DriveApi.DriveIdResult result) {
                if (!result.getStatus().isSuccess()) {
                    Log.d(BeeApplication.TAG, "Cannot find DriveId. Are you authorized to view this file?");
                    return;
                }
                new UploadPreferencesAsyncTask().execute(result.getDriveId().asDriveFile());
            }
        };
        if (FILE_ID_PREFERENCES != null)
            Drive.DriveApi.fetchDriveId(mApiClient, FILE_ID_PREFERENCES)
                    .setResultCallback(idCallback);
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallbackPreferences =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while trying to create new file contents");
                        return;
                    }

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(context.getPackageName() + "_preferences.xml")
                            .setMimeType("text/plain")
                            .build();
                    Drive.DriveApi.getAppFolder(mApiClient)
                            .createFile(mApiClient, changeSet, result.getDriveContents())
                            .setResultCallback(fileCallbackPreferences);
                }
            };

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallbackPreferences = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.d(BeeApplication.TAG, "Error while trying to create the file");
                        return;
                    }
                    Log.d(BeeApplication.TAG, "Created a file in App Folder: "
                            + result.getDriveFile().getDriveId());
                    FILE_ID_PREFERENCES = result.getDriveFile().getDriveId().getResourceId();
                    if (FILE_ID_PREFERENCES != null)
                        copyPreferencesToDrive();
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

                File preferenceFile = new File(context.getApplicationInfo().dataDir
                        + "/shared_prefs/" + context.getPackageName() + "_preferences.xml");
                FileInputStream fis = new FileInputStream(preferenceFile);

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
                OutputStream output = new FileOutputStream(context.getApplicationInfo().dataDir
                        + "/shared_prefs/" + context.getPackageName() + "_preferences.xml");

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
                Log.d(BeeApplication.TAG, "Error while fetching");
                return;
            }
            Log.d(BeeApplication.TAG, "Successfully fetched Preferences");
        }
    }*/

}
