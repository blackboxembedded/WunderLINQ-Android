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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_C;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_N;
import com.blackboxembedded.WunderLINQ.protocols.KeyboardHID;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HWSettingsActionActivity extends AppCompatActivity {

    private final static String TAG = "HWSettingsActionAct";

    private int actionID = -1;

    private TextView actionLabelTV;
    private Spinner actionTypeSP;
    private Spinner actionKeySP;
    private MultiSpinner actionModifiersSP;
    private Button saveBT;
    private Button cancelBT;
    private ArrayAdapter<String> keymodes;
    private ArrayAdapter<String> usb;
    private ArrayAdapter<Integer> sensitivity;
    private ArrayAdapter<String> types;
    private ArrayAdapter<String> keyboard;
    private ArrayAdapter<String> consumer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hwsettings_action);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            actionID = extras.getInt("ACTIONID");
            Log.d(TAG,"Editing Action: " + actionID);
        }

        actionLabelTV = findViewById(R.id.tvActionLabel);
        actionTypeSP = findViewById(R.id.spType);
        actionKeySP = findViewById(R.id.spKey);
        actionModifiersSP = findViewById(R.id.msModifiers);
        saveBT = findViewById(R.id.btSave);
        cancelBT = findViewById(R.id.btCancel);

        keymodes = new ArrayAdapter<String>(this,
                R.layout.item_hwsettings_spinners, new String[]{getResources().getString(R.string.keymode_default_label),
                getResources().getString(R.string.keymode_custom_label),
                getResources().getString(R.string.keymode_media_label),
                getResources().getString(R.string.keymode_dmd2_label)});

        usb = new ArrayAdapter<String>(this,
                R.layout.item_hwsettings_spinners, new String[]{getResources().getString(R.string.usbcontrol_on_label),
                getResources().getString(R.string.usbcontrol_engine_label),
                getResources().getString(R.string.usbcontrol_off_label)});

        types = new ArrayAdapter<String>(this,
                R.layout.item_hwsettings_spinners, getResources().getStringArray(R.array.hid_type_names_array));
        keyboard = new ArrayAdapter<String>(this,
                R.layout.item_hwsettings_spinners, getResources().getStringArray(R.array.hid_keyboard_usage_table_names_array));
        consumer = new ArrayAdapter<String>(this,
                R.layout.item_hwsettings_spinners, getResources().getStringArray(R.array.hid_consumer_usage_table_names_array));

        actionTypeSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                if (actionID == WLQ_N.KEYMODE || actionID == WLQ_C.KEYMODE){
                    if(Data.wlq.getKeyMode() != pos){
                        saveBT.setVisibility(View.VISIBLE);
                    } else {
                        saveBT.setVisibility(View.INVISIBLE);
                    }
                } else if (actionID == WLQ_N.USB){
                    if((pos == 0) && (WLQ_N.USBVinThreshold == 0x0000)){
                        saveBT.setVisibility(View.INVISIBLE);
                    } else if((pos == 1) && (WLQ_N.USBVinThreshold != 0x0000) && (WLQ_N.USBVinThreshold != 0xFFFF)){
                        saveBT.setVisibility(View.INVISIBLE);
                    } else if((pos == 2) && (WLQ_N.USBVinThreshold == 0xFFFF)){
                        saveBT.setVisibility(View.INVISIBLE);
                    } else {
                        saveBT.setVisibility(View.VISIBLE);
                    }
                } else if (actionID == WLQ_N.RTKDoublePressSensitivity){
                    if ((pos) == WLQ_N.RTKSensitivity){
                        saveBT.setVisibility(View.INVISIBLE);
                    } else {
                        saveBT.setVisibility(View.VISIBLE);
                    }
                } else if (actionID == WLQ_N.fullLongPressSensitivity){
                    if ((pos) == WLQ_N.fullSensitivity){
                        saveBT.setVisibility(View.INVISIBLE);
                    } else {
                        saveBT.setVisibility(View.VISIBLE);
                    }
                } else if (actionID == WLQ_C.longPressSensitivity){
                    if ((pos) == WLQ_N.fullSensitivity){
                        saveBT.setVisibility(View.INVISIBLE);
                    } else {
                        saveBT.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (pos == Data.wlq.UNDEFINED()){
                        actionKeySP.setVisibility(View.INVISIBLE);
                        actionModifiersSP.setVisibility(View.INVISIBLE);
                        if(Data.wlq.getActionKeyType(actionID) == Data.wlq.UNDEFINED()){
                            saveBT.setVisibility(View.INVISIBLE);
                        } else {
                            saveBT.setVisibility(View.VISIBLE);
                        }
                    } else if (pos == Data.wlq.KEYBOARD_HID()){
                        actionKeySP.setVisibility(View.VISIBLE);
                        actionModifiersSP.setVisibility(View.VISIBLE);
                        int position = actionKeySP.getSelectedItemPosition();
                        actionKeySP.setAdapter(keyboard);
                        actionKeySP.setSelection(position);
                        if(Data.wlq.getActionKeyType(actionID) == Data.wlq.KEYBOARD_HID()){
                            saveBT.setVisibility(View.INVISIBLE);
                        } else {
                            saveBT.setVisibility(View.VISIBLE);
                        }
                    } else if (pos == Data.wlq.CONSUMER_HID()){
                        actionKeySP.setVisibility(View.VISIBLE);
                        actionModifiersSP.setVisibility(View.INVISIBLE);
                        int position = actionKeySP.getSelectedItemPosition();
                        actionKeySP.setAdapter(consumer);
                        actionKeySP.setSelection(position);
                        if(Data.wlq.getActionKeyType(actionID) == Data.wlq.CONSUMER_HID()){
                            saveBT.setVisibility(View.INVISIBLE);
                        } else {
                            saveBT.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        actionKeySP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                if (actionID == WLQ_N.KEYMODE || actionID == WLQ_C.KEYMODE){

                } else if (actionID == WLQ_N.USB){

                } else if (actionID == WLQ_N.RTKDoublePressSensitivity){

                } else if (actionID == WLQ_N.fullLongPressSensitivity){

                } else if (actionID == WLQ_C.longPressSensitivity){

                } else {
                    if (actionTypeSP.getSelectedItemPosition() == Data.wlq.KEYBOARD_HID()) {
                        if (Data.wlq.getActionKeyType(actionID) == Data.wlq.KEYBOARD_HID()) {
                            if (pos == KeyboardHID.getKeyboardKeyPositionByCode(Data.wlq.getActionKey(actionID))) {
                                saveBT.setVisibility(View.INVISIBLE);
                            } else {
                                saveBT.setVisibility(View.VISIBLE);
                            }
                        } else {
                            saveBT.setVisibility(View.VISIBLE);
                        }
                    } else if (actionTypeSP.getSelectedItemPosition() == Data.wlq.CONSUMER_HID()) {
                        if (Data.wlq.getActionKeyType(actionID) == Data.wlq.CONSUMER_HID()) {
                            if (pos == KeyboardHID.getConsumerKeyPositionByCode(Data.wlq.getActionKey(actionID))) {
                                saveBT.setVisibility(View.INVISIBLE);
                            } else {
                                saveBT.setVisibility(View.VISIBLE);
                            }
                        } else {
                            saveBT.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        actionModifiersSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                if (actionID == WLQ_N.OldSensitivity){

                } else if (actionID == WLQ_N.KEYMODE || actionID == WLQ_C.KEYMODE){

                } else if (actionID == WLQ_N.USB){

                } else if (actionID == WLQ_N.RTKDoublePressSensitivity){

                } else if (actionID == WLQ_N.fullLongPressSensitivity){

                } else if (actionID == WLQ_C.longPressSensitivity){

                } else {
                    saveBT.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        saveBT.setOnClickListener(mClickListener);
        cancelBT.setOnClickListener(mClickListener);
        saveBT.setVisibility(View.INVISIBLE);

        showActionBar();

        updateDisplay();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.action_back) {
                // Go back
                Intent backIntent = new Intent(HWSettingsActionActivity.this, HWSettingsActivity.class);
                startActivity(backIntent);
            } else if (v.getId() == R.id.btSave) {
                if (actionID == WLQ_N.KEYMODE || actionID == WLQ_C.KEYMODE){
                    setHWMode(Data.wlq.KEYMODE_DEFAULT());
                    return;
                } else if (actionID == WLQ_N.USB){
                    if(actionTypeSP.getSelectedItemPosition() == 0){
                        Data.wlq.getTempConfig()[WLQ_N.USBVinThresholdHigh_INDEX] = 0x00;
                        Data.wlq.getTempConfig()[WLQ_N.USBVinThresholdLow_INDEX] = 0x00;
                    } else if(actionTypeSP.getSelectedItemPosition() == 1){
                        Data.wlq.getTempConfig()[WLQ_N.USBVinThresholdHigh_INDEX] = 0x02;
                        Data.wlq.getTempConfig()[WLQ_N.USBVinThresholdLow_INDEX] = (byte)0xBC;
                    } else if(actionTypeSP.getSelectedItemPosition() == 2){
                        Data.wlq.getTempConfig()[WLQ_N.USBVinThresholdHigh_INDEX] = (byte)0xFF;
                        Data.wlq.getTempConfig()[WLQ_N.USBVinThresholdLow_INDEX] = (byte)0xFF;
                    }
                } else if (actionID == WLQ_N.RTKDoublePressSensitivity){
                    Data.wlq.getTempConfig()[WLQ_N.RTKSensitivity_INDEX] = (byte)((actionTypeSP.getSelectedItemPosition() + 1) / 50);
                } else if (actionID == WLQ_N.fullLongPressSensitivity){
                    Data.wlq.getTempConfig()[WLQ_N.fullSensitivity_INDEX] = (byte)((actionTypeSP.getSelectedItemPosition() + 1) / 50);
                } else if (actionID == WLQ_C.longPressSensitivity){
                    //Data.wlq.getTempConfig()[WLQ_C.lpSensitivity_INDEX] = (byte)(actionTypeSP.getSelectedItemPosition() + 1);
                } else {
                    byte type = (byte)actionTypeSP.getSelectedItemPosition();
                    byte key = 0x00;
                    byte modifiers = 0x00;
                    if (type == Data.wlq.KEYBOARD_HID()) {
                        key = Integer.decode(getResources().getStringArray(R.array.hid_keyboard_usage_table_codes_array)[actionKeySP.getSelectedItemPosition()]).byteValue();
                        int i = -1;
                        for (boolean cc: actionModifiersSP.selected) {
                            i++;
                            if (cc) {
                                modifiers = (byte) (Integer.decode(getResources().getStringArray(R.array.hid_keyboard_modifier_usage_table_codes_array)[i]).byteValue() + modifiers);
                            }
                        }
                    } else if (type == Data.wlq.CONSUMER_HID()) {
                        key = Integer.decode(getResources().getStringArray(R.array.hid_consumer_usage_table_codes_array)[actionKeySP.getSelectedItemPosition()]).byteValue();
                    }
                    Data.wlq.setActionKey(actionID, type, modifiers, key);
                }
                Intent backIntent = new Intent(HWSettingsActionActivity.this, HWSettingsActivity.class);
                startActivity(backIntent);
            } else if (v.getId() == R.id.btCancel) {
                Intent backIntent = new Intent(HWSettingsActionActivity.this, HWSettingsActivity.class);
                startActivity(backIntent);
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
        navbarTitle.setText(Data.wlq.getActionName(actionID));

        ImageButton backButton = findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton forwardButton = findViewById(R.id.action_forward);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private void updateDisplay(){
        saveBT.setVisibility(View.INVISIBLE);
        actionLabelTV.setText(Data.wlq.getActionName(actionID));
        if (actionID == WLQ_N.KEYMODE || actionID == WLQ_C.KEYMODE){ //Key mode
            actionTypeSP.setAdapter(keymodes);
            actionKeySP.setVisibility(View.INVISIBLE);
            actionModifiersSP.setVisibility(View.INVISIBLE);
            actionTypeSP.setSelection(Data.wlq.getKeyMode());
        } else if (actionID ==  WLQ_N.USB){ //USB
            actionTypeSP.setAdapter(usb);
            actionKeySP.setVisibility(View.INVISIBLE);
            actionModifiersSP.setVisibility(View.INVISIBLE);
            if(WLQ_N.USBVinThreshold == 0x0000){
                actionTypeSP.setSelection(0);
            } else if(WLQ_N.USBVinThreshold == 0xFFFF){
                actionTypeSP.setSelection(2);
            } else if(WLQ_N.USBVinThreshold != 0xFFFF && WLQ_N.USBVinThreshold != 0x0000){
                actionTypeSP.setSelection(1);
            }
        } else if (actionID == WLQ_N.RTKDoublePressSensitivity){  //RTK Sensitivity
            int RTKSensitivityMax = 20;
            Integer[] intArray = new Integer[RTKSensitivityMax];
            for(int i = 0; i < RTKSensitivityMax; i++) {
                intArray[i] = i * 50;
            }
            sensitivity = new ArrayAdapter<Integer>(this,
                    R.layout.item_hwsettings_spinners, intArray);
            actionTypeSP.setAdapter(sensitivity);
            actionKeySP.setVisibility(View.INVISIBLE);
            actionModifiersSP.setVisibility(View.INVISIBLE);
            actionTypeSP.setSelection(WLQ_N.RTKSensitivity);
        } else if (actionID == WLQ_N.fullLongPressSensitivity){  //Full Sensitivity
            int fullSensitivityMax = 30;
            Integer[] intArray = new Integer[fullSensitivityMax];
            for(int i = 0; i < fullSensitivityMax; i++) {
                intArray[i] = i * 50;
            }
            sensitivity = new ArrayAdapter<Integer>(this,
                    R.layout.item_hwsettings_spinners, intArray);
            actionTypeSP.setAdapter(sensitivity);
            actionKeySP.setVisibility(View.INVISIBLE);
            actionModifiersSP.setVisibility(View.INVISIBLE);
            actionTypeSP.setSelection(WLQ_N.fullSensitivity);
            Log.d(TAG,"WLQ_N.fullSensitivity: " + WLQ_N.fullSensitivity);
        } else if (actionID == WLQ_C.longPressSensitivity){
            /*
            int fullSensitivityMax = 30;
            Integer[] intArray = new Integer[fullSensitivityMax];
            for(int i = 0; i < fullSensitivityMax; i++) {
                intArray[i] = i + 1;
            }
            sensitivity = new ArrayAdapter<Integer>(this,
                    R.layout.item_hwsettings_spinners, intArray);
            actionTypeSP.setAdapter(sensitivity);
            actionKeySP.setVisibility(View.INVISIBLE);
            actionModifiersSP.setVisibility(View.INVISIBLE);
            actionTypeSP.setSelection(WLQ_C.sensitivity - 1);

             */
        } else {    // Keys
            if (Data.wlq.getHardwareVersion() != null) {
                if (Data.wlq.getHardwareVersion().equals(WLQ_N.hardwareVersion1)){
                    types = new ArrayAdapter<String>(this,
                            R.layout.item_hwsettings_spinners, getResources().getStringArray(R.array.hw1_hid_type_names_array));
                }
            }
            actionTypeSP.setAdapter(types);
            if (Data.wlq.getActionKeyType(actionID) >= types.getCount()){
                actionTypeSP.setSelection(0);
            } else {
                actionTypeSP.setSelection(Data.wlq.getActionKeyType(actionID));
            }

            updateModifierSpinner1(Data.wlq.getActionKeyModifiers(actionID));
            if (Data.wlq.getActionKeyType(actionID) == WLQ_N.KEYBOARD_HID) {
                actionKeySP.setAdapter(keyboard);
                actionKeySP.setSelection(KeyboardHID.getKeyboardKeyPositionByCode(Data.wlq.getActionKey(actionID)));
                actionModifiersSP.setVisibility(View.VISIBLE);
            } else if (Data.wlq.getActionKeyType(actionID) == WLQ_N.CONSUMER_HID) {
                actionKeySP.setAdapter(consumer);
                actionKeySP.setSelection(KeyboardHID.getConsumerKeyPositionByCode(Data.wlq.getActionKey(actionID)));
                actionModifiersSP.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void updateModifierSpinner1(byte mask){
        if (mask != 0x00) {
            if (Utils.isSet(mask, (byte)0x01)) {
                actionModifiersSP.selected[KeyboardHID.getModifierKeyPositionByCode((byte) 0x01)] = true;
            }
            if (Utils.isSet(mask, (byte)0x02)) {
                actionModifiersSP.selected[KeyboardHID.getModifierKeyPositionByCode((byte) 0x02)] = true;
            }
            if (Utils.isSet(mask, (byte)0x04)) {
                actionModifiersSP.selected[KeyboardHID.getModifierKeyPositionByCode((byte) 0x04)] = true;
            }
            if (Utils.isSet(mask, (byte)0x08)) {
                actionModifiersSP.selected[KeyboardHID.getModifierKeyPositionByCode((byte) 0x08)] = true;
            }
            if (Utils.isSet(mask, (byte)0x10)) {
                actionModifiersSP.selected[KeyboardHID.getModifierKeyPositionByCode((byte) 0x10)] = true;
            }
            if (Utils.isSet(mask, (byte)0x20)) {
                actionModifiersSP.selected[KeyboardHID.getModifierKeyPositionByCode((byte) 0x20)] = true;
            }
            if (Utils.isSet(mask, (byte)0x40)) {
                actionModifiersSP.selected[KeyboardHID.getModifierKeyPositionByCode((byte) 0x40)] = true;
            }
            if (Utils.isSet(mask, (byte)0x80)) {
                actionModifiersSP.selected[KeyboardHID.getModifierKeyPositionByCode((byte) 0x80)] = true;
            }
            actionModifiersSP.updateText();
        }
    }

    private void setHWMode(byte mode){
        Log.d(TAG,"setHWMode()");
        // Display dialog
        final AlertDialog.Builder resetBuilder = new AlertDialog.Builder(HWSettingsActionActivity.this);
        resetBuilder.setTitle(getString(R.string.hwsave_alert_title));
        resetBuilder.setMessage(getString(R.string.hwsave_alert_body));
        resetBuilder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            outputStream.write(Data.wlq.WRITE_MODE_CMD());
                            outputStream.write(mode);
                            outputStream.write(Data.wlq.CMD_EOM());
                            byte[] writeConfigCmd = outputStream.toByteArray();
                            BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, writeConfigCmd, BluetoothLeService.WriteType.WITH_RESPONSE);
                        } catch (IOException e) {
                            Log.d(TAG, e.toString());
                        }
                        finish();
                        Intent backIntent = new Intent(HWSettingsActionActivity.this, MainActivity.class);
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
}