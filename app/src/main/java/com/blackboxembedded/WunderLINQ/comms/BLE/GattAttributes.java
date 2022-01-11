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

import java.util.HashMap;
import java.util.UUID;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    private static HashMap<UUID, String> attributesUUID = new HashMap<UUID, String>();

    /**
     * WunderLINQ Service and Characteristics
     */
    public static final String WUNDERLINQ_SERVICE = "02997340-015f-11e5-8c2b-0002a5d5c51b";
    public static final String WUNDERLINQ_LINMESSAGE_CHARACTERISTIC = "00000003-0000-1000-8000-00805f9b34fb";
    public static final String WUNDERLINQ_CANMESSAGE_CHARACTERISTIC = "00000013-0010-0080-0000-805f9b34fb00";
    public static final String WUNDERLINQ_COMMAND_CHARACTERISTIC = "00000004-0000-1000-8000-00805f9b34fb";

    /**
     * Descriptor UUID's
     */
    public static final String CHARACTERISTIC_EXTENDED_PROPERTIES = "00002900-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_USER_DESCRIPTION = "00002901-0000-1000-8000-00805f9b34fb";
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String SERVER_CHARACTERISTIC_CONFIGURATION = "00002903-0000-1000-8000-00805f9b34fb";
    public static final String CHARACTERISTIC_PRESENTATION_FORMAT = "00002904-0000-1000-8000-00805f9b34fb";
    public static final String REPORT_REFERENCE = "00002908-0000-1000-8000-00805f9b34fb";

    /**
     * Device information characteristics
     */
    public static final String SYSTEM_ID = "00002a23-0000-1000-8000-00805f9b34fb";
    public static final String MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
    public static final String SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
    public static final String FIRMWARE_REVISION_STRING = "00002a26-0000-1000-8000-00805f9b34fb";
    public static final String HARDWARE_REVISION_STRING = "00002a27-0000-1000-8000-00805f9b34fb";
    public static final String SOFTWARE_REVISION_STRING = "00002a28-0000-1000-8000-00805f9b34fb";
    public static final String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    public static final String PNP_ID = "00002a50-0000-1000-8000-00805f9b34fb";
    public static final String IEEE = "00002a2a-0000-1000-8000-00805f9b34fb";
    public static final String DEVICE_INFORMATION_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";

    static {
        // WunderLINQ
        attributesUUID.put(UUIDDatabase.UUID_WUNDERLINQ_SERVICE, "WunderLINQ Service");
        attributesUUID.put(UUIDDatabase.UUID_WUNDERLINQ_LINMESSAGE_CHARACTERISTIC, "WunderLINQ LIN Messages");
        attributesUUID.put(UUIDDatabase.UUID_WUNDERLINQ_CANMESSAGE_CHARACTERISTIC, "WunderLINQ CAN Messages");
        attributesUUID.put(UUIDDatabase.UUID_WUNDERLINQ_COMMAND_CHARACTERISTIC, "WunderLINQ Commands");

        // Descriptors
        attributesUUID.put(UUIDDatabase.UUID_CHARACTERISTIC_EXTENDED_PROPERTIES, "Characteristic Extended Properties");
        attributesUUID.put(UUIDDatabase.UUID_CHARACTERISTIC_USER_DESCRIPTION, "Characteristic User Description");
        attributesUUID.put(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG, "Client Characteristic Configuration");
        attributesUUID.put(UUIDDatabase.UUID_SERVER_CHARACTERISTIC_CONFIGURATION, "Server Characteristic Configuration");
        attributesUUID.put(UUIDDatabase.UUID_CHARACTERISTIC_PRESENTATION_FORMAT, "Characteristic Presentation Format");
        attributesUUID.put(UUIDDatabase.UUID_REPORT_REFERENCE, "Report Reference");

        // Device Information Characteristics
        attributesUUID.put(UUIDDatabase.UUID_SYSTEM_ID, "System ID");
        attributesUUID.put(UUIDDatabase.UUID_MODEL_NUMBER_STRING, "Model Number String");
        attributesUUID.put(UUIDDatabase.UUID_SERIAL_NUMBER_STRING, "Serial Number String");
        attributesUUID.put(UUIDDatabase.UUID_FIRMWARE_REVISION_STRING, "Firmware Revision String");
        attributesUUID.put(UUIDDatabase.UUID_HARDWARE_REVISION_STRING, "Hardware Revision String");
        attributesUUID.put(UUIDDatabase.UUID_SOFTWARE_REVISION_STRING, "Software Revision String");
        attributesUUID.put(UUIDDatabase.UUID_MANUFACTURE_NAME_STRING, "Manufacturer Name String");
        attributesUUID.put(UUIDDatabase.UUID_PNP_ID, "PnP ID");
        attributesUUID.put(UUIDDatabase.UUID_IEEE,
                "IEEE 11073-20601 Regulatory Certification Data List");
    }
    public static String lookupUUID(UUID uuid, String defaultName) {
        String name = attributesUUID.get(uuid);
        return name == null ? defaultName : name;
    }
}
