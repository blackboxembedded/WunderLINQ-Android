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
import androidx.preference.PreferenceManager;
import android.util.Log;

import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.comms.BLE.KeyboardHID;

import java.util.Arrays;

public class WLQ_U extends WLQ_BASE {

    private final static String TAG = "WLQ_U";

    public static String hardwareVersion1 = "WLQU1.0";

    private static final int configFlashSize = 42;
    private static final int firmwareVersionMajor_INDEX = 0;
    private static final int firmwareVersionMinor_INDEX = 1;

    private static final byte[] defaultConfig = {
            0x00,                               // Orientation
            0x11,                               // Long Press Sensitivity
            0x01, 0x00, 0x52, 0x00, 0x00, 0x00, // Up - Arrow Up
            0x01, 0x00, 0x51, 0x00, 0x00, 0x00, // Down - Arrow Down
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29, // Left - Arrow Left
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28, // Right - Arrow Right
            0x01, 0x00, 0x29, 0x00, 0x00, 0x00, // FX1 - Escape
            0x01, 0x00, 0x28, 0x00, 0x00, 0x00, // FX2 - Enter
            0x00,                               // PDM Channel 1 Mode
            0x00,                               // PDM Channel 2 Mode
            0x00,                               // PDM Channel 3 Mode
            0x00                                // PDM Channel 4 Mode
    };

    // Config message
    private static final int keyMode_INDEX = 5;
    private static final int orientation_INDEX = 0;
    private static final int sensitivity_INDEX = 1;
    private static final int upKeyType_INDEX = 2;
    private static final int upKeyModifier_INDEX = 3;
    private static final int upKey_INDEX = 4;
    private static final int upLongKeyType_INDEX = 5;
    private static final int upLongKeyModifier_INDEX = 6;
    private static final int upLongKey_INDEX = 7;
    private static final int downKeyType_INDEX = 8;
    private static final int downKeyModifier_INDEX = 9;
    private static final int downKey_INDEX = 10;
    private static final int downLongKeyType_INDEX = 11;
    private static final int downLongKeyModifier_INDEX = 12;
    private static final int downLongKey_INDEX = 13;
    private static final int leftKeyType_INDEX = 14;
    private static final int leftKeyModifier_INDEX = 15;
    private static final int leftKey_INDEX = 16;
    private static final int leftLongKeyType_INDEX = 17;
    private static final int leftLongKeyModifier_INDEX = 18;
    private static final int leftLongKey_INDEX = 19;
    private static final int rightKeyType_INDEX = 20;
    private static final int rightKeyModifier_INDEX = 21;
    private static final int rightKey_INDEX = 22;
    private static final int rightLongKeyType_INDEX = 23;
    private static final int rightLongKeyModifier_INDEX = 24;
    private static final int rightLongKey_INDEX = 25;
    private static final int fx1KeyType_INDEX = 26;
    private static final int fx1KeyModifier_INDEX = 27;
    private static final int fx1Key_INDEX = 28;
    private static final int fx1LongKeyType_INDEX = 29;
    private static final int fx1LongKeyModifier_INDEX = 30;
    private static final int fx1LongKey_INDEX = 31;
    private static final int fx2KeyType_INDEX = 32;
    private static final int fx2KeyModifier_INDEX = 33;
    private static final int fx2Key_INDEX = 34;
    private static final int fx2LongKeyType_INDEX = 35;
    private static final int fx2LongKeyModifier_INDEX = 36;
    private static final int fx2LongKey_INDEX = 37;
    private static final int pdmChannel1_INDEX = 38;
    private static final int pdmChannel2_INDEX = 39;
    private static final int pdmChannel3_INDEX = 40;
    private static final int pdmChannel4_INDEX = 41;
    private static final int accessories_INDEX = 45;

    // PDM Status message
    private static final int statusSize = 6;
    private static final int NUM_CHAN_INDEX = 0;
    private static final int ACTIVE_CHAN_INDEX = 1;
    private static final int ACC_PDM_CHANNEL1_VAL_RAW_INDEX = 2;
    private static final int ACC_PDM_CHANNEL2_VAL_RAW_INDEX = 3;
    private static final int ACC_PDM_CHANNEL3_VAL_RAW_INDEX = 4;
    private static final int ACC_PDM_CHANNEL4_VAL_RAW_INDEX = 5;

