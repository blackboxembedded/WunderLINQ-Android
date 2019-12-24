package com.blackboxembedded.WunderLINQ;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class FWConfigActivity extends AppCompatActivity {

    private final static String TAG = "FWConfigActvity";

    BluetoothGattCharacteristic characteristic;

    private TextView fwVersionTV;
    private TextView wwModeLabelTV;
    private Spinner wwModeSpinner;
    private LinearLayout sensitivityLLayout;
    private TextView sensitivityTV;
    private SeekBar sensitivitySeekBar;
    private Button writeBtn;
    private Button resetBtn;

    private byte currentConfig;
    private byte currentSensitivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fwconfig);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        fwVersionTV = findViewById(R.id.tvFWVersion);
        wwModeLabelTV = findViewById(R.id.tvwwModeLabel);
        wwModeLabelTV.setVisibility(View.INVISIBLE);
        wwModeSpinner = findViewById(R.id.wwModeSpinner);
        wwModeSpinner.setVisibility(View.INVISIBLE);
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

        sensitivityLLayout = findViewById(R.id.llSensitivity);
        sensitivityTV = findViewById(R.id.tvSensitivityValue);
        sensitivitySeekBar = findViewById(R.id.sbSensitivity);
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
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        writeBtn = findViewById(R.id.writeBtn);
        writeBtn.setOnClickListener(mClickListener);
        writeBtn.setEnabled(false);
        writeBtn.setVisibility(View.INVISIBLE);
        resetBtn = findViewById(R.id.resetBtn);
        resetBtn.setOnClickListener(mClickListener);
        resetBtn.setVisibility(View.INVISIBLE);

        showActionBar();

        characteristic = MainActivity.gattCommandCharacteristic;
        if (characteristic != null) {
            // Read config
            byte[] readWLQConfigCmd = {0x57, 0x52, 0x57, 0x0D, 0x0A};
            characteristic.setValue(readWLQConfigCmd);
            BluetoothLeService.writeCharacteristic(characteristic);
        }
    }
    @Override
    public void recreate() {
        super.recreate();
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
            e.printStackTrace();
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.resetBtn:
                    // Display dialog
                    final AlertDialog.Builder resetBuilder = new AlertDialog.Builder(FWConfigActivity.this);
                    resetBuilder.setTitle(getString(R.string.hwsave_alert_title));
                    resetBuilder.setMessage(getString(R.string.hwsave_alert_body));
                    resetBuilder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    byte[] defaultConfig = {0x57,0x57,0x43,0x41,0x32,0x01,0x04,0x04,(byte)0xFE,(byte)0xFC,0x4F,0x28,0x0F,0x04,
                                            0x04,(byte)0xFD,(byte)0xFC,0x50,0x29,0x0F,0x04,0x06,0x00,0x00,0x00,0x00,0x34,0x02,0x01,0x01,0x65,
                                            0x55,0x4F,0x28,0x07,0x01,0x01,(byte)0x95,0x55,0x50,0x29,0x07,0x01,0x01,0x56,0x59,0x52,0x51,0x0D,0x0A};
                                    characteristic.setValue(defaultConfig);
                                    BluetoothLeService.writeCharacteristic(characteristic);
                                    finish();
                                }
                            });
                    resetBuilder.setNegativeButton(R.string.hwsave_alert_btn_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    resetBuilder.show();
                    break;
                case R.id.writeBtn:
                    // Display dialog
                    final AlertDialog.Builder builder = new AlertDialog.Builder(FWConfigActivity.this);
                    builder.setTitle(getString(R.string.hwsave_alert_title));
                    builder.setMessage(getString(R.string.hwsave_alert_body));
                    builder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
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
                                            } else {
                                                char sensTwo = sensitivityChar[1];
                                                byte[] writeSensitivityCmd = {0x57, 0x57, 0x43, 0x53, wwMode, 0x45, (byte) sensOne, (byte) sensTwo, 0x0D, 0x0A};
                                                characteristic.setValue(writeSensitivityCmd);
                                            }
                                            BluetoothLeService.writeCharacteristic(characteristic);
                                        }
                                    } else {
                                        Log.d(TAG,"Setting Mode");
                                        // Write mode
                                        byte[] writeConfigCmd = {0x57, 0x57, 0x53, 0x53, wwMode, 0x0D, 0x0A};
                                        characteristic.setValue(writeConfigCmd);
                                        BluetoothLeService.writeCharacteristic(characteristic);
                                    }
                                    finish();
                                }
                            });
                    builder.setNegativeButton(R.string.hwsave_alert_btn_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    builder.show();
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
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.fw_config_title);

        ImageButton backButton = findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton forwardButton = findViewById(R.id.action_forward);
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
                        Log.d(TAG, "UUID: " + bd.getString(BluetoothLeService.EXTRA_BYTE_UUID_VALUE) + " DATA: " + Utils.ByteArraytoHex(data));
                        if ((data[0] == 0x57) && (data[1] == 0x52) && (data[2] == 0x57)) {
                            byte mode = data[26];
                            currentConfig = mode;
                            byte sensitivity = data[34];
                            currentSensitivity = sensitivity;
                            if (mode == 0x32) {
                                wwModeLabelTV.setVisibility(View.VISIBLE);
                                wwModeSpinner.setVisibility(View.VISIBLE);
                                wwModeSpinner.setSelection(0);
                                sensitivitySeekBar.setMax(30);
                                sensitivityLLayout.setVisibility(View.VISIBLE);
                                sensitivitySeekBar.setVisibility(View.VISIBLE);
                                sensitivitySeekBar.setProgress(sensitivity);
                                writeBtn.setVisibility(View.VISIBLE);
                            } else if (mode == 0x34){
                                wwModeLabelTV.setVisibility(View.VISIBLE);
                                wwModeSpinner.setVisibility(View.VISIBLE);
                                wwModeSpinner.setSelection(1);
                                sensitivitySeekBar.setMax(20);
                                sensitivityLLayout.setVisibility(View.VISIBLE);
                                sensitivitySeekBar.setVisibility(View.VISIBLE);
                                sensitivitySeekBar.setProgress(sensitivity);
                                writeBtn.setVisibility(View.VISIBLE);
                            } else {
                                // Corrupt Config
                                resetBtn.setVisibility(View.VISIBLE);
                            }

                            // Write mode
                            byte[] getVersionCmd = {0x57, 0x52, 0x56};
                            characteristic.setValue(getVersionCmd);
                            BluetoothLeService.writeCharacteristic(characteristic);
                        } else if ((data[0] == 0x57) && (data[1] == 0x52) && (data[2] == 0x56)){
                            String version = data[3] + "." + data[4];
                            fwVersionTV.setText(getString(R.string.fw_version_label) + " " + version);
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
