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

import java.text.SimpleDateFormat;

public class BikeInfoActivity extends AppCompatActivity {

    private final static String TAG = "BikeInfoActvity";

    BluetoothGattCharacteristic characteristic;

    private TextView tvResetHeader;
    private Spinner spReset;
    private TextView tvResetLabel;
    private Button btReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_info);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        View view = findViewById(R.id.clBikeInfo);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(BikeInfoActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
        });

        showActionBar();

        TextView tvVIN = findViewById(R.id.tvVINValue);
        TextView tvNextServiceDate = findViewById(R.id.tvNextServiceDateValue);
        TextView tvNextService = findViewById(R.id.tvNextServiceValue);
        tvResetHeader = findViewById(R.id.tvResetHeader);
        tvResetLabel = findViewById(R.id.tvResetLabel);
        spReset = findViewById(R.id.spReset);
        btReset = findViewById(R.id.btReset);
        btReset.setOnClickListener(mClickListener);

        if (Data.getVin() != null){
            tvVIN.setText(Data.getVin());
        }

        if (Data.getNextServiceDate() != null){
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
            String dateString = format.format(Data.getNextServiceDate());
            tvNextServiceDate.setText(dateString);
        }

        if (Data.getNextService() != null){
            String distanceFormat = sharedPrefs.getString("prefDistance", "0");
            String nextService = Data.getNextService() + "(km)";
            if (distanceFormat.contains("1")) {
                nextService = Math.round(Utils.kmToMiles(Data.getNextService())) + "(mi)";
            }
            tvNextService.setText(nextService);
        }

        characteristic = MainActivity.gattCommandCharacteristic;
        if ((characteristic != null) & (FWConfig.firmwareVersion == null)) {
            // Get Version
            byte[] getVersionCmd = {0x57, 0x52, 0x56};
            characteristic.setValue(getVersionCmd);
            BluetoothLeService.writeCharacteristic(characteristic);
        }
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if (FWConfig.firmwareVersion == null) {
            Log.d(TAG, "Data.getFirmwareVersion() == null");
            if (characteristic != null) {
                // Get Version
                Log.d(TAG, "Get Version");
                byte[] getVersionCmd = {0x57, 0x52, 0x56};
                characteristic.setValue(getVersionCmd);
                BluetoothLeService.writeCharacteristic(characteristic);
            }
        } else {
            if (Double.parseDouble(FWConfig.firmwareVersion) >= 1.8) {
                Log.d(TAG, "Data.getFirmwareVersion() >= 1.8");
                tvResetHeader.setVisibility(View.VISIBLE);
                spReset.setVisibility(View.VISIBLE);
                tvResetLabel.setVisibility(View.VISIBLE);
                btReset.setVisibility(View.VISIBLE);
            }
        }
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
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
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
                            byte[] rstClusterSpeedCmd = {0x57, 0x57, 0x44, 0x52, 0x53};
                            characteristic.setValue(rstClusterSpeedCmd);
                            break;
                        case 1: // Reset Cluster Economy 1
                            byte[] rstClusterEcono1 = {0x57, 0x57, 0x44, 0x52, 0x45, 0x01};
                            characteristic.setValue(rstClusterEcono1);
                            break;
                        case 2: // Reset Cluster Economy 2
                            byte[] rstClusterEcono2 = {0x57, 0x57, 0x44, 0x52, 0x45, 0x02};
                            characteristic.setValue(rstClusterEcono2);
                            break;
                        case 3: // Reset Cluster Trip 1
                            byte[] rstClusterTrip1 = {0x57, 0x57, 0x44, 0x52, 0x54, 0x01};
                            characteristic.setValue(rstClusterTrip1);
                            break;
                        case 4: // Reset Cluster Trip 2
                            byte[] rstClusterTrip2 = {0x57, 0x57, 0x44, 0x52, 0x54, 0x02};
                            characteristic.setValue(rstClusterTrip2);
                            break;
                        default:
                            break;
                    }
                    BluetoothLeService.writeCharacteristic(characteristic);
                    break;
            }
        }
    };

    // Handles various events fired by the Service.
    // ACTION_WRITE_SUCCESS: received when write is successful
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Bundle bd = intent.getExtras();
                if(bd != null){
                    if(bd.getString(BluetoothLeService.EXTRA_BYTE_UUID_VALUE).contains(GattAttributes.WUNDERLINQ_COMMAND_CHARACTERISTIC)) {
                        byte[] data = bd.getByteArray(BluetoothLeService.EXTRA_BYTE_VALUE);
                        Log.d(TAG, "UUID: " + bd.getString(BluetoothLeService.EXTRA_BYTE_UUID_VALUE) + " DATA: " + Utils.ByteArraytoHex(data));
                        if ((data[0] == 0x57) && (data[1] == 0x52) && (data[2] == 0x56)){
                            if (Double.parseDouble(FWConfig.firmwareVersion) >=1.8) {
                                tvResetHeader.setVisibility(View.VISIBLE);
                                spReset.setVisibility(View.VISIBLE);
                                tvResetLabel.setVisibility(View.VISIBLE);
                                btReset.setVisibility(View.VISIBLE);
                            }
                        }

                    }
                }
            } else if(BluetoothLeService.ACTION_WRITE_SUCCESS.equals(action)){
                Log.d(TAG,"Write Success Received");
                BluetoothLeService.readCharacteristic(characteristic);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_SUCCESS);
        return intentFilter;
    }
}
