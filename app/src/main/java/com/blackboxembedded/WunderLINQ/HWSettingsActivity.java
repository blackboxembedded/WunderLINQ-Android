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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class HWSettingsActivity extends AppCompatActivity implements HWSettingsRecyclerViewAdapter.ItemClickListener  {

    private final static String TAG = "HWSettingsActvity";

    private ImageButton resetButton;
    private TextView fwVersionTV;
    private TextView hwKeyModeTV;
    private HWSettingsRecyclerViewAdapter adapter;
    private Button hwConfigBtn;

    final ArrayList<ActionItem> actionItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hwsettings);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        fwVersionTV = findViewById(R.id.tvFWVersion);
        hwKeyModeTV = findViewById(R.id.tvHWKeyMode);
        RecyclerView recyclerView = findViewById(R.id.rvActions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HWSettingsRecyclerViewAdapter(this, actionItems);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                LinearLayoutManager.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        hwConfigBtn = findViewById(R.id.btnHWConfig);
        hwConfigBtn.setOnClickListener(mClickListener);
        hwConfigBtn.setVisibility(View.INVISIBLE);

        showActionBar();
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

        // Read config
        if (MainActivity.gattCommandCharacteristic != null) {
            Log.d(TAG, "Sending get config command");
            MainActivity.gattCommandCharacteristic.setValue(WLQ.GET_CONFIG_CMD);
            BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
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

    @Override
    public void onItemClick(View view, int position) {
        int actionID = adapter.getActionID(position);
        if (Double.parseDouble(WLQ.firmwareVersion) >= 2.0) {
            if (WLQ.keyMode == WLQ.keyMode_custom) {
                if (actionID != -1) {
                    Intent intent = new Intent(HWSettingsActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTIONID", actionID);
                    startActivity(intent);
                }
            }
        } else {
            if (actionID == WLQ.OldSensitivity) {
                Intent intent = new Intent(HWSettingsActivity.this, HWSettingsActionActivity.class);
                intent.putExtra("ACTIONID", actionID);
                startActivity(intent);
            }
        }
    }

    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btnHWConfig) {
                if (hwConfigBtn.getText().equals(getString(R.string.reset_btn_label))) {
                    resetHWConfig();
                } else if (hwConfigBtn.getText().equals(getString(R.string.customize_btn_label))) {
                    // Set to Customize Mode
                    if (Double.parseDouble(WLQ.firmwareVersion) >= 2.0) {
                        if (WLQ.keyMode != WLQ.keyMode_custom) {
                            setHWMode(WLQ.keyMode_custom);
                        }
                    }
                } else if (hwConfigBtn.getText().equals(getString(R.string.default_btn_label))) {
                    // Set to Default Mode
                    if (Double.parseDouble(WLQ.firmwareVersion) >= 2.0) {
                        if (WLQ.keyMode != WLQ.keyMode_default) {
                            setHWMode(WLQ.keyMode_default);
                        }
                    }
                } else if (hwConfigBtn.getText().equals(getString(R.string.config_write_label))) {
                    // Set Config Changes
                    setHWConfig();
                } else if (hwConfigBtn.getText().equals(getString(R.string.wwMode1))) {
                    // Set to full Mode
                    if (Double.parseDouble(WLQ.firmwareVersion) < 2.0) {
                        if (WLQ.wheelMode != WLQ.wheelMode_full) {
                            setHWMode(WLQ.wheelMode_full);
                        }
                    }
                } else if (hwConfigBtn.getText().equals(getString(R.string.wwMode2))) {
                    // Set to full Mode
                    if (Double.parseDouble(WLQ.firmwareVersion) < 2.0) {
                        if (WLQ.wheelMode != WLQ.wheelMode_rtk) {
                            setHWMode(WLQ.wheelMode_rtk);
                        }
                    }
                }
            } else if (v.getId() == R.id.action_reset) {
                // Reset
                resetHWConfig();
            } else if (v.getId() == R.id.action_back) {
                // Go back
                Intent backIntent = new Intent(HWSettingsActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
        }
    };

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav_hwsettings, null);
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

        resetButton = findViewById(R.id.action_reset);
        resetButton.setOnClickListener(mClickListener);
    }

    private void updateDisplay(){
        actionItems.clear();
        if (WLQ.firmwareVersion != null) {
            fwVersionTV.setText(getString(R.string.fw_version_label) + " " + WLQ.firmwareVersion);
            if (Double.parseDouble(WLQ.firmwareVersion) >= 2.0) {
                if (WLQ.keyMode == WLQ.keyMode_default || WLQ.keyMode == WLQ.keyMode_custom) {
                    //Check for config from FW 1.x
                    if(WLQ.flashConfig[0] == WLQ.defaultConfig1[0] &&
                            WLQ.flashConfig[1] == WLQ.defaultConfig1[1] &&
                            WLQ.flashConfig[2] == WLQ.defaultConfig1[2] &&
                            WLQ.flashConfig[3] == WLQ.defaultConfig1[3]){
                        // Corrupt Config
                        hwKeyModeTV.setText(getString(R.string.corrupt_config_label));
                        hwKeyModeTV.setVisibility(View.VISIBLE);
                        hwConfigBtn.setText(getString(R.string.reset_btn_label));
                    } else {
                        actionItems.add(new ActionItem(WLQ.USB,getString(R.string.usb_threshold_label),WLQ.getActionValue(WLQ.USB))); // USB
                        actionItems.add(new ActionItem(-1,getString(R.string.wwMode1),"")); //Full
                        actionItems.add(new ActionItem(WLQ.fullLongPressSensitivity,getString(R.string.long_press_label),WLQ.getActionValue(WLQ.fullLongPressSensitivity)));
                        actionItems.add(new ActionItem(WLQ.fullScrollUp,getString(R.string.full_scroll_up_label),WLQ.getActionValue(WLQ.fullScrollUp)));
                        actionItems.add(new ActionItem(WLQ.fullScrollDown,getString(R.string.full_scroll_down_label),WLQ.getActionValue(WLQ.fullScrollDown)));
                        actionItems.add(new ActionItem(WLQ.fullToggleRight,getString(R.string.full_toggle_right_label),WLQ.getActionValue(WLQ.fullToggleRight)));
                        actionItems.add(new ActionItem(WLQ.fullToggleRightLongPress,getString(R.string.full_toggle_right_long_label),WLQ.getActionValue(WLQ.fullToggleRightLongPress)));
                        actionItems.add(new ActionItem(WLQ.fullToggleLeft,getString(R.string.full_toggle_left_label),WLQ.getActionValue(WLQ.fullToggleLeft)));
                        actionItems.add(new ActionItem(WLQ.fullToggleLeftLongPress,getString(R.string.full_toggle_left_long_label),WLQ.getActionValue(WLQ.fullToggleLeftLongPress)));
                        actionItems.add(new ActionItem(WLQ.fullSignalCancel,getString(R.string.full_signal_cancel_label),WLQ.getActionValue(WLQ.fullSignalCancel)));
                        actionItems.add(new ActionItem(WLQ.fullSignalCancelLongPress,getString(R.string.full_signal_cancel_long_label),WLQ.getActionValue(WLQ.fullSignalCancelLongPress)));
                        actionItems.add(new ActionItem(-1,getString(R.string.wwMode2),""));  //RTK1600
                        actionItems.add(new ActionItem(WLQ.RTKDoublePressSensitivity,getString(R.string.double_press_label),WLQ.getActionValue(WLQ.RTKDoublePressSensitivity)));
                        actionItems.add(new ActionItem(WLQ.RTKPage,getString(R.string.rtk_page_label),WLQ.getActionValue(WLQ.RTKPage)));
                        actionItems.add(new ActionItem(WLQ.RTKPageDoublePress,getString(R.string.rtk_page_double_label),WLQ.getActionValue(WLQ.RTKPageDoublePress)));
                        actionItems.add(new ActionItem(WLQ.RTKZoomPlus,getString(R.string.rtk_zoomp_label),WLQ.getActionValue(WLQ.RTKZoomPlus)));
                        actionItems.add(new ActionItem(WLQ.RTKZoomPlusDoublePress,getString(R.string.rtk_zoomp_double_label),WLQ.getActionValue(WLQ.RTKZoomPlusDoublePress)));
                        actionItems.add(new ActionItem(WLQ.RTKZoomMinus,getString(R.string.rtk_zoomm_label),WLQ.getActionValue(WLQ.RTKZoomMinus)));
                        actionItems.add(new ActionItem(WLQ.RTKZoomMinusDoublePress,getString(R.string.rtk_zoomm_double_label),WLQ.getActionValue(WLQ.RTKZoomMinusDoublePress)));
                        actionItems.add(new ActionItem(WLQ.RTKSpeak,getString(R.string.rtk_speak_label),WLQ.getActionValue(WLQ.RTKSpeak)));
                        actionItems.add(new ActionItem(WLQ.RTKSpeakDoublePress,getString(R.string.rtk_speak_double_label),WLQ.getActionValue(WLQ.RTKSpeakDoublePress)));
                        actionItems.add(new ActionItem(WLQ.RTKMute,getString(R.string.rtk_mute_label),WLQ.getActionValue(WLQ.RTKMute)));
                        actionItems.add(new ActionItem(WLQ.RTKMuteDoublePress,getString(R.string.rtk_mute_double_label),WLQ.getActionValue(WLQ.RTKMuteDoublePress)));
                        actionItems.add(new ActionItem(WLQ.RTKDisplayOff,getString(R.string.rtk_display_label),WLQ.getActionValue(WLQ.RTKDisplayOff)));
                        actionItems.add(new ActionItem(WLQ.RTKDisplayOffDoublePress,getString(R.string.rtk_display_double_label),WLQ.getActionValue(WLQ.RTKDisplayOffDoublePress)));

                        hwKeyModeTV.setVisibility(View.VISIBLE);
                        if (WLQ.keyMode == WLQ.keyMode_default) {
                            hwKeyModeTV.setText(getString(R.string.keymode_label) + " " + getString(R.string.keymode_default_label));
                            hwConfigBtn.setText(getString(R.string.customize_btn_label));
                            resetButton.setVisibility(View.INVISIBLE);
                        } else if (WLQ.keyMode == WLQ.keyMode_custom){
                            resetButton.setVisibility(View.VISIBLE);
                            hwKeyModeTV.setText(getString(R.string.keymode_label) + " " + getString(R.string.keymode_custom_label));
                            if (!Arrays.equals(WLQ.flashConfig, WLQ.tempConfig)) {
                                Log.d(TAG,"New Config found");
                                Log.d(TAG, "tempConfig: " + Utils.ByteArraytoHex(WLQ.tempConfig));
                                hwConfigBtn.setText(getString(R.string.config_write_label));
                            } else {
                                hwConfigBtn.setText(getString(R.string.default_btn_label));
                            }
                        }
                    }



                } else {
                    // Corrupt Config
                    hwKeyModeTV.setVisibility(View.INVISIBLE);
                    hwConfigBtn.setText(getString(R.string.reset_btn_label));
                }
            } else {
                hwKeyModeTV.setVisibility(View.VISIBLE);
                if (WLQ.wheelMode == WLQ.wheelMode_full || WLQ.wheelMode == WLQ.wheelMode_rtk) {
                    // Customize
                    if (WLQ.wheelMode == WLQ.wheelMode_full) {
                        hwKeyModeTV.setText(getString(R.string.wwtype_label) + " " + getString(R.string.wwMode1));
                        actionItems.add(new ActionItem(WLQ.OldSensitivity,getString(R.string.long_press_label),WLQ.getActionValue(WLQ.OldSensitivity)));
                        actionItems.add(new ActionItem(-1,getString(R.string.full_scroll_up_label),getString(R.string.keyboard_hid_0x52_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.full_scroll_down_label),getString(R.string.keyboard_hid_0x51_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.full_toggle_right_label),getString(R.string.keyboard_hid_0x4F_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.full_toggle_right_long_label),getString(R.string.keyboard_hid_0x28_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.full_toggle_left_label),getString(R.string.keyboard_hid_0x50_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.full_toggle_left_long_label),getString(R.string.keyboard_hid_0x29_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.full_signal_cancel_long_label),getString(R.string.consumer_hid_0xB8_label)));
                        hwConfigBtn.setText(getString(R.string.wwMode2));
                    } else {
                        hwKeyModeTV.setText(getString(R.string.wwtype_label) + " " + getString(R.string.wwMode2));
                        actionItems.add(new ActionItem(WLQ.OldSensitivity,getString(R.string.double_press_label),WLQ.getActionValue(WLQ.OldSensitivity)));
                        actionItems.add(new ActionItem(-1,getString(R.string.rtk_page_label),getString(R.string.keyboard_hid_0x4F_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.rtk_page_double_label),getString(R.string.keyboard_hid_0x28_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.rtk_zoomp_label),getString(R.string.keyboard_hid_0x52_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.rtk_zoomm_label),getString(R.string.keyboard_hid_0x51_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.rtk_speak_label),getString(R.string.keyboard_hid_0x50_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.rtk_speak_double_label),getString(R.string.keyboard_hid_0x29_label)));
                        actionItems.add(new ActionItem(-1,getString(R.string.rtk_display_label),getString(R.string.consumer_hid_0xB8_label)));
                        hwConfigBtn.setText(getString(R.string.wwMode1));
                    }
                    if(WLQ.sensitivity != WLQ.tempSensitivity){
                        Log.d(TAG,"New Sensitivity found");
                        hwConfigBtn.setText(getString(R.string.config_write_label));
                    }
                } else {
                    // Corrupt Config
                    hwConfigBtn.setText(getString(R.string.reset_btn_label));
                }
            }
            hwConfigBtn.setVisibility(View.VISIBLE);
        }
    }

    private void resetHWConfig(){
        Log.d(TAG,"resetHWConfig()");
        // Display dialog
        final AlertDialog.Builder resetBuilder = new AlertDialog.Builder(HWSettingsActivity.this);
        resetBuilder.setTitle(getString(R.string.hwsave_alert_title));
        resetBuilder.setMessage(getString(R.string.hwreset_alert_body));
        resetBuilder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            if (WLQ.firmwareVersion != null) {
                                if (Double.parseDouble(WLQ.firmwareVersion) >= 2.0) {
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    outputStream.write(WLQ.WRITE_CONFIG_CMD);
                                    outputStream.write(WLQ.defaultConfig2);
                                    outputStream.write(WLQ.CMD_EOM);
                                    byte[] writeConfigCmd = outputStream.toByteArray();
                                    Log.d(TAG, "Command Sent: " + Utils.ByteArraytoHex(writeConfigCmd));
                                    MainActivity.gattCommandCharacteristic.setValue(writeConfigCmd);
                                    BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                } else {
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    outputStream.write(WLQ.WRITE_CONFIG_CMD);
                                    outputStream.write(WLQ.defaultConfig1);
                                    outputStream.write(WLQ.CMD_EOM);
                                    byte[] writeConfigCmd = outputStream.toByteArray();
                                    Log.d(TAG, "Command Sent: " + Utils.ByteArraytoHex(writeConfigCmd));
                                    MainActivity.gattCommandCharacteristic.setValue(writeConfigCmd);
                                    BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                }
                            }
                        } catch (IOException e) {
                            Log.d(TAG, e.toString());
                        }
                        finish();
                        Intent backIntent = new Intent(HWSettingsActivity.this, MainActivity.class);
                        backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(backIntent);
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
    }

    private void setHWConfig(){
        Log.d(TAG,"setHWConfig()");
        // Display dialog
        final AlertDialog.Builder resetBuilder = new AlertDialog.Builder(HWSettingsActivity.this);
        resetBuilder.setTitle(getString(R.string.hwsave_alert_title));
        resetBuilder.setMessage(getString(R.string.hwsave_alert_body));
        resetBuilder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (WLQ.firmwareVersion != null) {
                            if (Double.parseDouble(WLQ.firmwareVersion) >= 2.0) {
                                if (!Arrays.equals(WLQ.wunderLINQConfig, WLQ.tempConfig)) {
                                    try {
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        outputStream.write(WLQ.WRITE_CONFIG_CMD);
                                        outputStream.write(WLQ.tempConfig);
                                        outputStream.write(WLQ.CMD_EOM);
                                        byte[] writeConfigCmd = outputStream.toByteArray();
                                        Log.d(TAG, "Command Sent: " + Utils.ByteArraytoHex(writeConfigCmd));
                                        MainActivity.gattCommandCharacteristic.setValue(writeConfigCmd);
                                        BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                    } catch (IOException e) {
                                        Log.d(TAG, e.toString());
                                    }
                                } else {
                                    Log.d(TAG, "New config not found");
                                }
                            } else {
                                if(WLQ.sensitivity != WLQ.tempSensitivity){
                                    // Write sensitivity
                                    char[] sensitivityChar = String.valueOf(WLQ.tempSensitivity).toCharArray();
                                    char sensOne = sensitivityChar[0];
                                    try {
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        outputStream.write(WLQ.WRITE_SENSITIVITY_CMD);
                                        if (sensitivityChar.length == 1) {
                                            outputStream.write(WLQ.wheelMode);
                                            outputStream.write(0x45);
                                            outputStream.write((byte) sensOne);
                                            outputStream.write(WLQ.CMD_EOM);
                                            byte[] writeSensitivityCmd = outputStream.toByteArray();
                                            MainActivity.gattCommandCharacteristic.setValue(writeSensitivityCmd);
                                        } else {
                                            char sensTwo = sensitivityChar[1];
                                            outputStream.write(WLQ.wheelMode);
                                            outputStream.write(0x45);
                                            outputStream.write((byte) sensOne);
                                            outputStream.write((byte) sensTwo);
                                            outputStream.write(WLQ.CMD_EOM);
                                            byte[] writeSensitivityCmd = outputStream.toByteArray();
                                            MainActivity.gattCommandCharacteristic.setValue(writeSensitivityCmd);
                                        }
                                        BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                    } catch (IOException e){
                                        Log.d(TAG,e.toString());
                                    }
                                } else {
                                    Log.d(TAG, "New sensitivity not found");
                                }
                            }
                        }
                        finish();
                        Intent backIntent = new Intent(HWSettingsActivity.this, MainActivity.class);
                        backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(backIntent);
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

    }

    private void setHWMode(byte mode){
        Log.d(TAG,"setHWMode()");
        // Display dialog
        final AlertDialog.Builder resetBuilder = new AlertDialog.Builder(HWSettingsActivity.this);
        resetBuilder.setTitle(getString(R.string.hwsave_alert_title));
        resetBuilder.setMessage(getString(R.string.hwsave_alert_body));
        resetBuilder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            outputStream.write(WLQ.WRITE_MODE_CMD);
                            outputStream.write(mode);
                            outputStream.write(WLQ.CMD_EOM);
                            byte[] writeConfigCmd = outputStream.toByteArray();
                            Log.d(TAG, "Command Sent: " + Utils.ByteArraytoHex(writeConfigCmd));
                            MainActivity.gattCommandCharacteristic.setValue(writeConfigCmd);
                            BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                        } catch (IOException e) {
                            Log.d(TAG, e.toString());
                        }
                        finish();
                        Intent backIntent = new Intent(HWSettingsActivity.this, MainActivity.class);
                        backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(backIntent);
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
                        updateDisplay();
                    }
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
