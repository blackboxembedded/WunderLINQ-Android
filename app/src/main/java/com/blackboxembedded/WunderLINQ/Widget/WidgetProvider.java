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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.blackboxembedded.WunderLINQ.MainActivity;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;

import java.util.ArrayList;
import java.util.List;

public class WidgetProvider extends AppWidgetProvider {
    public final static String TAG = "WidgetProvider";
    private static SharedPreferences sharedPrefs;
    private static BroadcastReceiver customReceiver;
    private static Boolean hasFocus = false;
    public static List<String> labels = new ArrayList<>();
    public static List<String> data = new ArrayList<>();
    public static List<String> newData = new ArrayList<>();
    public static List<Drawable> icons = new ArrayList<>();

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
            // Get the layout for the app widget
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            if (sharedPrefs.getBoolean("prefFocusIndication", false)) {
                int color = ContextCompat.getColor(context, R.color.colorPrimary);
                if (MotorcycleData.getHasFocus()) {
                    color = androidx.preference.PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).getInt("prefHighlightColor", R.color.colorAccent);
                }
                views.setInt(R.id.widget_layout, "setBackgroundColor", color);
            }

            Intent intent = new Intent(context, GridWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))); // force unique intent
            views.setRemoteAdapter(R.id.widget_grid, intent);

            // Create an Intent to launch your main activity (or any other activity)
            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setPendingIntentTemplate(R.id.widget_grid, pendingIntent);
            // Attach the onClick listener to the whole widget layout (or any specific view in your widget)
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

            // Update the widget
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
                int cell5Data = Integer.parseInt(sharedPrefs.getString("prefCellFive", "1"));//Default:
                int cell6Data = Integer.parseInt(sharedPrefs.getString("prefCellSix", "2"));//Default:
                int cell7Data = Integer.parseInt(sharedPrefs.getString("prefCellSeven", "20"));//Default:
                int cell8Data = Integer.parseInt(sharedPrefs.getString("prefCellEight", "8"));//Default:

                Bundle extras = intent.getExtras();
                newData.set(0,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell1Data))));
                newData.set(1,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell2Data))));
                newData.set(2,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell3Data))));
                newData.set(3,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell4Data))));
                newData.set(4,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell5Data))));
                newData.set(5,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell6Data))));
                newData.set(6,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell7Data))));
                newData.set(7,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell8Data))));

                if (!data.equals(newData) || hasFocus != MotorcycleData.getHasFocus()){
                    // Display the received data
                    hasFocus = MotorcycleData.getHasFocus();
                    data.set(0,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell1Data))));
                    data.set(1,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell2Data))));
                    data.set(2,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell3Data))));
                    data.set(3,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell4Data))));
                    data.set(4,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell5Data))));
                    data.set(5,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell6Data))));
                    data.set(6,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell7Data))));
                    data.set(7,getDataExtra(extras, MotorcycleData.getExtraKey(MotorcycleData.DataType.fromValue(cell8Data))));

                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

                    // Notify the widget's GridView that the data has changed
                    // Get the AppWidgetManager instance
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

                    // Update the widget
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
                    for (int appWidgetId : appWidgetIds) {
                        Intent svcIntent = new Intent(context, GridWidgetService.class);
                        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME))); // Important

                        if (sharedPrefs.getBoolean("prefFocusIndication", false)) {
                            int color = ContextCompat.getColor(context, R.color.colorPrimary);
                            if (MotorcycleData.getHasFocus()) {
                                color = androidx.preference.PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).getInt("prefHighlightColor", R.color.colorAccent);
                            }
                            views.setInt(R.id.widget_layout, "setBackgroundColor", color);
                        }

                        views.setRemoteAdapter(R.id.widget_grid, svcIntent);

                        // Create an Intent to launch your main activity (or any other activity)
                        Intent launchIntent = new Intent(context, MainActivity.class);
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setPendingIntentTemplate(R.id.widget_grid, pendingIntent);
                        // Attach the onClick listener to the whole widget layout (or any specific view in your widget)
                        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

                        appWidgetManager.updateAppWidget(appWidgetId, views);
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid);
                    }
                }

            } else if (intent.getAction().equals(BluetoothLeService.ACTION_FOCUS_CHANGED)) {
                Log.d(TAG,"DataReceiver: onReceive - Focus Changed");
                hasFocus = MotorcycleData.getHasFocus();
                if (sharedPrefs.getBoolean("prefFocusIndication", false)) {
                    int color = ContextCompat.getColor(context, R.color.colorPrimary);
                    if (MotorcycleData.getHasFocus()) {
                        color = androidx.preference.PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).getInt("prefHighlightColor", R.color.colorAccent);
                    }

                    // Get the layout for the app widget
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                    views.setInt(R.id.widget_layout, "setBackgroundColor", color);

                    // Notify the widget's GridView that the data has changed
                    // Get the AppWidgetManager instance
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

                    // Update the widget
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
                    for (int appWidgetId : appWidgetIds) {
                        Intent svcIntent = new Intent(context, GridWidgetService.class);
                        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME))); // Important

                        views.setRemoteAdapter(R.id.widget_grid, svcIntent); // rebind adapter

                        // Create an Intent to launch your main activity (or any other activity)
                        Intent launchIntent = new Intent(context, MainActivity.class);
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        views.setPendingIntentTemplate(R.id.widget_grid, pendingIntent);
                        // Attach the onClick listener to the whole widget layout (or any specific view in your widget)
                        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

                        appWidgetManager.updateAppWidget(appWidgetId, views);
                    }

                }
            }
        }
    }

    private void updateCellData(){
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        icons.clear();
        labels.clear();
        data.clear();
        newData.clear();
        int cell1Data = Integer.parseInt(sharedPrefs.getString("prefCellOne", "14"));//Default:Speed
        int cell2Data = Integer.parseInt(sharedPrefs.getString("prefCellTwo", "29"));//Default:RPM
        int cell3Data = Integer.parseInt(sharedPrefs.getString("prefCellThree", "3"));//Default:Speed
        int cell4Data = Integer.parseInt(sharedPrefs.getString("prefCellFour", "0"));//Default:Gear
        int cell5Data = Integer.parseInt(sharedPrefs.getString("prefCellFive", "1"));//Default:
        int cell6Data = Integer.parseInt(sharedPrefs.getString("prefCellSix", "2"));//Default:
        int cell7Data = Integer.parseInt(sharedPrefs.getString("prefCellSeven", "20"));//Default:
        int cell8Data = Integer.parseInt(sharedPrefs.getString("prefCellEight", "8"));//Default:
        Object[] cell1Obj =  MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cell1Data]);
        String cell1DataVal = (String) cell1Obj[0];
        String cell1Label = (String) cell1Obj[1];
        Drawable cell1Icon = (Drawable) cell1Obj[2];
        icons.add(0,cell1Icon);
        labels.add(0,cell1Label);
        data.add(0,cell1DataVal);
        newData.add(0,cell1DataVal);
        Object[] cell2Obj =  MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cell2Data]);
        String cell2DataVal = (String) cell2Obj[0];
        String cell2Label = (String) cell2Obj[1];
        Drawable cell2Icon = (Drawable) cell2Obj[2];
        icons.add(1,cell2Icon);
        labels.add(1,cell2Label);
        data.add(1,cell2DataVal);
        newData.add(1,cell2DataVal);
        Object[] cell3Obj =  MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cell3Data]);
        String cell3DataVal = (String) cell3Obj[0];
        String cell3Label = (String) cell3Obj[1];
        Drawable cell3Icon = (Drawable) cell3Obj[2];
        icons.add(2,cell3Icon);
        labels.add(2,cell3Label);
        data.add(2,cell3DataVal);
        newData.add(2,cell3DataVal);
        Object[] cell4Obj =  MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cell4Data]);
        String cell4DataVal = (String) cell4Obj[0];
        String cell4Label = (String) cell4Obj[1];
        Drawable cell4Icon = (Drawable) cell4Obj[2];
        icons.add(3,cell4Icon);
        labels.add(3,cell4Label);
        data.add(3,cell4DataVal);
        newData.add(3,cell4DataVal);
        Object[] cell5Obj =  MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cell5Data]);
        String cell5DataVal = (String) cell5Obj[0];
        String cell5Label = (String) cell5Obj[1];
        Drawable cell5Icon = (Drawable) cell5Obj[2];
        icons.add(4,cell5Icon);
        labels.add(4,cell5Label);
        data.add(4,cell5DataVal);
        newData.add(4,cell5DataVal);
        Object[] cell6Obj =  MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cell6Data]);
        String cell6DataVal = (String) cell6Obj[0];
        String cell6Label = (String) cell6Obj[1];
        Drawable cell6Icon = (Drawable) cell6Obj[2];
        icons.add(5,cell6Icon);
        labels.add(5,cell6Label);
        data.add(5,cell6DataVal);
        newData.add(5,cell6DataVal);
        Object[] cell7Obj =  MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cell7Data]);
        String cell7DataVal = (String) cell7Obj[0];
        String cell7Label = (String) cell7Obj[1];
        Drawable cell7Icon = (Drawable) cell7Obj[2];
        icons.add(6,cell7Icon);
        labels.add(6,cell7Label);
        data.add(6,cell7DataVal);
        newData.add(6,cell7DataVal);
        Object[] cell8Obj =  MotorcycleData.getCombinedData(MotorcycleData.DataType.values()[cell8Data]);
        String cell8DataVal = (String) cell8Obj[0];
        String cell8Label = (String) cell8Obj[1];
        Drawable cell8Icon = (Drawable) cell8Obj[2];
        icons.add(7,cell8Icon);
        labels.add(7,cell8Label);
        data.add(7,cell8DataVal);
        newData.add(7,cell8DataVal);
    }

    public static String getDataExtra(Bundle extras, String key){
        Object objValue = extras.get(key);
        if (objValue != null) {
            return objValue.toString();
        }
        return "";
    }
}

