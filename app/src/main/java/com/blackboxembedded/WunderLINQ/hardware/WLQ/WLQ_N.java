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

public class WLQ_N extends WLQ_BASE {

    private final static String TAG = "WLQ_N";

    public static String hardwareVersion1 = "2PCB1.9 10/18";
    private static String hardwareVersion2 = "1PCB2.0 12/19";
    private static String hardwareVersion2_1 = "2PCB2.2 081920";
    private static String hardwareVersion3 = "WLQN3.0";

    private static int firmwareVersionMajor_INDEX = 9;
    private static int firmwareVersionMinor_INDEX = 10;

    private static int configFlashSize = 64;
    private static byte[] defaultConfig2 = {
            0x00, 0x00,                                         // USB Input Voltage threshold
            0x07,                                               // RT/K Sensitivity
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28,                 // Menu
            0x01, 0x00, 0x52, 0x00, 0x00, 0x00,                 // Zoom+
            0x01, 0x00, 0x51, 0x00, 0x00, 0x00,                 // Zoom-
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29,                 // Speak
            0x02, 0x00, (byte) 0xE2, 0x00, 0x00, 0x00,          // Mute
            0x02, 0x00, (byte) 0xB8, 0x00, 0x00, 0x00,          // Display
            0x11,                                               // Full Sensitivity
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28,                 // Right Toggle
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29,                 // Left Toggle
            0x01, 0x00, 0x52, 0x01, 0x00, 0x51,                 // Scroll
            0x02, 0x00, (byte) 0xB8, 0x02, 0x00, (byte) 0xE2    // Signal Cancel
    };

    private static byte[] defaultConfig2HW1 = {
            0x00, 0x00,                         // USB Input Voltage threshold
            0x07,                               // RT/K Sensitivity
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28, // Menu
            0x01, 0x00, 0x52, 0x00, 0x00, 0x00, // Zoom+
            0x01, 0x00, 0x51, 0x00, 0x00, 0x00, // Zoom-
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29, // Speak
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Mute
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Display
            0x11,                               // Full Sensitivity
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28, // Right Toggle
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29, // Left Toggle
            0x01, 0x00, 0x52, 0x01, 0x00, 0x51, // Scroll
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00  // Signal Cancel
    };

    private static int keyMode_INDEX = 25;
    private static int USBVinThresholdHigh_INDEX = 0;
    private static int USBVinThresholdLow_INDEX = 1;
    private static int RTKSensitivity_INDEX = 2;
    private static int RTKPagePressKeyType_INDEX = 3;
    private static int RTKPagePressKeyModifier_INDEX = 4;
    private static int RTKPagePressKey_INDEX = 5;
    private static int RTKPageDoublePressKeyType_INDEX = 6;
    private static int RTKPageDoublePressKeyModifier_INDEX = 7;
    private static int RTKPageDoublePressKey_INDEX = 8;
    private static int RTKZoomPPressKeyType_INDEX = 9;
    private static int RTKZoomPPressKeyModifier_INDEX = 10;
    private static int RTKZoomPPressKey_INDEX = 11;
    private static int RTKZoomPDoublePressKeyType_INDEX = 12;
    private static int RTKZoomPDoublePressKeyModifier_INDEX = 13;
    private static int RTKZoomPDoublePressKey_INDEX = 14;
    private static int RTKZoomMPressKeyType_INDEX = 15;
    private static int RTKZoomMPressKeyModifier_INDEX = 16;
    private static int RTKZoomMPressKey_INDEX = 17;
    private static int RTKZoomMDoublePressKeyType_INDEX = 18;
    private static int RTKZoomMDoublePressKeyModifier_INDEX = 19;
    private static int RTKZoomMDoublePressKey_INDEX = 20;
    private static int RTKSpeakPressKeyType_INDEX = 21;
    private static int RTKSpeakPressKeyModifier_INDEX = 22;
    private static int RTKSpeakPressKey_INDEX = 23;
    private static int RTKSpeakDoublePressKeyType_INDEX = 24;
    private static int RTKSpeakDoublePressKeyModifier_INDEX = 25;
    private static int RTKSpeakDoublePressKey_INDEX = 26;
    private static int RTKMutePressKeyType_INDEX = 27;
    private static int RTKMutePressKeyModifier_INDEX = 28;
    private static int RTKMutePressKey_INDEX = 29;
    private static int RTKMuteDoublePressKeyType_INDEX = 30;
    private static int RTKMuteDoublePressKeyModifier_INDEX = 31;
    private static int RTKMuteDoublePressKey_INDEX = 32;
    private static int RTKDisplayPressKeyType_INDEX = 33;
    private static int RTKDisplayPressKeyModifier_INDEX = 34;
    private static int RTKDisplayPressKey_INDEX = 35;
    private static int RTKDisplayDoublePressKeyType_INDEX = 36;
    private static int RTKDisplayDoublePressKeyModifier_INDEX = 37;
    private static int RTKDisplayDoublePressKey_INDEX = 38;
    private static int fullSensitivity_INDEX = 39;
    private static int fullRightPressKeyType_INDEX = 40;
    private static int fullRightPressKeyModifier_INDEX = 41;
    private static int fullRightPressKey_INDEX = 42;
    private static int fullRightLongPressKeyType_INDEX = 43;
    private static int fullRightLongPressKeyModifier_INDEX = 44;
    private static int fullRightLongPressKey_INDEX = 45;
    private static int fullLeftPressKeyType_INDEX = 46;
    private static int fullLeftPressKeyModifier_INDEX = 47;
    private static int fullLeftPressKey_INDEX = 48;
    private static int fullLeftLongPressKeyType_INDEX = 49;
    private static int fullLeftLongPressKeyModifier_INDEX = 50;
    private static int fullLeftLongPressKey_INDEX = 51;
    private static int fullScrollUpKeyType_INDEX = 52;
    private static int fullScrollUpKeyModifier_INDEX = 53;
    private static int fullScrollUpKey_INDEX = 54;
    private static int fullScrollDownKeyType_INDEX = 55;
    private static int fullScrollDownKeyModifier_INDEX = 56;
    private static int fullScrollDownKey_INDEX = 57;
    private static int fullSignalPressKeyType_INDEX = 58;
    private static int fullSignalPressKeyModifier_INDEX = 59;
    private static int fullSignalPressKey_INDEX = 60;
    private static int fullSignalLongPressKeyType_INDEX = 61;
    private static int fullSignalLongPressKeyModifier_INDEX = 62;
    private static int fullSignalLongPressKey_INDEX = 63;

