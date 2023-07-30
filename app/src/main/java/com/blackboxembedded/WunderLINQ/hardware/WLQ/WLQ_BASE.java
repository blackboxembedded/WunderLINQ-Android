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

public abstract class WLQ_BASE implements WLQ {
    //Commands
    public static byte[] GET_CONFIG_CMD = {0x57, 0x52, 0x57, 0x0D, 0x0A};
    public static byte[] WRITE_CONFIG_CMD = {0x57, 0x57, 0x43, 0x41};
    public static byte[] WRITE_MODE_CMD = {0x57, 0x57, 0x53, 0x53};
    public static byte[] CMD_EOM = {0x0D, 0x0A};

    public static byte KEYMODE_DEFAULT = 0x00;
    public static byte KEYMODE_CUSTOM = 0x01;
    public static byte KEYMODE_MEDIA = 0x02;
    public static byte KEYMODE_DMD2 = 0x03;
    public static byte KEYBOARD_HID = 0x01;
    public static byte CONSUMER_HID = 0x02;
    public static byte UNDEFINED = 0x00;
}
