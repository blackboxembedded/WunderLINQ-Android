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
package com.blackboxembedded.WunderLINQ.hardware.WLQ;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blackboxembedded.WunderLINQ.protocols.KeyboardHID;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;

import java.util.Arrays;

public class WLQ_C extends WLQ_BASE {
    //WunderLINQ Commander
    private final static String TAG = "WLQ_C";

    public static String hardwareVersion1 = "2PCB7.0 11/19/21";

    public static byte[] GET_STATUS_CMD = {0x57, 0x52, 0x41, 0x50};

    private static int configFlashSize = 46;
    private static int firmwareVersionMajor_INDEX = 0;
    private static int firmwareVersionMinor_INDEX = 1;

    public static byte[] defaultConfig = {
            0x02, 0x00,                                         // SP Sensitivity
            0x05, 0x00,                                         // LP Sensitivity
            0x01, 0x00, 0x52,                                   // Scroll Up
            0x01, 0x00, 0x51,                                   // Scroll Down
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29,                 // Wheel Left
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28,                 // Wheel Right
            0x02, 0x00, (byte)0xE9, 0x02, 0x00, (byte)0xB8,     // Rocker1 Up
            0x02, 0x00, (byte)0xEA, 0x02, 0x00, (byte)0xE2,     // Rocker1 Down
            0x02, 0x00, (byte)0xB5, 0x02, 0x00, (byte)0xB0,     // Rocker2 Up
            0x02, 0x00, (byte)0xB6, 0x02, 0x00, (byte)0xB7};    // Rocker2 Down

    public static final int KEYMODE = 100;
    public static final int longPressSensitivity = 25;
    public static final int wheelScrollUp = 26;
    public static final int wheelScrollDown = 27;
    public static final int wheelToggleRight = 28;
    public static final int wheelToggleRightLongPress = 29;
    public static final int wheelToggleLeft = 30;
    public static final int wheelToggleLeftLongPress = 31;
    public static final int rocker1Up = 32;
    public static final int rocker1UpLongPress = 33;
    public static final int rocker1Down = 34;
    public static final int rocker1DownLongPress = 35;
    public static final int rocker2Up = 36;
    public static final int rocker2UpLongPress = 37;
    public static final int rocker2Down = 38;
    public static final int rocker2DownLongPress = 39;

    // Config message
    private static int keyMode_INDEX = 5;
    public static int spSensitivityHigh_INDEX = 0;
    public static int spSensitivityLow_INDEX = 1;
    public static int lpSensitivityHigh_INDEX = 2;
    public static int lpSensitivityLow_INDEX = 3;
    private static int wheelScrollUpKeyType_INDEX = 4;
    private static int wheelScrollUpKeyModifier_INDEX = 5;
    private static int wheelScrollUpKey_INDEX = 6;
    private static int wheelScrollDownKeyType_INDEX = 7;
    private static int wheelScrollDownKeyModifier_INDEX = 8;
    private static int wheelScrollDownKey_INDEX = 9;
    private static int wheelLeftPressKeyType_INDEX = 10;
    private static int wheelLeftPressKeyModifier_INDEX = 11;
    private static int wheelLeftPressKey_INDEX = 12;
    private static int wheelLeftLongPressKeyType_INDEX = 13;
    private static int wheelLeftLongPressKeyModifier_INDEX = 14;
    private static int wheelLeftLongPressKey_INDEX = 15;
    private static int wheelRightPressKeyType_INDEX = 16;
    private static int wheelRightPressKeyModifier_INDEX = 17;
    private static int wheelRightPressKey_INDEX = 18;
    private static int wheelRightLongPressKeyType_INDEX = 19;
    private static int wheelRightLongPressKeyModifier_INDEX = 20;
    private static int wheelRightLongPressKey_INDEX = 21;
    private static int rocker1UpPressKeyType_INDEX = 22;
    private static int rocker1UpPressKeyModifier_INDEX = 23;
    private static int rocker1UpPressKey_INDEX = 24;
    private static int rocker1UpLongPressKeyType_INDEX = 25;
    private static int rocker1UpLongPressKeyModifier_INDEX = 26;
    private static int rocker1UpLongPressKey_INDEX = 27;
    private static int rocker1DownPressKeyType_INDEX = 28;
    private static int rocker1DownPressKeyModifier_INDEX = 29;
    private static int rocker1DownPressKey_INDEX = 30;
    private static int rocker1DownLongPressKeyType_INDEX = 31;
    private static int rocker1DownLongPressKeyModifier_INDEX = 32;
    private static int rocker1DownLongPressKey_INDEX = 33;
    private static int rocker2UpPressKeyType_INDEX = 34;
    private static int rocker2UpPressKeyModifier_INDEX = 35;
    private static int rocker2UpPressKey_INDEX = 36;
    private static int rocker2UpLongPressKeyType_INDEX = 37;
    private static int rocker2UpLongPressKeyModifier_INDEX = 38;
    private static int rocker2UpLongPressKey_INDEX = 39;
    private static int rocker2DownPressKeyType_INDEX = 40;
    private static int rocker2DownPressKeyModifier_INDEX = 41;
    private static int rocker2DownPressKey_INDEX = 42;
    private static int rocker2DownLongPressKeyType_INDEX = 43;
    private static int rocker2DownLongPressKeyModifier_INDEX = 44;
    private static int rocker2DownLongPressKey_INDEX = 45;

