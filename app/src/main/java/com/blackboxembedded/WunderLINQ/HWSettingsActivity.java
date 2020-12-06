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
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

public class HWSettingsActivity extends AppCompatActivity {

    private final static String TAG = "HWSettingsActvity";

    private TextView fwVersionTV;
    private TextView hwKeyModeTV;
    private ConstraintLayout actionOneCL;
    private ConstraintLayout actionTwoCL;
    private ConstraintLayout actionThreeCL;
    private ConstraintLayout actionFourCL;
    private ConstraintLayout actionFiveCL;
    private ConstraintLayout actionSixCL;
    private ConstraintLayout actionSevenCL;
    private ConstraintLayout actionEightCL;
    private ConstraintLayout actionNineCL;
    private ConstraintLayout actionTenCL;
    private ConstraintLayout actionElevenCL;
    private ConstraintLayout actionTwelveCL;
    private ConstraintLayout action13CL;
    private ConstraintLayout action14CL;
    private ConstraintLayout action15CL;
    private ConstraintLayout action16CL;
    private ConstraintLayout action17CL;
    private ConstraintLayout action18CL;
    private ConstraintLayout action19CL;
    private ConstraintLayout action20CL;
    private ConstraintLayout action21CL;
    private ConstraintLayout action22CL;
    private TextView actionOneLabelTV;
    private TextView actionTwoLabelTV;
    private TextView actionThreeLabelTV;
    private TextView actionFourLabelTV;
    private TextView actionFiveLabelTV;
    private TextView actionSixLabelTV;
    private TextView actionSevenLabelTV;
    private TextView actionEightLabelTV;
    private TextView actionNineLabelTV;
    private TextView actionTenLabelTV;
    private TextView actionElevenLabelTV;
    private TextView actionTwelveLabelTV;
    private TextView action13LabelTV;
    private TextView action14LabelTV;
    private TextView action15LabelTV;
    private TextView action16LabelTV;
    private TextView action17LabelTV;
    private TextView action18LabelTV;
    private TextView action19LabelTV;
    private TextView action20LabelTV;
    private TextView action21LabelTV;
    private TextView action22LabelTV;
    private TextView actionOneActionTV;
    private TextView actionTwoActionTV;
    private TextView actionThreeActionTV;
    private TextView actionFourActionTV;
    private TextView actionFiveActionTV;
    private TextView actionSixActionTV;
    private TextView actionSevenActionTV;
    private TextView actionEightActionTV;
    private TextView actionNineActionTV;
    private TextView actionTenActionTV;
    private TextView actionElevenActionTV;
    private TextView actionTwelveActionTV;
    private TextView action13ActionTV;
    private TextView action14ActionTV;
    private TextView action15ActionTV;
    private TextView action16ActionTV;
    private TextView action17ActionTV;
    private TextView action18ActionTV;
    private TextView action19ActionTV;
    private TextView action20ActionTV;
    private TextView action21ActionTV;
    private TextView action22ActionTV;
    private Button hwConfigBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hwsettings);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        fwVersionTV = findViewById(R.id.tvFWVersion);
        hwKeyModeTV = findViewById(R.id.tvHWKeyMode);

        actionOneCL = findViewById(R.id.clActionOne);
        actionTwoCL = findViewById(R.id.clActionTwo);
        actionThreeCL = findViewById(R.id.clActionThree);
        actionFourCL = findViewById(R.id.clActionFour);
        actionFiveCL = findViewById(R.id.clActionFive);
        actionSixCL = findViewById(R.id.clActionSix);
        actionSevenCL = findViewById(R.id.clActionSeven);
        actionEightCL = findViewById(R.id.clActionEight);
        actionNineCL = findViewById(R.id.clActionNine);
        actionTenCL = findViewById(R.id.clActionTen);
        actionElevenCL = findViewById(R.id.clActionEleven);
        actionTwelveCL = findViewById(R.id.clActionTwelve);
        action13CL = findViewById(R.id.clAction13);
        action14CL = findViewById(R.id.clAction14);
        action15CL = findViewById(R.id.clAction15);
        action16CL = findViewById(R.id.clAction16);
        action17CL = findViewById(R.id.clAction17);
        action18CL = findViewById(R.id.clAction18);
        action19CL = findViewById(R.id.clAction19);
        action20CL = findViewById(R.id.clAction20);
        action21CL = findViewById(R.id.clAction21);
        action22CL = findViewById(R.id.clAction22);
        actionOneLabelTV = findViewById(R.id.tvActionOneLabel);
        actionTwoLabelTV = findViewById(R.id.tvActionTwoLabel);
        actionThreeLabelTV = findViewById(R.id.tvActionThreeLabel);
        actionFourLabelTV = findViewById(R.id.tvActionFourLabel);
        actionFiveLabelTV = findViewById(R.id.tvActionFiveLabel);
        actionSixLabelTV = findViewById(R.id.tvActionSixLabel);
        actionSevenLabelTV = findViewById(R.id.tvActionSevenLabel);
        actionEightLabelTV = findViewById(R.id.tvActionEightLabel);
        actionNineLabelTV = findViewById(R.id.tvActionNineLabel);
        actionTenLabelTV = findViewById(R.id.tvActionTenLabel);
        actionElevenLabelTV = findViewById(R.id.tvActionElevenLabel);
        actionTwelveLabelTV = findViewById(R.id.tvActionTwelveLabel);
        action13LabelTV = findViewById(R.id.tvAction13Label);
        action14LabelTV = findViewById(R.id.tvAction14Label);
        action15LabelTV = findViewById(R.id.tvAction15Label);
        action16LabelTV = findViewById(R.id.tvAction16Label);
        action17LabelTV = findViewById(R.id.tvAction17Label);
        action18LabelTV = findViewById(R.id.tvAction18Label);
        action19LabelTV = findViewById(R.id.tvAction19Label);
        action20LabelTV = findViewById(R.id.tvAction20Label);
        action21LabelTV = findViewById(R.id.tvAction21Label);
        action22LabelTV = findViewById(R.id.tvAction22Label);
        actionOneActionTV = findViewById(R.id.tvActionOneAction);
        actionTwoActionTV = findViewById(R.id.tvActionTwoAction);
        actionThreeActionTV = findViewById(R.id.tvActionThreeAction);
        actionFourActionTV = findViewById(R.id.tvActionFourAction);
        actionFiveActionTV = findViewById(R.id.tvActionFiveAction);
        actionSixActionTV = findViewById(R.id.tvActionSixAction);
        actionSevenActionTV = findViewById(R.id.tvActionSevenAction);
        actionEightActionTV = findViewById(R.id.tvActionEightAction);
        actionNineActionTV = findViewById(R.id.tvActionNineAction);
        actionTenActionTV = findViewById(R.id.tvActionTenAction);
        actionElevenActionTV = findViewById(R.id.tvActionElevenAction);
        actionTwelveActionTV = findViewById(R.id.tvActionTwelveAction);
        action13ActionTV = findViewById(R.id.tvAction13Action);
        action14ActionTV = findViewById(R.id.tvAction14Action);
        action15ActionTV = findViewById(R.id.tvAction15Action);
        action16ActionTV = findViewById(R.id.tvAction16Action);
        action17ActionTV = findViewById(R.id.tvAction17Action);
        action18ActionTV = findViewById(R.id.tvAction18Action);
        action19ActionTV = findViewById(R.id.tvAction19Action);
        action20ActionTV = findViewById(R.id.tvAction20Action);
        action21ActionTV = findViewById(R.id.tvAction21Action);
        action22ActionTV = findViewById(R.id.tvAction22Action);
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
            MainActivity.gattCommandCharacteristic.setValue(FWConfig.GET_CONFIG_CMD);
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

    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btnHWConfig) {
                if (hwConfigBtn.getText().equals(getString(R.string.reset_btn_label))) {
                    resetHWConfig();
                } else if (hwConfigBtn.getText().equals(getString(R.string.customize_btn_label))) {
                    // Customize
                    if (Double.parseDouble(FWConfig.firmwareVersion) >= 2.0) {
                        if (FWConfig.keyMode == FWConfig.keyMode_custom) {
                            Intent backIntent = new Intent(HWSettingsActivity.this, HWSettingsCustomizeActivity.class);
                            startActivity(backIntent);
                        } else if (FWConfig.keyMode == FWConfig.keyMode_default){
                            // Write mode
                            try {
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                outputStream.write(FWConfig.WRITE_MODE_CMD);
                                outputStream.write(FWConfig.keyMode_custom);
                                outputStream.write(FWConfig.CMD_EOM);
                                byte[] writeModeCmd = outputStream.toByteArray();
                                MainActivity.gattCommandCharacteristic.setValue(writeModeCmd);
                                BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                            } catch (IOException e){
                                Log.d(TAG,e.toString());
                            }
                        }
                    } else {
                        Intent backIntent = new Intent(HWSettingsActivity.this, HWSettingsCustomizeActivity.class);
                        startActivity(backIntent);
                    }
                }
            } else if (v.getId() == R.id.action_back) {
                // Go back
                Intent backIntent = new Intent(HWSettingsActivity.this, MainActivity.class);
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
        navbarTitle.setText(R.string.fw_config_title);

        ImageButton backButton = findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton forwardButton = findViewById(R.id.action_forward);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private void updateDisplay(){
        if (FWConfig.firmwareVersion != null) {
            fwVersionTV.setText(getString(R.string.fw_version_label) + " " + FWConfig.firmwareVersion);
            if (Double.parseDouble(FWConfig.firmwareVersion) >= 2.0) {
                if (FWConfig.keyMode == FWConfig.keyMode_default || FWConfig.keyMode == FWConfig.keyMode_custom) {
                    if (FWConfig.keyMode == FWConfig.keyMode_default) {
                        hwKeyModeTV.setText(getString(R.string.keymode_label) + " " + getString(R.string.keymode_default_label));
                        actionOneLabelTV.setText(getString(R.string.wwMode1));
                        actionOneActionTV.setText("");
                        actionOneLabelTV.setTextSize(25);
                        actionTwoLabelTV.setText(getString(R.string.full_scroll_up_label));
                        actionTwoActionTV.setText(getString(R.string.keyboard_hid_0x52_label));
                        actionThreeLabelTV.setText(getString(R.string.full_scroll_down_label));
                        actionThreeActionTV.setText(getString(R.string.keyboard_hid_0x51_label));
                        actionFourLabelTV.setText(getString(R.string.full_toggle_right_label));
                        actionFourActionTV.setText(getString(R.string.keyboard_hid_0x4F_label));
                        actionFiveLabelTV.setText(getString(R.string.full_toggle_right_label) + " " + getString(R.string.full_long_press_label));
                        actionFiveActionTV.setText(getString(R.string.keyboard_hid_0x28_label));
                        actionSixLabelTV.setText(getString(R.string.full_toggle_left_label));
                        actionSixActionTV.setText(getString(R.string.keyboard_hid_0x50_label));
                        actionSevenLabelTV.setText(getString(R.string.full_toggle_left_label) + " " + getString(R.string.full_long_press_label));
                        actionSevenActionTV.setText(getString(R.string.keyboard_hid_0x29_label));
                        actionEightLabelTV.setText(getString(R.string.full_signal_cancel_label));
                        actionEightActionTV.setText(getString(R.string.consumer_hid_0xB8_label));
                        actionNineLabelTV.setText(getString(R.string.full_signal_cancel_label) + " " + getString(R.string.full_long_press_label));
                        actionNineActionTV.setText(getString(R.string.consumer_hid_0xE2_label));

                        actionTenLabelTV.setText(getString(R.string.wwMode2));
                        actionTenLabelTV.setTextSize(25);
                        actionTenActionTV.setText("");
                        actionElevenLabelTV.setText(getString(R.string.rtk_page_label));
                        actionElevenActionTV.setText(getString(R.string.keyboard_hid_0x4F_label));
                        actionTwelveLabelTV.setText(getString(R.string.rtk_page_label) + " " + getString(R.string.rtk_double_press_label));
                        actionTwelveActionTV.setText(getString(R.string.keyboard_hid_0x28_label));
                        action13LabelTV.setText(getString(R.string.rtk_zoomp_label));
                        action13ActionTV.setText(getString(R.string.keyboard_hid_0x52_label));
                        action14LabelTV.setText(getString(R.string.rtk_zoomm_label));
                        action14ActionTV.setText(getString(R.string.keyboard_hid_0x51_label));
                        action15LabelTV.setText(getString(R.string.rtk_speak_label));
                        action15ActionTV.setText(getString(R.string.keyboard_hid_0x50_label));
                        action16LabelTV.setText(getString(R.string.rtk_speak_label) + " " + getString(R.string.rtk_double_press_label));
                        action16ActionTV.setText(getString(R.string.keyboard_hid_0x29_label));
                        action17LabelTV.setText(getString(R.string.rtk_mute_label));
                        action17ActionTV.setText(getString(R.string.consumer_hid_0xE2_label));
                        action18LabelTV.setText(getString(R.string.rtk_display_label));
                        action18ActionTV.setText(getString(R.string.consumer_hid_0xB8_label));

                        action19CL.setVisibility(View.INVISIBLE);
                        action20CL.setVisibility(View.INVISIBLE);
                        action21CL.setVisibility(View.INVISIBLE);
                        action22CL.setVisibility(View.INVISIBLE);
                    } else {
                        hwKeyModeTV.setText(getString(R.string.keymode_label) + " " + getString(R.string.keymode_custom_label));
                        actionOneLabelTV.setText(getString(R.string.wwMode1));
                        actionOneActionTV.setText("");
                        actionOneLabelTV.setTextSize(25);
                        actionTwoLabelTV.setText(getString(R.string.full_scroll_up_label));
                        if(FWConfig.fullScrollUpKeyType == FWConfig.KEYBOARD_HID){
                            actionTwoActionTV.setText(getKeyboardKeyByCode(FWConfig.fullScrollUpKey));
                        } else if(FWConfig.fullScrollUpKeyType == FWConfig.CONSUMER_HID){
                            actionTwoActionTV.setText(getConsumerKeyByCode(FWConfig.fullScrollUpKey));
                        } else if(FWConfig.fullScrollUpKeyType == FWConfig.UNDEFINED){
                            actionTwoActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        actionThreeLabelTV.setText(getString(R.string.full_scroll_down_label));
                        if(FWConfig.fullScrollDownKeyType == FWConfig.KEYBOARD_HID){
                            actionThreeActionTV.setText(getKeyboardKeyByCode(FWConfig.fullScrollDownKey));
                        } else if(FWConfig.fullScrollDownKeyType == FWConfig.CONSUMER_HID){
                            actionThreeActionTV.setText(getConsumerKeyByCode(FWConfig.fullScrollDownKey));
                        } else if(FWConfig.fullScrollDownKeyType == FWConfig.UNDEFINED){
                            actionThreeActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        actionFourLabelTV.setText(getString(R.string.full_toggle_right_label));
                        if(FWConfig.fullRightPressKeyType == FWConfig.KEYBOARD_HID){
                            actionFourActionTV.setText(getKeyboardKeyByCode(FWConfig.fullRightPressKey));
                        } else if(FWConfig.fullRightPressKeyType == FWConfig.CONSUMER_HID){
                            actionFourActionTV.setText(getConsumerKeyByCode(FWConfig.fullRightPressKey));
                        } else if(FWConfig.fullRightPressKeyType == FWConfig.UNDEFINED){
                            actionFourActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        actionFiveLabelTV.setText(getString(R.string.full_toggle_right_label) + " " + getString(R.string.full_long_press_label));
                        if(FWConfig.fullRightLongPressKeyType == FWConfig.KEYBOARD_HID){
                            actionFiveActionTV.setText(getKeyboardKeyByCode(FWConfig.fullRightLongPressKey));
                        } else if(FWConfig.fullRightLongPressKeyType  == FWConfig.CONSUMER_HID){
                            actionFiveActionTV.setText(getConsumerKeyByCode(FWConfig.fullRightLongPressKey));
                        } else if(FWConfig.fullRightLongPressKeyType == FWConfig.UNDEFINED){
                            actionFiveActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        actionSixLabelTV.setText(getString(R.string.full_toggle_left_label));
                        if(FWConfig.fullLeftPressKeyType == FWConfig.KEYBOARD_HID){
                            actionSixActionTV.setText(getKeyboardKeyByCode(FWConfig.fullLeftPressKey));
                        } else if(FWConfig.fullLeftPressKeyType == FWConfig.CONSUMER_HID){
                            actionSixActionTV.setText(getConsumerKeyByCode(FWConfig.fullLeftPressKey));
                        } else if(FWConfig.fullLeftPressKeyType == FWConfig.UNDEFINED){
                            actionSixActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        actionSevenLabelTV.setText(getString(R.string.full_toggle_left_label) + " " + getString(R.string.full_long_press_label));
                        if(FWConfig.fullLeftLongPressKeyType == FWConfig.KEYBOARD_HID){
                            actionSevenActionTV.setText(getKeyboardKeyByCode(FWConfig.fullLeftLongPressKey));
                        } else if(FWConfig.fullLeftLongPressKeyType  == FWConfig.CONSUMER_HID){
                            actionSevenActionTV.setText(getConsumerKeyByCode(FWConfig.fullLeftLongPressKey));
                        } else if(FWConfig.fullLeftLongPressKeyType == FWConfig.UNDEFINED){
                            actionSevenActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        actionEightLabelTV.setText(getString(R.string.full_signal_cancel_label));
                        if(FWConfig.fullSignalPressKeyType == FWConfig.KEYBOARD_HID){
                            actionEightActionTV.setText(getKeyboardKeyByCode(FWConfig.fullSignalPressKey));
                        } else if(FWConfig.fullSignalPressKeyType == FWConfig.CONSUMER_HID){
                            actionEightActionTV.setText(getConsumerKeyByCode(FWConfig.fullSignalPressKey));
                        } else if(FWConfig.fullSignalPressKeyType == FWConfig.UNDEFINED){
                            actionEightActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        actionNineLabelTV.setText(getString(R.string.full_signal_cancel_label) + " " + getString(R.string.full_long_press_label));
                        if(FWConfig.fullSignalLongPressKeyType == FWConfig.KEYBOARD_HID){
                            actionNineActionTV.setText(getKeyboardKeyByCode(FWConfig.fullSignalLongPressKey));
                        } else if(FWConfig.fullSignalLongPressKeyType == FWConfig.CONSUMER_HID){
                            actionNineActionTV.setText(getConsumerKeyByCode(FWConfig.fullSignalLongPressKey));
                        } else if(FWConfig.fullSignalLongPressKeyType == FWConfig.UNDEFINED){
                            actionNineActionTV.setText(getString(R.string.hid_0x00_label));
                        }

                        actionTenLabelTV.setText(getString(R.string.wwMode2));
                        actionTenLabelTV.setTextSize(25);
                        actionTenActionTV.setText("");
                        actionElevenLabelTV.setText(getString(R.string.rtk_page_label));
                        if (FWConfig.RTKPagePressKeyType == FWConfig.KEYBOARD_HID) {
                            actionElevenActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKPagePressKey));
                        } else if (FWConfig.RTKPagePressKeyType == FWConfig.CONSUMER_HID) {
                            actionElevenActionTV.setText(getConsumerKeyByCode(FWConfig.RTKPagePressKey));
                        } else if (FWConfig.RTKPagePressKeyType == FWConfig.UNDEFINED){
                            actionElevenActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        actionTwelveLabelTV.setText(getString(R.string.rtk_page_label) + " " + getString(R.string.rtk_double_press_label));
                        if (FWConfig.RTKPageDoublePressKeyType == FWConfig.KEYBOARD_HID) {
                            actionTwelveActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKPageDoublePressKey));
                        } else if (FWConfig.RTKPageDoublePressKeyType == FWConfig.CONSUMER_HID) {
                            actionTwelveActionTV.setText(getConsumerKeyByCode(FWConfig.RTKPageDoublePressKey));
                        } else if (FWConfig.RTKPageDoublePressKeyType == FWConfig.UNDEFINED){
                            actionTwelveActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action13LabelTV.setText(getString(R.string.rtk_zoomp_label));
                        if (FWConfig.RTKZoomPPressKeyType == FWConfig.KEYBOARD_HID) {
                            action13ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKZoomPPressKey));
                        } else if (FWConfig.RTKZoomPPressKeyType == FWConfig.CONSUMER_HID) {
                            action13ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKZoomPPressKey));
                        } else if (FWConfig.RTKZoomPPressKeyType == FWConfig.UNDEFINED){
                            action13ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action14LabelTV.setText(getString(R.string.rtk_zoomp_label) + " " + getString(R.string.rtk_double_press_label));
                        if (FWConfig.RTKZoomPDoublePressKeyType == FWConfig.KEYBOARD_HID) {
                            action14ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKZoomPDoublePressKey));
                        } else if (FWConfig.RTKZoomPDoublePressKeyType == FWConfig.CONSUMER_HID) {
                            action14ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKZoomPDoublePressKey));
                        } else if (FWConfig.RTKZoomPDoublePressKeyType == FWConfig.UNDEFINED){
                            action14ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action15LabelTV.setText(getString(R.string.rtk_zoomm_label));
                        if(FWConfig.RTKZoomMPressKeyType == FWConfig.KEYBOARD_HID){
                            action15ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKZoomMPressKey));
                        } else if(FWConfig.RTKZoomMPressKeyType == FWConfig.CONSUMER_HID){
                            action15ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKZoomMPressKey));
                        } else if (FWConfig.RTKZoomMPressKeyType == FWConfig.UNDEFINED){
                            action15ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action16LabelTV.setText(getString(R.string.rtk_zoomm_label) + " " + getString(R.string.rtk_double_press_label));
                        if(FWConfig.RTKZoomMDoublePressKeyType == FWConfig.KEYBOARD_HID){
                            action16ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKZoomMDoublePressKey));
                        } else if(FWConfig.RTKZoomMDoublePressKeyType == FWConfig.CONSUMER_HID){
                            action16ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKZoomMDoublePressKey));
                        } else if (FWConfig.RTKZoomMDoublePressKeyType == FWConfig.UNDEFINED){
                            action16ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action17LabelTV.setText(getString(R.string.rtk_speak_label));
                        if(FWConfig.RTKSpeakPressKeyType == FWConfig.KEYBOARD_HID){
                            action17ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKSpeakPressKey));
                        } else if(FWConfig.RTKSpeakPressKeyType == FWConfig.CONSUMER_HID){
                            action17ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKSpeakPressKey));
                        } else if (FWConfig.RTKSpeakPressKeyType == FWConfig.UNDEFINED){
                            action17ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action18LabelTV.setText(getString(R.string.rtk_speak_label) + " " + getString(R.string.rtk_double_press_label));
                        if(FWConfig.RTKSpeakDoublePressKeyType == FWConfig.KEYBOARD_HID){
                            action18ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKSpeakDoublePressKey));
                        } else if(FWConfig.RTKSpeakDoublePressKeyType == FWConfig.CONSUMER_HID){
                            action18ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKSpeakDoublePressKey));
                        } else if (FWConfig.RTKSpeakDoublePressKeyType == FWConfig.UNDEFINED){
                            action18ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action19LabelTV.setText(getString(R.string.rtk_mute_label));
                        if(FWConfig.RTKMutePressKeyType == FWConfig.KEYBOARD_HID){
                            action19ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKMutePressKey));
                        } else if(FWConfig.RTKMutePressKeyType == FWConfig.CONSUMER_HID){
                            action19ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKMutePressKey));
                        } else if (FWConfig.RTKMutePressKeyType == FWConfig.UNDEFINED){
                            action19ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action20LabelTV.setText(getString(R.string.rtk_mute_label) + " " + getString(R.string.rtk_double_press_label));
                        if(FWConfig.RTKMuteDoublePressKeyType == FWConfig.KEYBOARD_HID){
                            action20ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKMuteDoublePressKey));
                        } else if(FWConfig.RTKMuteDoublePressKeyType == FWConfig.CONSUMER_HID){
                            action20ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKMuteDoublePressKey));
                        } else if (FWConfig.RTKMuteDoublePressKeyType == FWConfig.UNDEFINED){
                            action20ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action21LabelTV.setText(getString(R.string.rtk_display_label));
                        if(FWConfig.RTKDisplayPressKeyType == FWConfig.KEYBOARD_HID){
                            action21ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKDisplayPressKey));
                        } else if(FWConfig.RTKDisplayPressKeyType == FWConfig.CONSUMER_HID){
                            action21ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKDisplayPressKey));
                        } else if (FWConfig.RTKDisplayPressKeyType == FWConfig.UNDEFINED){
                            action21ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                        action22LabelTV.setText(getString(R.string.rtk_display_label) + " " + getString(R.string.rtk_double_press_label));
                        if(FWConfig.RTKDisplayDoublePressKeyType == FWConfig.KEYBOARD_HID){
                            action22ActionTV.setText(getKeyboardKeyByCode(FWConfig.RTKDisplayDoublePressKey));
                        } else if(FWConfig.RTKDisplayDoublePressKeyType == FWConfig.CONSUMER_HID){
                            action22ActionTV.setText(getConsumerKeyByCode(FWConfig.RTKDisplayDoublePressKey));
                        } else if (FWConfig.RTKDisplayDoublePressKeyType == FWConfig.UNDEFINED){
                            action22ActionTV.setText(getString(R.string.hid_0x00_label));
                        }
                    }

                    // Customize
                    hwKeyModeTV.setVisibility(View.VISIBLE);
                    hwConfigBtn.setText(getString(R.string.customize_btn_label));
                } else {
                    // Corrupt Config
                    hwKeyModeTV.setVisibility(View.INVISIBLE);
                    hwConfigBtn.setText(getString(R.string.reset_btn_label));
                }
            } else {
                hwKeyModeTV.setVisibility(View.VISIBLE);
                if (FWConfig.wheelMode == FWConfig.wheelMode_full || FWConfig.wheelMode == FWConfig.wheelMode_rtk) {
                    // Customize
                    if (FWConfig.wheelMode == FWConfig.wheelMode_full) {
                        actionOneLabelTV.setText(getString(R.string.full_scroll_up_label));
                        actionOneActionTV.setText(getString(R.string.keyboard_hid_0x52_label));
                        actionTwoLabelTV.setText(getString(R.string.full_scroll_down_label));
                        actionTwoActionTV.setText(getString(R.string.keyboard_hid_0x51_label));
                        actionThreeLabelTV.setText(getString(R.string.full_toggle_right_label));
                        actionThreeActionTV.setText(getString(R.string.keyboard_hid_0x4F_label));
                        actionFourLabelTV.setText(getString(R.string.full_toggle_right_label) + " " + getString(R.string.full_long_press_label));
                        actionFourActionTV.setText(getString(R.string.keyboard_hid_0x28_label));
                        actionFiveLabelTV.setText(getString(R.string.full_toggle_left_label));
                        actionFiveActionTV.setText(getString(R.string.keyboard_hid_0x50_label));
                        actionSixLabelTV.setText(getString(R.string.full_toggle_left_label) + " " + getString(R.string.full_long_press_label));
                        actionSixActionTV.setText(getString(R.string.keyboard_hid_0x29_label));
                        actionSevenCL.setVisibility(View.INVISIBLE);
                        actionEightCL.setVisibility(View.INVISIBLE);
                        actionNineCL.setVisibility(View.INVISIBLE);
                        actionTenCL.setVisibility(View.INVISIBLE);
                        actionElevenCL.setVisibility(View.INVISIBLE);
                        actionTwelveCL.setVisibility(View.INVISIBLE);
                        action13CL.setVisibility(View.INVISIBLE);
                        action14CL.setVisibility(View.INVISIBLE);
                        action15CL.setVisibility(View.INVISIBLE);
                        action16CL.setVisibility(View.INVISIBLE);
                        action17CL.setVisibility(View.INVISIBLE);
                        action18CL.setVisibility(View.INVISIBLE);
                        action19CL.setVisibility(View.INVISIBLE);
                        action20CL.setVisibility(View.INVISIBLE);
                        action21CL.setVisibility(View.INVISIBLE);
                        action22CL.setVisibility(View.INVISIBLE);
                        hwKeyModeTV.setText(getString(R.string.wwtype_label) + " " + getString(R.string.wwMode1));
                    } else {
                        actionOneLabelTV.setText(getString(R.string.rtk_page_label));
                        actionOneActionTV.setText(getString(R.string.keyboard_hid_0x4F_label));
                        actionTwoLabelTV.setText(getString(R.string.rtk_page_label) + " " + getString(R.string.rtk_double_press_label));
                        actionTwoActionTV.setText(getString(R.string.keyboard_hid_0x28_label));
                        actionThreeLabelTV.setText(getString(R.string.rtk_zoomp_label));
                        actionThreeActionTV.setText(getString(R.string.keyboard_hid_0x52_label));
                        actionFourLabelTV.setText(getString(R.string.rtk_zoomm_label));
                        actionFourActionTV.setText(getString(R.string.keyboard_hid_0x50_label));
                        actionFiveLabelTV.setText(getString(R.string.rtk_speak_label));
                        actionFiveActionTV.setText(getString(R.string.keyboard_hid_0x51_label));
                        actionSixLabelTV.setText(getString(R.string.rtk_speak_label) + " " + getString(R.string.rtk_double_press_label));
                        actionSixActionTV.setText(getString(R.string.keyboard_hid_0x29_label));
                        actionSevenCL.setVisibility(View.INVISIBLE);
                        actionEightCL.setVisibility(View.INVISIBLE);
                        actionNineCL.setVisibility(View.INVISIBLE);
                        actionTenCL.setVisibility(View.INVISIBLE);
                        actionElevenCL.setVisibility(View.INVISIBLE);
                        actionTwelveCL.setVisibility(View.INVISIBLE);
                        action13CL.setVisibility(View.INVISIBLE);
                        action14CL.setVisibility(View.INVISIBLE);
                        action15CL.setVisibility(View.INVISIBLE);
                        action16CL.setVisibility(View.INVISIBLE);
                        action17CL.setVisibility(View.INVISIBLE);
                        action18CL.setVisibility(View.INVISIBLE);
                        action19CL.setVisibility(View.INVISIBLE);
                        action20CL.setVisibility(View.INVISIBLE);
                        action21CL.setVisibility(View.INVISIBLE);
                        action22CL.setVisibility(View.INVISIBLE);
                        hwKeyModeTV.setText(getString(R.string.wwtype_label) + " " + getString(R.string.wwMode2));
                    }
                    hwConfigBtn.setText(getString(R.string.customize_btn_label));
                } else {
                    // Corrupt Config
                    hwConfigBtn.setText(getString(R.string.reset_btn_label));
                }
            }
            hwConfigBtn.setVisibility(View.VISIBLE);
            Log.d(TAG,"Mode: " + FWConfig.wheelMode);
        }
    }

    private void resetHWConfig(){
        // Display dialog
        final AlertDialog.Builder resetBuilder = new AlertDialog.Builder(HWSettingsActivity.this);
        resetBuilder.setTitle(getString(R.string.hwsave_alert_title));
        resetBuilder.setMessage(getString(R.string.hwsave_alert_body));
        resetBuilder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Double.parseDouble(FWConfig.firmwareVersion) >= 2.0) {
                            MainActivity.gattCommandCharacteristic.setValue(FWConfig.defaultConfig2);
                            BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                        } else {
                            MainActivity.gattCommandCharacteristic.setValue(FWConfig.defaultConfig1);
                            BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                        }
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

    private String getKeyboardKeyByCode(byte code) {
        int i = -1;
        for (String cc: getResources().getStringArray(R.array.hid_keyboard_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return getResources().getStringArray(R.array.hid_keyboard_usage_table_names_array)[i];
    }

    private String getConsumerKeyByCode(byte code) {
        int i = -1;
        for (String cc: getResources().getStringArray(R.array.hid_consumer_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return getResources().getStringArray(R.array.hid_consumer_usage_table_names_array)[i];
    }
}
