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
    public int TYPE_N = 1;
    public int TYPE_X = 3;
    public int TYPE_C = 3;
    public int TYPE_U = 4;

    public byte[] GET_CONFIG_CMD();

    public byte[] WRITE_CONFIG_CMD();
    public byte[] WRITE_MODE_CMD();
    public byte[] CMD_EOM();

    public byte KEYMODE_DEFAULT();
    public byte KEYMODE_CUSTOM();
    public byte KEYMODE_MEDIA();
    public byte KEYMODE_DMD2();

    public byte KEYBOARD_HID();
    public byte CONSUMER_HID();
    public byte UNDEFINED();

    public int getHardwareType();
    public void setHardwareVersion(String version);
    public String getHardwareVersion();
    public String getFirmwareVersion();

    public byte[] getFlash();
    public byte[] getConfig();
    public byte[] getTempConfig();

    public byte[] getStatus();
    public void setStatus(byte[] status);

    public byte getKeyMode();

    public String getActionName(int id);
    public String getActionValue(int id);
    public byte getActionKeyType(int id);
    public byte getActionKey(int id);
    public byte getActionKeyModifiers(int id);
    public void setActionKey(int id, byte type, byte modifiers, byte key);
}
