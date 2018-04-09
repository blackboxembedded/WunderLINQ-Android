package com.badasscompany.NavLINq.OTAFirmwareUpdate;

/**
 * Class created for bootloader commans constants
 */
class BootLoaderCommands {

    /**
     * Bootloading Commands
     */
    public static final int VERIFY_CHECK_SUM = 0x31;
    public static final int GET_FLASH_SIZE = 0x32;
    // --Commented out by Inspection (25/3/15 4:33 PM):public static final String GET_APPLICATION_SIZE = "0x33";
    // --Commented out by Inspection (25/3/15 4:33 PM):public static final String ERASE_ROW = "0x34";
    // --Commented out by Inspection (25/3/15 4:33 PM):public static final String SYNC_BOOTLOADER = "0x35";
    // --Commented out by Inspection (25/3/15 4:33 PM):public static final String SET_ACTIVE_APPLICATION = "0x36";
    public static final int ENTER_BOOTLOADER = 0x38;
    public static final int SEND_DATA = 0x37;
    public static final int PROGRAM_ROW = 0x39;
    public static final int VERIFY_ROW = 0x3A;
    public static final int EXIT_BOOTLOADER = 0x3B;
    public static final int PACKET_END = 0x17;
    public static final int MAX_DATA_SIZE = 133;
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
// --Commented out by Inspection START (25/3/15 4:33 PM):
////////////////////////////////    /**
////////////////////////////////     * Bootloading Status / Error Codes
////////////////////////////////     */
////////////////////////////////    public static final int CYRET_SUCCESS = 0x00;
//////////////////////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
//////////////////////////////    public static final String CYRET_ERR_FILE = "0x01";
////////////////////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
////////////////////////////    public static final String CYRET_ERR_EOF = "0x02";
//////////////////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
//////////////////////////    public static final String CYRET_ERR_LENGTH = "0x03";
////////////////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
////////////////////////    public static final String CYRET_ERR_DATA = "0x04";
//////////////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
//////////////////////    public static final String CYRET_ERR_CMD = "0x05";
////////////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
////////////////////    public static final String CYRET_ERR_DEVICE = "0x06";
//////////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
//////////////////    public static final String CYRET_ERR_VERSION = "0x07";
////////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
////////////////    public static final String CYRET_ERR_CHECKSUM = "0x08";
//////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
//////////////    public static final String CYRET_ERR_ARRAY = "0x09";
////////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
////////////    public static final String CYRET_ERR_ROW = "0x0A";
//////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
//////////    public static final String CYRET_BTLDR = "0x0B";
////////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
////////    public static final String CYRET_ERR_APP = "0x0C";
//////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
//////    public static final String CYRET_ERR_ACTIVE = "0x0D";
////// --Commented out by Inspection STOP (25/3/15 4:33 PM)
////    public static final String CYRET_ERR_UNK = "0x0F";
//// --Commented out by Inspection STOP (25/3/15 4:33 PM)
//    public static final String CYRET_ABORT = "0xFF";
// --Commented out by Inspection STOP (25/3/15 4:33 PM)

    public static final int BASE_CMD_SIZE = 0x07;
    // --Commented out by Inspection (25/3/15 4:33 PM):public static final int BASE_PROGRAM_ROW_CMD_SIZE = 10;

}