    private static byte[] wunderLINQStatus;
    private static int activeChannel;
    private static int channel1ValueRaw;
    private static int channel2ValueRaw;
    private static int channel3ValueRaw;
    private static int channel4ValueRaw;

    private static byte[] wunderLINQConfig;
    private static byte[] flashConfig;
    private static byte[] tempConfig;
    private static String firmwareVersion;
    private static String hardwareVersion;
    private static byte keyMode;
    private static int orientation;
    private static byte sensitivity;
    private static byte rightKeyType;
    private static byte rightKeyModifier;
    private static byte rightKey;
    private static byte rightLongKeyType;
    private static byte rightLongKeyModifier;
    private static byte rightLongKey;
    private static byte leftKeyType;
    private static byte leftKeyModifier;
    private static byte leftKey;
    private static byte leftLongKeyType;
    private static byte leftLongKeyModifier;
    private static byte leftLongKey;
    private static byte upKeyType;
    private static byte upKeyModifier;
    private static byte upKey;
    private static byte upLongKeyType;
    private static byte upLongKeyModifier;
    private static byte upLongKey;
    private static byte downKeyType;
    private static byte downKeyModifier;
    private static byte downKey;
    private static byte downLongKeyType;
    private static byte downLongKeyModifier;
    private static byte downLongKey;
    private static byte fx1KeyType;
    private static byte fx1KeyModifier;
    private static byte fx1Key;
    private static byte fx1LongKeyType;
    private static byte fx1LongKeyModifier;
    private static byte fx1LongKey;
    private static byte fx2KeyType;
    private static byte fx2KeyModifier;
    private static byte fx2Key;
    private static byte fx2LongKeyType;
    private static byte fx2LongKeyModifier;
    private static byte fx2LongKey;

    private static byte pdmChannel1Setting;
    private static byte pdmChannel2Setting;
    private static byte pdmChannel3Setting;
    private static byte pdmChannel4Setting;
    private static byte accessories;