    // PDM Status message
    private static int statusSize = 3;
    public static int ACTIVE_CHAN_INDEX = 0;
    public static int LIN_ACC_CHANNEL1_VAL_RAW_INDEX = 1;
    public static int LIN_ACC_CHANNEL2_VAL_RAW_INDEX = 2;

    private static byte[] wunderLINQStatus;
    public static int activeChannel;
    public static int channe1ValueRaw;
    public static int channel2ValueRaw;

    private static byte[] wunderLINQConfig;
    private static byte[] flashConfig;
    private static byte[] tempConfig;
    private static String firmwareVersion;
    private static String hardwareVersion;
    private static byte keyMode;
    private static int spSensitivity;
    public static int lpSensitivity;
    public static byte wheelRightPressKeyType;
    public static byte wheelRightPressKeyModifier;
    public static byte wheelRightPressKey;
    public static byte wheelRightLongPressKeyType;
    public static byte wheelRightLongPressKeyModifier;
    public static byte wheelRightLongPressKey;
    public static byte wheelLeftPressKeyType;
    public static byte wheelLeftPressKeyModifier;
    public static byte wheelLeftPressKey;
    public static byte wheelLeftLongPressKeyType;
    public static byte wheelLeftLongPressKeyModifier;
    public static byte wheelLeftLongPressKey;
    public static byte wheelScrollUpKeyType;
    public static byte wheelScrollUpKeyModifier;
    public static byte wheelScrollUpKey;
    public static byte wheelScrollDownKeyType;
    public static byte wheelScrollDownKeyModifier;
    public static byte wheelScrollDownKey;
    public static byte rocker1UpPressKeyType;
    public static byte rocker1UpPressKeyModifier;
    public static byte rocker1UpPressKey;
    public static byte rocker1UpLongPressKeyType;
    public static byte rocker1UpLongPressKeyModifier;
    public static byte rocker1UpLongPressKey;
    public static byte rocker1DownPressKeyType;
    public static byte rocker1DownPressKeyModifier;
    public static byte rocker1DownPressKey;
    public static byte rocker1DownLongPressKeyType;
    public static byte rocker1DownLongPressKeyModifier;
    public static byte rocker1DownLongPressKey;
    public static byte rocker2UpPressKeyType;
    public static byte rocker2UpPressKeyModifier;
    public static byte rocker2UpPressKey;
    public static byte rocker2UpLongPressKeyType;
    public static byte rocker2UpLongPressKeyModifier;
    public static byte rocker2UpLongPressKey;
    public static byte rocker2DownPressKeyType;
    public static byte rocker2DownPressKeyModifier;
    public static byte rocker2DownPressKey;
    public static byte rocker2DownLongPressKeyType;
    public static byte rocker2DownLongPressKeyModifier;
    public static byte rocker2DownLongPressKey;

