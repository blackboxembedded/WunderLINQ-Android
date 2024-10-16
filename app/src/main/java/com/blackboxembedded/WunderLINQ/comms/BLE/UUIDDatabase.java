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
package com.blackboxembedded.WunderLINQ.comms.BLE;

import java.util.UUID;

/**
 * This class will store the UUID of the GATT services and characteristics
 */
public class UUIDDatabase {

    /**
     * WunderLINQ related UUID
     */
    public static final UUID UUID_WUNDERLINQ_SERVICE = UUID
            .fromString(GattAttributes.WUNDERLINQ_SERVICE);

    public static final UUID UUID_WUNDERLINQ_PERFORMANCE_CHARACTERISTIC = UUID
            .fromString(GattAttributes.WUNDERLINQ_PERFORMANCE_CHARACTERISTIC);

    public final static UUID UUID_WUNDERLINQ_N_COMMAND_CHARACTERISTIC = UUID
            .fromString(GattAttributes.WUNDERLINQ_N_COMMAND_CHARACTERISTIC);

    public final static UUID UUID_WUNDERLINQ_X_COMMAND_CHARACTERISTIC = UUID
            .fromString(GattAttributes.WUNDERLINQ_X_COMMAND_CHARACTERISTIC);

    public final static UUID UUID_WUNDERLINQ_C_COMMAND_CHARACTERISTIC = UUID
            .fromString(GattAttributes.WUNDERLINQ_C_COMMAND_CHARACTERISTIC);

    public final static UUID UUID_WUNDERLINQ_U_COMMAND_CHARACTERISTIC = UUID
            .fromString(GattAttributes.WUNDERLINQ_U_COMMAND_CHARACTERISTIC);

    /**
     * Descriptor UUID
     */
    public final static UUID UUID_CLIENT_CHARACTERISTIC_CONFIG = UUID
            .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
    public final static UUID UUID_CHARACTERISTIC_EXTENDED_PROPERTIES = UUID
            .fromString(GattAttributes.CHARACTERISTIC_EXTENDED_PROPERTIES);
    public final static UUID UUID_CHARACTERISTIC_USER_DESCRIPTION = UUID
            .fromString(GattAttributes.CHARACTERISTIC_USER_DESCRIPTION);
    public final static UUID UUID_SERVER_CHARACTERISTIC_CONFIGURATION = UUID
            .fromString(GattAttributes.SERVER_CHARACTERISTIC_CONFIGURATION);
    public final static UUID UUID_CHARACTERISTIC_PRESENTATION_FORMAT = UUID
            .fromString(GattAttributes.CHARACTERISTIC_PRESENTATION_FORMAT);
    public final static UUID UUID_REPORT_REFERENCE = UUID
            .fromString(GattAttributes.REPORT_REFERENCE);

    /**
     * Device information related UUID
     */
    public final static UUID UUID_DEVICE_INFORMATION_SERVICE = UUID
            .fromString(GattAttributes.DEVICE_INFORMATION_SERVICE);
    public final static UUID UUID_SYSTEM_ID = UUID
            .fromString(GattAttributes.SYSTEM_ID);
    public static final UUID UUID_MANUFACTURE_NAME_STRING = UUID
            .fromString(GattAttributes.MANUFACTURER_NAME_STRING);
    public static final UUID UUID_MODEL_NUMBER_STRING = UUID
            .fromString(GattAttributes.MODEL_NUMBER_STRING);
    public static final UUID UUID_SERIAL_NUMBER_STRING = UUID
            .fromString(GattAttributes.SERIAL_NUMBER_STRING);
    public static final UUID UUID_HARDWARE_REVISION_STRING = UUID
            .fromString(GattAttributes.HARDWARE_REVISION_STRING);
    public static final UUID UUID_FIRMWARE_REVISION_STRING = UUID
            .fromString(GattAttributes.FIRMWARE_REVISION_STRING);
    public static final UUID UUID_SOFTWARE_REVISION_STRING = UUID
            .fromString(GattAttributes.SOFTWARE_REVISION_STRING);
    public static final UUID UUID_PNP_ID = UUID
            .fromString(GattAttributes.PNP_ID);
    public static final UUID UUID_IEEE = UUID
            .fromString(GattAttributes.IEEE);
}