package com.blackboxembedded.WunderLINQ.hardware.WLQ;

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

    private static int configFlashSize = 36;
    private static int firmwareVersionMajor_INDEX = 0;
    private static int firmwareVersionMinor_INDEX = 1;

    public static byte[] defaultConfig = {
            0x40, (byte)0xD9, 0x42,                             // CAN
            0x00, 0x00,                                         // USB
            0x11,                                               // Sensitivity
            0x01, 0x00, 0x52,                                   // Scroll Up
            0x01, 0x00, 0x51,                                   // Scroll Down
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29,                 // Wheel Left
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28,                 // Wheel Right
            0x02, 0x00, (byte)0xE9, 0x02, 0x00, (byte)0xE2,     // Menu Up
            0x02, 0x00, (byte)0xEA, 0x02, 0x00, (byte)0xE2};    // Menu Down

    public static final int longPressSensitivity = 25;
    public static final int wheelScrollUp = 26;
    public static final int wheelScrollDown = 27;
    public static final int wheelToggleRight = 28;
    public static final int wheelToggleRightLongPress = 29;
    public static final int wheelToggleLeft = 30;
    public static final int wheelToggleLeftLongPress = 31;
    public static final int menuUp = 32;
    public static final int menuUpLongPress = 33;
    public static final int menuDown = 34;
    public static final int menuDownLongPress = 35;

    private static int keyMode_INDEX = 5;
    private static int CANCF1_INDEX = 0;
    private static int CANCF2_INDEX = 1;
    private static int CANCF3_INDEX = 2;
    private static int USBVinThresholdLow_INDEX = 3;
    private static int USBVinThresholdHigh_INDEX = 4;
    public static int Sensitivity_INDEX = 5;
    private static int wheelScrollUpKeyType_INDEX = 6;
    private static int wheelScrollUpKeyModifier_INDEX = 7;
    private static int wheelScrollUpKey_INDEX = 8;
    private static int wheelScrollDownKeyType_INDEX = 9;
    private static int wheelScrollDownKeyModifier_INDEX = 10;
    private static int wheelScrollDownKey_INDEX = 11;
    private static int wheelLeftPressKeyType_INDEX = 12;
    private static int wheelLeftPressKeyModifier_INDEX = 13;
    private static int wheelLeftPressKey_INDEX = 14;
    private static int wheelLeftLongPressKeyType_INDEX = 15;
    private static int wheelLeftLongPressKeyModifier_INDEX = 16;
    private static int wheelLeftLongPressKey_INDEX = 17;
    private static int wheelRightPressKeyType_INDEX = 18;
    private static int wheelRightPressKeyModifier_INDEX = 19;
    private static int wheelRightPressKey_INDEX = 20;
    private static int wheelRightLongPressKeyType_INDEX = 21;
    private static int wheelRightLongPressKeyModifier_INDEX = 22;
    private static int wheelRightLongPressKey_INDEX = 23;
    private static int menuUpPressKeyType_INDEX = 24;
    private static int menuUpPressKeyModifier_INDEX = 25;
    private static int menuUpPressKey_INDEX = 26;
    private static int menuUpLongPressKeyType_INDEX = 27;
    private static int menuUpLongPressKeyModifier_INDEX = 28;
    private static int menuUpLongPressKey_INDEX = 29;
    private static int menuDownPressKeyType_INDEX = 30;
    private static int menuDownPressKeyModifier_INDEX = 31;
    private static int menuDownPressKey_INDEX = 32;
    private static int menuDownLongPressKeyType_INDEX = 33;
    private static int menuDownLongPressKeyModifier_INDEX = 34;
    private static int menuDownLongPressKey_INDEX = 35;

    private static byte[] wunderLINQConfig;
    private static byte[] flashConfig;
    private static byte[] tempConfig;
    private static String firmwareVersion;
    private static String hardwareVersion;
    private static byte[] CANSpeed;
    private static byte keyMode;
    private static int USBVinThreshold;
    public static byte sensitivity;
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
    public static byte menuUpPressKeyType;
    public static byte menuUpPressKeyModifier;
    public static byte menuUpPressKey;
    public static byte menuUpLongPressKeyType;
    public static byte menuUpLongPressKeyModifier;
    public static byte menuUpLongPressKey;
    public static byte menuDownPressKeyType;
    public static byte menuDownPressKeyModifier;
    public static byte menuDownPressKey;
    public static byte menuDownLongPressKeyType;
    public static byte menuDownLongPressKeyModifier;
    public static byte menuDownLongPressKey;

    public WLQ_C(byte[] bytes) {
        wunderLINQConfig = new byte[bytes.length];
        System.arraycopy(bytes, 0, wunderLINQConfig, 0, bytes.length);

        Log.d(TAG, "WLQConfig: " + Utils.ByteArraytoHex(wunderLINQConfig));

        byte[] flashConfigPart = new byte[configFlashSize];
        System.arraycopy(bytes, 6, flashConfigPart, 0, configFlashSize);

        if (!Arrays.equals(flashConfig, flashConfigPart)) {
            flashConfig = new byte[configFlashSize];
            System.arraycopy(flashConfigPart, 0, flashConfig, 0, flashConfigPart.length);

            tempConfig = new byte[flashConfig.length];
            System.arraycopy(flashConfig, 0, tempConfig, 0, flashConfig.length);

            Log.d(TAG, "New flashConfig: " + Utils.ByteArraytoHex(flashConfig));

            firmwareVersion = bytes[firmwareVersionMajor_INDEX] + "." + bytes[firmwareVersionMinor_INDEX];

            keyMode = bytes[keyMode_INDEX];
            CANSpeed = new byte[3];
            CANSpeed[0] = flashConfig[CANCF1_INDEX];
            CANSpeed[1] = flashConfig[CANCF2_INDEX];
            CANSpeed[2] = flashConfig[CANCF3_INDEX];
            USBVinThreshold = ((flashConfig[USBVinThresholdHigh_INDEX] & 0xFF) << 8) | (flashConfig[USBVinThresholdLow_INDEX] & 0xFF);
            sensitivity = flashConfig[Sensitivity_INDEX];
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
            menuUpPressKeyType = flashConfig[menuUpPressKeyType_INDEX];
            menuUpPressKeyModifier = flashConfig[menuUpPressKeyModifier_INDEX];
            menuUpPressKey = flashConfig[menuUpPressKey_INDEX];
            menuUpLongPressKeyType = flashConfig[menuUpLongPressKeyType_INDEX];
            menuUpLongPressKeyModifier = flashConfig[menuUpLongPressKeyModifier_INDEX];
            menuUpLongPressKey = flashConfig[menuUpLongPressKey_INDEX];
            menuDownPressKeyType = flashConfig[menuDownPressKeyType_INDEX];
            menuDownPressKeyModifier = flashConfig[menuDownPressKeyModifier_INDEX];
            menuDownPressKey = flashConfig[menuDownPressKey_INDEX];
            menuDownLongPressKeyType = flashConfig[menuDownLongPressKeyType_INDEX];
            menuDownLongPressKeyModifier = flashConfig[menuDownLongPressKeyModifier_INDEX];
            menuDownLongPressKey = flashConfig[menuDownLongPressKey_INDEX];
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
            case menuUp:
                return MyApplication.getContext().getString(R.string.full_menu_up_label);
            case menuUpLongPress:
                return MyApplication.getContext().getString(R.string.full_menu_up_long_label);
            case menuDown:
                return MyApplication.getContext().getString(R.string.full_menu_down_label);
            case menuDownLongPress:
                return MyApplication.getContext().getString(R.string.full_menu_down_long_label);
            default:
                Log.d(TAG, "Unknown ActionID");
                return "";
        }
    }

    @Override
    public String getActionValue(int id){
        switch (id){
            case longPressSensitivity:
                return String.valueOf(sensitivity);
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
            case menuUp:
                if(menuUpPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(menuUpPressKey));
                } else if(menuUpPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(menuUpPressKey));
                } else if(menuUpPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case menuUpLongPress:
                if(menuUpLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(menuUpLongPressKey));
                } else if(menuUpLongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(menuUpLongPressKey));
                } else if(menuUpLongPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case menuDown:
                if(menuDownPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(menuDownPressKey));
                } else if(menuDownPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(menuDownPressKey));
                } else if(menuDownPressKeyType == UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case menuDownLongPress:
                if(menuDownLongPressKeyType == KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(menuDownLongPressKey));
                } else if(menuDownLongPressKeyType == CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(menuDownLongPressKey));
                } else if(menuDownLongPressKeyType == UNDEFINED){
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
            case menuUp:
                return menuUpPressKeyType;
            case menuUpLongPress:
                return menuUpLongPressKeyType;
            case menuDown:
                return menuDownPressKeyType;
            case menuDownLongPress:
                return menuDownLongPressKeyType;
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
            case menuUp:
                return menuUpPressKey;
            case menuUpLongPress:
                return menuUpLongPressKey;
            case menuDown:
                return menuDownPressKey;
            case menuDownLongPress:
                return menuDownLongPressKey;
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
            case menuUp:
                return menuUpPressKeyModifier;
            case menuUpLongPress:
                return menuUpLongPressKeyModifier;
            case menuDown:
                return menuDownPressKeyModifier;
            case menuDownLongPress:
                return menuDownLongPressKeyModifier;
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
            case menuUp:
                tempConfig[menuUpPressKeyType_INDEX] = type;
                tempConfig[menuUpPressKeyModifier_INDEX] = modifiers;
                tempConfig[menuUpPressKey_INDEX] = key;
                break;
            case menuUpLongPress:
                tempConfig[menuUpLongPressKeyType_INDEX] = type;
                tempConfig[menuUpLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[menuUpLongPressKey_INDEX] = key;
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
    public byte getSensitivity() {
        return sensitivity;
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
}
