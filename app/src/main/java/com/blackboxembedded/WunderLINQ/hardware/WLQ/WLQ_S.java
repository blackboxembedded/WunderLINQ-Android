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

import com.blackboxembedded.WunderLINQ.comms.BLE.KeyboardHID;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;

import java.util.Arrays;

public class WLQ_S extends WLQ_BASE {
    //WunderLINQ Switch
    private final static String TAG = "WLQ_S";

    public static String hardwareVersion1 = "WLQS1.0";

    public static byte[] GET_STATUS_CMD = {0x57, 0x52, 0x41, 0x50};

    private static int configFlashSize = 24;
    private static int firmwareVersionMajor_INDEX = 3;
    private static int firmwareVersionMinor_INDEX = 4;

    public static byte[] defaultConfig = {
            0x01, 0x00, 0x52,           // Up, BMW: Scroll Up, Triumph Joystick Up
            0x01, 0x00, 0x51,           // Down, BMW: Scroll Down, Triumph Joystick Down
            0x01, 0x00, 0x50,           // Left, BMW: Wheel Left, Triumph Joystick Left
            0x01, 0x00, 0x4F,           // Right, BMW: Wheel Right, Triumph Joystick Right
            0x01, 0x00, 0x29,           // FX2, Rocker1 Up, Triumph ?
            0x01, 0x00, 0x28,           // FX1, Rocker1 Down, Triumph Joystick In
            0x01, 0x00, 0x57,           // FX4, Rocker2 Up, Triumph ?
            0x01, 0x00, 0x58};          // FX3, Rocker2 Down, Triumph ?

    public static final int KEYMODE = 100;
    public static final int wheelScrollUp = 26;
    public static final int wheelScrollDown = 27;
    public static final int wheelToggleRight = 28;
    public static final int wheelToggleLeft = 30;
    public static final int rocker1Up = 32;
    public static final int rocker1Down = 34;
    public static final int rocker2Up = 36;
    public static final int rocker2Down = 38;

    // Config message
    private static int keyMode_INDEX = 5;

    private static int wheelScrollUpKeyType_INDEX = 0;
    private static int wheelScrollUpKeyModifier_INDEX = 1;
    private static int wheelScrollUpKey_INDEX = 2;
    private static int wheelScrollDownKeyType_INDEX = 3;
    private static int wheelScrollDownKeyModifier_INDEX = 4;
    private static int wheelScrollDownKey_INDEX = 5;
    private static int wheelLeftPressKeyType_INDEX = 6;
    private static int wheelLeftPressKeyModifier_INDEX = 7;
    private static int wheelLeftPressKey_INDEX = 8;
    private static int wheelRightPressKeyType_INDEX = 9;
    private static int wheelRightPressKeyModifier_INDEX = 10;
    private static int wheelRightPressKey_INDEX = 11;
    private static int rocker1UpPressKeyType_INDEX = 12;
    private static int rocker1UpPressKeyModifier_INDEX = 13;
    private static int rocker1UpPressKey_INDEX = 14;
    private static int rocker1DownPressKeyType_INDEX = 15;
    private static int rocker1DownPressKeyModifier_INDEX = 16;
    private static int rocker1DownPressKey_INDEX = 17;
    private static int rocker2UpPressKeyType_INDEX = 18;
    private static int rocker2UpPressKeyModifier_INDEX = 19;
    private static int rocker2UpPressKey_INDEX = 20;
    private static int rocker2DownPressKeyType_INDEX = 21;
    private static int rocker2DownPressKeyModifier_INDEX = 22;
    private static int rocker2DownPressKey_INDEX = 23;

    // PDM Status message
    private static int statusSize = 3;
    public static int ACTIVE_CHAN_INDEX = 0;
    public static int LIN_ACC_CHANNEL1_VAL_RAW_INDEX = 1;
    public static int LIN_ACC_CHANNEL2_VAL_RAW_INDEX = 2;

    private static byte[] wunderLINQStatus;
    public static int activeChannel;
    public static int channel1ValueRaw;
    public static int channel2ValueRaw;

    private static byte[] wunderLINQConfig;
    private static byte[] flashConfig;
    private static byte[] tempConfig;
    private static String firmwareVersion;
    private static String hardwareVersion;
    private static byte keyMode;
    public static byte wheelRightPressKeyType;
    public static byte wheelRightPressKeyModifier;
    public static byte wheelRightPressKey;
    public static byte wheelLeftPressKeyType;
    public static byte wheelLeftPressKeyModifier;
    public static byte wheelLeftPressKey;
    public static byte wheelScrollUpKeyType;
    public static byte wheelScrollUpKeyModifier;
    public static byte wheelScrollUpKey;
    public static byte wheelScrollDownKeyType;
    public static byte wheelScrollDownKeyModifier;
    public static byte wheelScrollDownKey;
    public static byte rocker1UpPressKeyType;
    public static byte rocker1UpPressKeyModifier;
    public static byte rocker1UpPressKey;
    public static byte rocker1DownPressKeyType;
    public static byte rocker1DownPressKeyModifier;
    public static byte rocker1DownPressKey;
    public static byte rocker2UpPressKeyType;
    public static byte rocker2UpPressKeyModifier;
    public static byte rocker2UpPressKey;
    public static byte rocker2DownPressKeyType;
    public static byte rocker2DownPressKeyModifier;
    public static byte rocker2DownPressKey;

