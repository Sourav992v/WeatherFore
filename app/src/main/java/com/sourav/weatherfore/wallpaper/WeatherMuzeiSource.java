package com.sourav.weatherfore.wallpaper;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;
import com.sourav.weatherfore.MainActivity;
import com.sourav.weatherfore.db.WeatherContract;
import com.sourav.weatherfore.db.WeatherPreferences;
import com.sourav.weatherfore.sync.SyncUtils;
import com.sourav.weatherfore.utilities.WeatherUtils;

/**
 * Created by Sourav on 3/10/2018.
 */

/**
 * Muzei source that changes your background based on the current weather conditions
 */
public class WeatherMuzeiSource extends MuzeiArtSource {
    private static final String[] FORECAST_COLUMNS = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_SHORT_DESC = 1;

    public WeatherMuzeiSource() {
        super("WeatherMuzeiSource");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        boolean dataUpdated = intent != null &&
                SyncUtils.ACTION_DATA_UPDATED.equals(intent.getAction());
        if (dataUpdated && isEnabled()) {
            onUpdate(UPDATE_REASON_OTHER);
        }
    }

    @Override
    protected void onUpdate(int reason) {
        String location = WeatherPreferences.getPreferredWeatherLocation(this);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                location, System.currentTimeMillis());
        Cursor cursor = getContentResolver().query(weatherForLocationUri, FORECAST_COLUMNS, null,
                null, WeatherContract.WeatherEntry.COLUMN_DATE + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            String desc = cursor.getString(INDEX_SHORT_DESC);

            String imageUrl = WeatherUtils.getImageUrlForWeatherCondition(weatherId);
            // Only publish a new wallpaper if we have a valid image
            if (imageUrl != null) {
                publishArtwork(new Artwork.Builder()
                        .imageUri(Uri.parse(imageUrl))
                        .title(desc)
                        .byline(location)
                        .viewIntent(new Intent(this, MainActivity.class))
                        .build());
            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }
}