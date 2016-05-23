package com.oligon.bienentracker;


import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatDelegate;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.ecommerce.Product;
import com.google.android.gms.analytics.ecommerce.ProductAction;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.oligon.bienentracker.util.AnalyticsTracker;

import java.util.ArrayList;

public class BeeApplication extends Application implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static final String TAG = BeeApplication.class.getSimpleName();

    private static GoogleApiClient mGoogleApiClient;

    private static BeeApplication mInstance;

    public static IInAppBillingService mService;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                    .putBoolean("premium_user", isPremiumUser())
                    .putBoolean("statistics_package", isStatisticsUser()).apply();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        AnalyticsTracker.initialize(this);
        AnalyticsTracker.getInstance().get(AnalyticsTracker.Target.APP);

        bindBillingService();

    }

    private void initializeGoogleApis(BeeApplication beeApplication) {
        if (mGoogleApiClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(beeApplication)
                    //.enableAutoManage(ctx /* FragmentActivity */, (GoogleApiClient.OnConnectionFailedListener) ctx /* OnConnectionFailedListener */)
                    .addConnectionCallbacks(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .addApi(Drive.API)
                    .build();
        }
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mService != null) {
            try {
                unbindService(mServiceConn);
            } finally {
                mService = null;
            }
        }
        if (mGoogleApiClient != null) {
            try {
                mGoogleApiClient.disconnect();
            } finally {
                mGoogleApiClient = null;
            }
        }
    }

    private void bindBillingService() {
        try {
            Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
            serviceIntent.setPackage("com.android.vending");
            if (mService == null)
                bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized BeeApplication getInstance() {
        return mInstance;
    }

    private synchronized Tracker getGoogleAnalyticsTracker() {
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

        t.setScreenName(screenName);
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

    /**
     * Tracking In-App Purchase
     *
     * @param token Product Token
     * @param name  Product Name
     * @param price Pricing with tax
     * @param tax   Tax to be subtracted
     */
    public void trackInAppPurchase(String token, String name, float price, float tax) {
        Product product = new Product()
                .setId(token)
                .setName(name)
                .setPrice(price)
                .setQuantity(1);
        ProductAction productAction = new ProductAction(ProductAction.ACTION_PURCHASE)
                .setTransactionAffiliation("Google Store - Online")
                .setTransactionRevenue(price)
                .setTransactionTax(tax);
        HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder()
                .addProduct(product)
                .setProductAction(productAction);

        Tracker t = getGoogleAnalyticsTracker();
        t.setScreenName("transaction");
        t.send(builder.build());
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public static synchronized GoogleApiClient getApiClient(Context ctx) {
        if (mGoogleApiClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                    .requestEmail()
                    .build();

            mGoogleApiClient = new GoogleApiClient.Builder(ctx)
                    .enableAutoManage((FragmentActivity) ctx, (GoogleApiClient.OnConnectionFailedListener) ctx)
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

    private boolean isPremiumUser() {
        try {
            if (mService != null) {
                Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
                int response = ownedItems.getInt("RESPONSE_CODE");
                if (response == 0) {
                    ArrayList<String> ownedSkus =
                            ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    if (ownedSkus != null)
                        return ownedSkus.contains("premium_user");
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isStatisticsUser() {
        try {
            if (mService != null) {
                Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
                int response = ownedItems.getInt("RESPONSE_CODE");
                if (response == 0) {
                    ArrayList<String> ownedSkus =
                            ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    if (ownedSkus != null)
                        return ownedSkus.contains("statistics_package");
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
