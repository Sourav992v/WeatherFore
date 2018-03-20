package com.sourav.weatherfore.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.sourav.weatherfore.R;
import com.sourav.weatherfore.db.WeatherContract;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Created by Sourav on 11/21/2017.
 */

public class SyncUtils {

    // Add constant values to sync Sunshine every 3 - 4 hours
    /*
     * Interval at which to sync with the weather. Use TimeUnit for convenience, rather than
     * writing out a bunch of multiplication ourselves and risk making a silly mistake.
     */
    private static final int SYNC_INTERVAL_HOURS = 3;
    private static final int SYNC_INTERVAL_SECONDS = (int) TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS);
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS / 3;

    public static final String ACTION_DATA_UPDATED = "com.sourav.weatherfore.ACTION_DATA_UPDATED" ;

    private static boolean sInitialized;

    //  Add a sync tag to identify our sync job
    private static final String WEATHERFORE_SYNC_TAG = "weatherfore_sync";

    // Create a method to schedule our periodic weather sync

    /**
     * Schedules a repeating sync of WeatherFore's weather data using FirebaseJobDispatcher.
     *
     * @param context Context used to create the GooglePlayDriver that powers the
     *                FirebaseJobDispatcher
     */

    private static void scheduleFirebaseJobDispatcherSync(Context context) {

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

         /* Create the Job to periodically sync Sunshine */
        Job syncJob = dispatcher.newJobBuilder()
                /* The Service that will be used to sync Sunshine's data */
                .setService(FirebaseJobService.class)
                /* Set the UNIQUE tag used to identify this Job */
                .setTag(WEATHERFORE_SYNC_TAG)
                 /*
                 * Network constraints on which this Job should run. We choose to run on any
                 * network, but you can also choose to run only on un-metered networks or when the
                 * device is charging. It might be a good idea to include a preference for this,
                 * as some users may not want to download any data on their mobile plan. ($$$)
                 */
                .setConstraints(Constraint.ON_ANY_NETWORK)
                 /*
                 * setLifetime sets how long this job should persist. The options are to keep the
                 * Job "forever" or to have it die the next time the device boots up.
                 */
                .setLifetime(Lifetime.FOREVER)
                /*
                 * We want  weather data to stay up to date, so we tell this Job to recur.
                 */
                .setRecurring(true)
                /*
                 * We want the weather data to be synced every 3 to 4 hours. The first argument for
                 * Trigger's static executionWindow method is the start of the time frame when the
                 * sync should be performed. The second argument is the latest point in time at
                 * which the data should be synced. Please note that this end time is not
                 * guaranteed, but is more of a guideline for FirebaseJobDispatcher to go off of.
                 */
                .setTrigger(Trigger.executionWindow(
                        SYNC_INTERVAL_SECONDS,
                        SYNC_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                 /*
                 * If a Job with the tag with provided already exists, this new job will replace
                 * the old one.
                 */
                .setReplaceCurrent(true)
                 /* Once the Job is ready, call the builder's build method to return the Job */
                .build();
                 /* Schedule the Job with the dispatcher */
        dispatcher.schedule(syncJob);
    }

    /**
     * Creates periodic sync tasks and checks to see if an immediate sync is required. If an
     * immediate sync is required, this method will take care of making sure that sync occurs.
     *
     * @param context Context that will be passed to other methods and used to access the
     *                ContentResolver
     */
    //  COMPLETED (2) Create a synchronized public static void method called initialize
    @SuppressLint("StaticFieldLeak")
    synchronized public static void initialize(@NonNull final Context context) {
        //Only execute this method body if sInitialized is false
        /*
         * Only perform initialization once per app lifetime. If initialization has already been
         * performed, we have nothing to do in this method.
         */
        if (sInitialized) return;

        //If the method body is executed, set sInitialized to true
        sInitialized = true;

        //Call the method you created to schedule a periodic weather sync
        /*
         * This method call triggers Sunshine to create its task to synchronize weather data
         * periodically.
         */

        getSyncAccount(context);
        scheduleFirebaseJobDispatcherSync(context);
         /*
         * We need to check to see if our ContentProvider has data to display in our forecast
         * list. However, performing a query on the main thread is a bad idea as this may
         * cause our UI to lag. Therefore, we create a thread in which we will run the query
         * to check the contents of our ContentProvider.
         */

        Thread checkForEmpty = new Thread(new Runnable() {
            @Override
            public void run() {
                  /* URI for every row of weather data in our weather table*/
                Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;
                 /*
                 * Since this query is going to be used only as a check to see if we have any
                 * data (rather than to display data), we just need to PROJECT the ID of each
                 * row. In our queries where we display data, we need to PROJECT more columns
                 * to determine what weather details need to be displayed.
                 */
                String[] projectionColumns = {WeatherContract.WeatherEntry._ID};
                String selectionStatement = WeatherContract.WeatherEntry
                        .getSqlSelectForTodayOnwards();

                   /* Here, we perform the query to check to see if we have any weather data */
                Cursor cursor = context.getContentResolver().query(
                        forecastQueryUri,
                        projectionColumns,
                        selectionStatement,
                        null,
                        null);
                /*
                 * A Cursor object can be null for various different reasons. A few are
                 * listed below.
                 *
                 *   1) Invalid URI
                 *   2) A certain ContentProvider's query method returns null
                 *   3) A RemoteException was thrown.
                 *
                 * Bottom line, it is generally a good idea to check if a Cursor returned
                 * from a ContentResolver is null.
                 *
                 * If the Cursor was null OR if it was empty, we need to sync immediately to
                 * be able to display data to the user.
                 */
                if (null == cursor || cursor.getCount() == 0) {
                    startImmediateSync(context);
                }

                /* Make sure to close the Cursor to avoid memory leaks! */
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
        checkForEmpty.start();
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    private static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        // we can enable inexact timers in our periodic sync
        SyncRequest request = new SyncRequest.Builder().
                syncPeriodic(syncInterval, flexTime).
                setSyncAdapter(account, authority).
                setExtras(new Bundle()).build();
        ContentResolver.requestSync(request);
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    private static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (accountManager != null && null == accountManager.getPassword(newAccount)) {

            /*
             * Add the account and account type, no password or user data
             * If successful, return the Account object, otherwise report an error.
             */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
                /*
                 * If you don't set android:syncable="true" in
                 * in your <provider> element in the manifest,
                 * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
                 * here.
                 */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SyncUtils.configurePeriodicSync(context, SYNC_INTERVAL_SECONDS , SYNC_FLEXTIME_SECONDS);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    /**
     * Helper method to perform a sync immediately using an IntentService for asynchronous
     * execution.
     *
     * @param context The Context used to start the IntentService for the sync.
     */
    public static void startImmediateSync(final Context context) {
        //Within that method, start the SyncIntentService
        Intent intentToSyncImmediately = new Intent(context, SyncIntentService.class);
        context.startService(intentToSyncImmediately);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_SERVER_INVALID,  LOCATION_STATUS_UNKNOWN, LOCATION_STATUS_INVALID})
    public @interface LocationStatus {}

    public static final int LOCATION_STATUS_OK = 0;
    public static final int LOCATION_STATUS_SERVER_DOWN = 1;
    public static final int LOCATION_STATUS_SERVER_INVALID = 2;
    public static final int LOCATION_STATUS_UNKNOWN = 3;
    public static final int LOCATION_STATUS_INVALID = 4;

}
