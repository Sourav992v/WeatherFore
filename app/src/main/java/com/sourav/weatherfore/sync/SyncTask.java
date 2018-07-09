package com.sourav.weatherfore.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;

import com.sourav.weatherfore.db.WeatherContract;
import com.sourav.weatherfore.db.WeatherPreferences;
import com.sourav.weatherfore.utilities.NetworkUtils;
import com.sourav.weatherfore.utilities.NotificationUtils;
import com.sourav.weatherfore.utilities.OpenWeatherJsonUtils;
import com.sourav.weatherfore.utilities.WeatherUtils;

import java.net.URL;

/**
 * Created by Sourav on 11/20/2017.
 */

class SyncTask {

    /**
     * Performs the network request for updated weather, parses the JSON from that request, and
     * inserts the new weather information into our ContentProvider. Will notify the user that new
     * weather has been loaded if the user hasn't been notified of the weather within the last day
     * AND they haven't disabled notifications in the preferences screen.
     *
     * @param context Used to access utility methods and the ContentResolver
     */
    synchronized static void syncWeather(Context context){

        try{
            /*
             * The getUrl method will return the URL that we need to get the forecast JSON for the
             * weather. It will decide whether to create a URL based off of the latitude and
             * longitude or off of a simple location as a String.
             */
            URL weatherRequestUrl = NetworkUtils.getUrl(context);

            /* Use the URL to retrieve the JSON */
            String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);


            /* Parse the JSON into a list of weather values */
            ContentValues[] weatherValues = OpenWeatherJsonUtils
                    .getWeatherContentValuesFromJson(context,jsonWeatherResponse);

             /*
             * In cases where our JSON contained an error code, getWeatherContentValuesFromJson
             * would have returned null. We need to check for those cases here to prevent any
             * NullPointerExceptions being thrown. We also have no reason to insert fresh data if
             * there isn't any to insert.
             */
             if (weatherValues != null && weatherValues.length != 0) {
                 /* Get a handle on the ContentResolver to delete and insert data */
                 ContentResolver weatherForeContentResolver = context.getContentResolver();
                 // If we have valid results, delete the old data and insert the new
                /* Delete old weather data because we don't need to keep multiple days' data */

                 weatherForeContentResolver.delete(
                         WeatherContract.WeatherEntry.CONTENT_URI,
                         null,
                         null);


                 weatherForeContentResolver.bulkInsert(
                         WeatherContract.WeatherEntry.CONTENT_URI,
                         weatherValues);


                 WeatherUtils.updateWidgets(context);
                 WeatherUtils.updateMuzei(context);
                 // Check if notifications are enabled
                /*
                 * Finally, after we insert data into the ContentProvider, determine whether or not
                 * we should notify the user that the weather has been refreshed.
                 */

                 boolean notificationsEnabled = WeatherPreferences.areNotificationsEnabled(context);

                  /*
                 * If the last notification was shown was more than 1 day ago, we want to send
                 * another notification to the user that the weather has been updated. Remember,
                 * it's important that you shouldn't spam your users with notifications.
                 */
                 long timeSinceLastNotification = WeatherPreferences.
                         getEllapsedTimeSinceLastNotification(context);

                 boolean oneDayPassedSinceLastNotification = false;

                 // Check if a day has passed since the last notification
                 if (timeSinceLastNotification >= DateUtils.DAY_IN_MILLIS) {
                     oneDayPassedSinceLastNotification = true;
                 }

                  /*
                 * We only want to show the notification if the user wants them shown and we
                 * haven't shown a notification in the past day.
                 */
                 // If more than a day have passed and notifications are enabled, notify the user
                 if (notificationsEnabled && oneDayPassedSinceLastNotification) {
                     NotificationUtils.notifyUserOfNewWeather(context);
                 }
             }else {
                 SyncUtils.setLocationStatus(context,SyncUtils.LOCATION_STATUS_INVALID);
             }
        }catch (Exception e){
             /* Server probably invalid */

            e.printStackTrace();
        }
    }
}
