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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_BASE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class HWSettingsActivity extends AppCompatActivity implements HWSettingsRecyclerViewAdapter.ItemClickListener  {

    private final static String TAG = "HWSettingsActivity";

    private ImageButton resetButton;
    private TextView fwVersionTV;
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
        super.onResume();

        ContextCompat.registerReceiver(this, mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);

        // Read config
        if (BluetoothLeService.gattCommandCharacteristic != null) {
            BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, WLQ_BASE.GET_CONFIG_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
        }

        // Read HW Version
        if (MotorcycleData.hardwareVersion == null) {
            if (BluetoothLeService.gattHWCharacteristic != null) {
                BluetoothLeService.readCharacteristic(BluetoothLeService.gattCommandCharacteristic);
            }
        }

        updateDisplay();
    }

    @Override
    protected void onDestroy() {
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
        if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()) {
            if (actionID != -1) {
                Intent intent = new Intent(HWSettingsActivity.this, HWSettingsActionActivity.class);
                intent.putExtra("ACTIONID", actionID);
                startActivity(intent);
            }
        } else {
            if (actionID == WLQ.KEYMODE || actionID == WLQ.pdmChannel1 || actionID == WLQ.pdmChannel2
                    || actionID == WLQ.pdmChannel3 || actionID == WLQ.pdmChannel4) {
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
                } else if (hwConfigBtn.getText().equals(getString(R.string.config_write_label))) {
                    // Set Config Changes
                    setHWConfig();
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
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.actionbar_nav_hwsettings, null);
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
        resetButton.setVisibility(View.INVISIBLE);
        if (MotorcycleData.wlq != null) {
            resetButton.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay(){
        actionItems.clear();
        if (MotorcycleData.wlq != null) {
            if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_N) {
                if (MotorcycleData.wlq.getFirmwareVersion() != null) {
                    fwVersionTV.setText(getString(R.string.fw_version_label) + " " + MotorcycleData.wlq.getFirmwareVersion());
                    if (Double.parseDouble(MotorcycleData.wlq.getFirmwareVersion()) >= 2.0) {
                        if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DEFAULT() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()
                                || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_MEDIA() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DMD2()) {
                            actionItems.add(new ActionItem(WLQ.KEYMODE, MotorcycleData.wlq.getActionName(WLQ.KEYMODE), MotorcycleData.wlq.getActionValue(WLQ.KEYMODE))); // Key mode
                            actionItems.add(new ActionItem(WLQ.USB, getString(R.string.usb_threshold_label), MotorcycleData.wlq.getActionValue(WLQ.USB))); // USB
                            actionItems.add(new ActionItem(-1, getString(R.string.wwMode1), "")); //Full
                            actionItems.add(new ActionItem(WLQ.longPressSensitivity, MotorcycleData.wlq.getActionName(WLQ.longPressSensitivity), MotorcycleData.wlq.getActionValue(WLQ.longPressSensitivity)));
                            actionItems.add(new ActionItem(WLQ.fullScrollUp, MotorcycleData.wlq.getActionName(WLQ.fullScrollUp), MotorcycleData.wlq.getActionValue(WLQ.fullScrollUp)));
                            actionItems.add(new ActionItem(WLQ.fullScrollDown, MotorcycleData.wlq.getActionName(WLQ.fullScrollDown), MotorcycleData.wlq.getActionValue(WLQ.fullScrollDown)));
                            actionItems.add(new ActionItem(WLQ.fullToggleRight, MotorcycleData.wlq.getActionName(WLQ.fullToggleRight), MotorcycleData.wlq.getActionValue(WLQ.fullToggleRight)));
                            actionItems.add(new ActionItem(WLQ.fullToggleRightLongPress, MotorcycleData.wlq.getActionName(WLQ.fullToggleRightLongPress), MotorcycleData.wlq.getActionValue(WLQ.fullToggleRightLongPress)));
                            actionItems.add(new ActionItem(WLQ.fullToggleLeft, MotorcycleData.wlq.getActionName(WLQ.fullToggleLeft), MotorcycleData.wlq.getActionValue(WLQ.fullToggleLeft)));
                            actionItems.add(new ActionItem(WLQ.fullToggleLeftLongPress, MotorcycleData.wlq.getActionName(WLQ.fullToggleLeftLongPress), MotorcycleData.wlq.getActionValue(WLQ.fullToggleLeftLongPress)));
                            actionItems.add(new ActionItem(WLQ.fullSignalCancel, MotorcycleData.wlq.getActionName(WLQ.fullSignalCancel), MotorcycleData.wlq.getActionValue(WLQ.fullSignalCancel)));
                            actionItems.add(new ActionItem(WLQ.fullSignalCancelLongPress, MotorcycleData.wlq.getActionName(WLQ.fullSignalCancelLongPress), MotorcycleData.wlq.getActionValue(WLQ.fullSignalCancelLongPress)));
                            actionItems.add(new ActionItem(-1, getString(R.string.wwMode2), ""));  //RTK1600
                            actionItems.add(new ActionItem(WLQ.doublePressSensitivity, MotorcycleData.wlq.getActionName(WLQ.doublePressSensitivity), MotorcycleData.wlq.getActionValue(WLQ.doublePressSensitivity)));
                            actionItems.add(new ActionItem(WLQ.RTKPage, MotorcycleData.wlq.getActionName(WLQ.RTKPage), MotorcycleData.wlq.getActionValue(WLQ.RTKPage)));
                            actionItems.add(new ActionItem(WLQ.RTKPageDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKPageDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKPageDoublePress)));
                            actionItems.add(new ActionItem(WLQ.RTKZoomPlus, MotorcycleData.wlq.getActionName(WLQ.RTKZoomPlus), MotorcycleData.wlq.getActionValue(WLQ.RTKZoomPlus)));
                            actionItems.add(new ActionItem(WLQ.RTKZoomPlusDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKZoomPlusDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKZoomPlusDoublePress)));
                            actionItems.add(new ActionItem(WLQ.RTKZoomMinus, MotorcycleData.wlq.getActionName(WLQ.RTKZoomMinus), MotorcycleData.wlq.getActionValue(WLQ.RTKZoomMinus)));
                            actionItems.add(new ActionItem(WLQ.RTKZoomMinusDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKZoomMinusDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKZoomMinusDoublePress)));
                            actionItems.add(new ActionItem(WLQ.RTKSpeak, MotorcycleData.wlq.getActionName(WLQ.RTKSpeak), MotorcycleData.wlq.getActionValue(WLQ.RTKSpeak)));
                            actionItems.add(new ActionItem(WLQ.RTKSpeakDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKSpeakDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKSpeakDoublePress)));
                            actionItems.add(new ActionItem(WLQ.RTKMute, MotorcycleData.wlq.getActionName(WLQ.RTKMute), MotorcycleData.wlq.getActionValue(WLQ.RTKMute)));
                            actionItems.add(new ActionItem(WLQ.RTKMuteDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKMuteDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKMuteDoublePress)));
                            actionItems.add(new ActionItem(WLQ.RTKDisplayOff, MotorcycleData.wlq.getActionName(WLQ.RTKDisplayOff), MotorcycleData.wlq.getActionValue(WLQ.RTKDisplayOff)));
                            actionItems.add(new ActionItem(WLQ.RTKDisplayOffDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKDisplayOffDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKDisplayOffDoublePress)));

                            resetButton.setVisibility(View.INVISIBLE);
                            hwConfigBtn.setVisibility(View.INVISIBLE);
                            if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()) {
                                resetButton.setVisibility(View.VISIBLE);
                                if (!Arrays.equals(MotorcycleData.wlq.getConfig(), MotorcycleData.wlq.getTempConfig())) {
                                    Log.d(TAG, "New Config found");
                                    Log.d(TAG, "Config: " + Utils.ByteArrayToHex(MotorcycleData.wlq.getConfig()));
                                    Log.d(TAG, "tempConfig: " + Utils.ByteArrayToHex(MotorcycleData.wlq.getTempConfig()));
                                    hwConfigBtn.setText(getString(R.string.config_write_label));
                                    hwConfigBtn.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            // Corrupt Config
                            hwConfigBtn.setText(getString(R.string.reset_btn_label));
                            hwConfigBtn.setVisibility(View.VISIBLE);
                        }
                    } else {
                        //Only provide settings for up to date firmware
                        // Needs upgrade to >= 2.0
                        hwConfigBtn.setText("WunderLINQ-DFU");
                        hwConfigBtn.setVisibility(View.VISIBLE);
                    }
                }
            } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_X) {
                if (MotorcycleData.wlq.getFirmwareVersion() != null) {
                    fwVersionTV.setText(getString(R.string.fw_version_label) + " " + MotorcycleData.wlq.getFirmwareVersion());
                }
                if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DEFAULT() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()
                        || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_MEDIA() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DMD2()) {
                    actionItems.add(new ActionItem(WLQ.KEYMODE, MotorcycleData.wlq.getActionName(WLQ.KEYMODE), MotorcycleData.wlq.getActionValue(WLQ.KEYMODE))); // Key mode
                    actionItems.add(new ActionItem(-1, getString(R.string.wwMode1), "")); //Full
                    actionItems.add(new ActionItem(WLQ.longPressSensitivity, MotorcycleData.wlq.getActionName(WLQ.longPressSensitivity), MotorcycleData.wlq.getActionValue(WLQ.longPressSensitivity)));
                    actionItems.add(new ActionItem(WLQ.fullScrollUp, MotorcycleData.wlq.getActionName(WLQ.fullScrollUp), MotorcycleData.wlq.getActionValue(WLQ.fullScrollUp)));
                    actionItems.add(new ActionItem(WLQ.fullScrollDown, MotorcycleData.wlq.getActionName(WLQ.fullScrollDown), MotorcycleData.wlq.getActionValue(WLQ.fullScrollDown)));
                    actionItems.add(new ActionItem(WLQ.fullToggleRight, MotorcycleData.wlq.getActionName(WLQ.fullToggleRight), MotorcycleData.wlq.getActionValue(WLQ.fullToggleRight)));
                    actionItems.add(new ActionItem(WLQ.fullToggleRightLongPress, MotorcycleData.wlq.getActionName(WLQ.fullToggleRightLongPress), MotorcycleData.wlq.getActionValue(WLQ.fullToggleRightLongPress)));
                    actionItems.add(new ActionItem(WLQ.fullToggleLeft, MotorcycleData.wlq.getActionName(WLQ.fullToggleLeft), MotorcycleData.wlq.getActionValue(WLQ.fullToggleLeft)));
                    actionItems.add(new ActionItem(WLQ.fullToggleLeftLongPress, MotorcycleData.wlq.getActionName(WLQ.fullToggleLeftLongPress), MotorcycleData.wlq.getActionValue(WLQ.fullToggleLeftLongPress)));
                    actionItems.add(new ActionItem(WLQ.fullSignalCancel, MotorcycleData.wlq.getActionName(WLQ.fullSignalCancel), MotorcycleData.wlq.getActionValue(WLQ.fullSignalCancel)));
                    actionItems.add(new ActionItem(WLQ.fullSignalCancelLongPress, MotorcycleData.wlq.getActionName(WLQ.fullSignalCancelLongPress), MotorcycleData.wlq.getActionValue(WLQ.fullSignalCancelLongPress)));
                    actionItems.add(new ActionItem(-1, getString(R.string.wwMode2), ""));  //RTK1600
                    actionItems.add(new ActionItem(WLQ.doublePressSensitivity, MotorcycleData.wlq.getActionName(WLQ.doublePressSensitivity), MotorcycleData.wlq.getActionValue(WLQ.doublePressSensitivity)));
                    actionItems.add(new ActionItem(WLQ.RTKPage, MotorcycleData.wlq.getActionName(WLQ.RTKPage), MotorcycleData.wlq.getActionValue(WLQ.RTKPage)));
                    actionItems.add(new ActionItem(WLQ.RTKPageDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKPageDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKPageDoublePress)));
                    actionItems.add(new ActionItem(WLQ.RTKZoomPlus, MotorcycleData.wlq.getActionName(WLQ.RTKZoomPlus), MotorcycleData.wlq.getActionValue(WLQ.RTKZoomPlus)));
                    actionItems.add(new ActionItem(WLQ.RTKZoomPlusDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKZoomPlusDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKZoomPlusDoublePress)));
                    actionItems.add(new ActionItem(WLQ.RTKZoomMinus, MotorcycleData.wlq.getActionName(WLQ.RTKZoomMinus), MotorcycleData.wlq.getActionValue(WLQ.RTKZoomMinus)));
                    actionItems.add(new ActionItem(WLQ.RTKZoomMinusDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKZoomMinusDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKZoomMinusDoublePress)));
                    actionItems.add(new ActionItem(WLQ.RTKSpeak, MotorcycleData.wlq.getActionName(WLQ.RTKSpeak), MotorcycleData.wlq.getActionValue(WLQ.RTKSpeak)));
                    actionItems.add(new ActionItem(WLQ.RTKSpeakDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKSpeakDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKSpeakDoublePress)));
                    actionItems.add(new ActionItem(WLQ.RTKMute, MotorcycleData.wlq.getActionName(WLQ.RTKMute), MotorcycleData.wlq.getActionValue(WLQ.RTKMute)));
                    actionItems.add(new ActionItem(WLQ.RTKMuteDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKMuteDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKMuteDoublePress)));
                    actionItems.add(new ActionItem(WLQ.RTKDisplayOff, MotorcycleData.wlq.getActionName(WLQ.RTKDisplayOff), MotorcycleData.wlq.getActionValue(WLQ.RTKDisplayOff)));
                    actionItems.add(new ActionItem(WLQ.RTKDisplayOffDoublePress, MotorcycleData.wlq.getActionName(WLQ.RTKDisplayOffDoublePress), MotorcycleData.wlq.getActionValue(WLQ.RTKDisplayOffDoublePress)));
                    if (MotorcycleData.wlq.getAccessories() > 0){
                        actionItems.add(new ActionItem(-1, getString(R.string.pdm_label), ""));  //Power Controller
                        actionItems.add(new ActionItem(WLQ.pdmChannel1, MotorcycleData.wlq.getActionName(WLQ.pdmChannel1), MotorcycleData.wlq.getActionValue(WLQ.pdmChannel1)));
                        actionItems.add(new ActionItem(WLQ.pdmChannel2, MotorcycleData.wlq.getActionName(WLQ.pdmChannel2), MotorcycleData.wlq.getActionValue(WLQ.pdmChannel2)));
                    }
                    resetButton.setVisibility(View.INVISIBLE);
                    hwConfigBtn.setVisibility(View.INVISIBLE);
                    if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()) {
                        resetButton.setVisibility(View.VISIBLE);
                    }
                    if (!Arrays.equals(MotorcycleData.wlq.getConfig(), MotorcycleData.wlq.getTempConfig())) {
                        Log.d(TAG, "New Config found");
                        Log.d(TAG, "Config: " + Utils.ByteArrayToHex(MotorcycleData.wlq.getConfig()));
                        Log.d(TAG, "tempConfig: " + Utils.ByteArrayToHex(MotorcycleData.wlq.getTempConfig()));
                        hwConfigBtn.setText(getString(R.string.config_write_label));
                        hwConfigBtn.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Corrupt Config
                    hwConfigBtn.setText(getString(R.string.reset_btn_label));
                    hwConfigBtn.setVisibility(View.VISIBLE);
                }
            } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_S) {
                if (MotorcycleData.wlq.getFirmwareVersion() != null) {
                    fwVersionTV.setText(getString(R.string.fw_version_label) + " " + MotorcycleData.wlq.getFirmwareVersion());
                }
                if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DEFAULT() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()
                        || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_MEDIA() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DMD2()) {
                    actionItems.add(new ActionItem(WLQ.KEYMODE, MotorcycleData.wlq.getActionName(WLQ.KEYMODE), MotorcycleData.wlq.getActionValue(WLQ.KEYMODE))); // Keymode
                    actionItems.add(new ActionItem(WLQ.longPressSensitivity, getString(R.string.long_press_label), MotorcycleData.wlq.getActionValue(WLQ.longPressSensitivity)));
                    actionItems.add(new ActionItem(WLQ.up, MotorcycleData.wlq.getActionName(WLQ.up), MotorcycleData.wlq.getActionValue(WLQ.up)));
                    actionItems.add(new ActionItem(WLQ.upLong, MotorcycleData.wlq.getActionName(WLQ.upLong), MotorcycleData.wlq.getActionValue(WLQ.upLong)));
                    actionItems.add(new ActionItem(WLQ.down, MotorcycleData.wlq.getActionName(WLQ.down), MotorcycleData.wlq.getActionValue(WLQ.down)));
                    actionItems.add(new ActionItem(WLQ.down, MotorcycleData.wlq.getActionName(WLQ.downLong), MotorcycleData.wlq.getActionValue(WLQ.downLong)));
                    actionItems.add(new ActionItem(WLQ.right, MotorcycleData.wlq.getActionName(WLQ.right), MotorcycleData.wlq.getActionValue(WLQ.right)));
                    actionItems.add(new ActionItem(WLQ.rightLong, MotorcycleData.wlq.getActionName(WLQ.rightLong), MotorcycleData.wlq.getActionValue(WLQ.rightLong)));
                    actionItems.add(new ActionItem(WLQ.left, MotorcycleData.wlq.getActionName(WLQ.left), MotorcycleData.wlq.getActionValue(WLQ.left)));
                    actionItems.add(new ActionItem(WLQ.leftLong, MotorcycleData.wlq.getActionName(WLQ.leftLong), MotorcycleData.wlq.getActionValue(WLQ.leftLong)));
                    actionItems.add(new ActionItem(WLQ.fx1, MotorcycleData.wlq.getActionName(WLQ.fx1), MotorcycleData.wlq.getActionValue(WLQ.fx1)));
                    actionItems.add(new ActionItem(WLQ.fx1Long, MotorcycleData.wlq.getActionName(WLQ.fx1Long), MotorcycleData.wlq.getActionValue(WLQ.fx1Long)));
                    actionItems.add(new ActionItem(WLQ.fx2, MotorcycleData.wlq.getActionName(WLQ.fx2), MotorcycleData.wlq.getActionValue(WLQ.fx2)));
                    actionItems.add(new ActionItem(WLQ.fx2Long, MotorcycleData.wlq.getActionName(WLQ.fx2Long), MotorcycleData.wlq.getActionValue(WLQ.fx2Long)));
                    if (MotorcycleData.wlq.getAccessories() > 0){
                        actionItems.add(new ActionItem(-1, getString(R.string.pdm_label), ""));  //Power Controller
                        actionItems.add(new ActionItem(WLQ.pdmChannel1, MotorcycleData.wlq.getActionName(WLQ.pdmChannel1), MotorcycleData.wlq.getActionValue(WLQ.pdmChannel1)));
                        actionItems.add(new ActionItem(WLQ.pdmChannel2, MotorcycleData.wlq.getActionName(WLQ.pdmChannel2), MotorcycleData.wlq.getActionValue(WLQ.pdmChannel2)));
                    }

                    resetButton.setVisibility(View.INVISIBLE);
                    hwConfigBtn.setVisibility(View.INVISIBLE);
                    if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()) {
                        resetButton.setVisibility(View.VISIBLE);
                    }
                    if (!Arrays.equals(MotorcycleData.wlq.getConfig(), MotorcycleData.wlq.getTempConfig())) {
                        Log.d(TAG, "New Config found");
                        Log.d(TAG, "Config: " + Utils.ByteArrayToHex(MotorcycleData.wlq.getConfig()));
                        Log.d(TAG, "tempConfig: " + Utils.ByteArrayToHex(MotorcycleData.wlq.getTempConfig()));
                        hwConfigBtn.setText(getString(R.string.config_write_label));
                        hwConfigBtn.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Corrupt Config
                    hwConfigBtn.setText(getString(R.string.reset_btn_label));
                    hwConfigBtn.setVisibility(View.VISIBLE);
                }
            } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_U) {
                if (MotorcycleData.wlq.getFirmwareVersion() != null) {
                    fwVersionTV.setText(getString(R.string.fw_version_label) + " " + MotorcycleData.wlq.getFirmwareVersion());
                }
                if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DEFAULT() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()) {
                    actionItems.add(new ActionItem(WLQ.KEYMODE, MotorcycleData.wlq.getActionName(WLQ.KEYMODE), MotorcycleData.wlq.getActionValue(WLQ.KEYMODE)));
                    actionItems.add(new ActionItem(WLQ.ORIENTATION, MotorcycleData.wlq.getActionName(WLQ.ORIENTATION), MotorcycleData.wlq.getActionValue(WLQ.ORIENTATION)));
                    actionItems.add(new ActionItem(WLQ.longPressSensitivity, getString(R.string.long_press_label), MotorcycleData.wlq.getActionValue(WLQ.longPressSensitivity)));
                    actionItems.add(new ActionItem(WLQ.up, MotorcycleData.wlq.getActionName(WLQ.up), MotorcycleData.wlq.getActionValue(WLQ.up)));
                    actionItems.add(new ActionItem(WLQ.upLong, MotorcycleData.wlq.getActionName(WLQ.upLong), MotorcycleData.wlq.getActionValue(WLQ.upLong)));
                    actionItems.add(new ActionItem(WLQ.down, MotorcycleData.wlq.getActionName(WLQ.down), MotorcycleData.wlq.getActionValue(WLQ.down)));
                    actionItems.add(new ActionItem(WLQ.down, MotorcycleData.wlq.getActionName(WLQ.downLong), MotorcycleData.wlq.getActionValue(WLQ.downLong)));
                    actionItems.add(new ActionItem(WLQ.right, MotorcycleData.wlq.getActionName(WLQ.right), MotorcycleData.wlq.getActionValue(WLQ.right)));
                    actionItems.add(new ActionItem(WLQ.rightLong, MotorcycleData.wlq.getActionName(WLQ.rightLong), MotorcycleData.wlq.getActionValue(WLQ.rightLong)));
                    actionItems.add(new ActionItem(WLQ.left, MotorcycleData.wlq.getActionName(WLQ.left), MotorcycleData.wlq.getActionValue(WLQ.left)));
                    actionItems.add(new ActionItem(WLQ.leftLong, MotorcycleData.wlq.getActionName(WLQ.leftLong), MotorcycleData.wlq.getActionValue(WLQ.leftLong)));
                    actionItems.add(new ActionItem(WLQ.fx1, MotorcycleData.wlq.getActionName(WLQ.fx1), MotorcycleData.wlq.getActionValue(WLQ.fx1)));
                    actionItems.add(new ActionItem(WLQ.fx1Long, MotorcycleData.wlq.getActionName(WLQ.fx1Long), MotorcycleData.wlq.getActionValue(WLQ.fx1Long)));
                    actionItems.add(new ActionItem(WLQ.fx2, MotorcycleData.wlq.getActionName(WLQ.fx2), MotorcycleData.wlq.getActionValue(WLQ.fx2)));
                    actionItems.add(new ActionItem(WLQ.fx2Long, MotorcycleData.wlq.getActionName(WLQ.fx2Long), MotorcycleData.wlq.getActionValue(WLQ.fx2Long)));
                    if (MotorcycleData.wlq.getAccessories() > 0){
                        actionItems.add(new ActionItem(-1, getString(R.string.pdm_label), ""));  //Power Controller
                        actionItems.add(new ActionItem(WLQ.pdmChannel1, MotorcycleData.wlq.getActionName(WLQ.pdmChannel1), MotorcycleData.wlq.getActionValue(WLQ.pdmChannel1)));
                        actionItems.add(new ActionItem(WLQ.pdmChannel2, MotorcycleData.wlq.getActionName(WLQ.pdmChannel2), MotorcycleData.wlq.getActionValue(WLQ.pdmChannel2)));
                    }
                    resetButton.setVisibility(View.INVISIBLE);
                    hwConfigBtn.setVisibility(View.INVISIBLE);
                    if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()) {
                        resetButton.setVisibility(View.VISIBLE);
                    }
                    if (!Arrays.equals(MotorcycleData.wlq.getConfig(), MotorcycleData.wlq.getTempConfig())) {
                        Log.d(TAG, "New Config found");
                        Log.d(TAG, "Config: " + Utils.ByteArrayToHex(MotorcycleData.wlq.getConfig()));
                        Log.d(TAG, "tempConfig: " + Utils.ByteArrayToHex(MotorcycleData.wlq.getTempConfig()));
                        hwConfigBtn.setText(getString(R.string.config_write_label));
                        hwConfigBtn.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Corrupt Config
                    hwConfigBtn.setText(getString(R.string.reset_btn_label));
                    hwConfigBtn.setVisibility(View.VISIBLE);
                }
            }

        } else {
            //TODO: Add No Config msg & get Config button
            // Read config
            if (BluetoothLeService.gattCommandCharacteristic != null) {
                BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, WLQ_BASE.GET_CONFIG_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
            }
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
                            if (MotorcycleData.wlq != null) {
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                outputStream.write(MotorcycleData.wlq.WRITE_CONFIG_CMD());
                                outputStream.write(MotorcycleData.wlq.getDefaultConfig());
                                outputStream.write(MotorcycleData.wlq.CMD_EOM());
                                byte[] writeConfigCmd = outputStream.toByteArray();
                                Log.d(TAG, "Reset Command Sent: " + Utils.ByteArrayToHex(writeConfigCmd));
                                BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, writeConfigCmd, BluetoothLeService.WriteType.WITH_RESPONSE);
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
                        try {
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            outputStream.write(MotorcycleData.wlq.WRITE_CONFIG_CMD());
                            outputStream.write(MotorcycleData.wlq.getTempConfig());
                            outputStream.write(MotorcycleData.wlq.CMD_EOM());
                            byte[] writeConfigCmd = outputStream.toByteArray();
                            BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, writeConfigCmd, BluetoothLeService.WriteType.WITH_RESPONSE);
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
            if (BluetoothLeService.ACTION_CMDSTATUS_AVAILABLE.equals(action)) {
                updateDisplay();
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_CMDSTATUS_AVAILABLE);
        return intentFilter;
    }
}
