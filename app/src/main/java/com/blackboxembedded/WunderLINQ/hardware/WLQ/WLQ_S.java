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

    private static int configFlashSize = 37;
    private static int firmwareVersionMajor_INDEX = 3;
    private static int firmwareVersionMinor_INDEX = 4;

    public static byte[] defaultConfig = {
            0x11,                               // Long Press Sensitivity
            0x01, 0x00, 0x52, 0x00, 0x00, 0x00, // Scroll Up - Up Arrow
            0x01, 0x00, 0x51, 0x00, 0x00, 0x00, // Scroll Down - Down Arrow
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29, // Wheel Left - Left Arrow
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28, // Wheel Right - Right Arrow
            0x01, 0x00, 0x29, 0x00, 0x00, 0x00, // Rocker2 Up - FX1
            0x01, 0x00, 0x28, 0x00, 0x00, 0x00, // Rocker2 Down - FX2
    };

    public static final int KEYMODE = 100;
    public static final int fullLongPressSensitivity = 3;
    public static final int up = 26;
    public static final int upLong = 27;
    public static final int down = 28;
    public static final int downLong = 29;
    public static final int right = 30;
    public static final int rightLong = 31;
    public static final int left = 32;
    public static final int leftLong = 33;
    public static final int fx1 = 34;
    public static final int fx1Long = 35;
    public static final int fx2 = 36;
    public static final int fx2Long = 37;

    // Config message
    private static int keyMode_INDEX = 5;

    private static int sensitivity_INDEX = 0;
    private static int upKeyType_INDEX = 1;
    private static int upKeyModifier_INDEX = 2;
    private static int upKey_INDEX = 3;
    private static int upLongKeyType_INDEX = 4;
    private static int upLongKeyModifier_INDEX = 5;
    private static int upLongKey_INDEX = 6;
    private static int downKeyType_INDEX = 7;
    private static int downKeyModifier_INDEX = 8;
    private static int downKey_INDEX = 9;
    private static int downLongKeyType_INDEX = 10;
    private static int downLongKeyModifier_INDEX = 11;
    private static int downLongKey_INDEX = 12;
    private static int leftPressKeyType_INDEX = 13;
    private static int leftPressKeyModifier_INDEX = 14;
    private static int leftPressKey_INDEX = 15;
    private static int leftLongPressKeyType_INDEX = 16;
    private static int leftLongPressKeyModifier_INDEX = 17;
    private static int leftLongPressKey_INDEX = 18;
    private static int rightPressKeyType_INDEX = 19;
    private static int rightPressKeyModifier_INDEX = 20;
    private static int rightPressKey_INDEX = 21;
    private static int rightLongPressKeyType_INDEX = 22;
    private static int rightLongPressKeyModifier_INDEX = 23;
    private static int rightLongPressKey_INDEX = 24;
    private static int fx1PressKeyType_INDEX = 25;
    private static int fx1PressKeyModifier_INDEX = 26;
    private static int fx1PressKey_INDEX = 27;
    private static int fx1LongPressKeyType_INDEX = 28;
    private static int fx1LongPressKeyModifier_INDEX = 29;
    private static int fx1LongPressKey_INDEX = 30;
    private static int fx2PressKeyType_INDEX = 31;
    private static int fx2PressKeyModifier_INDEX = 32;
    private static int fx2PressKey_INDEX = 33;
    private static int fx2LongPressKeyType_INDEX = 34;
    private static int fx2LongPressKeyModifier_INDEX = 35;
    private static int fx2LongPressKey_INDEX = 36;

    // PDM Status message
    private static int statusSize = 6;
    public static int NUM_CHAN_INDEX = 0;
    public static int ACTIVE_CHAN_INDEX = 1;
    public static int LIN_ACC_CHANNEL1_VAL_RAW_INDEX = 2;
    public static int LIN_ACC_CHANNEL2_VAL_RAW_INDEX = 3;
    public static int LIN_ACC_CHANNEL3_VAL_RAW_INDEX = 4;
    public static int LIN_ACC_CHANNEL4_VAL_RAW_INDEX = 5;

    private static byte[] wunderLINQStatus;
    public static int activeChannel;
    public static int channel1ValueRaw;
    public static int channel2ValueRaw;
    public static int channel3ValueRaw;
    public static int channel4ValueRaw;

    private static byte[] wunderLINQConfig;
    private static byte[] flashConfig;
    private static byte[] tempConfig;
    private static String firmwareVersion;
    private static String hardwareVersion;
    private static byte keyMode;
    public static byte sensitivity;
    public static byte rightPressKeyType;
    public static byte rightPressKeyModifier;
    public static byte rightPressKey;
    public static byte rightLongPressKeyType;
    public static byte rightLongPressKeyModifier;
    public static byte rightLongPressKey;
    public static byte leftPressKeyType;
    public static byte leftPressKeyModifier;
    public static byte leftPressKey;
    public static byte leftLongPressKeyType;
    public static byte leftLongPressKeyModifier;
    public static byte leftLongPressKey;
    public static byte upKeyType;
    public static byte upKeyModifier;
    public static byte upKey;
    public static byte upLongKeyType;
    public static byte upLongKeyModifier;
    public static byte upLongKey;
    public static byte downKeyType;
    public static byte downKeyModifier;
    public static byte downKey;
    public static byte downLongKeyType;
    public static byte downLongKeyModifier;
    public static byte downLongKey;
    public static byte fx1PressKeyType;
    public static byte fx1PressKeyModifier;
    public static byte fx1PressKey;
    public static byte fx1LongPressKeyType;
    public static byte fx1LongPressKeyModifier;
    public static byte fx1LongPressKey;
    public static byte fx2PressKeyType;
    public static byte fx2PressKeyModifier;
    public static byte fx2PressKey;
    public static byte fx2LongPressKeyType;
    public static byte fx2LongPressKeyModifier;
    public static byte fx2LongPressKey;

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
            sensitivity = flashConfig[sensitivity_INDEX];
            rightPressKeyType = flashConfig[rightPressKeyType_INDEX];
            rightPressKeyModifier = flashConfig[rightPressKeyModifier_INDEX];
            rightPressKey = flashConfig[rightPressKey_INDEX];
            rightLongPressKeyType = flashConfig[rightLongPressKeyType_INDEX];
            rightLongPressKeyModifier = flashConfig[rightLongPressKeyModifier_INDEX];
            rightLongPressKey = flashConfig[rightLongPressKey_INDEX];
            leftPressKeyType = flashConfig[leftPressKeyType_INDEX];
            leftPressKeyModifier = flashConfig[leftPressKeyModifier_INDEX];
            leftPressKey = flashConfig[leftPressKey_INDEX];
            leftLongPressKeyType = flashConfig[leftLongPressKeyType_INDEX];
            leftLongPressKeyModifier = flashConfig[leftLongPressKeyModifier_INDEX];
            leftLongPressKey = flashConfig[leftLongPressKey_INDEX];
            upKeyType = flashConfig[upKeyType_INDEX];
            upKeyModifier = flashConfig[upKeyModifier_INDEX];
            upKey = flashConfig[upKey_INDEX];
            upLongKeyType = flashConfig[upLongKeyType_INDEX];
            upLongKeyModifier = flashConfig[upLongKeyModifier_INDEX];
            upLongKey = flashConfig[upLongKey_INDEX];
            downKeyType = flashConfig[downKeyType_INDEX];
            downKeyModifier = flashConfig[downKeyModifier_INDEX];
            downKey = flashConfig[downKey_INDEX];
            downLongKeyType = flashConfig[downLongKeyType_INDEX];
            downLongKeyModifier = flashConfig[downLongKeyModifier_INDEX];
            downLongKey = flashConfig[downLongKey_INDEX];
            fx1PressKeyType = flashConfig[fx1PressKeyType_INDEX];
            fx1PressKeyModifier = flashConfig[fx1PressKeyModifier_INDEX];
            fx1PressKey = flashConfig[fx1PressKey_INDEX];
            fx1LongPressKeyType = flashConfig[fx1LongPressKeyType_INDEX];
            fx1LongPressKeyModifier = flashConfig[fx1LongPressKeyModifier_INDEX];
            fx1LongPressKey = flashConfig[fx1LongPressKey_INDEX];
            fx2PressKeyType = flashConfig[fx2PressKeyType_INDEX];
            fx2PressKeyModifier = flashConfig[fx2PressKeyModifier_INDEX];
            fx2PressKey = flashConfig[fx2PressKey_INDEX];
            fx2LongPressKeyType = flashConfig[fx2LongPressKeyType_INDEX];
            fx2LongPressKeyModifier = flashConfig[fx2LongPressKeyModifier_INDEX];
            fx2LongPressKey = flashConfig[fx2LongPressKey_INDEX];
        }
    }

    @Override
    public String getActionName(int id){
        switch (id){
            case fullLongPressSensitivity:
                return MyApplication.getContext().getString(R.string.long_press_label);
            case up:
                return MyApplication.getContext().getString(R.string.up_label);
            case upLong:
                return MyApplication.getContext().getString(R.string.up_long_label);
            case down:
                return MyApplication.getContext().getString(R.string.down_label);
            case downLong:
                return MyApplication.getContext().getString(R.string.down_long_label);
            case right:
                return MyApplication.getContext().getString(R.string.right_label);
            case rightLong:
                return MyApplication.getContext().getString(R.string.right_long_label);
            case left:
                return MyApplication.getContext().getString(R.string.left_label);
            case leftLong:
                return MyApplication.getContext().getString(R.string.left_long_label);
            case fx1:
                return MyApplication.getContext().getString(R.string.fx1_label);
            case fx1Long:
                return MyApplication.getContext().getString(R.string.fx1_long_label);
            case fx2:
                return MyApplication.getContext().getString(R.string.fx2_label);
            case fx2Long:
                return MyApplication.getContext().getString(R.string.fx2_long_label);
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
            case fullLongPressSensitivity:
                return String.valueOf(sensitivity * 50) + "ms";
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
            case upLong:
                if(upLongKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(upLongKey));
                } else if(upLongKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(upLongKey));
                } else if(upLongKeyType == UNDEFINED){
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
            case downLong:
                if(downLongKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(downLongKey));
                } else if(downLongKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(downLongKey));
                } else if(downLongKeyType == UNDEFINED){
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
            case rightLong:
                if(rightLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rightLongPressKey));
                } else if(rightLongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rightLongPressKey));
                } else if(rightLongPressKeyType == UNDEFINED){
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
            case leftLong:
                if(leftLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(leftLongPressKey));
                } else if(leftLongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(leftLongPressKey));
                } else if(leftLongPressKeyType == UNDEFINED){
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
            case fx1Long:
                if(fx1LongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(fx1LongPressKey));
                } else if(fx1LongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(fx1LongPressKey));
                } else if(fx1LongPressKeyType == UNDEFINED){
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
            case fx2Long:
                if(fx2LongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(fx2LongPressKey));
                } else if(fx2LongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(fx2LongPressKey));
                } else if(fx2LongPressKeyType == UNDEFINED){
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
            case upLong:
                return upLongKeyType;
            case down:
                return downKeyType;
            case downLong:
                return downLongKeyType;
            case right:
                return rightPressKeyType;
            case rightLong:
                return rightLongPressKeyType;
            case left:
                return leftPressKeyType;
            case leftLong:
                return leftLongPressKeyType;
            case fx1:
                return fx1PressKeyType;
            case fx1Long:
                return fx1LongPressKeyType;
            case fx2:
                return fx2PressKeyType;
            case fx2Long:
                return fx2LongPressKeyType;
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
            case upLong:
                return upLongKey;
            case down:
                return downKey;
            case downLong:
                return downLongKey;
            case right:
                return rightPressKey;
            case rightLong:
                return rightLongPressKey;
            case left:
                return leftPressKey;
            case leftLong:
                return leftLongPressKey;
            case fx1:
                return fx1PressKey;
            case fx1Long:
                return fx1LongPressKey;
            case fx2:
                return fx2PressKey;
            case fx2Long:
                return fx2LongPressKey;
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
            case upLong:
                return upLongKeyModifier;
            case down:
                return downKeyModifier;
            case downLong:
                return downLongKeyModifier;
            case right:
                return rightPressKeyModifier;
            case rightLong:
                return rightLongPressKeyModifier;
            case left:
                return leftPressKeyModifier;
            case leftLong:
                return leftLongPressKeyModifier;
            case fx1:
                return fx1PressKeyModifier;
            case fx1Long:
                return fx1LongPressKeyModifier;
            case fx2:
                return fx2PressKeyModifier;
            case fx2Long:
                return fx2LongPressKeyModifier;
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
            case upLong:
                tempConfig[upLongKeyType_INDEX] = type;
                tempConfig[upLongKeyModifier_INDEX] = modifiers;
                tempConfig[upLongKey_INDEX] = key;
                break;
            case down:
                tempConfig[downKeyType_INDEX] = type;
                tempConfig[downKeyModifier_INDEX] = modifiers;
                tempConfig[downKey_INDEX] = key;
                break;
            case downLong:
                tempConfig[downLongKeyType_INDEX] = type;
                tempConfig[downLongKeyModifier_INDEX] = modifiers;
                tempConfig[downLongKey_INDEX] = key;
                break;
            case right:
                tempConfig[rightPressKeyType_INDEX] = type;
                tempConfig[rightPressKeyModifier_INDEX] = modifiers;
                tempConfig[rightPressKey_INDEX] = key;
                break;
            case rightLong:
                tempConfig[rightLongPressKeyType_INDEX] = type;
                tempConfig[rightLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[rightLongPressKey_INDEX] = key;
                break;
            case left:
                tempConfig[leftPressKeyType_INDEX] = type;
                tempConfig[leftPressKeyModifier_INDEX] = modifiers;
                tempConfig[leftPressKey_INDEX] = key;
                break;
            case leftLong:
                tempConfig[leftLongPressKeyType_INDEX] = type;
                tempConfig[leftLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[leftLongPressKey_INDEX] = key;
                break;
            case fx1:
                tempConfig[fx1PressKeyType_INDEX] = type;
                tempConfig[fx1PressKeyModifier_INDEX] = modifiers;
                tempConfig[fx1PressKey_INDEX] = key;
                break;
            case fx1Long:
                tempConfig[fx1LongPressKeyType_INDEX] = type;
                tempConfig[fx1LongPressKeyModifier_INDEX] = modifiers;
                tempConfig[fx1LongPressKey_INDEX] = key;
                break;
            case fx2:
                tempConfig[fx2PressKeyType_INDEX] = type;
                tempConfig[fx2PressKeyModifier_INDEX] = modifiers;
                tempConfig[fx2PressKey_INDEX] = key;
                break;
            case fx2Long:
                tempConfig[fx2LongPressKeyType_INDEX] = type;
                tempConfig[fx2LongPressKeyModifier_INDEX] = modifiers;
                tempConfig[fx2LongPressKey_INDEX] = key;
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
        channel3ValueRaw = (wunderLINQStatus[LIN_ACC_CHANNEL1_VAL_RAW_INDEX] & 0xFF);
        channel4ValueRaw = (wunderLINQStatus[LIN_ACC_CHANNEL2_VAL_RAW_INDEX] & 0xFF);
    }
}
