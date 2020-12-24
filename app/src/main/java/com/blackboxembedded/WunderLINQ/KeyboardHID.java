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

public class KeyboardHID {

    public static String getKeyboardKeyByCode(byte code) {
        int i = -1;
        for (String cc: MyApplication.getContext().getResources().getStringArray(R.array.hid_keyboard_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return MyApplication.getContext().getResources().getStringArray(R.array.hid_keyboard_usage_table_names_array)[i];
    }

    public static String getConsumerKeyByCode(byte code) {
        int i = -1;
        for (String cc: MyApplication.getContext().getResources().getStringArray(R.array.hid_consumer_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return MyApplication.getContext().getResources().getStringArray(R.array.hid_consumer_usage_table_names_array)[i];
    }

    public static int getModifierKeyPositionByCode(byte code) {
        int i = -1;
        for (String cc: MyApplication.getContext().getResources().getStringArray(R.array.hid_keyboard_modifier_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return i;
    }

    public static int getKeyboardKeyPositionByCode(byte code) {
        int i = -1;
        for (String cc: MyApplication.getContext().getResources().getStringArray(R.array.hid_keyboard_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return i;
    }

    public static int getConsumerKeyPositionByCode(byte code) {
        int i = -1;
        for (String cc: MyApplication.getContext().getResources().getStringArray(R.array.hid_consumer_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return i;
    }
}
