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
import android.widget.Spinner;
import android.widget.TextView;

public class FWConfigActivity extends AppCompatActivity {

    private final static String TAG = "FWConfigActvity";

    BluetoothGattCharacteristic characteristic;

    private ActionBar actionBar;
    private ImageButton backButton;
    private TextView navbarTitle;
    private Spinner wwModeSpinner;
    private Button writeBtn;

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
                Log.d(TAG,"Item Selected: " + adapterView.getItemAtPosition(pos).toString());
                //TODO When custom selected unhide custom widgets
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        writeBtn = (Button) findViewById(R.id.writeBtn);
        writeBtn.setOnClickListener(mClickListener);

        showActionBar();

        characteristic = MainActivity.gattCommandCharacteristic;
        BluetoothLeService.readCharacteristic(characteristic);
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
                    byte wwMode = 0x00;
                    // Get Selection
                    Log.d(TAG,"Position: " + wwModeSpinner.getSelectedItemPosition());
                    if (wwModeSpinner.getSelectedItemPosition() > 0){
                        wwMode = 0x22;
                    }
                    // Write config
                    byte[] writeConfigCmd = {0x57,0x57,0x53,0x53,wwMode};
                    characteristic.setValue(writeConfigCmd);
                    BluetoothLeService.writeCharacteristic(characteristic);
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
                    if(bd.getString(BluetoothLeService.EXTRA_BYTE_UUID_VALUE).contains(GattAttributes.WUNDERLINQ_COMMAND_CHARACTERISTIC)){
                        byte [] data = bd.getByteArray(BluetoothLeService.EXTRA_BYTE_VALUE);
                        String characteristicValue = Utils.ByteArraytoHex(data) + " ";
                        Log.d(TAG,"UUID: "+ bd.getString(BluetoothLeService.EXTRA_BYTE_UUID_VALUE) + " DATA: "+ characteristicValue);
                        byte mode =  data[24];
                        if (mode == 0x00){
                            wwModeSpinner.setSelection(0);
                        } else {
                            wwModeSpinner.setSelection(1);
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
