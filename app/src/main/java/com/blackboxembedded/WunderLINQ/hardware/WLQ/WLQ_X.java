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

public class WLQ_X extends WLQ_BASE {

    public final static String TAG = "WLQ_X";

    public static String hardwareVersion1 = "WLQX1.0";

    private static int firmwareVersionMajor_INDEX = 3;
    private static int firmwareVersionMinor_INDEX = 4;

    public static byte wheelMode;
    public static byte sensitivity;
    public static byte tempSensitivity;

    public static int configFlashSize = 62;
    public static byte[] defaultConfig = {
            0x07, // RT/K Start // Sensitivity
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28, // Menu
            0x01, 0x00, 0x52, 0x00, 0x00, 0x00, // Zoom+
            0x01, 0x00, 0x51, 0x00, 0x00, 0x00, // Zoom-
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29, // Speak
            0x02, 0x00, (byte) 0xE2, 0x00, 0x00, 0x00, // Mute
            0x02, 0x00, (byte) 0xB8, 0x00, 0x00, 0x00, // Display
            0x11, // Full Start // Sensitivity
            0x01, 0x00, 0x4F, 0x01, 0x00, 0x28, // Right Toggle
            0x01, 0x00, 0x50, 0x01, 0x00, 0x29, // Left Toggle
            0x01, 0x00, 0x52, 0x01, 0x00, 0x51, // Scroll
            0x02, 0x00, (byte) 0xB8, 0x02, 0x00, (byte) 0xE2}; // Signal Cancel

    public static byte KEYMODE_DEFAULT = 0x00;
    public static byte KEYMODE_CUSTOM = 0x01;

    public static byte KEYBOARD_HID = 0x01;
    public static byte CONSUMER_HID = 0x02;
    public static byte UNDEFINED = 0x00;

    public static final int KEYMODE = 100;
    public static final int RTKDoublePressSensitivity = 2;
    public static final int fullLongPressSensitivity = 3;
    public static final int RTKPage = 4;
    public static final int RTKPageDoublePress = 5;
    public static final int RTKZoomPlus = 6;
    public static final int RTKZoomPlusDoublePress = 7;
    public static final int RTKZoomMinus = 8;
    public static final int RTKZoomMinusDoublePress = 9;
    public static final int RTKSpeak = 10;
    public static final int RTKSpeakDoublePress = 11;
    public static final int RTKMute = 12;
    public static final int RTKMuteDoublePress = 13;
    public static final int RTKDisplayOff = 14;
    public static final int RTKDisplayOffDoublePress = 15;
    public static final int fullScrollUp = 16;
    public static final int fullScrollDown = 17;
    public static final int fullToggleRight = 18;
    public static final int fullToggleRightLongPress = 19;
    public static final int fullToggleLeft = 20;
    public static final int fullToggleLeftLongPress = 21;
    public static final int fullSignalCancel = 22;
    public static final int fullSignalCancelLongPress = 23;

