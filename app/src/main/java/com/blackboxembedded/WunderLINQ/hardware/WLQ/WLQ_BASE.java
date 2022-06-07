package com.blackboxembedded.WunderLINQ.hardware.WLQ;

public abstract class WLQ_BASE implements WLQ {
    //Commands
    public static byte[] GET_CONFIG_CMD = {0x57, 0x52, 0x57, 0x0D, 0x0A};
    public static byte[] WRITE_CONFIG_CMD = {0x57, 0x57, 0x43, 0x41};
    public static byte[] WRITE_MODE_CMD = {0x57, 0x57, 0x53, 0x53};
    public static byte[] CMD_EOM = {0x0D, 0x0A};

    public static byte KEYMODE_DEFAULT = 0x00;
    public static byte KEYMODE_CUSTOM = 0x01;
    public static byte KEYBOARD_HID = 0x01;
    public static byte CONSUMER_HID = 0x02;
    public static byte UNDEFINED = 0x00;
}