    public WLQ_C(byte[] bytes) {
        wunderLINQConfig = new byte[bytes.length];
        System.arraycopy(bytes, 0, wunderLINQConfig, 0, bytes.length);

        Log.d(TAG, "WLQConfig: " + Utils.ByteArrayToHex(wunderLINQConfig));

        byte[] flashConfigPart = new byte[configFlashSize];
        System.arraycopy(bytes, 6, flashConfigPart, 0, configFlashSize);

        if (!Arrays.equals(flashConfig, flashConfigPart)) {
            flashConfig = new byte[configFlashSize];
            System.arraycopy(flashConfigPart, 0, flashConfig, 0, flashConfigPart.length);

            tempConfig = new byte[flashConfig.length];
            System.arraycopy(flashConfig, 0, tempConfig, 0, flashConfig.length);

            Log.d(TAG, "New flashConfig: " + Utils.ByteArrayToHex(flashConfig));

            firmwareVersion = bytes[firmwareVersionMajor_INDEX] + "." + bytes[firmwareVersionMinor_INDEX];

            Log.d(TAG, "Firmware Version: " + firmwareVersion);

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit();
            editor.putString("firmwareVersion", firmwareVersion);
            editor.apply();

            keyMode = bytes[keyMode_INDEX];
            spSensitivity = ((flashConfig[spSensitivityHigh_INDEX] & 0xFF) << 8) | (flashConfig[spSensitivityLow_INDEX] & 0xFF);
            lpSensitivity = ((flashConfig[lpSensitivityHigh_INDEX] & 0xFF) << 8) | (flashConfig[lpSensitivityLow_INDEX] & 0xFF);
            wheelRightPressKeyType = flashConfig[wheelRightPressKeyType_INDEX];
            wheelRightPressKeyModifier = flashConfig[wheelRightPressKeyModifier_INDEX];
            wheelRightPressKey = flashConfig[wheelRightPressKey_INDEX];
            wheelRightLongPressKeyType = flashConfig[wheelRightLongPressKeyType_INDEX];
            wheelRightLongPressKeyModifier = flashConfig[wheelRightLongPressKeyModifier_INDEX];
            wheelRightLongPressKey = flashConfig[wheelRightLongPressKey_INDEX];
            wheelLeftPressKeyType = flashConfig[wheelLeftPressKeyType_INDEX];
            wheelLeftPressKeyModifier = flashConfig[wheelLeftPressKeyModifier_INDEX];
            wheelLeftPressKey = flashConfig[wheelLeftPressKey_INDEX];
            wheelLeftLongPressKeyType = flashConfig[wheelLeftLongPressKeyType_INDEX];
            wheelLeftLongPressKeyModifier = flashConfig[wheelLeftLongPressKeyModifier_INDEX];
            wheelLeftLongPressKey = flashConfig[wheelLeftLongPressKey_INDEX];
            wheelScrollUpKeyType = flashConfig[wheelScrollUpKeyType_INDEX];
            wheelScrollUpKeyModifier = flashConfig[wheelScrollUpKeyModifier_INDEX];
            wheelScrollUpKey = flashConfig[wheelScrollUpKey_INDEX];
            wheelScrollDownKeyType = flashConfig[wheelScrollDownKeyType_INDEX];
            wheelScrollDownKeyModifier = flashConfig[wheelScrollDownKeyModifier_INDEX];
            wheelScrollDownKey = flashConfig[wheelScrollDownKey_INDEX];
            rocker1UpPressKeyType = flashConfig[rocker1UpPressKeyType_INDEX];
            rocker1UpPressKeyModifier = flashConfig[rocker1UpPressKeyModifier_INDEX];
            rocker1UpPressKey = flashConfig[rocker1UpPressKey_INDEX];
            rocker1UpLongPressKeyType = flashConfig[rocker1UpLongPressKeyType_INDEX];
            rocker1UpLongPressKeyModifier = flashConfig[rocker1UpLongPressKeyModifier_INDEX];
            rocker1UpLongPressKey = flashConfig[rocker1UpLongPressKey_INDEX];
            rocker1DownPressKeyType = flashConfig[rocker1DownPressKeyType_INDEX];
            rocker1DownPressKeyModifier = flashConfig[rocker1DownPressKeyModifier_INDEX];
            rocker1DownPressKey = flashConfig[rocker1DownPressKey_INDEX];
            rocker1DownLongPressKeyType = flashConfig[rocker1DownLongPressKeyType_INDEX];
            rocker1DownLongPressKeyModifier = flashConfig[rocker1DownLongPressKeyModifier_INDEX];
            rocker1DownLongPressKey = flashConfig[rocker1DownLongPressKey_INDEX];
            rocker2UpPressKeyType = flashConfig[rocker2UpPressKeyType_INDEX];
            rocker2UpPressKeyModifier = flashConfig[rocker2UpPressKeyModifier_INDEX];
            rocker2UpPressKey = flashConfig[rocker2UpPressKey_INDEX];
            rocker2UpLongPressKeyType = flashConfig[rocker2UpLongPressKeyType_INDEX];
            rocker2UpLongPressKeyModifier = flashConfig[rocker2UpLongPressKeyModifier_INDEX];
            rocker2UpLongPressKey = flashConfig[rocker2UpLongPressKey_INDEX];
            rocker2DownPressKeyType = flashConfig[rocker2DownPressKeyType_INDEX];
            rocker2DownPressKeyModifier = flashConfig[rocker2DownPressKeyModifier_INDEX];
            rocker2DownPressKey = flashConfig[rocker2DownPressKey_INDEX];
            rocker2DownLongPressKeyType = flashConfig[rocker2DownLongPressKeyType_INDEX];
            rocker2DownLongPressKeyModifier = flashConfig[rocker2DownLongPressKeyModifier_INDEX];
            rocker2DownLongPressKey = flashConfig[rocker2DownLongPressKey_INDEX];
        }
    }