    public static int keyMode_INDEX = 5;
    public static int RTKSensitivity_INDEX = 0;
    public static int RTKPagePressKeyType_INDEX = 1;
    public static int RTKPagePressKeyModifier_INDEX = 2;
    public static int RTKPagePressKey_INDEX = 3;
    public static int RTKPageDoublePressKeyType_INDEX = 4;
    public static int RTKPageDoublePressKeyModifier_INDEX = 5;
    public static int RTKPageDoublePressKey_INDEX = 6;
    public static int RTKZoomPPressKeyType_INDEX = 7;
    public static int RTKZoomPPressKeyModifier_INDEX = 8;
    public static int RTKZoomPPressKey_INDEX = 9;
    public static int RTKZoomPDoublePressKeyType_INDEX = 10;
    public static int RTKZoomPDoublePressKeyModifier_INDEX = 11;
    public static int RTKZoomPDoublePressKey_INDEX = 12;
    public static int RTKZoomMPressKeyType_INDEX = 13;
    public static int RTKZoomMPressKeyModifier_INDEX = 14;
    public static int RTKZoomMPressKey_INDEX = 15;
    public static int RTKZoomMDoublePressKeyType_INDEX = 16;
    public static int RTKZoomMDoublePressKeyModifier_INDEX = 17;
    public static int RTKZoomMDoublePressKey_INDEX = 18;
    public static int RTKSpeakPressKeyType_INDEX = 19;
    public static int RTKSpeakPressKeyModifier_INDEX = 20;
    public static int RTKSpeakPressKey_INDEX = 21;
    public static int RTKSpeakDoublePressKeyType_INDEX = 22;
    public static int RTKSpeakDoublePressKeyModifier_INDEX = 23;
    public static int RTKSpeakDoublePressKey_INDEX = 24;
    public static int RTKMutePressKeyType_INDEX = 25;
    public static int RTKMutePressKeyModifier_INDEX = 26;
    public static int RTKMutePressKey_INDEX = 27;
    public static int RTKMuteDoublePressKeyType_INDEX = 28;
    public static int RTKMuteDoublePressKeyModifier_INDEX = 29;
    public static int RTKMuteDoublePressKey_INDEX = 30;
    public static int RTKDisplayPressKeyType_INDEX = 31;
    public static int RTKDisplayPressKeyModifier_INDEX = 32;
    public static int RTKDisplayPressKey_INDEX = 33;
    public static int RTKDisplayDoublePressKeyType_INDEX = 34;
    public static int RTKDisplayDoublePressKeyModifier_INDEX = 35;
    public static int RTKDisplayDoublePressKey_INDEX = 36;
    public static int fullSensitivity_INDEX = 37;
    public static int fullRightPressKeyType_INDEX = 38;
    public static int fullRightPressKeyModifier_INDEX = 39;
    public static int fullRightPressKey_INDEX = 40;
    public static int fullRightLongPressKeyType_INDEX = 41;
    public static int fullRightLongPressKeyModifier_INDEX = 42;
    public static int fullRightLongPressKey_INDEX = 43;
    public static int fullLeftPressKeyType_INDEX = 44;
    public static int fullLeftPressKeyModifier_INDEX = 45;
    public static int fullLeftPressKey_INDEX = 46;
    public static int fullLeftLongPressKeyType_INDEX = 47;
    public static int fullLeftLongPressKeyModifier_INDEX = 48;
    public static int fullLeftLongPressKey_INDEX = 49;
    public static int fullScrollUpKeyType_INDEX = 50;
    public static int fullScrollUpKeyModifier_INDEX = 51;
    public static int fullScrollUpKey_INDEX = 52;
    public static int fullScrollDownKeyType_INDEX = 53;
    public static int fullScrollDownKeyModifier_INDEX = 54;
    public static int fullScrollDownKey_INDEX = 55;
    public static int fullSignalPressKeyType_INDEX = 56;
    public static int fullSignalPressKeyModifier_INDEX = 57;
    public static int fullSignalPressKey_INDEX = 58;
    public static int fullSignalLongPressKeyType_INDEX = 59;
    public static int fullSignalLongPressKeyModifier_INDEX = 60;
    public static int fullSignalLongPressKey_INDEX = 61;

