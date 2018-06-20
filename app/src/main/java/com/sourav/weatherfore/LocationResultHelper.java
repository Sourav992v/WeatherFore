package com.sourav.weatherfore;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

import com.sourav.weatherfore.sync.SyncUtils;
import com.sourav.weatherfore.utilities.WeatherUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Sourav on 4/19/2018.
 */

class LocationResultHelper {


    private Context mContext;
    private List<Location> mLocations;

    LocationResultHelper(Context context, List<Location> locations) {
        mContext = context;
        mLocations = locations;
    }


    private String getLocationResultText() {
        if (mLocations.isEmpty()) {
            return mContext.getString(R.string.unknown_location);
        }
        StringBuilder sb = new StringBuilder();
        for (Location location : mLocations) {

            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (addresses != null) {
                sb.append(addresses.get(0).getSubAdminArea());
                sb.append(", ");
                sb.append(addresses.get(0).getLocality());
            }

        }
        return sb.toString();
    }

    /**
     * Saves location result as a string to {@link android.content.SharedPreferences}.
     */
    void saveResults() {
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .putString(mContext.getString(R.string.pref_location_key), getLocationResultText())
                .commit();
    }
}