    @Override
    public String getActionName(int id){
        switch (id){
            case longPressSensitivity:
                return MyApplication.getContext().getString(R.string.full_long_press_label);
            case wheelScrollUp:
                return MyApplication.getContext().getString(R.string.full_scroll_up_label);
            case wheelScrollDown:
                return MyApplication.getContext().getString(R.string.full_scroll_down_label);
            case wheelToggleRight:
                return MyApplication.getContext().getString(R.string.full_toggle_right_label);
            case wheelToggleRightLongPress:
                return MyApplication.getContext().getString(R.string.full_toggle_right_long_label);
            case wheelToggleLeft:
                return MyApplication.getContext().getString(R.string.full_toggle_left_label);
            case wheelToggleLeftLongPress:
                return MyApplication.getContext().getString(R.string.full_toggle_left_long_label);
            case rocker1Up:
                return MyApplication.getContext().getString(R.string.full_rocker1_up_label);
            case rocker1UpLongPress:
                return MyApplication.getContext().getString(R.string.full_rocker1_up_long_label);
            case rocker1Down:
                return MyApplication.getContext().getString(R.string.full_rocker1_down_label);
            case rocker1DownLongPress:
                return MyApplication.getContext().getString(R.string.full_rocker1_down_long_label);
            default:
                Log.d(TAG, "Unknown ActionID");
                return "";
        }
    }

