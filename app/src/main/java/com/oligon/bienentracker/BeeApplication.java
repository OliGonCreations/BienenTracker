package com.oligon.bienentracker;


import android.app.Application;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.oligon.bienentracker.util.AnalyticsTracker;

public class BeeApplication extends Application implements GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = BeeApplication.class.getSimpleName();

    private static GoogleApiClient mGoogleApiClient;

    private static BeeApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        AnalyticsTracker.initialize(this);
        AnalyticsTracker.getInstance().get(AnalyticsTracker.Target.APP);
    }

    public static synchronized BeeApplication getInstance() {
        return mInstance;
    }

    public synchronized Tracker getGoogleAnalyticsTracker() {
        Tracker tracker = AnalyticsTracker.getInstance().get(AnalyticsTracker.Target.APP);
        tracker.enableAdvertisingIdCollection(true);
        return tracker;
    }

    /***
     * Tracking screen view
     *
     * @param screenName screen name to be displayed on GA dashboard
     */
    public void trackScreenView(String screenName) {
        Tracker t = getGoogleAnalyticsTracker();

        // Set screen name.
        t.setScreenName(screenName);

        // Send a screen view.
        t.send(new HitBuilders.ScreenViewBuilder().build());

        GoogleAnalytics.getInstance(this).dispatchLocalHits();
    }

    /***
     * Tracking exception
     *
     * @param e exception to be tracked
     */
    public void trackException(Exception e) {
        if (e != null) {
            Tracker t = getGoogleAnalyticsTracker();

            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription(
                            new StandardExceptionParser(this, null)
                                    .getDescription(Thread.currentThread().getName(), e))
                    .setFatal(false)
                    .build()
            );
        }
    }

    /***
     * Tracking event
     *
     * @param category event category
     * @param action   action of the event
     * @param label    label
     */
    public void trackEvent(String category, String action, String label) {
        Tracker t = getGoogleAnalyticsTracker();

        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder().setCategory(category).setAction(action).setLabel(label).build());
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public static synchronized GoogleApiClient getApiClient(FragmentActivity ctx) {
        if (mGoogleApiClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                    .enableAutoManage(ctx /* FragmentActivity */, (GoogleApiClient.OnConnectionFailedListener) ctx /* OnConnectionFailedListener */)
                    .addConnectionCallbacks((GoogleApiClient.ConnectionCallbacks) ctx)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .addApi(Drive.API)
                    .build();
        }
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        return mGoogleApiClient;
    }
}
