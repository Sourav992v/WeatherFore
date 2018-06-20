package com.sourav.weatherfore.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

/**
 * Created by Sourav on 11/21/2017.
 */

public class FirebaseJobService extends JobService {
    // Declare an ASyncTask field called mFetchWeatherTask
    private AsyncTask<Void, Void, Void> mFetchWeatherTask;
    // Override onStartJob and within it, spawn off a separate ASyncTask to sync weather data
    /**
     * The entry point to your Job. Implementations should offload work to another thread of
     * execution as soon as possible.
     *
     * This is called by the Job Dispatcher to tell us we should start our job. Keep in mind this
     * method is run on the application's main thread, so we need to offload work to a background
     * thread.
     *
     * @return whether there is more work remaining.
     */
    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters job) {
        mFetchWeatherTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Context context = getApplicationContext();
                SyncTask.syncWeather(context);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(job,false);

            }
        };
        mFetchWeatherTask.execute();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        if (mFetchWeatherTask != null){
            mFetchWeatherTask.cancel(true);
        }
        return false;
    }
}