    @Override
    public String getActionValue(int id){
        switch (id){
            case KEYMODE:
                switch (keyMode){
                    case 0:
                        return MyApplication.getContext().getString(R.string.keymode_default_label);
                    case 1:
                        return MyApplication.getContext().getString(R.string.keymode_custom_label);
                    case 2:
                        return MyApplication.getContext().getString(R.string.keymode_media_label);
                    case 3:
                        return MyApplication.getContext().getString(R.string.keymode_dmd2_label);
                    default:
                        return "";
                }
            case longPressSensitivity:
                return String.valueOf(lpSensitivity);
            case wheelScrollUp:
                if(wheelScrollUpKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(wheelScrollUpKey));
                } else if(wheelScrollUpKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(wheelScrollUpKey));
                } else if(wheelScrollUpKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case wheelScrollDown:
                if(wheelScrollDownKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(wheelScrollDownKey));
                } else if(wheelScrollDownKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(wheelScrollDownKey));
                } else if(wheelScrollDownKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case wheelToggleRight:
                if(wheelRightPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(wheelRightPressKey));
                } else if(wheelRightPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(wheelRightPressKey));
                } else if(wheelRightPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case wheelToggleRightLongPress:
                if(wheelRightLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(wheelRightLongPressKey));
                } else if(wheelRightLongPressKeyType  == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(wheelRightLongPressKey));
                } else if(wheelRightLongPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case wheelToggleLeft:
                if(wheelLeftPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(wheelLeftPressKey));
                } else if(wheelLeftPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(wheelLeftPressKey));
                } else if(wheelLeftPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case wheelToggleLeftLongPress:
                if(wheelLeftLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(wheelLeftLongPressKey));
                } else if(wheelLeftLongPressKeyType  == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(wheelLeftLongPressKey));
                } else if(wheelLeftLongPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rocker1Up:
                if(rocker1UpPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rocker1UpPressKey));
                } else if(rocker1UpPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rocker1UpPressKey));
                } else if(rocker1UpPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rocker1UpLongPress:
                if(rocker1UpLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rocker1UpLongPressKey));
                } else if(rocker1UpLongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rocker1UpLongPressKey));
                } else if(rocker1UpLongPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rocker1Down:
                if(rocker1DownPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rocker1DownPressKey));
                } else if(rocker1DownPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rocker1DownPressKey));
                } else if(rocker1DownPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rocker1DownLongPress:
                if(rocker1DownLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rocker1DownLongPressKey));
                } else if(rocker1DownLongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rocker1DownLongPressKey));
                } else if(rocker1DownLongPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rocker2Up:
                if(rocker2UpPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rocker2UpPressKey));
                } else if(rocker2UpPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rocker2UpPressKey));
                } else if(rocker2UpPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rocker2UpLongPress:
                if(rocker2UpLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rocker2UpLongPressKey));
                } else if(rocker2UpLongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rocker2UpLongPressKey));
                } else if(rocker2UpLongPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rocker2Down:
                if(rocker2DownPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rocker2DownPressKey));
                } else if(rocker2DownPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rocker2DownPressKey));
                } else if(rocker2DownPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rocker2DownLongPress:
                if(rocker2DownLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rocker2DownLongPressKey));
                } else if(rocker2DownLongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rocker2DownLongPressKey));
                } else if(rocker2DownLongPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            default:
                Log.d(TAG, "Unknown ActionID");
                return "";
        }
    }

    @Override
    public byte getActionKeyType(int id){
        switch (id){
            case wheelScrollUp:
                return wheelScrollUpKeyType;
            case wheelScrollDown:
                return wheelScrollDownKeyType;
            case wheelToggleRight:
                return wheelRightPressKeyType;
            case wheelToggleRightLongPress:
                return wheelRightLongPressKeyType;
            case wheelToggleLeft:
                return wheelLeftPressKeyType;
            case wheelToggleLeftLongPress:
                return wheelLeftLongPressKeyType;
            case rocker1Up:
                return rocker1UpPressKeyType;
            case rocker1UpLongPress:
                return rocker1UpLongPressKeyType;
            case rocker1Down:
                return rocker1DownPressKeyType;
            case rocker1DownLongPress:
                return rocker1DownLongPressKeyType;
            case rocker2Up:
                return rocker2UpPressKeyType;
            case rocker2UpLongPress:
                return rocker2UpLongPressKeyType;
            case rocker2Down:
                return rocker2DownPressKeyType;
            case rocker2DownLongPress:
                return rocker2DownLongPressKeyType;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public byte getActionKey(int id) {
        switch (id) {
            case wheelScrollUp:
                return wheelScrollUpKey;
            case wheelScrollDown:
                return wheelScrollDownKey;
            case wheelToggleRight:
                return wheelRightPressKey;
            case wheelToggleRightLongPress:
                return wheelRightLongPressKey;
            case wheelToggleLeft:
                return wheelLeftPressKey;
            case wheelToggleLeftLongPress:
                return wheelLeftLongPressKey;
            case rocker1Up:
                return rocker1UpPressKey;
            case rocker1UpLongPress:
                return rocker1UpLongPressKey;
            case rocker1Down:
                return rocker1DownPressKey;
            case rocker1DownLongPress:
                return rocker1DownLongPressKey;
            case rocker2Up:
                return rocker2UpPressKey;
            case rocker2UpLongPress:
                return rocker2UpLongPressKey;
            case rocker2Down:
                return rocker2DownPressKey;
            case rocker2DownLongPress:
                return rocker2DownLongPressKey;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public byte getActionKeyModifiers(int id) {
        switch (id) {
            case wheelScrollUp:
                return wheelScrollUpKeyModifier;
            case wheelScrollDown:
                return wheelScrollDownKeyModifier;
            case wheelToggleRight:
                return wheelRightPressKeyModifier;
            case wheelToggleRightLongPress:
                return wheelRightLongPressKeyModifier;
            case wheelToggleLeft:
                return wheelLeftPressKeyModifier;
            case wheelToggleLeftLongPress:
                return wheelLeftLongPressKeyModifier;
            case rocker1Up:
                return rocker1UpPressKeyModifier;
            case rocker1UpLongPress:
                return rocker1UpLongPressKeyModifier;
            case rocker1Down:
                return rocker1DownPressKeyModifier;
            case rocker1DownLongPress:
                return rocker1DownLongPressKeyModifier;
            case rocker2Up:
                return rocker2UpPressKeyModifier;
            case rocker2UpLongPress:
                return rocker2UpLongPressKeyModifier;
            case rocker2Down:
                return rocker2DownPressKeyModifier;
            case rocker2DownLongPress:
                return rocker2DownLongPressKeyModifier;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public void setActionKey(int id, byte type, byte modifiers, byte key) {
        switch (id) {
            case wheelScrollUp:
                tempConfig[wheelScrollUpKeyType_INDEX] = type;
                tempConfig[wheelScrollUpKeyModifier_INDEX] = modifiers;
                tempConfig[wheelScrollUpKey_INDEX] = key;
                break;
            case wheelScrollDown:
                tempConfig[wheelScrollDownKeyType_INDEX] = type;
                tempConfig[wheelScrollDownKeyModifier_INDEX] = modifiers;
                tempConfig[wheelScrollDownKey_INDEX] = key;
                break;
            case wheelToggleRight:
                tempConfig[wheelRightPressKeyType_INDEX] = type;
                tempConfig[wheelRightPressKeyModifier_INDEX] = modifiers;
                tempConfig[wheelRightPressKey_INDEX] = key;
                break;
            case wheelToggleRightLongPress:
                tempConfig[wheelRightLongPressKeyType_INDEX] = type;
                tempConfig[wheelRightLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[wheelRightLongPressKey_INDEX] = key;
                break;
            case wheelToggleLeft:
                tempConfig[wheelLeftPressKeyType_INDEX] = type;
                tempConfig[wheelLeftPressKeyModifier_INDEX] = modifiers;
                tempConfig[wheelLeftPressKey_INDEX] = key;
                break;
            case wheelToggleLeftLongPress:
                tempConfig[wheelLeftLongPressKeyType_INDEX] = type;
                tempConfig[wheelLeftLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[wheelLeftLongPressKey_INDEX] = key;
                break;
            case rocker1Up:
                tempConfig[rocker1UpPressKeyType_INDEX] = type;
                tempConfig[rocker1UpPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker1UpPressKey_INDEX] = key;
                break;
            case rocker1UpLongPress:
                tempConfig[rocker1UpLongPressKeyType_INDEX] = type;
                tempConfig[rocker1UpLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker1UpLongPressKey_INDEX] = key;
                break;
            case rocker1Down:
                tempConfig[rocker1DownPressKeyType_INDEX] = type;
                tempConfig[rocker1DownPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker1DownPressKey_INDEX] = key;
                break;
            case rocker1DownLongPress:
                tempConfig[rocker1DownLongPressKeyType_INDEX] = type;
                tempConfig[rocker1DownLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker1DownLongPressKey_INDEX] = key;
            case rocker2Up:
                tempConfig[rocker2UpPressKeyType_INDEX] = type;
                tempConfig[rocker2UpPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker2UpPressKey_INDEX] = key;
                break;
            case rocker2UpLongPress:
                tempConfig[rocker2UpLongPressKeyType_INDEX] = type;
                tempConfig[rocker2UpLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker2UpLongPressKey_INDEX] = key;
                break;
            case rocker2Down:
                tempConfig[rocker2DownPressKeyType_INDEX] = type;
                tempConfig[rocker2DownPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker2DownPressKey_INDEX] = key;
                break;
            case rocker2DownLongPress:
                tempConfig[rocker2DownLongPressKeyType_INDEX] = type;
                tempConfig[rocker2DownLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker2DownLongPressKey_INDEX] = key;
                break;
            default:
                Log.d(TAG, "Unknown ActionID");
                break;
        }
    }

    @Override
    public void setHardwareVersion(String version) {
        hardwareVersion = version;
    }

    @Override
    public String getHardwareVersion() {
        return hardwareVersion;
    }

    @Override
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    @Override
    public byte[] getFlash() {
        return wunderLINQConfig;
    }

    @Override
    public byte[] getConfig() {
        return flashConfig;
    }

    @Override
    public byte[] getTempConfig() {
        return tempConfig;
    }

    @Override
    public byte getKeyMode() {
        return keyMode;
    }

    @Override
    public byte[] GET_CONFIG_CMD() {
        return WLQ_BASE.GET_CONFIG_CMD;
    }

    @Override
    public byte[] WRITE_CONFIG_CMD() {
        return WLQ_BASE.WRITE_CONFIG_CMD;
    }

    @Override
    public byte[] WRITE_MODE_CMD() {
        return WLQ_BASE.WRITE_MODE_CMD;
    }

    @Override
    public byte[] CMD_EOM() {
        return WLQ_BASE.CMD_EOM;
    }

    @Override
    public byte KEYMODE_DEFAULT() {
        return WLQ_BASE.KEYMODE_DEFAULT;
    }

    @Override
    public byte KEYMODE_CUSTOM() {
        return WLQ_BASE.KEYMODE_CUSTOM;
    }

    @Override
    public byte KEYMODE_MEDIA() {
        return WLQ_BASE.KEYMODE_MEDIA;
    }

    @Override
    public byte KEYMODE_DMD2() { return WLQ_BASE.KEYMODE_DMD2; }

    @Override
    public byte KEYBOARD_HID() {
        return WLQ_BASE.KEYBOARD_HID;
    }

    @Override
    public byte CONSUMER_HID() {
        return WLQ_BASE.CONSUMER_HID;
    }

    @Override
    public byte UNDEFINED() {
        return WLQ_BASE.UNDEFINED;
    }

    @Override
    public int getHardwareType() {
        return 2;
    }

    @Override
    public byte[] getStatus() {
        return wunderLINQStatus;
    }

    @Override
    public void setStatus(byte[] status) {
        wunderLINQStatus = new byte[statusSize];
        System.arraycopy(status, 4, wunderLINQStatus, 0, statusSize);
        activeChannel = (wunderLINQStatus[ACTIVE_CHAN_INDEX] & 0xFF);
        channe1ValueRaw = (wunderLINQStatus[LIN_ACC_CHANNEL1_VAL_RAW_INDEX] & 0xFF);
        channel2ValueRaw = (wunderLINQStatus[LIN_ACC_CHANNEL2_VAL_RAW_INDEX] & 0xFF);
    }
}
