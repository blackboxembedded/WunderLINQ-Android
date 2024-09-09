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

import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.comms.BLE.KeyboardHID;

import java.util.Arrays;

public class WLQ_X extends WLQ_BASE {
    // X
    private final static String TAG = "WLQ_X";

    public static String hardwareVersion1 = "1.0";

    public static byte[] GET_STATUS_CMD = {0x57, 0x52, 0x41, 0x50};

    private static int configFlashSize = 19;
    private static int firmwareVersionMajor_INDEX = 0;
    private static int firmwareVersionMinor_INDEX = 1;

    public static byte[] defaultConfig = {
            0x00,             // Orientation
            0x01, 0x00, 0x52, // Up - Arrow Up
            0x01, 0x00, 0x51, // Down - Arrow Down
            0x01, 0x00, 0x50, // Left - Arrow Left
            0x01, 0x00, 0x4F, // Right - Arrow Right
            0x01, 0x00, 0x29, // FX1 - Escape
            0x01, 0x00, 0x28};// FX2 - Enter

    public static final int KEYMODE = 100;
    public static final int ORIENTATION = 101;
    public static final int up = 26;
    public static final int down = 27;
    public static final int right = 28;
    public static final int left = 30;
    public static final int fx1 = 32;
    public static final int fx2 = 36;

    // Config message
    private static int keyMode_INDEX = 5;
    public static int orientation_INDEX = 0;
    private static int upKeyType_INDEX = 1;
    private static int upKeyModifier_INDEX = 2;
    private static int upKey_INDEX = 3;
    private static int downKeyType_INDEX = 4;
    private static int downKeyModifier_INDEX = 5;
    private static int downKey_INDEX = 6;
    private static int leftPressKeyType_INDEX = 7;
    private static int leftPressKeyModifier_INDEX = 8;
    private static int leftPressKey_INDEX = 9;
    private static int rightPressKeyType_INDEX = 10;
    private static int rightPressKeyModifier_INDEX = 11;
    private static int rightPressKey_INDEX = 12;
    private static int fx1PressKeyType_INDEX = 13;
    private static int fx1PressKeyModifier_INDEX = 14;
    private static int fx1PressKey_INDEX = 15;
    private static int fx2PressKeyType_INDEX = 16;
    private static int fx2PressKeyModifier_INDEX = 17;
    private static int fx2PressKey_INDEX = 18;

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
    public static int orientation;
    public static byte rightPressKeyType;
    public static byte rightPressKeyModifier;
    public static byte rightPressKey;
    public static byte leftPressKeyType;
    public static byte leftPressKeyModifier;
    public static byte leftPressKey;
    public static byte upKeyType;
    public static byte upKeyModifier;
    public static byte upKey;
    public static byte downKeyType;
    public static byte downKeyModifier;
    public static byte downKey;
    public static byte fx1PressKeyType;
    public static byte fx1PressKeyModifier;
    public static byte fx1PressKey;
    public static byte fx2PressKeyType;
    public static byte fx2PressKeyModifier;
    public static byte fx2PressKey;