    public WLQ_U(byte[] bytes) {
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
            sensitivity = flashConfig[sensitivity_INDEX];
            rightKeyType = flashConfig[rightKeyType_INDEX];
            rightKeyModifier = flashConfig[rightKeyModifier_INDEX];
            rightKey = flashConfig[rightKey_INDEX];
            rightLongKeyType = flashConfig[rightLongKeyType_INDEX];
            rightLongKeyModifier = flashConfig[rightLongKeyModifier_INDEX];
            rightLongKey = flashConfig[rightLongKey_INDEX];
            leftKeyType = flashConfig[leftKeyType_INDEX];
            leftKeyModifier = flashConfig[leftKeyModifier_INDEX];
            leftKey = flashConfig[leftKey_INDEX];
            leftLongKeyType = flashConfig[leftLongKeyType_INDEX];
            leftLongKeyModifier = flashConfig[leftLongKeyModifier_INDEX];
            leftLongKey = flashConfig[leftLongKey_INDEX];
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
            fx1KeyType = flashConfig[fx1KeyType_INDEX];
            fx1KeyModifier = flashConfig[fx1KeyModifier_INDEX];
            fx1Key = flashConfig[fx1Key_INDEX];
            fx1LongKeyType = flashConfig[fx1LongKeyType_INDEX];
            fx1LongKeyModifier = flashConfig[fx1LongKeyModifier_INDEX];
            fx1LongKey = flashConfig[fx1LongKey_INDEX];
            fx2KeyType = flashConfig[fx2KeyType_INDEX];
            fx2KeyModifier = flashConfig[fx2KeyModifier_INDEX];
            fx2Key = flashConfig[fx2Key_INDEX];
            fx2LongKeyType = flashConfig[fx2LongKeyType_INDEX];
            fx2LongKeyModifier = flashConfig[fx2LongKeyModifier_INDEX];
            fx2LongKey = flashConfig[fx2LongKey_INDEX];
            pdmChannel1Setting = flashConfig[pdmChannel1_INDEX];
            pdmChannel2Setting = flashConfig[pdmChannel2_INDEX];
            pdmChannel3Setting = flashConfig[pdmChannel3_INDEX];
            pdmChannel4Setting = flashConfig[pdmChannel4_INDEX];
            accessories = bytes[accessories_INDEX];
        }
    }

    @Override
    public String getActionName(int id){
        return switch (id) {
            case ORIENTATION -> MyApplication.getContext().getString(R.string.orientation_label);
            case longPressSensitivity ->
                    MyApplication.getContext().getString(R.string.long_press_label);
            case up -> MyApplication.getContext().getString(R.string.up_label);
            case upLong -> MyApplication.getContext().getString(R.string.up_long_label);
            case down -> MyApplication.getContext().getString(R.string.down_label);
            case downLong -> MyApplication.getContext().getString(R.string.down_long_label);
            case right -> MyApplication.getContext().getString(R.string.right_label);
            case rightLong -> MyApplication.getContext().getString(R.string.right_long_label);
            case left -> MyApplication.getContext().getString(R.string.left_label);
            case leftLong -> MyApplication.getContext().getString(R.string.left_long_label);
            case fx1 -> MyApplication.getContext().getString(R.string.fx1_label);
            case fx1Long -> MyApplication.getContext().getString(R.string.fx1_long_label);
            case fx2 -> MyApplication.getContext().getString(R.string.fx2_label);
            case fx2Long -> MyApplication.getContext().getString(R.string.fx2_long_label);
            case pdmChannel1 -> MyApplication.getContext().getString(R.string.pdm_channel1_label);
            case pdmChannel2 -> MyApplication.getContext().getString(R.string.pdm_channel2_label);
            case pdmChannel3 -> MyApplication.getContext().getString(R.string.pdm_channel3_label);
            case pdmChannel4 -> MyApplication.getContext().getString(R.string.pdm_channel4_label);
            default -> {
                Log.d(TAG, "Unknown ActionID");
                yield "";
            }
        };
    }

    @Override
    public String getActionValue(int id){
        switch (id){
            case KEYMODE:
                return switch (keyMode) {
                    case 0 -> MyApplication.getContext().getString(R.string.keymode_default_label);
                    case 1 -> MyApplication.getContext().getString(R.string.keymode_custom_label);
                    case 2 -> MyApplication.getContext().getString(R.string.keymode_media_label);
                    case 3 -> MyApplication.getContext().getString(R.string.KEYMODE_DIRECT_label);
                    default -> "";
                };
            case ORIENTATION:
                return switch (orientation) {
                    case 1 -> MyApplication.getContext().getString(R.string.orientation_180_label);
                    case 2 -> MyApplication.getContext().getString(R.string.orientation_270_label);
                    case 3 -> MyApplication.getContext().getString(R.string.orientation_90_label);
                    default ->
                            MyApplication.getContext().getString(R.string.orientation_default_label);
                };
            case longPressSensitivity:
                return String.valueOf(sensitivity * 50);
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
                if(rightKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rightKey));
                } else if(rightKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rightKey));
                } else if(rightKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case rightLong:
                if(rightLongKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(rightLongKey));
                } else if(rightLongKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(rightLongKey));
                } else if(rightLongKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case left:
                if(leftKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(leftKey));
                } else if(leftKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(leftKey));
                } else if(leftKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case leftLong:
                if(leftLongKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(leftLongKey));
                } else if(leftLongKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(leftLongKey));
                } else if(leftLongKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fx1:
                if(fx1KeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(fx1Key));
                } else if(fx1KeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(fx1Key));
                } else if(fx1KeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fx1Long:
                if(fx1LongKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(fx1LongKey));
                } else if(fx1LongKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(fx1LongKey));
                } else if(fx1LongKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fx2:
                if(fx2KeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(fx2Key));
                } else if(fx2KeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(fx2Key));
                } else if(fx2KeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fx2Long:
                if(fx2LongKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(fx2LongKey));
                } else if(fx2LongKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(fx2LongKey));
                } else if(fx2LongKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case pdmChannel1:
                int index1 = java.util.Arrays.asList(
                        MyApplication.getContext().getResources().getStringArray(R.array.pdm_mode_value_array)
                ).indexOf(String.format("0x%02X", pdmChannel1Setting));
                if (index1 != -1) {
                    return MyApplication.getContext().getResources().getStringArray(R.array.pdm_mode_array)[index1];
                } else {
                    Log.d(TAG, "Unknown pdmChannel1Setting Value: " + String.format("0x%02X", pdmChannel1Setting));
                    return "";
                }
            case pdmChannel2:
                int index2 = java.util.Arrays.asList(
                        MyApplication.getContext().getResources().getStringArray(R.array.pdm_mode_value_array)
                ).indexOf(String.format("0x%02X", pdmChannel2Setting));
                if (index2 != -1) {
                    return MyApplication.getContext().getResources().getStringArray(R.array.pdm_mode_array)[index2];
                } else {
                    Log.d(TAG, "Unknown pdmChannel2Setting Value: " + String.format("0x%02X", pdmChannel2Setting));
                    return "";
                }
            case pdmChannel3:
                int index3 = java.util.Arrays.asList(
                        MyApplication.getContext().getResources().getStringArray(R.array.pdm_mode_value_array)
                ).indexOf(String.format("0x%02X", pdmChannel3Setting));
                if (index3 != -1) {
                    return MyApplication.getContext().getResources().getStringArray(R.array.pdm_mode_array)[index3];
                } else {
                    Log.d(TAG, "Unknown pdmChannel3Setting Value: " + String.format("0x%02X", pdmChannel3Setting));
                    return "";
                }
            case pdmChannel4:
                int index4 = java.util.Arrays.asList(
                        MyApplication.getContext().getResources().getStringArray(R.array.pdm_mode_value_array)
                ).indexOf(String.format("0x%02X", pdmChannel4Setting));
                if (index4 != -1) {
                    return MyApplication.getContext().getResources().getStringArray(R.array.pdm_mode_array)[index4];
                } else {
                    Log.d(TAG, "Unknown pdmChannel4Setting Value: " + String.format("0x%02X", pdmChannel4Setting));
                    return "";
                }
            default:
                Log.d(TAG, "Unknown ActionID");
                return "";
        }
    }

    @Override
    public void setActionValue(int id, byte value) {
        switch (id) {
            case ORIENTATION:
                tempConfig[orientation_INDEX] = value;
            case longPressSensitivity:
                tempConfig[sensitivity_INDEX] = value;
            case pdmChannel1:
                tempConfig[pdmChannel1_INDEX] = value;
            case pdmChannel2:
                tempConfig[pdmChannel2_INDEX] = value;
            case pdmChannel3:
                tempConfig[pdmChannel3_INDEX] = value;
            case pdmChannel4:
                tempConfig[pdmChannel4_INDEX] = value;
            default:
                Log.d(TAG, "Unknown ActionID");
        }
    }

    @Override
    public byte getActionKeyType(int id){
        return switch (id) {
            case up -> upKeyType;
            case upLong -> upLongKeyType;
            case down -> downKeyType;
            case downLong -> downLongKeyType;
            case right -> rightKeyType;
            case rightLong -> rightLongKeyType;
            case left -> leftKeyType;
            case leftLong -> leftLongKeyType;
            case fx1 -> fx1KeyType;
            case fx1Long -> fx1LongKeyType;
            case fx2 -> fx2KeyType;
            case fx2Long -> fx2LongKeyType;
            default -> {
                Log.d(TAG, "Unknown ActionID");
                yield 0x00;
            }
        };
    }

    @Override
    public byte getActionKey(int id) {
        return switch (id) {
            case up -> upKey;
            case upLong -> upLongKey;
            case down -> downKey;
            case downLong -> downLongKey;
            case right -> rightKey;
            case rightLong -> rightLongKey;
            case left -> leftKey;
            case leftLong -> leftLongKey;
            case fx1 -> fx1Key;
            case fx1Long -> fx1LongKey;
            case fx2 -> fx2Key;
            case fx2Long -> fx2LongKey;
            default -> {
                Log.d(TAG, "Unknown ActionID");
                yield 0x00;
            }
        };
    }

    @Override
    public byte getActionKeyModifiers(int id) {
        return switch (id) {
            case up -> upKeyModifier;
            case upLong -> upLongKeyModifier;
            case down -> downKeyModifier;
            case downLong -> downLongKeyModifier;
            case right -> rightKeyModifier;
            case rightLong -> rightLongKeyModifier;
            case left -> leftKeyModifier;
            case leftLong -> leftLongKeyModifier;
            case fx1 -> fx1KeyModifier;
            case fx1Long -> fx1LongKeyModifier;
            case fx2 -> fx2KeyModifier;
            case fx2Long -> fx2LongKeyModifier;
            default -> {
                Log.d(TAG, "Unknown ActionID");
                yield 0x00;
            }
        };
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
                tempConfig[rightKeyType_INDEX] = type;
                tempConfig[rightKeyModifier_INDEX] = modifiers;
                tempConfig[rightKey_INDEX] = key;
                break;
            case rightLong:
                tempConfig[rightLongKeyType_INDEX] = type;
                tempConfig[rightLongKeyModifier_INDEX] = modifiers;
                tempConfig[rightLongKey_INDEX] = key;
                break;
            case left:
                tempConfig[leftKeyType_INDEX] = type;
                tempConfig[leftKeyModifier_INDEX] = modifiers;
                tempConfig[leftKey_INDEX] = key;
                break;
            case leftLong:
                tempConfig[leftLongKeyType_INDEX] = type;
                tempConfig[leftLongKeyModifier_INDEX] = modifiers;
                tempConfig[leftLongKey_INDEX] = key;
                break;
            case fx1:
                tempConfig[fx1KeyType_INDEX] = type;
                tempConfig[fx1KeyModifier_INDEX] = modifiers;
                tempConfig[fx1Key_INDEX] = key;
                break;
            case fx1Long:
                tempConfig[fx1LongKeyType_INDEX] = type;
                tempConfig[fx1LongKeyModifier_INDEX] = modifiers;
                tempConfig[fx1LongKey_INDEX] = key;
                break;
            case fx2:
                tempConfig[fx2KeyType_INDEX] = type;
                tempConfig[fx2KeyModifier_INDEX] = modifiers;
                tempConfig[fx2Key_INDEX] = key;
                break;
            case fx2Long:
                tempConfig[fx2LongKeyType_INDEX] = type;
                tempConfig[fx2LongKeyModifier_INDEX] = modifiers;
                tempConfig[fx2LongKey_INDEX] = key;
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
    public byte[] getDefaultConfig() {
        return defaultConfig;
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
    public byte KEYMODE_DIRECT() { return WLQ_BASE.KEYMODE_DIRECT; }

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
        return WLQ.TYPE_U;
    }

    @Override
    public byte getAccessories() {
        return accessories;
    }

    @Override
    public byte[] getAccStatus() {
        return wunderLINQStatus;
    }

    @Override
    public void setAccStatus(byte[] status) {
        wunderLINQStatus = new byte[statusSize];
        System.arraycopy(status, 4, wunderLINQStatus, 0, statusSize);
        activeChannel = (wunderLINQStatus[ACTIVE_CHAN_INDEX] & 0xFF);
        channel1ValueRaw = (wunderLINQStatus[ACC_PDM_CHANNEL1_VAL_RAW_INDEX] & 0xFF);
        channel2ValueRaw = (wunderLINQStatus[ACC_PDM_CHANNEL2_VAL_RAW_INDEX] & 0xFF);
        channel3ValueRaw = (wunderLINQStatus[ACC_PDM_CHANNEL1_VAL_RAW_INDEX] & 0xFF);
        channel4ValueRaw = (wunderLINQStatus[ACC_PDM_CHANNEL2_VAL_RAW_INDEX] & 0xFF);
    }

    @Override
    public void setAccActive(int active) {
        wunderLINQStatus[ACTIVE_CHAN_INDEX] = (byte) active;
    }
}
