package com.blackboxembedded.WunderLINQ.hardware.WLQ;

public interface WLQ {
    public int TYPE_NAVIGATOR = 1;
    public int TYPE_COMMANDER = 2;

    public byte[] GET_CONFIG_CMD();

    public byte[] WRITE_CONFIG_CMD();
    public byte[] WRITE_MODE_CMD();
    public byte[] CMD_EOM();

    public byte KEYMODE_DEFAULT();
    public byte KEYMODE_CUSTOM ();

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

    public byte getKeyMode();

    public byte getSensitivity();

    public String getActionName(int id);
    public String getActionValue(int id);
    public byte getActionKeyType(int id);
    public byte getActionKey(int id);
    public byte getActionKeyModifiers(int id);
    public void setActionKey(int id, byte type, byte modifiers, byte key);
}