    public WLQ_X(byte[] bytes) {
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
            orientation = flashConfig[orientation_INDEX];
            rightPressKeyType = flashConfig[rightPressKeyType_INDEX];
            rightPressKeyModifier = flashConfig[rightPressKeyModifier_INDEX];
            rightPressKey = flashConfig[rightPressKey_INDEX];
            leftPressKeyType = flashConfig[leftPressKeyType_INDEX];
            leftPressKeyModifier = flashConfig[leftPressKeyModifier_INDEX];
            leftPressKey = flashConfig[leftPressKey_INDEX];
            upKeyType = flashConfig[upKeyType_INDEX];
            upKeyModifier = flashConfig[upKeyModifier_INDEX];
            upKey = flashConfig[upKey_INDEX];
            downKeyType = flashConfig[downKeyType_INDEX];
            downKeyModifier = flashConfig[downKeyModifier_INDEX];
            downKey = flashConfig[downKey_INDEX];
            fx1PressKeyType = flashConfig[fx1PressKeyType_INDEX];
            fx1PressKeyModifier = flashConfig[fx1PressKeyModifier_INDEX];
            fx1PressKey = flashConfig[fx1PressKey_INDEX];
            fx2PressKeyType = flashConfig[fx2PressKeyType_INDEX];
            fx2PressKeyModifier = flashConfig[fx2PressKeyModifier_INDEX];
            fx2PressKey = flashConfig[fx2PressKey_INDEX];
        }
    }

    @Override
    public String getActionName(int id){
        switch (id){
            case ORIENTATION:
                return MyApplication.getContext().getString(R.string.orientation_label);
            case up:
                return MyApplication.getContext().getString(R.string.up_label);
            case down:
                return MyApplication.getContext().getString(R.string.down_label);
            case right:
                return MyApplication.getContext().getString(R.string.right_label);
            case left:
                return MyApplication.getContext().getString(R.string.left_label);
            case fx1:
                return MyApplication.getContext().getString(R.string.fx1_label);
            case fx2:
                return MyApplication.getContext().getString(R.string.fx2_label);
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
            case ORIENTATION:
                switch (orientation){
                    case 1:
                        return MyApplication.getContext().getString(R.string.orientation_180_label);
                    case 2:
                        return MyApplication.getContext().getString(R.string.orientation_270_label);
                    case 4:
                        return MyApplication.getContext().getString(R.string.orientation_90_label);
                    default:
                        return MyApplication.getContext().getString(R.string.orientation_default_label);
                }
            case up:
                if(upKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(upKey));
                } else if(upKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(upKey));
                } else if(upKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case down:
                if(downKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(downKey));
                } else if(downKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(downKey));
                } else if(downKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case right:
                if(rightPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rightPressKey));
                } else if(rightPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rightPressKey));
                } else if(rightPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case left:
                if(leftPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(leftPressKey));
                } else if(leftPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(leftPressKey));
                } else if(leftPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fx1:
                if(fx1PressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(fx1PressKey));
                } else if(fx1PressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(fx1PressKey));
                } else if(fx1PressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fx2:
                if(fx2PressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(fx2PressKey));
                } else if(fx2PressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(fx2PressKey));
                } else if(fx2PressKeyType == UNDEFINED){
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
            case up:
                return upKeyType;
            case down:
                return downKeyType;
            case right:
                return rightPressKeyType;
            case left:
                return leftPressKeyType;
            case fx1:
                return fx1PressKeyType;
            case fx2:
                return fx2PressKeyType;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public byte getActionKey(int id) {
        switch (id) {
            case up:
                return upKey;
            case down:
                return downKey;
            case right:
                return rightPressKey;
            case left:
                return leftPressKey;
            case fx1:
                return fx1PressKey;
            case fx2:
                return fx2PressKey;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public byte getActionKeyModifiers(int id) {
        switch (id) {
            case up:
                return upKeyModifier;
            case down:
                return downKeyModifier;
            case right:
                return rightPressKeyModifier;
            case left:
                return leftPressKeyModifier;
            case fx1:
                return fx1PressKeyModifier;
            case fx2:
                return fx2PressKeyModifier;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public void setActionKey(int id, byte type, byte modifiers, byte key) {
        switch (id) {
            case up:
                tempConfig[upKeyType_INDEX] = type;
                tempConfig[upKeyModifier_INDEX] = modifiers;
                tempConfig[upKey_INDEX] = key;
                break;
            case down:
                tempConfig[downKeyType_INDEX] = type;
                tempConfig[downKeyModifier_INDEX] = modifiers;
                tempConfig[downKey_INDEX] = key;
                break;
            case right:
                tempConfig[rightPressKeyType_INDEX] = type;
                tempConfig[rightPressKeyModifier_INDEX] = modifiers;
                tempConfig[rightPressKey_INDEX] = key;
                break;
            case left:
                tempConfig[leftPressKeyType_INDEX] = type;
                tempConfig[leftPressKeyModifier_INDEX] = modifiers;
                tempConfig[leftPressKey_INDEX] = key;
                break;
            case fx1:
                tempConfig[fx1PressKeyType_INDEX] = type;
                tempConfig[fx1PressKeyModifier_INDEX] = modifiers;
                tempConfig[fx1PressKey_INDEX] = key;
                break;
            case fx2:
                tempConfig[fx2PressKeyType_INDEX] = type;
                tempConfig[fx2PressKeyModifier_INDEX] = modifiers;
                tempConfig[fx2PressKey_INDEX] = key;
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
        return 3;
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
