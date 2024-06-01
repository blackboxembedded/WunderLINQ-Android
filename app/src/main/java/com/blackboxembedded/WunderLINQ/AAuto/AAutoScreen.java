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
package com.blackboxembedded.WunderLINQ.AAuto;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Template;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;

public class AAutoScreen extends Screen {

    public final static String TAG = "AAutoScreen";
    private GridTemplate gridTemplate;
    ItemList.Builder listBuilder;
    private final SharedPreferences sharedPrefs;

    public AAutoScreen(CarContext carContext) {
        super(carContext);
        Log.d(TAG,"AAutoScreen Create");
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        // Initialize your BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE);
        ContextCompat.registerReceiver(getCarContext(), bearingReceiver, filter, ContextCompat.RECEIVER_EXPORTED);

        gridTemplate = new GridTemplate.Builder()
                .setTitle(MyApplication.getContext().getString(R.string.app_name))
                .setHeaderAction(Action.APP_ICON)
                .setLoading(true)
                .build();
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return gridTemplate;
    }

    // BroadcastReceiver to handle the incoming intents
    private final BroadcastReceiver bearingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle the intent and update the UI
            if (intent.getAction() != null && intent.getAction().equals(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE)) {
                updateUI();
            }
        }
    };

    // Method to update the UI components
    private void updateUI() {
        listBuilder = new ItemList.Builder();
        // Cell One
        int cell1Data = Integer.parseInt(sharedPrefs.getString("prefCellOne", "14"));//Default:Speed
        listBuilder.addItem(getCellData(cell1Data));
        // Cell Two
        int cell2Data = Integer.parseInt(sharedPrefs.getString("prefCellTwo", "29"));//Default:RPM
        listBuilder.addItem(getCellData(cell2Data));
        // Cell Three
        int cell3Data = Integer.parseInt(sharedPrefs.getString("prefCellThree", "3"));//Default:Speed
        listBuilder.addItem(getCellData(cell3Data));
        // Cell Four
        int cell4Data = Integer.parseInt(sharedPrefs.getString("prefCellFour", "0"));//Default:Gear
        listBuilder.addItem(getCellData(cell4Data));
        // Cell Five
        int cell5Data = Integer.parseInt(sharedPrefs.getString("prefCellFive", "1"));//Default:Engine Temp
        listBuilder.addItem(getCellData(cell5Data));
        // Cell Six
        int cell6Data = Integer.parseInt(sharedPrefs.getString("prefCellSix", "2"));//Default:Air Temp
        listBuilder.addItem(getCellData(cell6Data));
        // Cell Seven
        int cell7Data = Integer.parseInt(sharedPrefs.getString("prefCellSeven", "20"));//Default:Shifts
        listBuilder.addItem(getCellData(cell7Data));
        // Cell Eight
        int cell8Data = Integer.parseInt(sharedPrefs.getString("prefCellEight", "8"));//Default:Front Brakes
        listBuilder.addItem(getCellData(cell8Data));
        // Cell Nine
        int cell9Data = Integer.parseInt(sharedPrefs.getString("prefCellNine", "9"));//Default:Rear Brakes
        listBuilder.addItem(getCellData(cell9Data));
        // Cell Ten
        int cell10Data = Integer.parseInt(sharedPrefs.getString("prefCellTen", "7"));//Default:Throttle
        listBuilder.addItem(getCellData(cell10Data));
        // Cell Eleven
        int cell11Data = Integer.parseInt(sharedPrefs.getString("prefCellEleven", "24"));//Default:time
        listBuilder.addItem(getCellData(cell11Data));
        // Cell Twelve
        int cell12Data = Integer.parseInt(sharedPrefs.getString("prefCellTwelve", "28"));//Default:Sunrise/Sunset
        listBuilder.addItem(getCellData(cell12Data));
        // Cell Thirteen
        int cell13Data = Integer.parseInt(sharedPrefs.getString("prefCellThirteen", "27"));//Default:Altitude
        listBuilder.addItem(getCellData(cell13Data));
        // Cell Fourteen
        int cell14Data = Integer.parseInt(sharedPrefs.getString("prefCellFourteen", "23"));//Default:Bearing
        listBuilder.addItem(getCellData(cell14Data));
        // Cell Fifteen
        int cell15Data = Integer.parseInt(sharedPrefs.getString("prefCellFifteen", "22"));//Default:g-force
        listBuilder.addItem(getCellData(cell15Data));

        gridTemplate = new GridTemplate.Builder()
                .setTitle(MyApplication.getContext().getString(R.string.app_name))
                .setHeaderAction(Action.APP_ICON)
                .setSingleList(listBuilder.build())
                .build();
        invalidate();  // Request the system to call onGetTemplate again
    }

    public GridItem getCellData(int dataPoint){
        return new GridItem.Builder()
                .setImage(Data.getCarIcon(dataPoint))
                .setTitle(Data.getLabel(dataPoint))
                .setText((!Data.getValue(dataPoint).isEmpty()) ? Data.getValue(dataPoint) : MyApplication.getContext().getString(R.string.blank_field))
                .build();
    }
}
