package com.sourav.weatherfore.widget;


/**
 * Created by Sourav on 3/9/2018.
 */

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.sourav.weatherfore.sync.SyncUtils;

/**
 * Provider for a widget showing today's weather.
 */
public class TodayWidgetProvider extends AppWidgetProvider {


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        context.startService(new Intent(context, TodayWidgetIntentService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        context.startService(new Intent(context, TodayWidgetIntentService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (SyncUtils.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            context.startService(new Intent(context, TodayWidgetIntentService.class));
        }
    }
}
