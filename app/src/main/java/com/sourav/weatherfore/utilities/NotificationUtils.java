package com.sourav.weatherfore.utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.sourav.weatherfore.MainActivity;
import com.sourav.weatherfore.R;
import com.sourav.weatherfore.db.WeatherContract;
import com.sourav.weatherfore.db.WeatherPreferences;

/**
 * Created by Sourav on 11/21/2017.
 */

public class NotificationUtils {
    /*
    * The columns of data that we are interested in displaying within our notification to let
    * the user know there is new weather data available.
    */
    private static final String[] WEATHER_NOTIFICATION_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
    };

    /*
    * We store the indices of the values in the array of Strings above to more quickly be able
    * to access the data from our query. If the order of the Strings above changes, these
    * indices must be adjusted to match the order of the Strings.
    */
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    /*
    * This notification ID can be used to access our notification after we've displayed it. This
    * can be handy when we need to cancel the notification, or perhaps update it. This number is
    * arbitrary and can be set to whatever you like. 3004 is in no way significant.
    */

    //  Create a constant int value to identify the notification
    private static final int WEATHER_NOTIFICATION_ID = 3004;
    private static final String WEATHER_NOTIFICATION_CHANNEL_ID = "weather_notification_channel";
    private static final int WEATHER_PENDING_INTENT_ID = 2001;

    /**
     * Constructs and displays a notification for the newly updated weather for today.
     *
     * @param context Context used to query our ContentProvider and use various Utility methods
     */
    public static void notifyUserOfNewWeather(Context context) {
          /* Build the URI for today's weather in order to show up to date data in notification */
        String locationSetting = WeatherPreferences.getPreferredWeatherLocation(context);
        Uri todaysWeatherUri = WeatherContract.WeatherEntry
                .buildWeatherUriWithDate(locationSetting,
                        WeatherDateUtils.normalizeDate(System.currentTimeMillis()));
        Cursor todayWeatherCursor = context.getContentResolver().query(
                todaysWeatherUri,
                WEATHER_NOTIFICATION_PROJECTION,
                null,
                null,
                null);


        /*
         * If todayWeatherCursor is empty, moveToFirst will return false. If our cursor is not
         * empty, we want to show the notification.
         */
        if (todayWeatherCursor != null && todayWeatherCursor.moveToFirst()) {

                /* Weather ID as returned by API, used to identify the icon to be used */
            int weatherId = todayWeatherCursor.getInt(INDEX_WEATHER_ID);
            double high = todayWeatherCursor.getDouble(INDEX_MAX_TEMP);
            double low = todayWeatherCursor.getDouble(INDEX_MIN_TEMP);

            Resources resources = context.getResources();
            int largeArtResourceId = WeatherUtils
                    .getLargeArtResourceIdForWeatherCondition(weatherId);

            Bitmap largeIcon = BitmapFactory.decodeResource(
                    resources,
                    largeArtResourceId);

            String notificationTitle = context.getString(R.string.app_name);

            String notificationText = getNotificationText(context, weatherId, high, low);

                /* getSmallArtResourceIdForWeatherCondition returns the proper art to show given an ID */
            int smallArtResourceId = WeatherUtils
                    .getSmallArtResourceIdForWeatherCondition(weatherId);

                 /*
                 * NotificationCompat Builder is a very convenient way to build backward-compatible
                 * notifications. In order to use it, we provide a context and specify a color for the
                 * notification, a couple of different icons, the title for the notification, and
                 * finally the text of the notification, which in our case in a summary of today's
                 * forecast.
                 */
            //          COMPLETED (2) Use NotificationCompat.Builder to begin building the notification

            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(WEATHER_NOTIFICATION_CHANNEL_ID,
                        context.getString(R.string.weather_notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH);
                if (mNotificationManager != null) {
                    mNotificationManager.createNotificationChannel(mChannel);
                }
            }

            // NotificationCompatBuilder is a very convenient way to build backward-compatible
            // notifications.  Just throw in some data.
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, WEATHER_NOTIFICATION_CHANNEL_ID)
                            .setColor(resources.getColor(R.color.accent))
                            .setSmallIcon(smallArtResourceId)
                            .setLargeIcon(largeIcon)
                            .setContentTitle(notificationTitle)
                            .setContentText(notificationText)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText))
                            .setAutoCancel(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
            }
            mBuilder.setContentIntent(contentIntent(context));

            // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
            if (mNotificationManager != null) {
                mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());
            }

            // Save the time at which the notification occurred using SunshinePreferences
                /*
                 * Since we just showed a notification, save the current time. That way, we can check
                 * next time the weather is refreshed if we should show another notification.
                 */
            WeatherPreferences.saveLastNotificationTime(context, System.currentTimeMillis());
        }
        if (todayWeatherCursor != null) {
            todayWeatherCursor.close();
        }
    }

    private static PendingIntent contentIntent(Context context){
        Intent startIntent = new Intent(context,MainActivity.class);

        return PendingIntent.getActivity(
                context,
                WEATHER_PENDING_INTENT_ID,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Constructs and returns the summary of a particular day's forecast using various utility
     * methods and resources for formatting. This method is only used to create the text for the
     * notification that appears when the weather is refreshed.
     * <p>
     * The String returned from this method will look something like this:
     * <p>
     * Forecast: Sunny - High: 14°C Low 7°C
     *
     * @param context   Used to access utility methods and resources
     * @param weatherId ID as determined by Open Weather Map
     * @param high      High temperature (either celsius or fahrenheit depending on preferences)
     * @param low       Low temperature (either celsius or fahrenheit depending on preferences)
     * @return Summary of a particular day's forecast
     */
    private static String getNotificationText(Context context, int weatherId, double high, double low) {
         /*
         * Short description of the weather, as provided by the API.
         * e.g "clear" vs "sky is clear".
         */
        String shortDescription = WeatherUtils.getStringForWeatherCondition(
                context,weatherId);
        String notificationFormat = context.getString(R.string.format_notification);

        /* Using String's format method, we create the forecast summary */

        return String.format(notificationFormat,
                shortDescription,
                WeatherUtils.formatTemperature(context,high),
                WeatherUtils.formatTemperature(context,low));
    }
}


