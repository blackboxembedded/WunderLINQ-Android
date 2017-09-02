package com.badasscompany.NavLINq;

/**
 * Created by keithconger on 7/22/17.
 */

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static String MOTORCYCLE_SERVICE = "02997340-015f-11e5-8c2b-0002a5d5c51b";
    public static String LIN_MESSAGE_CHARACTERISTIC = "00000003-0000-1000-8000-00805f9b34fb";

    static {
        // Services
        attributes.put(MOTORCYCLE_SERVICE, "Motorcycle Service");
        // Characteristics
        attributes.put(LIN_MESSAGE_CHARACTERISTIC, "LIN Message");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