    public WLQ_S(byte[] bytes) {
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
            wheelRightPressKeyType = flashConfig[wheelRightPressKeyType_INDEX];
            wheelRightPressKeyModifier = flashConfig[wheelRightPressKeyModifier_INDEX];
            wheelRightPressKey = flashConfig[wheelRightPressKey_INDEX];
            wheelLeftPressKeyType = flashConfig[wheelLeftPressKeyType_INDEX];
            wheelLeftPressKeyModifier = flashConfig[wheelLeftPressKeyModifier_INDEX];
            wheelLeftPressKey = flashConfig[wheelLeftPressKey_INDEX];
            wheelScrollUpKeyType = flashConfig[wheelScrollUpKeyType_INDEX];
            wheelScrollUpKeyModifier = flashConfig[wheelScrollUpKeyModifier_INDEX];
            wheelScrollUpKey = flashConfig[wheelScrollUpKey_INDEX];
            wheelScrollDownKeyType = flashConfig[wheelScrollDownKeyType_INDEX];
            wheelScrollDownKeyModifier = flashConfig[wheelScrollDownKeyModifier_INDEX];
            wheelScrollDownKey = flashConfig[wheelScrollDownKey_INDEX];
            rocker1UpPressKeyType = flashConfig[rocker1UpPressKeyType_INDEX];
            rocker1UpPressKeyModifier = flashConfig[rocker1UpPressKeyModifier_INDEX];
            rocker1UpPressKey = flashConfig[rocker1UpPressKey_INDEX];
            rocker1DownPressKeyType = flashConfig[rocker1DownPressKeyType_INDEX];
            rocker1DownPressKeyModifier = flashConfig[rocker1DownPressKeyModifier_INDEX];
            rocker1DownPressKey = flashConfig[rocker1DownPressKey_INDEX];
            rocker2UpPressKeyType = flashConfig[rocker2UpPressKeyType_INDEX];
            rocker2UpPressKeyModifier = flashConfig[rocker2UpPressKeyModifier_INDEX];
            rocker2UpPressKey = flashConfig[rocker2UpPressKey_INDEX];
            rocker2DownPressKeyType = flashConfig[rocker2DownPressKeyType_INDEX];
            rocker2DownPressKeyModifier = flashConfig[rocker2DownPressKeyModifier_INDEX];
            rocker2DownPressKey = flashConfig[rocker2DownPressKey_INDEX];
        }
    }

    @Override
    public String getActionName(int id){
        switch (id){
            case wheelScrollUp:
                return MyApplication.getContext().getString(R.string.full_scroll_up_label);
            case wheelScrollDown:
                return MyApplication.getContext().getString(R.string.full_scroll_down_label);
            case wheelToggleRight:
                return MyApplication.getContext().getString(R.string.full_toggle_right_label);
            case wheelToggleLeft:
                return MyApplication.getContext().getString(R.string.full_toggle_left_label);
            case rocker1Up:
                return MyApplication.getContext().getString(R.string.full_rocker1_up_label);
            case rocker1Down:
                return MyApplication.getContext().getString(R.string.full_rocker1_down_label);
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
            case wheelToggleLeft:
                return wheelLeftPressKeyType;
            case rocker1Up:
                return rocker1UpPressKeyType;
            case rocker1Down:
                return rocker1DownPressKeyType;
            case rocker2Up:
                return rocker2UpPressKeyType;
            case rocker2Down:
                return rocker2DownPressKeyType;
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
            case wheelToggleLeft:
                return wheelLeftPressKey;
            case rocker1Up:
                return rocker1UpPressKey;
            case rocker1Down:
                return rocker1DownPressKey;
            case rocker2Up:
                return rocker2UpPressKey;
            case rocker2Down:
                return rocker2DownPressKey;
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
            case wheelToggleLeft:
                return wheelLeftPressKeyModifier;
            case rocker1Up:
                return rocker1UpPressKeyModifier;
            case rocker1Down:
                return rocker1DownPressKeyModifier;
            case rocker2Up:
                return rocker2UpPressKeyModifier;
            case rocker2Down:
                return rocker2DownPressKeyModifier;
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
            case wheelToggleLeft:
                tempConfig[wheelLeftPressKeyType_INDEX] = type;
                tempConfig[wheelLeftPressKeyModifier_INDEX] = modifiers;
                tempConfig[wheelLeftPressKey_INDEX] = key;
                break;
            case rocker1Up:
                tempConfig[rocker1UpPressKeyType_INDEX] = type;
                tempConfig[rocker1UpPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker1UpPressKey_INDEX] = key;
                break;
            case rocker1Down:
                tempConfig[rocker1DownPressKeyType_INDEX] = type;
                tempConfig[rocker1DownPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker1DownPressKey_INDEX] = key;
                break;
            case rocker2Up:
                tempConfig[rocker2UpPressKeyType_INDEX] = type;
                tempConfig[rocker2UpPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker2UpPressKey_INDEX] = key;
                break;
            case rocker2Down:
                tempConfig[rocker2DownPressKeyType_INDEX] = type;
                tempConfig[rocker2DownPressKeyModifier_INDEX] = modifiers;
                tempConfig[rocker2DownPressKey_INDEX] = key;
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
        return WLQ.TYPE_S;
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
        channel1ValueRaw = (wunderLINQStatus[LIN_ACC_CHANNEL1_VAL_RAW_INDEX] & 0xFF);
        channel2ValueRaw = (wunderLINQStatus[LIN_ACC_CHANNEL2_VAL_RAW_INDEX] & 0xFF);
    }
}
