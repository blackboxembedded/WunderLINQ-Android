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
package com.blackboxembedded.WunderLINQ;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_BASE;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_N;

import java.time.format.DateTimeFormatter;

public class BikeInfoActivity extends AppCompatActivity {

    private final static String TAG = "BikeInfoActivity";

    BluetoothGattCharacteristic characteristic;

    private TextView tvVIN;
    private TextView tvNextServiceDate;
    private TextView tvNextService;
    private TextView tvResetHeader;
    private Spinner spReset;
    private TextView tvResetLabel;
    private Button btReset;

    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_info);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        View view = findViewById(R.id.clBikeInfo);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(BikeInfoActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
        });

        showActionBar();

        tvVIN = findViewById(R.id.tvVINValue);
        tvNextServiceDate = findViewById(R.id.tvNextServiceDateValue);
        tvNextService = findViewById(R.id.tvNextServiceValue);
        tvResetHeader = findViewById(R.id.tvResetHeader);
        tvResetLabel = findViewById(R.id.tvResetLabel);
        spReset = findViewById(R.id.spReset);
        btReset = findViewById(R.id.btReset);
        btReset.setOnClickListener(mClickListener);

        characteristic = BluetoothLeService.gattCommandCharacteristic;
        if (MotorcycleData.wlq != null) {
            if ((characteristic != null) & (MotorcycleData.wlq.getFirmwareVersion() == null)) {
                // Get Config
                BluetoothLeService.writeCharacteristic(characteristic, WLQ_N.GET_CONFIG_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
            }
        }
        updateDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();

        ContextCompat.registerReceiver(this, mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);

        if (MotorcycleData.wlq != null) {
            if (MotorcycleData.wlq.getFirmwareVersion() == null) {
                if (characteristic != null) {
                    // Get Config
                    BluetoothLeService.writeCharacteristic(characteristic, WLQ_N.GET_CONFIG_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
                }
            }
        }
        updateDisplay();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"In onDestroy");
        super.onDestroy();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void showActionBar(){
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.bike_info_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(BikeInfoActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.btReset:
                    switch(spReset.getSelectedItemPosition()){
                        case 0: // Reset Cluster Average Speed
                            BluetoothLeService.writeCharacteristic(characteristic, WLQ_BASE.RESET_CLUSTER_SPEED_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
                            break;
                        case 1: // Reset Cluster Economy 1
                            BluetoothLeService.writeCharacteristic(characteristic, WLQ_BASE.RESET_CLUSTER_ECONO1_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
                            break;
                        case 2: // Reset Cluster Economy 2
                            BluetoothLeService.writeCharacteristic(characteristic, WLQ_BASE.RESET_CLUSTER_ECONO2_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
                            break;
                        case 3: // Reset Cluster Trip 1
                            BluetoothLeService.writeCharacteristic(characteristic, WLQ_BASE.RESET_CLUSTER_TRIP1_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
                            break;
                        case 4: // Reset Cluster Trip 2
                            BluetoothLeService.writeCharacteristic(characteristic, WLQ_BASE.RESET_CLUSTER_TRIP2_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }
    };

    private void updateDisplay(){
        if (MotorcycleData.wlq != null) {
            if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_N || MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_X) {
                tvResetHeader.setVisibility(View.VISIBLE);
                spReset.setVisibility(View.VISIBLE);
                tvResetLabel.setVisibility(View.VISIBLE);
                btReset.setVisibility(View.VISIBLE);
            }
        }

        if (MotorcycleData.getVin() != null){
            tvVIN.setText(MotorcycleData.getVin());
        }

        if (MotorcycleData.getNextServiceDate() != null){
            // Creating a DateTimeFormatter object with desired format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            // Formatting the date using the formatter
            String formattedDate = MotorcycleData.getNextServiceDate().format(formatter);
            tvNextServiceDate.setText(formattedDate);
        }

        if (MotorcycleData.getNextService() != null){
            String distanceFormat = sharedPrefs.getString("prefDistance", "0");
            String nextService = MotorcycleData.getNextService() + "(km)";
            if (distanceFormat.contains("1")) {
                nextService = Utils.toZeroDecimalString(Utils.kmToMiles(MotorcycleData.getNextService())) + "(mi)";
            }
            tvNextService.setText(nextService);
        }
    }

    // Handles various events fired by the Service.
    // ACTION_WRITE_SUCCESS: received when write is successful
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_CMDSTATUS_AVAILABLE.equals(action)) {
                Bundle bd = intent.getExtras();
                if(bd != null){
                    //if(bd.getString(BluetoothLeService.EXTRA_BYTE_UUID_VALUE).contains(GattAttributes.WUNDERLINQ_N_COMMAND_CHARACTERISTIC)) {
                        updateDisplay();
                    //}
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_CMDSTATUS_AVAILABLE);
        return intentFilter;
    }
}
