/*
WunderLINQ Client Application
Copyright (C) 2020  Keith Conger, Black Box Embedded, LLC

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.blackboxembedded.WunderLINQ.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.blackboxembedded.WunderLINQ.MainActivity;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;

import java.util.ArrayList;
import java.util.List;

public class WidgetProvider extends AppWidgetProvider {
    public final static String TAG = "WidgetProvider";
    private static SharedPreferences sharedPrefs;
    private static BroadcastReceiver customReceiver;
    public static List<String> labels = new ArrayList<>();
    public static List<String> data = new ArrayList<>();
    public static List<String> newData = new ArrayList<>();
    public static List<Drawable> icons = new ArrayList<>();

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        int cell1Data = Integer.parseInt(sharedPrefs.getString("prefCellOne", "14"));//Default:Speed
        int cell2Data = Integer.parseInt(sharedPrefs.getString("prefCellTwo", "29"));//Default:RPM
        int cell3Data = Integer.parseInt(sharedPrefs.getString("prefCellThree", "3"));//Default:Speed
        int cell4Data = Integer.parseInt(sharedPrefs.getString("prefCellFour", "0"));//Default:Gear
        labels.add(0,Data.getLabel(cell1Data));
        labels.add(1,Data.getLabel(cell2Data));
        labels.add(2,Data.getLabel(cell3Data));
        labels.add(3,Data.getLabel(cell4Data));
        icons.add(0,Data.getIcon(cell1Data));
        icons.add(1,Data.getIcon(cell2Data));
        icons.add(2,Data.getIcon(cell3Data));
        icons.add(3,Data.getIcon(cell4Data));
        data.add(0,"");
        data.add(1,"");
        data.add(2,"");
        data.add(3,"");
        newData.add(0,"");
        newData.add(1,"");
        newData.add(2,"");
        newData.add(3,"");

        // Register your broadcast receiver
        if (customReceiver == null) {
            customReceiver = new DataReceiver();
            IntentFilter filter = new IntentFilter(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE);
            context.getApplicationContext().registerReceiver(customReceiver, filter);
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        // Unregister your broadcast receiver
        if (customReceiver != null) {
            context.getApplicationContext().unregisterReceiver(customReceiver);
            customReceiver = null;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            // Get the layout for the app widget
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            // Create an Intent to launch your main activity (or any other activity)
            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE);

            // Attach the onClick listener to the whole widget layout (or any specific view in your widget)
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent);

            Intent intent = new Intent(context, GridWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            views.setRemoteAdapter(R.id.widget_grid, intent);

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);

        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        int widgetWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int widgetHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

        // Width and height are in dp units. To convert them to pixels, you can use the following:
        Resources res = context.getResources();
        float widthInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widgetWidth, res.getDisplayMetrics());
        float heightInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widgetHeight, res.getDisplayMetrics());
        Log.d(TAG, "Widget Size - HxW: " + widgetHeight + "x" + widgetWidth + " - Pixels: " + widthInPixels + "x" + heightInPixels);

        // Based on width and height, adjust the number of cells, their content, or any other attributes.
        // You might decide to modify the number of items in your adapter, the size of text, etc.

        // Then update the widget
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        // Configure your RemoteViews (for example, set up the GridView with your adapter)

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static class DataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle the received broadcast
            if (intent.getAction().equals(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE)) {
                int cell1Data = Integer.parseInt(sharedPrefs.getString("prefCellOne", "14"));//Default:Speed
                int cell2Data = Integer.parseInt(sharedPrefs.getString("prefCellTwo", "29"));//Default:RPM
                int cell3Data = Integer.parseInt(sharedPrefs.getString("prefCellThree", "3"));//Default:Speed
                int cell4Data = Integer.parseInt(sharedPrefs.getString("prefCellFour", "0"));//Default:Gear
                labels.set(0,Data.getLabel(cell1Data));
                labels.set(1,Data.getLabel(cell2Data));
                labels.set(2,Data.getLabel(cell3Data));
                labels.set(3,Data.getLabel(cell4Data));
                icons.set(0,Data.getIcon(cell1Data));
                icons.set(1,Data.getIcon(cell2Data));
                icons.set(2,Data.getIcon(cell3Data));
                icons.set(3,Data.getIcon(cell4Data));

                Bundle extras = intent.getExtras();
                newData.set(0,getDataExtra(extras, Data.getExtraKey(cell1Data)));
                newData.set(1,getDataExtra(extras, Data.getExtraKey(cell2Data)));
                newData.set(2,getDataExtra(extras, Data.getExtraKey(cell3Data)));
                newData.set(3,getDataExtra(extras, Data.getExtraKey(cell4Data)));

                if (!data.equals(newData)){
                    // Display the received data
                    Log.d(TAG,"Update");
                    data.set(0,getDataExtra(extras, Data.getExtraKey(cell1Data)));
                    data.set(1,getDataExtra(extras, Data.getExtraKey(cell2Data)));
                    data.set(2,getDataExtra(extras, Data.getExtraKey(cell3Data)));
                    data.set(3,getDataExtra(extras, Data.getExtraKey(cell4Data)));

                    // Notify the widget's GridView that the data has changed
                    // Get the AppWidgetManager instance
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

                    // Update the widget
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
                    for (int appWidgetId : appWidgetIds) {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid);
                    }
                }

            }
        }
    }

    public static String getDataExtra(Bundle extras, String key){
        Object objValue = extras.get(key);
        if (objValue != null) {
            return objValue.toString();
        }
        return "";
    }
}

