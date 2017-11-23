package com.sourav.weatherfore.sync;

/**
 * Created by Sourav on 11/21/2017.
 */

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SyncIntentService extends IntentService {
    //Create a constructor that calls super and passes the name of this class

    public SyncIntentService() {
        super("SyncIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        SyncTask.syncWeather(this);
    }
}
