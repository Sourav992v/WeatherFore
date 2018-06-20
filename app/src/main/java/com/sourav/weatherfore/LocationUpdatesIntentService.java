package com.sourav.weatherfore;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationResult;

import java.util.List;

/**
 * Created by Sourav on 4/19/2018.
 */

public class LocationUpdatesIntentService extends IntentService {

    static final String ACTION_PROCESS_UPDATES =
            "com.sourav.weatherfore.action" + ".PROCESS_UPDATES";

    public LocationUpdatesIntentService() {
        // Name the worker thread.
        super("LocationUpdatesIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    LocationResultHelper locationResultHelper = new LocationResultHelper(this,
                            locations);
                    // Save the location data to SharedPreferences.
                    locationResultHelper.saveResults();
                }
            }
        }
    }
}
