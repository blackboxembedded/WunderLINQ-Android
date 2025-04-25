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
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_S;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_N;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_U;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_X;

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
            if (actionID == WLQ_N.KEYMODE || actionID == WLQ_X.KEYMODE || actionID == WLQ_S.KEYMODE || actionID == WLQ_U.KEYMODE) {
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
                            actionItems.add(new ActionItem(WLQ_N.KEYMODE, MotorcycleData.wlq.getActionName(WLQ_N.KEYMODE), MotorcycleData.wlq.getActionValue(WLQ_N.KEYMODE))); // Key mode
                            actionItems.add(new ActionItem(WLQ_N.USB, getString(R.string.usb_threshold_label), MotorcycleData.wlq.getActionValue(WLQ_N.USB))); // USB
                            actionItems.add(new ActionItem(-1, getString(R.string.wwMode1), "")); //Full
                            actionItems.add(new ActionItem(WLQ_N.fullLongPressSensitivity, MotorcycleData.wlq.getActionName(WLQ_N.fullLongPressSensitivity), MotorcycleData.wlq.getActionValue(WLQ_N.fullLongPressSensitivity)));
                            actionItems.add(new ActionItem(WLQ_N.fullScrollUp, MotorcycleData.wlq.getActionName(WLQ_N.fullScrollUp), MotorcycleData.wlq.getActionValue(WLQ_N.fullScrollUp)));
                            actionItems.add(new ActionItem(WLQ_N.fullScrollDown, MotorcycleData.wlq.getActionName(WLQ_N.fullScrollDown), MotorcycleData.wlq.getActionValue(WLQ_N.fullScrollDown)));
                            actionItems.add(new ActionItem(WLQ_N.fullToggleRight, MotorcycleData.wlq.getActionName(WLQ_N.fullToggleRight), MotorcycleData.wlq.getActionValue(WLQ_N.fullToggleRight)));
                            actionItems.add(new ActionItem(WLQ_N.fullToggleRightLongPress, MotorcycleData.wlq.getActionName(WLQ_N.fullToggleRightLongPress), MotorcycleData.wlq.getActionValue(WLQ_N.fullToggleRightLongPress)));
                            actionItems.add(new ActionItem(WLQ_N.fullToggleLeft, MotorcycleData.wlq.getActionName(WLQ_N.fullToggleLeft), MotorcycleData.wlq.getActionValue(WLQ_N.fullToggleLeft)));
                            actionItems.add(new ActionItem(WLQ_N.fullToggleLeftLongPress, MotorcycleData.wlq.getActionName(WLQ_N.fullToggleLeftLongPress), MotorcycleData.wlq.getActionValue(WLQ_N.fullToggleLeftLongPress)));
                            actionItems.add(new ActionItem(WLQ_N.fullSignalCancel, MotorcycleData.wlq.getActionName(WLQ_N.fullSignalCancel), MotorcycleData.wlq.getActionValue(WLQ_N.fullSignalCancel)));
                            actionItems.add(new ActionItem(WLQ_N.fullSignalCancelLongPress, MotorcycleData.wlq.getActionName(WLQ_N.fullSignalCancelLongPress), MotorcycleData.wlq.getActionValue(WLQ_N.fullSignalCancelLongPress)));
                            actionItems.add(new ActionItem(-1, getString(R.string.wwMode2), ""));  //RTK1600
                            actionItems.add(new ActionItem(WLQ_N.RTKDoublePressSensitivity, MotorcycleData.wlq.getActionName(WLQ_N.RTKDoublePressSensitivity), MotorcycleData.wlq.getActionValue(WLQ_N.RTKDoublePressSensitivity)));
                            actionItems.add(new ActionItem(WLQ_N.RTKPageDoublePress, MotorcycleData.wlq.getActionName(WLQ_N.RTKPageDoublePress), MotorcycleData.wlq.getActionValue(WLQ_N.RTKPageDoublePress)));
                            actionItems.add(new ActionItem(WLQ_N.RTKZoomPlus, MotorcycleData.wlq.getActionName(WLQ_N.RTKZoomPlus), MotorcycleData.wlq.getActionValue(WLQ_N.RTKZoomPlus)));
                            actionItems.add(new ActionItem(WLQ_N.RTKZoomPlusDoublePress, MotorcycleData.wlq.getActionName(WLQ_N.RTKZoomPlusDoublePress), MotorcycleData.wlq.getActionValue(WLQ_N.RTKZoomPlusDoublePress)));
                            actionItems.add(new ActionItem(WLQ_N.RTKZoomMinus, MotorcycleData.wlq.getActionName(WLQ_N.RTKZoomMinus), MotorcycleData.wlq.getActionValue(WLQ_N.RTKZoomMinus)));
                            actionItems.add(new ActionItem(WLQ_N.RTKZoomMinusDoublePress, MotorcycleData.wlq.getActionName(WLQ_N.RTKZoomMinusDoublePress), MotorcycleData.wlq.getActionValue(WLQ_N.RTKZoomMinusDoublePress)));
                            actionItems.add(new ActionItem(WLQ_N.RTKSpeak, MotorcycleData.wlq.getActionName(WLQ_N.RTKSpeak), MotorcycleData.wlq.getActionValue(WLQ_N.RTKSpeak)));
                            actionItems.add(new ActionItem(WLQ_N.RTKSpeakDoublePress, MotorcycleData.wlq.getActionName(WLQ_N.RTKSpeakDoublePress), MotorcycleData.wlq.getActionValue(WLQ_N.RTKSpeakDoublePress)));
                            actionItems.add(new ActionItem(WLQ_N.RTKMute, MotorcycleData.wlq.getActionName(WLQ_N.RTKMute), MotorcycleData.wlq.getActionValue(WLQ_X.RTKMute)));
                            actionItems.add(new ActionItem(WLQ_N.RTKMuteDoublePress, MotorcycleData.wlq.getActionName(WLQ_N.RTKMuteDoublePress), MotorcycleData.wlq.getActionValue(WLQ_N.RTKMuteDoublePress)));
                            actionItems.add(new ActionItem(WLQ_N.RTKDisplayOff, MotorcycleData.wlq.getActionName(WLQ_N.RTKDisplayOff), MotorcycleData.wlq.getActionValue(WLQ_N.RTKDisplayOff)));
                            actionItems.add(new ActionItem(WLQ_N.RTKDisplayOffDoublePress, MotorcycleData.wlq.getActionName(WLQ_N.RTKDisplayOffDoublePress), MotorcycleData.wlq.getActionValue(WLQ_N.RTKDisplayOffDoublePress)));

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
                    actionItems.add(new ActionItem(WLQ_X.KEYMODE, MotorcycleData.wlq.getActionName(WLQ_X.KEYMODE), MotorcycleData.wlq.getActionValue(WLQ_X.KEYMODE))); // Key mode
                    actionItems.add(new ActionItem(-1, getString(R.string.wwMode1), "")); //Full
                    actionItems.add(new ActionItem(WLQ_X.fullLongPressSensitivity, MotorcycleData.wlq.getActionName(WLQ_X.fullLongPressSensitivity), MotorcycleData.wlq.getActionValue(WLQ_X.fullLongPressSensitivity)));
                    actionItems.add(new ActionItem(WLQ_X.fullScrollUp, MotorcycleData.wlq.getActionName(WLQ_X.fullScrollUp), MotorcycleData.wlq.getActionValue(WLQ_X.fullScrollUp)));
                    actionItems.add(new ActionItem(WLQ_X.fullScrollDown, MotorcycleData.wlq.getActionName(WLQ_X.fullScrollDown), MotorcycleData.wlq.getActionValue(WLQ_X.fullScrollDown)));
                    actionItems.add(new ActionItem(WLQ_X.fullToggleRight, MotorcycleData.wlq.getActionName(WLQ_X.fullToggleRight), MotorcycleData.wlq.getActionValue(WLQ_X.fullToggleRight)));
                    actionItems.add(new ActionItem(WLQ_X.fullToggleRightLongPress, MotorcycleData.wlq.getActionName(WLQ_X.fullToggleRightLongPress), MotorcycleData.wlq.getActionValue(WLQ_X.fullToggleRightLongPress)));
                    actionItems.add(new ActionItem(WLQ_X.fullToggleLeft, MotorcycleData.wlq.getActionName(WLQ_X.fullToggleLeft), MotorcycleData.wlq.getActionValue(WLQ_X.fullToggleLeft)));
                    actionItems.add(new ActionItem(WLQ_X.fullToggleLeftLongPress, MotorcycleData.wlq.getActionName(WLQ_X.fullToggleLeftLongPress), MotorcycleData.wlq.getActionValue(WLQ_X.fullToggleLeftLongPress)));
                    actionItems.add(new ActionItem(WLQ_X.fullSignalCancel, MotorcycleData.wlq.getActionName(WLQ_X.fullSignalCancel), MotorcycleData.wlq.getActionValue(WLQ_X.fullSignalCancel)));
                    actionItems.add(new ActionItem(WLQ_X.fullSignalCancelLongPress, MotorcycleData.wlq.getActionName(WLQ_X.fullSignalCancelLongPress), MotorcycleData.wlq.getActionValue(WLQ_X.fullSignalCancelLongPress)));
                    actionItems.add(new ActionItem(-1, getString(R.string.wwMode2), ""));  //RTK1600
                    actionItems.add(new ActionItem(WLQ_X.RTKDoublePressSensitivity, MotorcycleData.wlq.getActionName(WLQ_X.RTKDoublePressSensitivity), MotorcycleData.wlq.getActionValue(WLQ_X.RTKDoublePressSensitivity)));
                    actionItems.add(new ActionItem(WLQ_X.RTKPage, MotorcycleData.wlq.getActionName(WLQ_X.RTKPage), MotorcycleData.wlq.getActionValue(WLQ_X.RTKPage)));
                    actionItems.add(new ActionItem(WLQ_X.RTKPageDoublePress, MotorcycleData.wlq.getActionName(WLQ_X.RTKPageDoublePress), MotorcycleData.wlq.getActionValue(WLQ_X.RTKPageDoublePress)));
                    actionItems.add(new ActionItem(WLQ_X.RTKZoomPlus, MotorcycleData.wlq.getActionName(WLQ_X.RTKZoomPlus), MotorcycleData.wlq.getActionValue(WLQ_X.RTKZoomPlus)));
                    actionItems.add(new ActionItem(WLQ_X.RTKZoomPlusDoublePress, MotorcycleData.wlq.getActionName(WLQ_X.RTKZoomPlusDoublePress), MotorcycleData.wlq.getActionValue(WLQ_X.RTKZoomPlusDoublePress)));
                    actionItems.add(new ActionItem(WLQ_X.RTKZoomMinus, MotorcycleData.wlq.getActionName(WLQ_X.RTKZoomMinus), MotorcycleData.wlq.getActionValue(WLQ_X.RTKZoomMinus)));
                    actionItems.add(new ActionItem(WLQ_X.RTKZoomMinusDoublePress, MotorcycleData.wlq.getActionName(WLQ_X.RTKZoomMinusDoublePress), MotorcycleData.wlq.getActionValue(WLQ_X.RTKZoomMinusDoublePress)));
                    actionItems.add(new ActionItem(WLQ_X.RTKSpeak, MotorcycleData.wlq.getActionName(WLQ_X.RTKSpeak), MotorcycleData.wlq.getActionValue(WLQ_X.RTKSpeak)));
                    actionItems.add(new ActionItem(WLQ_X.RTKSpeakDoublePress, MotorcycleData.wlq.getActionName(WLQ_X.RTKSpeakDoublePress), MotorcycleData.wlq.getActionValue(WLQ_N.RTKSpeakDoublePress)));
                    actionItems.add(new ActionItem(WLQ_X.RTKMute, MotorcycleData.wlq.getActionName(WLQ_X.RTKMute), MotorcycleData.wlq.getActionValue(WLQ_X.RTKMute)));
                    actionItems.add(new ActionItem(WLQ_X.RTKMuteDoublePress, MotorcycleData.wlq.getActionName(WLQ_X.RTKMuteDoublePress), MotorcycleData.wlq.getActionValue(WLQ_X.RTKMuteDoublePress)));
                    actionItems.add(new ActionItem(WLQ_X.RTKDisplayOff, MotorcycleData.wlq.getActionName(WLQ_X.RTKDisplayOff), MotorcycleData.wlq.getActionValue(WLQ_X.RTKDisplayOff)));
                    actionItems.add(new ActionItem(WLQ_X.RTKDisplayOffDoublePress, MotorcycleData.wlq.getActionName(WLQ_X.RTKDisplayOffDoublePress), MotorcycleData.wlq.getActionValue(WLQ_X.RTKDisplayOffDoublePress)));

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
            } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_S) {
                if (MotorcycleData.wlq.getFirmwareVersion() != null) {
                    fwVersionTV.setText(getString(R.string.fw_version_label) + " " + MotorcycleData.wlq.getFirmwareVersion());
                }
                if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DEFAULT() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()
                        || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_MEDIA() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DMD2()) {
                    actionItems.add(new ActionItem(WLQ_S.KEYMODE, MotorcycleData.wlq.getActionName(WLQ_S.KEYMODE), MotorcycleData.wlq.getActionValue(WLQ_S.KEYMODE))); // Keymode
                    actionItems.add(new ActionItem(WLQ_S.fullLongPressSensitivity, getString(R.string.long_press_label), MotorcycleData.wlq.getActionValue(WLQ_S.fullLongPressSensitivity)));
                    actionItems.add(new ActionItem(WLQ_S.up, MotorcycleData.wlq.getActionName(WLQ_S.up), MotorcycleData.wlq.getActionValue(WLQ_S.up)));
                    actionItems.add(new ActionItem(WLQ_S.upLong, MotorcycleData.wlq.getActionName(WLQ_S.upLong), MotorcycleData.wlq.getActionValue(WLQ_S.upLong)));
                    actionItems.add(new ActionItem(WLQ_S.down, MotorcycleData.wlq.getActionName(WLQ_S.down), MotorcycleData.wlq.getActionValue(WLQ_S.down)));
                    actionItems.add(new ActionItem(WLQ_S.down, MotorcycleData.wlq.getActionName(WLQ_S.downLong), MotorcycleData.wlq.getActionValue(WLQ_S.downLong)));
                    actionItems.add(new ActionItem(WLQ_S.right, MotorcycleData.wlq.getActionName(WLQ_S.right), MotorcycleData.wlq.getActionValue(WLQ_S.right)));
                    actionItems.add(new ActionItem(WLQ_S.rightLong, MotorcycleData.wlq.getActionName(WLQ_S.rightLong), MotorcycleData.wlq.getActionValue(WLQ_S.rightLong)));
                    actionItems.add(new ActionItem(WLQ_S.left, MotorcycleData.wlq.getActionName(WLQ_S.left), MotorcycleData.wlq.getActionValue(WLQ_S.left)));
                    actionItems.add(new ActionItem(WLQ_S.leftLong, MotorcycleData.wlq.getActionName(WLQ_S.leftLong), MotorcycleData.wlq.getActionValue(WLQ_S.leftLong)));
                    actionItems.add(new ActionItem(WLQ_S.fx1, MotorcycleData.wlq.getActionName(WLQ_S.fx1), MotorcycleData.wlq.getActionValue(WLQ_S.fx1)));
                    actionItems.add(new ActionItem(WLQ_S.fx1Long, MotorcycleData.wlq.getActionName(WLQ_S.fx1Long), MotorcycleData.wlq.getActionValue(WLQ_S.fx1Long)));
                    actionItems.add(new ActionItem(WLQ_S.fx2, MotorcycleData.wlq.getActionName(WLQ_S.fx2), MotorcycleData.wlq.getActionValue(WLQ_S.fx2)));
                    actionItems.add(new ActionItem(WLQ_S.fx2Long, MotorcycleData.wlq.getActionName(WLQ_S.fx2Long), MotorcycleData.wlq.getActionValue(WLQ_S.fx2Long)));

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
            } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_U) {
                if (MotorcycleData.wlq.getFirmwareVersion() != null) {
                    fwVersionTV.setText(getString(R.string.fw_version_label) + " " + MotorcycleData.wlq.getFirmwareVersion());
                }
                if (MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_DEFAULT() || MotorcycleData.wlq.getKeyMode() == MotorcycleData.wlq.KEYMODE_CUSTOM()) {
                    actionItems.add(new ActionItem(WLQ_U.KEYMODE, MotorcycleData.wlq.getActionName(WLQ_U.KEYMODE), MotorcycleData.wlq.getActionValue(WLQ_U.KEYMODE)));
                    actionItems.add(new ActionItem(WLQ_U.ORIENTATION, MotorcycleData.wlq.getActionName(WLQ_U.ORIENTATION), MotorcycleData.wlq.getActionValue(WLQ_U.ORIENTATION)));
                    actionItems.add(new ActionItem(WLQ_U.up, MotorcycleData.wlq.getActionName(WLQ_U.up), MotorcycleData.wlq.getActionValue(WLQ_U.up)));
                    actionItems.add(new ActionItem(WLQ_U.down, MotorcycleData.wlq.getActionName(WLQ_U.down), MotorcycleData.wlq.getActionValue(WLQ_U.down)));
                    actionItems.add(new ActionItem(WLQ_U.right, MotorcycleData.wlq.getActionName(WLQ_U.right), MotorcycleData.wlq.getActionValue(WLQ_U.right)));
                    actionItems.add(new ActionItem(WLQ_U.left, MotorcycleData.wlq.getActionName(WLQ_U.left), MotorcycleData.wlq.getActionValue(WLQ_U.left)));
                    actionItems.add(new ActionItem(WLQ_U.fx1, MotorcycleData.wlq.getActionName(WLQ_U.fx1), MotorcycleData.wlq.getActionValue(WLQ_U.fx1)));
                    actionItems.add(new ActionItem(WLQ_U.fx2, MotorcycleData.wlq.getActionName(WLQ_U.fx2), MotorcycleData.wlq.getActionValue(WLQ_U.fx2)));

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
                                if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_N) {
                                    if (MotorcycleData.wlq.getHardwareVersion() != null) {
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        outputStream.write(MotorcycleData.wlq.WRITE_CONFIG_CMD());
                                        if (MotorcycleData.wlq.getHardwareVersion().equals(WLQ_N.hardwareVersion1)) {
                                            outputStream.write(WLQ_N.defaultConfig2HW1);
                                        } else {
                                            outputStream.write(WLQ_N.defaultConfig2);
                                        }
                                        outputStream.write(MotorcycleData.wlq.CMD_EOM());
                                        byte[] writeConfigCmd = outputStream.toByteArray();
                                        Log.d(TAG, "Reset Command Sent: " + Utils.ByteArrayToHex(writeConfigCmd));
                                        BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, writeConfigCmd, BluetoothLeService.WriteType.WITH_RESPONSE);
                                    }
                                } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_X) {
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    outputStream.write(MotorcycleData.wlq.WRITE_CONFIG_CMD());
                                    outputStream.write(WLQ_X.defaultConfig);
                                    outputStream.write(MotorcycleData.wlq.CMD_EOM());
                                    byte[] writeConfigCmd = outputStream.toByteArray();
                                    Log.d(TAG, "Reset Command Sent: " + Utils.ByteArrayToHex(writeConfigCmd));
                                    BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, writeConfigCmd, BluetoothLeService.WriteType.WITH_RESPONSE);
                                } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_S) {
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    outputStream.write(MotorcycleData.wlq.WRITE_CONFIG_CMD());
                                    outputStream.write(WLQ_S.defaultConfig);
                                    outputStream.write(MotorcycleData.wlq.CMD_EOM());
                                    byte[] writeConfigCmd = outputStream.toByteArray();
                                    Log.d(TAG, "Reset Command Sent: " + Utils.ByteArrayToHex(writeConfigCmd));
                                    BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, writeConfigCmd, BluetoothLeService.WriteType.WITH_RESPONSE);
                                } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_U) {
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    outputStream.write(MotorcycleData.wlq.WRITE_CONFIG_CMD());
                                    outputStream.write(WLQ_U.defaultConfig);
                                    outputStream.write(MotorcycleData.wlq.CMD_EOM());
                                    byte[] writeConfigCmd = outputStream.toByteArray();
                                    Log.d(TAG, "Reset Command Sent: " + Utils.ByteArrayToHex(writeConfigCmd));
                                    BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, writeConfigCmd, BluetoothLeService.WriteType.WITH_RESPONSE);
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
                        if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_N) {
                            if (MotorcycleData.wlq.getFirmwareVersion() != null) {
                                if (Double.parseDouble(MotorcycleData.wlq.getFirmwareVersion()) >= 2.0) {
                                    if (!Arrays.equals(MotorcycleData.wlq.getConfig(), MotorcycleData.wlq.getTempConfig())) {
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
                                    }
                                }
                            }
                        } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_X){
                            if (!Arrays.equals(MotorcycleData.wlq.getConfig(), MotorcycleData.wlq.getTempConfig())) {
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
                            }
                        } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_S){
                            if (!Arrays.equals(MotorcycleData.wlq.getConfig(), MotorcycleData.wlq.getTempConfig())) {
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
                            }
                        } else if (MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_U){
                            if (!Arrays.equals(MotorcycleData.wlq.getConfig(), MotorcycleData.wlq.getTempConfig())) {
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
                    updateDisplay();
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
