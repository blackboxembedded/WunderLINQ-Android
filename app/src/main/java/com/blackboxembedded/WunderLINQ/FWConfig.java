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

import android.util.Log;

public class FWConfig {

    public static byte[] wunderLINQConfig;
    public static byte[] flashConfig;
    public static byte[] tempConfig;
    public static String firmwareVersion;
    public static int USBVinThreshold;

    public static int firmwareVersionMajor_INDEX = 9;
    public static int firmwareVersionMinor_INDEX = 10;

    public static byte[] GET_CONFIG_CMD = {0x57, 0x52, 0x57, 0x0D, 0x0A};
    public static byte[] WRITE_CONFIG_CMD = {0x57, 0x57, 0x43, 0x41};
    public static byte[] WRITE_MODE_CMD = {0x57, 0x57, 0x53, 0x53};
    public static byte[] WRITE_SENSITIVITY_CMD = {0x57, 0x57, 0x43, 0x53};
    public static byte[] CMD_EOM = {0x0D, 0x0A};

    //FW <2.0
    public static byte[] defaultConfig1 = {0x32, 0x01, 0x04, 0x04, (byte) 0xFE,
            (byte) 0xFC, 0x4F, 0x28, 0x0F, 0x04, 0x04, (byte) 0xFD, (byte) 0xFC, 0x50, 0x29, 0x0F,
            0x04, 0x06, 0x00, 0x00, 0x00, 0x00, 0x34, 0x02, 0x01, 0x01, 0x65, 0x55, 0x4F, 0x28,
            0x07, 0x01, 0x01, (byte) 0x95, 0x55, 0x50, 0x29, 0x07, 0x01, 0x01, 0x56, 0x59, 0x52,
            0x51};

    public static byte wheelMode_full = 0x32;
    public static byte wheelMode_rtk = 0x34;

    public static byte wheelMode;
    public static byte sensitivity;

    public static int wheelMode_INDEX = 26;
    public static int sensitivity_INDEX = 34;

