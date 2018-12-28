package com.blackboxembedded.WunderLINQ;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

public class FWConfigActivity extends AppCompatActivity {

    private final static String TAG = "FWConfigActvity";

    BluetoothGattCharacteristic characteristic;

    private ActionBar actionBar;
    private ImageButton backButton;
    private TextView navbarTitle;
    private Spinner wwModeSpinner;
    private LinearLayout sensitivityLLayout;
    private TextView sensitivityTV;
    private SeekBar sensitivitySeekBar;
    private Button writeBtn;

    private byte currentConfig;
    private byte currentSensitivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fwconfig);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        wwModeSpinner = (Spinner) findViewById(R.id.wwModeSpinner);
        wwModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                Log.d(TAG, "Item Selected: " + adapterView.getItemAtPosition(pos).toString());
                if ((currentConfig == 0x32) && (pos == 0)) {
                    sensitivitySeekBar.setMax(20);
                    sensitivityLLayout.setVisibility(View.VISIBLE);
                    sensitivitySeekBar.setVisibility(View.VISIBLE);
                    writeBtn.setEnabled(false);
                } else if ((currentConfig == 0x34) && (pos == 1)) {
                    sensitivitySeekBar.setMax(30);
                    sensitivityLLayout.setVisibility(View.VISIBLE);
                    sensitivitySeekBar.setVisibility(View.VISIBLE);
                    writeBtn.setEnabled(false);
                } else {
                    sensitivityLLayout.setVisibility(View.INVISIBLE);
                    sensitivitySeekBar.setVisibility(View.INVISIBLE);
                    writeBtn.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        sensitivityLLayout = (LinearLayout) findViewById(R.id.llSensitivity);
        sensitivityTV = (TextView) findViewById(R.id.tvSensitivityValue);
        sensitivitySeekBar = (SeekBar) findViewById(R.id.sbSensitivity);
        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivityTV.setText(String.valueOf(progress));
                if (progress == currentSensitivity){
                    writeBtn.setEnabled(false);
                } else {
                    writeBtn.setEnabled(true);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
        });

        writeBtn = (Button) findViewById(R.id.writeBtn);
        writeBtn.setOnClickListener(mClickListener);
        writeBtn.setEnabled(false);

        showActionBar();

        characteristic = MainActivity.gattCommandCharacteristic;
        // Read config
        byte[] readWLQConfigCmd = {0x57,0x52,0x57};
        characteristic.setValue(readWLQConfigCmd);
        BluetoothLeService.writeCharacteristic(characteristic);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "In onResume");
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"In onDestroy");
        super.onDestroy();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {

        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.writeBtn:
                    byte wwMode = 0x32;
                    // Get Selection
                    if (wwModeSpinner.getSelectedItemPosition() > 0){
                        //K52
                        wwMode = 0x34;
                    }
                    if (currentConfig == wwMode){
                        if(currentSensitivity != sensitivitySeekBar.getProgress()) {
                            Log.d(TAG, "Setting Sensitivity");
                            // Write sensitivity
                            char[] sensitivityChar = String.valueOf(sensitivitySeekBar.getProgress()).toCharArray();
                            char sensOne = sensitivityChar[0];
                            if (sensitivityChar.length == 1) {
                                byte[] writeSensitivityCmd = {0x57, 0x57, 0x43, 0x53, wwMode, 0x45, (byte) sensOne, 0x0D, 0x0A};
                                characteristic.setValue(writeSensitivityCmd);
                            } else if (sensitivityChar.length > 1) {
                                char sensTwo = sensitivityChar[1];
                                byte[] writeSensitivityCmd = {0x57, 0x57, 0x43, 0x53, wwMode, 0x45, (byte) sensOne, (byte) sensTwo, 0x0D, 0x0A};
                                characteristic.setValue(writeSensitivityCmd);
                            }
                            BluetoothLeService.writeCharacteristic(characteristic);
                        }
                    } else {
                        Log.d(TAG,"Setting Mode");
                        // Write mode
                        byte[] writeConfigCmd = {0x57,0x57,0x53,0x53,wwMode};
                        characteristic.setValue(writeConfigCmd);
                        BluetoothLeService.writeCharacteristic(characteristic);
                    }
                    finish();
                    break;
                case R.id.action_back:
                    // Go back
                    Intent backIntent = new Intent(FWConfigActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.fw_config_title);

        backButton = (ImageButton) findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton forwardButton = (ImageButton) findViewById(R.id.action_forward);
        forwardButton.setVisibility(View.INVISIBLE);
    }

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
                        String characteristicValue = Utils.ByteArraytoHex(data) + " ";
                        Log.d(TAG, "UUID: " + bd.getString(BluetoothLeService.EXTRA_BYTE_UUID_VALUE) + " DATA: " + characteristicValue);
                        if ((data[0] == 0x57) && (data[1] == 0x52) && (data[2] == 0x57)) {
                            byte mode = data[26];
                            currentConfig = mode;
                            byte sensitivity = data[34];
                            currentSensitivity = sensitivity;
                            if (mode == 0x32) {
                                wwModeSpinner.setSelection(0);
                                sensitivitySeekBar.setMax(30);
                                sensitivityLLayout.setVisibility(View.VISIBLE);
                                sensitivitySeekBar.setVisibility(View.VISIBLE);
                            } else {
                                wwModeSpinner.setSelection(1);
                                sensitivitySeekBar.setMax(20);
                                sensitivityLLayout.setVisibility(View.VISIBLE);
                                sensitivitySeekBar.setVisibility(View.VISIBLE);
                            }
                            sensitivitySeekBar.setProgress(sensitivity);
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