    private static byte[] wunderLINQConfig;
    private static byte[] flashConfig;
    private static byte[] tempConfig;
    private static String firmwareVersion;
    private static String hardwareVersion;
    private static byte keyMode;
    private static int USBVinThreshold;
    private static byte RTKSensitivity;
    private static byte RTKPagePressKeyType;
    private static byte RTKPagePressKeyModifier;
    private static byte RTKPagePressKey;
    private static byte RTKPageDoublePressKeyType;
    private static byte RTKPageDoublePressKeyModifier;
    private static byte RTKPageDoublePressKey;
    private static byte RTKZoomPPressKeyType;
    private static byte RTKZoomPPressKeyModifier;
    private static byte RTKZoomPPressKey;
    private static byte RTKZoomPDoublePressKeyType;
    private static byte RTKZoomPDoublePressKeyModifier;
    private static byte RTKZoomPDoublePressKey;
    private static byte RTKZoomMPressKeyType;
    private static byte RTKZoomMPressKeyModifier;
    private static byte RTKZoomMPressKey;
    private static byte RTKZoomMDoublePressKeyType;
    private static byte RTKZoomMDoublePressKeyModifier;
    private static byte RTKZoomMDoublePressKey;
    private static byte RTKSpeakPressKeyType;
    private static byte RTKSpeakPressKeyModifier;
    private static byte RTKSpeakPressKey;
    private static byte RTKSpeakDoublePressKeyType;
    private static byte RTKSpeakDoublePressKeyModifier;
    private static byte RTKSpeakDoublePressKey;
    private static byte RTKMutePressKeyType;
    private static byte RTKMutePressKeyModifier;
    private static byte RTKMutePressKey;
    private static byte RTKMuteDoublePressKeyType;
    private static byte RTKMuteDoublePressKeyModifier;
    private static byte RTKMuteDoublePressKey;
    private static byte RTKDisplayPressKeyType;
    private static byte RTKDisplayPressKeyModifier;
    private static byte RTKDisplayPressKey;
    private static byte RTKDisplayDoublePressKeyType;
    private static byte RTKDisplayDoublePressKeyModifier;
    private static byte RTKDisplayDoublePressKey;
    private static byte fullSensitivity;
    private static byte fullRightPressKeyType;
    private static byte fullRightPressKeyModifier;
    private static byte fullRightPressKey;
    private static byte fullRightLongPressKeyType;
    private static byte fullRightLongPressKeyModifier;
    private static byte fullRightLongPressKey;
    private static byte fullLeftPressKeyType;
    private static byte fullLeftPressKeyModifier;
    private static byte fullLeftPressKey;
    private static byte fullLeftLongPressKeyType;
    private static byte fullLeftLongPressKeyModifier;
    private static byte fullLeftLongPressKey;
    private static byte fullScrollUpKeyType;
    private static byte fullScrollUpKeyModifier;
    private static byte fullScrollUpKey;
    private static byte fullScrollDownKeyType;
    private static byte fullScrollDownKeyModifier;
    private static byte fullScrollDownKey;
    private static byte fullSignalPressKeyType;
    private static byte fullSignalPressKeyModifier;
    private static byte fullSignalPressKey;
    private static byte fullSignalLongPressKeyType;
    private static byte fullSignalLongPressKeyModifier;
    private static byte fullSignalLongPressKey;