    //FW >=2.0
    public static int configFlashSize = 64;
    public static byte[] defaultConfig2 = {
            0x00, 0x00, // USB Input Voltage threshold
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

    public static byte keyMode_default = 0x00;
    public static byte keyMode_custom = 0x01;

    public static byte KEYBOARD_HID = 0x01;
    public static byte CONSUMER_HID = 0x02;
    public static byte UNDEFINED = 0x00;

    public static int USB = 0;
    public static int RTKPage = 1;
    public static int RTKZoomPlus = 2;
    public static int RTKZoomMinus = 3;
    public static int RTKSpeak = 4;
    public static int RTKMute = 5;
    public static int RTKDisplayOff = 6;
    public static int fullScrollUp = 7;
    public static int fullScrollDown = 8;
    public static int fullToggleRight = 9;
    public static int fullToggleLeft = 10;
    public static int fullSignalCancel = 11;

    public static int keyMode_INDEX = 25;
    public static int USBVinThresholdHigh_INDEX = 0;
    public static int USBVinThresholdLow_INDEX = 1;
    public static int RTKSensitivity_INDEX = 2;
    public static int RTKPagePressKeyType_INDEX = 3;
    public static int RTKPagePressKeyModifier_INDEX = 4;
    public static int RTKPagePressKey_INDEX = 5;
    public static int RTKPageDoublePressKeyType_INDEX = 6;
    public static int RTKPageDoublePressKeyModifier_INDEX = 7;
    public static int RTKPageDoublePressKey_INDEX = 8;
    public static int RTKZoomPPressKeyType_INDEX = 9;
    public static int RTKZoomPPressKeyModifier_INDEX = 10;
    public static int RTKZoomPPressKey_INDEX = 11;
    public static int RTKZoomPDoublePressKeyType_INDEX = 12;
    public static int RTKZoomPDoublePressKeyModifier_INDEX = 13;
    public static int RTKZoomPDoublePressKey_INDEX = 14;
    public static int RTKZoomMPressKeyType_INDEX = 15;
    public static int RTKZoomMPressKeyModifier_INDEX = 16;
    public static int RTKZoomMPressKey_INDEX = 17;
    public static int RTKZoomMDoublePressKeyType_INDEX = 18;
    public static int RTKZoomMDoublePressKeyModifier_INDEX = 19;
    public static int RTKZoomMDoublePressKey_INDEX = 20;
    public static int RTKSpeakPressKeyType_INDEX = 21;
    public static int RTKSpeakPressKeyModifier_INDEX = 22;
    public static int RTKSpeakPressKey_INDEX = 23;
    public static int RTKSpeakDoublePressKeyType_INDEX = 24;
    public static int RTKSpeakDoublePressKeyModifier_INDEX = 25;
    public static int RTKSpeakDoublePressKey_INDEX = 26;
    public static int RTKMutePressKeyType_INDEX = 27;
    public static int RTKMutePressKeyModifier_INDEX = 28;
    public static int RTKMutePressKey_INDEX = 29;
    public static int RTKMuteDoublePressKeyType_INDEX = 30;
    public static int RTKMuteDoublePressKeyModifier_INDEX = 31;
    public static int RTKMuteDoublePressKey_INDEX = 32;
    public static int RTKDisplayPressKeyType_INDEX = 33;
    public static int RTKDisplayPressKeyModifier_INDEX = 34;
    public static int RTKDisplayPressKey_INDEX = 35;
    public static int RTKDisplayDoublePressKeyType_INDEX = 36;
    public static int RTKDisplayDoublePressKeyModifier_INDEX = 37;
    public static int RTKDisplayDoublePressKey_INDEX = 38;
    public static int fullSensitivity_INDEX = 39;
    public static int fullRightPressKeyType_INDEX = 40;
    public static int fullRightPressKeyModifier_INDEX = 41;
    public static int fullRightPressKey_INDEX = 42;
    public static int fullRightLongPressKeyType_INDEX = 43;
    public static int fullRightLongPressKeyModifier_INDEX = 44;
    public static int fullRightLongPressKey_INDEX = 45;
    public static int fullLeftPressKeyType_INDEX = 46;
    public static int fullLeftPressKeyModifier_INDEX = 47;
    public static int fullLeftPressKey_INDEX = 48;
    public static int fullLeftLongPressKeyType_INDEX = 49;
    public static int fullLeftLongPressKeyModifier_INDEX = 50;
    public static int fullLeftLongPressKey_INDEX = 51;
    public static int fullScrollUpKeyType_INDEX = 52;
    public static int fullScrollUpKeyModifier_INDEX = 53;
    public static int fullScrollUpKey_INDEX = 54;
    public static int fullScrollDownKeyType_INDEX = 55;
    public static int fullScrollDownKeyModifier_INDEX = 56;
    public static int fullScrollDownKey_INDEX = 57;
    public static int fullSignalPressKeyType_INDEX = 58;
    public static int fullSignalPressKeyModifier_INDEX = 59;
    public static int fullSignalPressKey_INDEX = 60;
    public static int fullSignalLongPressKeyType_INDEX = 61;
    public static int fullSignalLongPressKeyModifier_INDEX = 62;
    public static int fullSignalLongPressKey_INDEX = 63;

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

    public FWConfig(byte[] bytes) {

        wunderLINQConfig = new byte[bytes.length];
        System.arraycopy(bytes, 0, wunderLINQConfig, 0, bytes.length);

        flashConfig = new byte[configFlashSize];
        System.arraycopy(bytes, 26, flashConfig, 0, configFlashSize);

        tempConfig = new byte[flashConfig.length];
        System.arraycopy(flashConfig, 0, tempConfig, 0, flashConfig.length);

        Log.d("FWConfig", "flashConfig: " + Utils.ByteArraytoHex(flashConfig));

        firmwareVersion = bytes[firmwareVersionMajor_INDEX] + "." + bytes[firmwareVersionMinor_INDEX];

        if (Double.parseDouble(firmwareVersion) >= 2.0) {
            keyMode = bytes[keyMode_INDEX];
            USBVinThreshold =  ((flashConfig[USBVinThresholdHigh_INDEX] & 0xFF) >> 8) | (flashConfig[USBVinThresholdLow_INDEX] & 0xFF);
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
        } else {
            sensitivity = bytes[sensitivity_INDEX];
            wheelMode = bytes[wheelMode_INDEX];
        }
    }
}
