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

public interface WLQ {
    int TYPE_N = 1;
    int TYPE_X = 2;
    int TYPE_S = 3;
    int TYPE_U = 4;

    byte KEYBOARD_HID = 0x01;
    byte CONSUMER_HID = 0x02;
    byte UNDEFINED = 0x00;

    int USB = 1;
    int doublePressSensitivity = 2;
    int longPressSensitivity = 3;
    int RTKPage = 4;
    int RTKPageDoublePress = 5;
    int RTKZoomPlus = 6;
    int RTKZoomPlusDoublePress = 7;
    int RTKZoomMinus = 8;
    int RTKZoomMinusDoublePress = 9;
    int RTKSpeak = 10;
    int RTKSpeakDoublePress = 11;
    int RTKMute = 12;
    int RTKMuteDoublePress = 13;
    int RTKDisplayOff = 14;
    int RTKDisplayOffDoublePress = 15;
    int fullScrollUp = 16;
    int fullScrollDown = 17;
    int fullToggleRight = 18;
    int fullToggleRightLongPress = 19;
    int fullToggleLeft = 20;
    int fullToggleLeftLongPress = 21;
    int fullSignalCancel = 22;
    int fullSignalCancelLongPress = 23;
    int up = 26;
    int upLong = 27;
    int down = 28;
    int downLong = 29;
    int right = 30;
    int rightLong = 31;
    int left = 32;
    int leftLong = 33;
    int fx1 = 34;
    int fx1Long = 35;
    int fx2 = 36;
    int fx2Long = 37;
    int pdmChannel1 = 50;
    int pdmChannel2 = 51;
    int pdmChannel3 = 52;
    int pdmChannel4 = 53;
    int KEYMODE = 100;
    int ORIENTATION = 101;

    byte[] GET_CONFIG_CMD();

    byte[] WRITE_CONFIG_CMD();
    byte[] WRITE_MODE_CMD();
    byte[] CMD_EOM();

    byte KEYMODE_DEFAULT();
    byte KEYMODE_CUSTOM();
    byte KEYMODE_MEDIA();
    byte KEYMODE_DMD2();

    byte KEYBOARD_HID();
    byte CONSUMER_HID();
    byte UNDEFINED();

    int getHardwareType();
    void setHardwareVersion(String version);
    String getHardwareVersion();
    String getFirmwareVersion();

    byte[] getFlash();
    byte[] getDefaultConfig();
    byte[] getConfig();
    byte[] getTempConfig();

    byte getAccessories();
    void setAccActive(int active);
    byte[] getStatus();
    void setStatus(byte[] status);

    byte getKeyMode();

    String getActionName(int id);
    String getActionValue(int id);
    void setActionValue(int id, byte value);
    byte getActionKeyType(int id);
    byte getActionKey(int id);
    byte getActionKeyModifiers(int id);
    void setActionKey(int id, byte type, byte modifiers, byte key);
}