    public WLQ_N(byte[] bytes) {

        wunderLINQConfig = new byte[bytes.length];
        System.arraycopy(bytes, 0, wunderLINQConfig, 0, bytes.length);

        Log.d(TAG, "WLQConfig: " + Utils.ByteArrayToHex(wunderLINQConfig));

        byte[] flashConfigPart = new byte[configFlashSize];
        System.arraycopy(bytes, 26, flashConfigPart, 0, configFlashSize);

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
            USBVinThreshold = ((flashConfig[USBVinThresholdHigh_INDEX] & 0xFF) << 8) | (flashConfig[USBVinThresholdLow_INDEX] & 0xFF);
            RTKSensitivity = flashConfig[RTKSensitivity_INDEX];
            RTKPagePressKeyType = flashConfig[RTKPagePressKeyType_INDEX];
            RTKPagePressKeyModifier = flashConfig[RTKPagePressKeyModifier_INDEX];
            RTKPagePressKey = flashConfig[RTKPagePressKey_INDEX];
            RTKPageDoublePressKeyType = flashConfig[RTKPageDoublePressKeyType_INDEX];
            RTKPageDoublePressKeyModifier = flashConfig[RTKPageDoublePressKeyModifier_INDEX];
            RTKPageDoublePressKey = flashConfig[RTKPageDoublePressKey_INDEX];
            RTKZoomPPressKeyType = flashConfig[RTKZoomPPressKeyType_INDEX];
            RTKZoomPPressKeyModifier = flashConfig[RTKZoomPPressKeyModifier_INDEX];
            RTKZoomPPressKey = flashConfig[RTKZoomPPressKey_INDEX];
            RTKZoomPDoublePressKeyType = flashConfig[RTKZoomPDoublePressKeyType_INDEX];
            RTKZoomPDoublePressKeyModifier = flashConfig[RTKZoomPDoublePressKeyModifier_INDEX];
            RTKZoomPDoublePressKey = flashConfig[RTKZoomPDoublePressKey_INDEX];
            RTKZoomMPressKeyType = flashConfig[RTKZoomMPressKeyType_INDEX];
            RTKZoomMPressKeyModifier = flashConfig[RTKZoomMPressKeyModifier_INDEX];
            RTKZoomMPressKey = flashConfig[RTKZoomMPressKey_INDEX];
            RTKZoomMDoublePressKeyType = flashConfig[RTKZoomMDoublePressKeyType_INDEX];
            RTKZoomMDoublePressKeyModifier = flashConfig[RTKZoomMDoublePressKeyModifier_INDEX];
            RTKZoomMDoublePressKey = flashConfig[RTKZoomMDoublePressKey_INDEX];
            RTKSpeakPressKeyType = flashConfig[RTKSpeakPressKeyType_INDEX];
            RTKSpeakPressKeyModifier = flashConfig[RTKSpeakPressKeyModifier_INDEX];
            RTKSpeakPressKey = flashConfig[RTKSpeakPressKey_INDEX];
            RTKSpeakDoublePressKeyType = flashConfig[RTKSpeakDoublePressKeyType_INDEX];
            RTKSpeakDoublePressKeyModifier = flashConfig[RTKSpeakDoublePressKeyModifier_INDEX];
            RTKSpeakDoublePressKey = flashConfig[RTKSpeakDoublePressKey_INDEX];
            RTKMutePressKeyType = flashConfig[RTKMutePressKeyType_INDEX];
            RTKMutePressKeyModifier = flashConfig[RTKMutePressKeyModifier_INDEX];
            RTKMutePressKey = flashConfig[RTKMutePressKey_INDEX];
            RTKMuteDoublePressKeyType = flashConfig[RTKMuteDoublePressKeyType_INDEX];
            RTKMuteDoublePressKeyModifier = flashConfig[RTKMuteDoublePressKeyModifier_INDEX];
            RTKMuteDoublePressKey = flashConfig[RTKMuteDoublePressKey_INDEX];
            RTKDisplayPressKeyType = flashConfig[RTKDisplayPressKeyType_INDEX];
            RTKDisplayPressKeyModifier = flashConfig[RTKDisplayPressKeyModifier_INDEX];
            RTKDisplayPressKey = flashConfig[RTKDisplayPressKey_INDEX];
            RTKDisplayDoublePressKeyType = flashConfig[RTKDisplayDoublePressKeyType_INDEX];
            RTKDisplayDoublePressKeyModifier = flashConfig[RTKDisplayDoublePressKeyModifier_INDEX];
            RTKDisplayDoublePressKey = flashConfig[RTKDisplayDoublePressKey_INDEX];

            fullSensitivity = flashConfig[fullSensitivity_INDEX];
            fullRightPressKeyType = flashConfig[fullRightPressKeyType_INDEX];
            fullRightPressKeyModifier = flashConfig[fullRightPressKeyModifier_INDEX];
            fullRightPressKey = flashConfig[fullRightPressKey_INDEX];
            fullRightLongPressKeyType = flashConfig[fullRightLongPressKeyType_INDEX];
            fullRightLongPressKeyModifier = flashConfig[fullRightLongPressKeyModifier_INDEX];
            fullRightLongPressKey = flashConfig[fullRightLongPressKey_INDEX];
            fullLeftPressKeyType = flashConfig[fullLeftPressKeyType_INDEX];
            fullLeftPressKeyModifier = flashConfig[fullLeftPressKeyModifier_INDEX];
            fullLeftPressKey = flashConfig[fullLeftPressKey_INDEX];
            fullLeftLongPressKeyType = flashConfig[fullLeftLongPressKeyType_INDEX];
            fullLeftLongPressKeyModifier = flashConfig[fullLeftLongPressKeyModifier_INDEX];
            fullLeftLongPressKey = flashConfig[fullLeftLongPressKey_INDEX];
            fullScrollUpKeyType = flashConfig[fullScrollUpKeyType_INDEX];
            fullScrollUpKeyModifier = flashConfig[fullScrollUpKeyModifier_INDEX];
            fullScrollUpKey = flashConfig[fullScrollUpKey_INDEX];
            fullScrollDownKeyType = flashConfig[fullScrollDownKeyType_INDEX];
            fullScrollDownKeyModifier = flashConfig[fullScrollDownKeyModifier_INDEX];
            fullScrollDownKey = flashConfig[fullScrollDownKey_INDEX];
            fullSignalPressKeyType = flashConfig[fullSignalPressKeyType_INDEX];
            fullSignalPressKeyModifier = flashConfig[fullSignalPressKeyModifier_INDEX];
            fullSignalPressKey = flashConfig[fullSignalPressKey_INDEX];
            fullSignalLongPressKeyType = flashConfig[fullSignalLongPressKeyType_INDEX];
            fullSignalLongPressKeyModifier = flashConfig[fullSignalLongPressKeyModifier_INDEX];
            fullSignalLongPressKey = flashConfig[fullSignalLongPressKey_INDEX];
        }
    }

    @Override
    public String getActionName(int id){
        switch (id){
            case KEYMODE:
                return MyApplication.getContext().getString(R.string.keymode_label);
            case USB:
                return MyApplication.getContext().getString(R.string.usb_threshold_label);
            case doublePressSensitivity:
                return MyApplication.getContext().getString(R.string.double_press_label);
            case longPressSensitivity:
                return MyApplication.getContext().getString(R.string.long_press_label);
            case RTKPage:
                return MyApplication.getContext().getString(R.string.rtk_page_label);
            case RTKPageDoublePress:
                return MyApplication.getContext().getString(R.string.rtk_page_double_label);
            case RTKZoomPlus:
                return MyApplication.getContext().getString(R.string.rtk_zoomp_label);
            case RTKZoomPlusDoublePress:
                return MyApplication.getContext().getString(R.string.rtk_zoomm_double_label);
            case RTKZoomMinus:
                return MyApplication.getContext().getString(R.string.rtk_zoomm_label);
            case RTKZoomMinusDoublePress:
                return MyApplication.getContext().getString(R.string.rtk_zoomm_double_label);
            case RTKSpeak:
                return MyApplication.getContext().getString(R.string.rtk_speak_label);
            case RTKSpeakDoublePress:
                return MyApplication.getContext().getString(R.string.rtk_speak_double_label);
            case RTKMute:
                return MyApplication.getContext().getString(R.string.rtk_mute_label);
            case RTKMuteDoublePress:
                return MyApplication.getContext().getString(R.string.rtk_mute_double_label);
            case RTKDisplayOff:
                return MyApplication.getContext().getString(R.string.rtk_display_label);
            case RTKDisplayOffDoublePress:
                return MyApplication.getContext().getString(R.string.rtk_display_double_label);
            case fullScrollUp:
                return MyApplication.getContext().getString(R.string.full_scroll_up_label);
            case fullScrollDown:
                return MyApplication.getContext().getString(R.string.full_scroll_down_label);
            case fullToggleRight:
                return MyApplication.getContext().getString(R.string.full_toggle_right_label);
            case fullToggleRightLongPress:
                return MyApplication.getContext().getString(R.string.full_toggle_right_long_label);
            case fullToggleLeft:
                return MyApplication.getContext().getString(R.string.full_toggle_left_label);
            case fullToggleLeftLongPress:
                return MyApplication.getContext().getString(R.string.full_toggle_left_long_label);
            case fullSignalCancel:
                return MyApplication.getContext().getString(R.string.full_signal_cancel_label);
            case fullSignalCancelLongPress:
                return MyApplication.getContext().getString(R.string.full_signal_cancel_long_label);
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
            case USB:
                if (USBVinThreshold == 0x0000){
                    return MyApplication.getContext().getString(R.string.usbcontrol_on_label);
                } else if (USBVinThreshold == 0xFFFF){
                    return MyApplication.getContext().getString(R.string.usbcontrol_off_label);
                } else {
                    return MyApplication.getContext().getString(R.string.usbcontrol_engine_label);
                }
            case doublePressSensitivity:
                return String.valueOf(RTKSensitivity * 50);
            case longPressSensitivity:
                return String.valueOf(fullSensitivity * 50);
            case RTKPage:
                if (WLQ_N.RTKPagePressKeyType == WLQ_N.KEYBOARD_HID) {
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKPagePressKey));
                } else if (WLQ_N.RTKPagePressKeyType == WLQ_N.CONSUMER_HID) {
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKPagePressKey));
                } else if (WLQ_N.RTKPagePressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKPageDoublePress:
                if (WLQ_N.RTKPageDoublePressKeyType == WLQ_N.KEYBOARD_HID) {
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKPageDoublePressKey));
                } else if (WLQ_N.RTKPageDoublePressKeyType == WLQ_N.CONSUMER_HID) {
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKPageDoublePressKey));
                } else if (WLQ_N.RTKPageDoublePressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKZoomPlus:
                if (WLQ_N.RTKZoomPPressKeyType == WLQ_N.KEYBOARD_HID) {
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKZoomPPressKey));
                } else if (WLQ_N.RTKZoomPPressKeyType == WLQ_N.CONSUMER_HID) {
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKZoomPPressKey));
                } else if (WLQ_N.RTKZoomPPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKZoomPlusDoublePress:
                if (WLQ_N.RTKZoomPDoublePressKeyType == WLQ_N.KEYBOARD_HID) {
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKZoomPDoublePressKey));
                } else if (WLQ_N.RTKZoomPDoublePressKeyType == WLQ_N.CONSUMER_HID) {
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKZoomPDoublePressKey));
                } else if (WLQ_N.RTKZoomPDoublePressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKZoomMinus:
                if(WLQ_N.RTKZoomMPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKZoomMPressKey));
                } else if(WLQ_N.RTKZoomMPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKZoomMPressKey));
                } else if (WLQ_N.RTKZoomMPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKZoomMinusDoublePress:
                if(WLQ_N.RTKZoomMDoublePressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKZoomMDoublePressKey));
                } else if(WLQ_N.RTKZoomMDoublePressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKZoomMDoublePressKey));
                } else if (WLQ_N.RTKZoomMDoublePressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKSpeak:
                if(WLQ_N.RTKSpeakPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKSpeakPressKey));
                } else if(WLQ_N.RTKSpeakPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKSpeakPressKey));
                } else if (WLQ_N.RTKSpeakPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKSpeakDoublePress:
                if(WLQ_N.RTKSpeakDoublePressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKSpeakDoublePressKey));
                } else if(WLQ_N.RTKSpeakDoublePressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKSpeakDoublePressKey));
                } else if (WLQ_N.RTKSpeakDoublePressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKMute:
                if(WLQ_N.RTKMutePressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKMutePressKey));
                } else if(WLQ_N.RTKMutePressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKMutePressKey));
                } else if (WLQ_N.RTKMutePressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKMuteDoublePress:
                if(WLQ_N.RTKMuteDoublePressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKMuteDoublePressKey));
                } else if(WLQ_N.RTKMuteDoublePressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKMuteDoublePressKey));
                } else if (WLQ_N.RTKMuteDoublePressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKDisplayOff:
                if(WLQ_N.RTKDisplayPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKDisplayPressKey));
                } else if(WLQ_N.RTKDisplayPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKDisplayPressKey));
                } else if (WLQ_N.RTKDisplayPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKDisplayOffDoublePress:
                if(WLQ_N.RTKDisplayDoublePressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.RTKDisplayDoublePressKey));
                } else if(WLQ_N.RTKDisplayDoublePressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.RTKDisplayDoublePressKey));
                } else if (WLQ_N.RTKDisplayDoublePressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullScrollUp:
                if(WLQ_N.fullScrollUpKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.fullScrollUpKey));
                } else if(WLQ_N.fullScrollUpKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.fullScrollUpKey));
                } else if(WLQ_N.fullScrollUpKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullScrollDown:
                if(WLQ_N.fullScrollDownKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.fullScrollDownKey));
                } else if(WLQ_N.fullScrollDownKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.fullScrollDownKey));
                } else if(WLQ_N.fullScrollDownKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullToggleRight:
                if(WLQ_N.fullRightPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.fullRightPressKey));
                } else if(WLQ_N.fullRightPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.fullRightPressKey));
                } else if(WLQ_N.fullRightPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullToggleRightLongPress:
                if(WLQ_N.fullRightLongPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.fullRightLongPressKey));
                } else if(WLQ_N.fullRightLongPressKeyType  == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.fullRightLongPressKey));
                } else if(WLQ_N.fullRightLongPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullToggleLeft:
                if(WLQ_N.fullLeftPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.fullLeftPressKey));
                } else if(WLQ_N.fullLeftPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.fullLeftPressKey));
                } else if(WLQ_N.fullLeftPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullToggleLeftLongPress:
                if(WLQ_N.fullLeftLongPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.fullLeftLongPressKey));
                } else if(WLQ_N.fullLeftLongPressKeyType  == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.fullLeftLongPressKey));
                } else if(WLQ_N.fullLeftLongPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullSignalCancel:
                if(WLQ_N.fullSignalPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.fullSignalPressKey));
                } else if(WLQ_N.fullSignalPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.fullSignalPressKey));
                } else if(WLQ_N.fullSignalPressKeyType == WLQ_N.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullSignalCancelLongPress:
                if(WLQ_N.fullSignalLongPressKeyType == WLQ_N.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_N.fullSignalLongPressKey));
                } else if(WLQ_N.fullSignalLongPressKeyType == WLQ_N.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_N.fullSignalLongPressKey));
                } else if(WLQ_N.fullSignalLongPressKeyType == WLQ_N.UNDEFINED){
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
    public void setActionValue(int id, byte value) {
        switch (id) {
            case USB:
                if(value == 0){
                    tempConfig[USBVinThresholdHigh_INDEX] = 0x00;
                    tempConfig[USBVinThresholdLow_INDEX] = 0x00;
                } else if(value == 1){
                    tempConfig[USBVinThresholdHigh_INDEX] = 0x02;
                    tempConfig[USBVinThresholdLow_INDEX] = (byte)0xBC;
                } else if(value == 2){
                    tempConfig[USBVinThresholdHigh_INDEX] = (byte)0xFF;
                    tempConfig[USBVinThresholdLow_INDEX] = (byte)0xFF;
                }
            case doublePressSensitivity:
                tempConfig[RTKSensitivity_INDEX] = value;
            case longPressSensitivity:
                tempConfig[fullSensitivity_INDEX] = value;
            default:
                Log.d(TAG, "Unknown ActionID");
        }
    }

    @Override
    public byte getActionKeyType(int id){
        switch (id){
            case RTKPage:
                return RTKPagePressKeyType;
            case RTKPageDoublePress:
                return RTKPageDoublePressKeyType;
            case RTKZoomPlus:
                return RTKZoomPPressKeyType;
            case RTKZoomPlusDoublePress:
                return RTKZoomPDoublePressKeyType;
            case RTKZoomMinus:
                return RTKZoomMPressKeyType;
            case RTKZoomMinusDoublePress:
                return RTKZoomMDoublePressKeyType;
            case RTKSpeak:
                return RTKSpeakPressKeyType;
            case RTKSpeakDoublePress:
                return RTKSpeakDoublePressKeyType;
            case RTKMute:
                return RTKMutePressKeyType;
            case RTKMuteDoublePress:
                return RTKMuteDoublePressKeyType;
            case RTKDisplayOff:
                return RTKDisplayPressKeyType;
            case RTKDisplayOffDoublePress:
                return RTKDisplayDoublePressKeyType;
            case fullScrollUp:
                return fullScrollUpKeyType;
            case fullScrollDown:
                return fullScrollDownKeyType;
            case fullToggleRight:
                return fullRightPressKeyType;
            case fullToggleRightLongPress:
                return fullRightLongPressKeyType;
            case fullToggleLeft:
                return fullLeftPressKeyType;
            case fullToggleLeftLongPress:
                return fullLeftLongPressKeyType;
            case fullSignalCancel:
                return fullSignalPressKeyType;
            case fullSignalCancelLongPress:
                return fullSignalLongPressKeyType;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public byte getActionKey(int id) {
        switch (id) {
            case RTKPage:
                return RTKPagePressKey;
            case RTKPageDoublePress:
                return RTKPageDoublePressKey;
            case RTKZoomPlus:
                return RTKZoomPPressKey;
            case RTKZoomPlusDoublePress:
                return RTKZoomPDoublePressKey;
            case RTKZoomMinus:
                return RTKZoomMPressKey;
            case RTKZoomMinusDoublePress:
                return RTKZoomMDoublePressKey;
            case RTKSpeak:
                return RTKSpeakPressKey;
            case RTKSpeakDoublePress:
                return RTKSpeakDoublePressKey;
            case RTKMute:
                return RTKMutePressKey;
            case RTKMuteDoublePress:
                return RTKMuteDoublePressKey;
            case RTKDisplayOff:
                return RTKDisplayPressKey;
            case RTKDisplayOffDoublePress:
                return RTKDisplayDoublePressKey;
            case fullScrollUp:
                return fullScrollUpKey;
            case fullScrollDown:
                return fullScrollDownKey;
            case fullToggleRight:
                return fullRightPressKey;
            case fullToggleRightLongPress:
                return fullRightLongPressKey;
            case fullToggleLeft:
                return fullLeftPressKey;
            case fullToggleLeftLongPress:
                return fullLeftLongPressKey;
            case fullSignalCancel:
                return fullSignalPressKey;
            case fullSignalCancelLongPress:
                return fullSignalLongPressKey;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public byte getActionKeyModifiers(int id) {
        switch (id) {
            case RTKPage:
                return RTKPagePressKeyModifier;
            case RTKPageDoublePress:
                return RTKPageDoublePressKeyModifier;
            case RTKZoomPlus:
                return RTKZoomPPressKeyModifier;
            case RTKZoomPlusDoublePress:
                return RTKZoomPDoublePressKeyModifier;
            case RTKZoomMinus:
                return RTKZoomMPressKeyModifier;
            case RTKZoomMinusDoublePress:
                return RTKZoomMDoublePressKeyModifier;
            case RTKSpeak:
                return RTKSpeakPressKeyModifier;
            case RTKSpeakDoublePress:
                return RTKSpeakDoublePressKeyModifier;
            case RTKMute:
                return RTKMutePressKeyModifier;
            case RTKMuteDoublePress:
                return RTKMuteDoublePressKeyModifier;
            case RTKDisplayOff:
                return RTKDisplayPressKeyModifier;
            case RTKDisplayOffDoublePress:
                return RTKDisplayDoublePressKeyModifier;
            case fullScrollUp:
                return fullScrollUpKeyModifier;
            case fullScrollDown:
                return fullScrollDownKeyModifier;
            case fullToggleRight:
                return fullRightPressKeyModifier;
            case fullToggleRightLongPress:
                return fullRightLongPressKeyModifier;
            case fullToggleLeft:
                return fullLeftPressKeyModifier;
            case fullToggleLeftLongPress:
                return fullLeftLongPressKeyModifier;
            case fullSignalCancel:
                return fullSignalPressKeyModifier;
            case fullSignalCancelLongPress:
                return fullSignalLongPressKeyModifier;
            default:
                Log.d(TAG, "Unknown ActionID");
                return 0x00;
        }
    }

    @Override
    public void setActionKey(int id, byte type, byte modifiers, byte key) {
        switch (id) {
            case RTKPage:
                tempConfig[RTKPagePressKeyType_INDEX] = type;
                tempConfig[RTKPagePressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKPagePressKey_INDEX] = key;
                break;
            case RTKPageDoublePress:
                tempConfig[RTKPageDoublePressKeyType_INDEX] = type;
                tempConfig[RTKPageDoublePressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKPageDoublePressKey_INDEX] = key;
                break;
            case RTKZoomPlus:
                tempConfig[RTKZoomPPressKeyType_INDEX] = type;
                tempConfig[RTKZoomPPressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKZoomPPressKey_INDEX] = key;
                break;
            case RTKZoomPlusDoublePress:
                tempConfig[RTKZoomPDoublePressKeyType_INDEX] = type;
                tempConfig[RTKZoomPDoublePressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKZoomPDoublePressKey_INDEX] = key;
                break;
            case RTKZoomMinus:
                tempConfig[RTKZoomMPressKeyType_INDEX] = type;
                tempConfig[RTKZoomMPressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKZoomMPressKey_INDEX] = key;
                break;
            case RTKZoomMinusDoublePress:
                tempConfig[RTKZoomMDoublePressKeyType_INDEX] = type;
                tempConfig[RTKZoomMDoublePressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKZoomMDoublePressKey_INDEX] = key;
                break;
            case RTKSpeak:
                tempConfig[RTKSpeakPressKeyType_INDEX] = type;
                tempConfig[RTKSpeakPressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKSpeakPressKey_INDEX] = key;
                break;
            case RTKSpeakDoublePress:
                tempConfig[RTKSpeakDoublePressKeyType_INDEX] = type;
                tempConfig[RTKSpeakDoublePressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKSpeakDoublePressKey_INDEX] = key;
                break;
            case RTKMute:
                tempConfig[RTKMutePressKeyType_INDEX] = type;
                tempConfig[RTKMutePressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKMutePressKey_INDEX] = key;
                break;
            case RTKMuteDoublePress:
                tempConfig[RTKMuteDoublePressKeyType_INDEX] = type;
                tempConfig[RTKMuteDoublePressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKMuteDoublePressKey_INDEX] = key;
                break;
            case RTKDisplayOff:
                tempConfig[RTKDisplayPressKeyType_INDEX] = type;
                tempConfig[RTKDisplayPressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKDisplayPressKey_INDEX] = key;
                break;
            case RTKDisplayOffDoublePress:
                tempConfig[RTKDisplayDoublePressKeyType_INDEX] = type;
                tempConfig[RTKDisplayDoublePressKeyModifier_INDEX] = modifiers;
                tempConfig[RTKDisplayDoublePressKey_INDEX] = key;
                break;
            case fullScrollUp:
                tempConfig[fullScrollUpKeyType_INDEX] = type;
                tempConfig[fullScrollUpKeyModifier_INDEX] = modifiers;
                tempConfig[fullScrollUpKey_INDEX] = key;
                break;
            case fullScrollDown:
                tempConfig[fullScrollDownKeyType_INDEX] = type;
                tempConfig[fullScrollDownKeyModifier_INDEX] = modifiers;
                tempConfig[fullScrollDownKey_INDEX] = key;
                break;
            case fullToggleRight:
                tempConfig[fullRightPressKeyType_INDEX] = type;
                tempConfig[fullRightPressKeyModifier_INDEX] = modifiers;
                tempConfig[fullRightPressKey_INDEX] = key;
                break;
            case fullToggleRightLongPress:
                tempConfig[fullRightLongPressKeyType_INDEX] = type;
                tempConfig[fullRightLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[fullRightLongPressKey_INDEX] = key;
                break;
            case fullToggleLeft:
                tempConfig[fullLeftPressKeyType_INDEX] = type;
                tempConfig[fullLeftPressKeyModifier_INDEX] = modifiers;
                tempConfig[fullLeftPressKey_INDEX] = key;
                break;
            case fullToggleLeftLongPress:
                tempConfig[fullLeftLongPressKeyType_INDEX] = type;
                tempConfig[fullLeftLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[fullLeftLongPressKey_INDEX] = key;
                break;
            case fullSignalCancel:
                tempConfig[fullSignalPressKeyType_INDEX] = type;
                tempConfig[fullSignalPressKeyModifier_INDEX] = modifiers;
                tempConfig[fullSignalPressKey_INDEX] = key;
                break;
            case fullSignalCancelLongPress:
                tempConfig[fullSignalLongPressKeyType_INDEX] = type;
                tempConfig[fullSignalLongPressKeyModifier_INDEX] = modifiers;
                tempConfig[fullSignalLongPressKey_INDEX] = key;
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
        if (MotorcycleData.wlq.getHardwareVersion() != null) {
            if (MotorcycleData.wlq.getHardwareVersion().equals(hardwareVersion1)) {
                return defaultConfig2HW1;
            } else {
                return defaultConfig2;
            }
        } else {
            return defaultConfig2;
        }
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
    public byte KEYMODE_CUSTOM() { return WLQ_BASE.KEYMODE_CUSTOM; }

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
    public byte UNDEFINED() { return WLQ_BASE.UNDEFINED; }

    @Override
    public int getHardwareType() {
        return WLQ.TYPE_N;
    }

    @Override
    public byte getAccessories() {
        return 0x00;
    }

    @Override
    public byte[] getAccStatus() {
        return null;
    }

    @Override
    public void setAccStatus(byte[] status) {
        Log.d(TAG, "WLQ_N_STATUS: " + Utils.ByteArrayToHex(status));
    }
    @Override
    public void setAccActive(int active) {
        Log.d(TAG, "WLQ_N_ACC_ACTIVE: " + active);
    }
}