    private static byte[] wunderLINQConfig;
    private static byte[] flashConfig;
    private static byte[] tempConfig;
    private static String firmwareVersion;
    private static String hardwareVersion;
    public static byte keyMode;
    public static byte RTKSensitivity;
    public static byte RTKPagePressKeyType;
    public static byte RTKPagePressKeyModifier;
    public static byte RTKPagePressKey;
    public static byte RTKPageDoublePressKeyType;
    public static byte RTKPageDoublePressKeyModifier;
    public static byte RTKPageDoublePressKey;
    public static byte RTKZoomPPressKeyType;
    public static byte RTKZoomPPressKeyModifier;
    public static byte RTKZoomPPressKey;
    public static byte RTKZoomPDoublePressKeyType;
    public static byte RTKZoomPDoublePressKeyModifier;
    public static byte RTKZoomPDoublePressKey;
    public static byte RTKZoomMPressKeyType;
    public static byte RTKZoomMPressKeyModifier;
    public static byte RTKZoomMPressKey;
    public static byte RTKZoomMDoublePressKeyType;
    public static byte RTKZoomMDoublePressKeyModifier;
    public static byte RTKZoomMDoublePressKey;
    public static byte RTKSpeakPressKeyType;
    public static byte RTKSpeakPressKeyModifier;
    public static byte RTKSpeakPressKey;
    public static byte RTKSpeakDoublePressKeyType;
    public static byte RTKSpeakDoublePressKeyModifier;
    public static byte RTKSpeakDoublePressKey;
    public static byte RTKMutePressKeyType;
    public static byte RTKMutePressKeyModifier;
    public static byte RTKMutePressKey;
    public static byte RTKMuteDoublePressKeyType;
    public static byte RTKMuteDoublePressKeyModifier;
    public static byte RTKMuteDoublePressKey;
    public static byte RTKDisplayPressKeyType;
    public static byte RTKDisplayPressKeyModifier;
    public static byte RTKDisplayPressKey;
    public static byte RTKDisplayDoublePressKeyType;
    public static byte RTKDisplayDoublePressKeyModifier;
    public static byte RTKDisplayDoublePressKey;
    public static byte fullSensitivity;
    public static byte fullRightPressKeyType;
    public static byte fullRightPressKeyModifier;
    public static byte fullRightPressKey;
    public static byte fullRightLongPressKeyType;
    public static byte fullRightLongPressKeyModifier;
    public static byte fullRightLongPressKey;
    public static byte fullLeftPressKeyType;
    public static byte fullLeftPressKeyModifier;
    public static byte fullLeftPressKey;
    public static byte fullLeftLongPressKeyType;
    public static byte fullLeftLongPressKeyModifier;
    public static byte fullLeftLongPressKey;
    public static byte fullScrollUpKeyType;
    public static byte fullScrollUpKeyModifier;
    public static byte fullScrollUpKey;
    public static byte fullScrollDownKeyType;
    public static byte fullScrollDownKeyModifier;
    public static byte fullScrollDownKey;
    public static byte fullSignalPressKeyType;
    public static byte fullSignalPressKeyModifier;
    public static byte fullSignalPressKey;
    public static byte fullSignalLongPressKeyType;
    public static byte fullSignalLongPressKeyModifier;
    public static byte fullSignalLongPressKey;

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
            case RTKDoublePressSensitivity:
                return MyApplication.getContext().getString(R.string.double_press_label);
            case fullLongPressSensitivity:
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
            case RTKDoublePressSensitivity:
                return String.valueOf(RTKSensitivity * 50) + "ms";
            case fullLongPressSensitivity:
                return String.valueOf(fullSensitivity * 50) + "ms";
            case RTKPage:
                if (WLQ_X.RTKPagePressKeyType == WLQ_X.KEYBOARD_HID) {
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKPagePressKey));
                } else if (WLQ_X.RTKPagePressKeyType == WLQ_X.CONSUMER_HID) {
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKPagePressKey));
                } else if (WLQ_X.RTKPagePressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKPageDoublePress:
                if (WLQ_X.RTKPageDoublePressKeyType == WLQ_X.KEYBOARD_HID) {
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKPageDoublePressKey));
                } else if (WLQ_X.RTKPageDoublePressKeyType == WLQ_X.CONSUMER_HID) {
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKPageDoublePressKey));
                } else if (WLQ_X.RTKPageDoublePressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKZoomPlus:
                if (WLQ_X.RTKZoomPPressKeyType == WLQ_X.KEYBOARD_HID) {
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKZoomPPressKey));
                } else if (WLQ_X.RTKZoomPPressKeyType == WLQ_X.CONSUMER_HID) {
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKZoomPPressKey));
                } else if (WLQ_X.RTKZoomPPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKZoomPlusDoublePress:
                if (WLQ_X.RTKZoomPDoublePressKeyType == WLQ_X.KEYBOARD_HID) {
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKZoomPDoublePressKey));
                } else if (WLQ_X.RTKZoomPDoublePressKeyType == WLQ_X.CONSUMER_HID) {
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKZoomPDoublePressKey));
                } else if (WLQ_X.RTKZoomPDoublePressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKZoomMinus:
                if(WLQ_X.RTKZoomMPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKZoomMPressKey));
                } else if(WLQ_X.RTKZoomMPressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKZoomMPressKey));
                } else if (WLQ_X.RTKZoomMPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKZoomMinusDoublePress:
                if(WLQ_X.RTKZoomMDoublePressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKZoomMDoublePressKey));
                } else if(WLQ_X.RTKZoomMDoublePressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKZoomMDoublePressKey));
                } else if (WLQ_X.RTKZoomMDoublePressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKSpeak:
                if(WLQ_X.RTKSpeakPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKSpeakPressKey));
                } else if(WLQ_X.RTKSpeakPressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKSpeakPressKey));
                } else if (WLQ_X.RTKSpeakPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKSpeakDoublePress:
                if(WLQ_X.RTKSpeakDoublePressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKSpeakDoublePressKey));
                } else if(WLQ_X.RTKSpeakDoublePressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKSpeakDoublePressKey));
                } else if (WLQ_X.RTKSpeakDoublePressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKMute:
                if(WLQ_X.RTKMutePressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKMutePressKey));
                } else if(WLQ_X.RTKMutePressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKMutePressKey));
                } else if (WLQ_X.RTKMutePressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKMuteDoublePress:
                if(WLQ_X.RTKMuteDoublePressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKMuteDoublePressKey));
                } else if(WLQ_X.RTKMuteDoublePressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKMuteDoublePressKey));
                } else if (WLQ_X.RTKMuteDoublePressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKDisplayOff:
                if(WLQ_X.RTKDisplayPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKDisplayPressKey));
                } else if(WLQ_X.RTKDisplayPressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKDisplayPressKey));
                } else if (WLQ_X.RTKDisplayPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case RTKDisplayOffDoublePress:
                if(WLQ_X.RTKDisplayDoublePressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.RTKDisplayDoublePressKey));
                } else if(WLQ_X.RTKDisplayDoublePressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.RTKDisplayDoublePressKey));
                } else if (WLQ_X.RTKDisplayDoublePressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullScrollUp:
                if(WLQ_X.fullScrollUpKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.fullScrollUpKey));
                } else if(WLQ_X.fullScrollUpKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.fullScrollUpKey));
                } else if(WLQ_X.fullScrollUpKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullScrollDown:
                if(WLQ_X.fullScrollDownKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.fullScrollDownKey));
                } else if(WLQ_X.fullScrollDownKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.fullScrollDownKey));
                } else if(WLQ_X.fullScrollDownKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullToggleRight:
                if(WLQ_X.fullRightPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.fullRightPressKey));
                } else if(WLQ_X.fullRightPressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.fullRightPressKey));
                } else if(WLQ_X.fullRightPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullToggleRightLongPress:
                if(WLQ_X.fullRightLongPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.fullRightLongPressKey));
                } else if(WLQ_X.fullRightLongPressKeyType  == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.fullRightLongPressKey));
                } else if(WLQ_X.fullRightLongPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullToggleLeft:
                if(WLQ_X.fullLeftPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.fullLeftPressKey));
                } else if(WLQ_X.fullLeftPressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.fullLeftPressKey));
                } else if(WLQ_X.fullLeftPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullToggleLeftLongPress:
                if(WLQ_X.fullLeftLongPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.fullLeftLongPressKey));
                } else if(WLQ_X.fullLeftLongPressKeyType  == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.fullLeftLongPressKey));
                } else if(WLQ_X.fullLeftLongPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullSignalCancel:
                if(WLQ_X.fullSignalPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.fullSignalPressKey));
                } else if(WLQ_X.fullSignalPressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.fullSignalPressKey));
                } else if(WLQ_X.fullSignalPressKeyType == WLQ_X.UNDEFINED){
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                } else {
                    return(MyApplication.getContext().getString(R.string.hid_0x00_label));
                }
            case fullSignalCancelLongPress:
                if(WLQ_X.fullSignalLongPressKeyType == WLQ_X.KEYBOARD_HID){
                    return(KeyboardHID.getKeyboardKeyByCode(WLQ_X.fullSignalLongPressKey));
                } else if(WLQ_X.fullSignalLongPressKeyType == WLQ_X.CONSUMER_HID){
                    return(KeyboardHID.getConsumerKeyByCode(WLQ_X.fullSignalLongPressKey));
                } else if(WLQ_X.fullSignalLongPressKeyType == WLQ_X.UNDEFINED){
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
        return WLQ.TYPE_X;
    }

    @Override
    public byte[] getStatus() {
        return null;
    }

    @Override
    public void setStatus(byte[] status) {
        Log.d(TAG, "WLQ_X_STATUS: " + Utils.ByteArraytoHex(status));
    }
}
