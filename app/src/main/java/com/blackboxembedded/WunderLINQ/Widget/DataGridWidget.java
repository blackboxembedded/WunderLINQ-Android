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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.blackboxembedded.WunderLINQ.MainActivity;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;

import java.util.ArrayList;
import java.util.List;

public class DataGridWidget extends AppWidgetProvider {
    public final static String TAG = "DataGridWidget";
    private static SharedPreferences sharedPrefs;
    private static BroadcastReceiver customReceiver;
    public static List<String> labels = new ArrayList<>(8);
    public static List<String> data = new ArrayList<>(8);
    public static List<Drawable> icons = new ArrayList<>(8);

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "onEnabled");

        updateCellData();

        // Register your broadcast receiver
        if (customReceiver == null) {
            customReceiver = new DataReceiver();
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE);
            intentFilter.addAction(BluetoothLeService.ACTION_FOCUS_CHANGED);
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
            ContextCompat.registerReceiver(context.getApplicationContext(), customReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
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
        Log.d(TAG, "onUpdate");

        updateCellData();

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_datagrid);

            if (sharedPrefs.getBoolean("prefFocusIndication", false)) {
                int color = ContextCompat.getColor(context, R.color.colorPrimary);
                if (MotorcycleData.getHasFocus()) {
                    color = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).getInt("prefHighlightColor", R.color.colorAccent);
                }
                views.setInt(R.id.widget_layout, "setBackgroundColor", color);
            }

            for (int i = 0; i < 8; i++) {
                String labelIdName = "cell_" + i + "_label";
                String valueIdName = "cell_" + i + "_value";
                String iconIdName = "cell_" + i + "_icon";

                int labelId = context.getResources().getIdentifier(labelIdName, "id", context.getPackageName());
                int valueId = context.getResources().getIdentifier(valueIdName, "id", context.getPackageName());
                int iconId = context.getResources().getIdentifier(iconIdName, "id", context.getPackageName());

                boolean isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                if (isPortrait) {
                    if (labels.get(i).length() > 20) {
                        views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, 8f);
                    } else if (labels.get(i).length() > 12) {
                        views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, 10f);
                    } else {
                        views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, 12f);
                    }
                } else {
                    views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, 11f);
                }
                if (isPortrait) {
                    if (data.get(i).length() > 4) {
                        views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 28f);
                    } else if (data.get(i).length() > 2) {
                        views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 30f);
                    } else {
                        views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 34f);
                    }
                } else {
                    if (data.get(i).length() > 6) {
                        views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 16f);
                    } else if (data.get(i).length() > 4) {
                        views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 18f);
                    } else {
                        views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 20f);
                    }
                }

                views.setTextViewText(labelId, labels.get(i));
                views.setTextViewText(valueId, data.get(i));
                views.setImageViewBitmap(iconId, Utils.drawableToBitmap(icons.get(i), 100));
            }

            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        // Register your broadcast receiver
        if (customReceiver == null) {
            customReceiver = new DataReceiver();
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE);
            intentFilter.addAction(BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE);
            intentFilter.addAction(BluetoothLeService.ACTION_FOCUS_CHANGED);
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
            ContextCompat.registerReceiver(context.getApplicationContext(), customReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "onReceive");
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Log.d(TAG, "onAppWidgetOptionsChanged");
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
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_datagrid);
        // Configure your RemoteViews (for example, set up the GridView with your adapter)

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static class DataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle the received broadcast
            if (intent.getAction().equals(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE)) {

                updateCellData();

                // Notify the widget's GridView that the data has changed
                // Get the AppWidgetManager instance
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                // Update the widget
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, DataGridWidget.class));
                for (int appWidgetId : appWidgetIds) {
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_datagrid);

                    if (sharedPrefs.getBoolean("prefFocusIndication", false)) {
                        int color = ContextCompat.getColor(context, R.color.colorPrimary);
                        if (MotorcycleData.getHasFocus()) {
                            color = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).getInt("prefHighlightColor", R.color.colorAccent);
                        }
                        views.setInt(R.id.widget_layout, "setBackgroundColor", color);
                    }

                    for (int i = 0; i < 8; i++) {
                        String labelIdName = "cell_" + i + "_label";
                        String valueIdName = "cell_" + i + "_value";
                        String iconIdName = "cell_" + i + "_icon";

                        int labelId = context.getResources().getIdentifier(labelIdName, "id", context.getPackageName());
                        int valueId = context.getResources().getIdentifier(valueIdName, "id", context.getPackageName());
                        int iconId = context.getResources().getIdentifier(iconIdName, "id", context.getPackageName());

                        boolean isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                        if (isPortrait) {
                            if (labels.get(i).length() > 20) {
                                views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, 8f);
                            } else if (labels.get(i).length() > 12) {
                                views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, 10f);
                            } else {
                                views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, 12f);
                            }
                        } else {
                            views.setTextViewTextSize(labelId, TypedValue.COMPLEX_UNIT_SP, 11f);
                        }
                        if (isPortrait) {
                            if (data.get(i).length() > 4) {
                                views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 28f);
                            } else if (data.get(i).length() > 2) {
                                views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 30f);
                            } else {
                                views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 34f);
                            }
                        } else {
                            if (data.get(i).length() > 6) {
                                views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 16f);
                            } else if (data.get(i).length() > 4) {
                                views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 18f);
                            } else {
                                views.setTextViewTextSize(valueId, TypedValue.COMPLEX_UNIT_SP, 20f);
                            }
                        }
                        views.setTextViewText(labelId, labels.get(i));
                        views.setTextViewText(valueId, data.get(i));
                        views.setImageViewBitmap(iconId, Utils.drawableToBitmap(icons.get(i), 100));
                    }

                    Intent launchIntent = new Intent(context, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            } else if (intent.getAction().equals(BluetoothLeService.ACTION_FOCUS_CHANGED)) {
                if (sharedPrefs.getBoolean("prefFocusIndication", false)) {
                    int color = ContextCompat.getColor(context, R.color.colorPrimary);
                    if (MotorcycleData.getHasFocus()) {
                        color = androidx.preference.PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).getInt("prefHighlightColor", R.color.colorAccent);
                    }

                    // Get the layout for the app widget
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_datagrid);
                    views.setInt(R.id.widget_layout, "setBackgroundColor", color);

                    // Notify the widget's GridView that the data has changed
                    // Get the AppWidgetManager instance
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    // Update the widget
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, DataGridWidget.class));
                    for (int appWidgetId : appWidgetIds) {
                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }
                }
            }
        }
    }

    private static void updateCellData(){
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        icons.clear();
        labels.clear();
        data.clear();
        for (int i = 0; i < 8; i++) {
            int cellDataIndex = Integer.parseInt(sharedPrefs.getString("prefCell" + (i + 1), String.valueOf(MotorcycleData.defaultCellData[i])));
            Object[] cellObj = MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cellDataIndex]);
            String dataVal = (String) cellObj[0];
            String label = (String) cellObj[1];
            Drawable icon = (Drawable) cellObj[2];
            icons.add(icon);
            labels.add(label);
            data.add(dataVal.replaceAll("[\\r\\n]+", ""));
        }
    }
}

