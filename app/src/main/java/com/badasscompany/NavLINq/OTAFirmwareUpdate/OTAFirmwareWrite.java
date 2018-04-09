package com.badasscompany.NavLINq.OTAFirmwareUpdate;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.badasscompany.NavLINq.BluetoothLeService;

/**
 * Separate class for handling the write operation during OTA firmware upgrade
 */
public class OTAFirmwareWrite {
    private BluetoothGattCharacteristic mOTACharacteristic;

    private static  final int BYTE_START_CMD = 0;
    private static  final int BYTE_CMD_TYPE = 1;
    private static  final int BYTE_CMD_DATA_SIZE = 2;
    private static  final int BYTE_CMD_DATA_SIZE_SHIFT = 3;
    private static  final int BYTE_CHECKSUM = 4;
    private static  final int BYTE_CHECKSUM_SHIFT = 5;
    private static  final int BYTE_PACKET_END = 6;

    private static  final int BYTE_PACKET_END_VER_ROW = 9;
    private static  final int BYTE_ARRAY_ID = 4;
    private static  final int BYTE_ROW = 5;
    private static  final int BYTE_ROW_SHIFT = 6;
    private static  final int BYTE_CHECKSUM_VER_ROW = 7;
    private static  final int BYTE_CHECKSUM_VER_ROW_SHIFT = 8;

    private static  final int RADIX = 16;
    private static  final int ADDITIVE_OP = 8;
    private static  final int BYTE_ARRAY_SIZE = 7;


    public OTAFirmwareWrite(BluetoothGattCharacteristic writeCharacteristic) {
        this.mOTACharacteristic = writeCharacteristic;
    }

    /**
     * OTA Bootloader enter command method
     *
     * @param checkSumType
     */
    public void OTAEnterBootLoaderCmd(String checkSumType) {
        int startCommand = 0x01;
        int dataLength0 = 0x00;
        int dataLength1 = 0x00;

        byte[] commandBytes = new byte[BYTE_ARRAY_SIZE];
        commandBytes[BYTE_START_CMD] = (byte) startCommand;
        commandBytes[BYTE_CMD_TYPE] = (byte) BootLoaderCommands.ENTER_BOOTLOADER;
        commandBytes[BYTE_CMD_DATA_SIZE] = (byte) dataLength0;
        commandBytes[BYTE_CMD_DATA_SIZE_SHIFT] = (byte) dataLength1;
        String checkSum = Integer.toHexString(BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, 16), 4, commandBytes));
        long checksum = Long.parseLong(checkSum, RADIX);
        commandBytes[BYTE_CHECKSUM] = (byte) checksum;
        commandBytes[BYTE_CHECKSUM_SHIFT] = (byte) (checksum >> ADDITIVE_OP);
        commandBytes[BYTE_PACKET_END] = (byte) BootLoaderCommands.PACKET_END;
        Log.d("OTAFirmwareWrite","OTAEnterBootLoaderCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /**
     * OTA Bootloader Get Flash Size Command
     */
    public void OTAGetFlashSizeCmd(byte[] data, String checkSumType, int dataLength) {
        byte[] commandBytes = new byte[BootLoaderCommands.BASE_CMD_SIZE + dataLength];
        int startCommand = 0x01;
        commandBytes[BYTE_START_CMD] = (byte) startCommand;
        commandBytes[BYTE_CMD_TYPE] = (byte) BootLoaderCommands.GET_FLASH_SIZE;
        commandBytes[BYTE_CMD_DATA_SIZE] = (byte) dataLength;
        commandBytes[BYTE_CMD_DATA_SIZE_SHIFT] = (byte) (dataLength >> ADDITIVE_OP);
        int dataByteLocationStart = 4;
        int datByteLocationEnd;
        for (int count = 0; count < dataLength; count++) {
            commandBytes[dataByteLocationStart] = data[count];
            dataByteLocationStart++;
        }
        datByteLocationEnd = dataByteLocationStart;
        String checkSum = Integer.toHexString(BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, RADIX), commandBytes.length, commandBytes));
        long checksum = Long.parseLong(checkSum, RADIX);
        commandBytes[datByteLocationEnd] = (byte) checksum;
        commandBytes[datByteLocationEnd + 1] = (byte) (checksum >> ADDITIVE_OP);
        commandBytes[datByteLocationEnd + 2] = (byte) BootLoaderCommands.PACKET_END;
        Log.d("OTAFirmwareWrite","OTAGetFlashSizeCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /**
     * OTA Bootloader Program Row Send Command
     */
    public void OTAProgramRowSendDataCmd(byte[] data,
                                         String checksumType) {
        int totalSize = BootLoaderCommands.BASE_CMD_SIZE +
                data.length;
        int checksum;
        int i;
        byte[] commandBytes = new byte[totalSize];
        int startCommand = 0x01;

        commandBytes[BYTE_START_CMD] = (byte) startCommand;
        commandBytes[BYTE_CMD_TYPE] = (byte) BootLoaderCommands.SEND_DATA;
        commandBytes[BYTE_CMD_DATA_SIZE] = (byte) (data.length);
        commandBytes[BYTE_CMD_DATA_SIZE_SHIFT] = (byte) ((int) ((data.length) >> ADDITIVE_OP));
        for (i = 0; i < data.length; i++)
            commandBytes[i + 4] = data[i];
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checksumType, RADIX),
                data.length + 4, commandBytes);
        commandBytes[totalSize - 3] = (byte) checksum;
        commandBytes[totalSize - 2] = (byte) (checksum >> ADDITIVE_OP);
        commandBytes[totalSize - 1] = (byte) BootLoaderCommands.PACKET_END;
        Log.d("OTAFirmwareWrite","OTAProgramRowSendDataCmd Send size--->" + commandBytes.length);
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }


    /*
    *
    * OTA Bootloader Program row Command
    * */
    public void OTAProgramRowCmd(long rowMSB, long rowLSB, int arrayID, byte[] data,
                                 String checkSumType) {

        int COMMAND_DATA_SIZE = 3;
        int totalSize = BootLoaderCommands.BASE_CMD_SIZE + COMMAND_DATA_SIZE +
                data.length;
        int checksum;
        int i;
        byte[] commandBytes = new byte[totalSize];
        int startCommand = 0x01;

        commandBytes[BYTE_START_CMD] = (byte) startCommand;
        commandBytes[BYTE_CMD_TYPE] = (byte) BootLoaderCommands.PROGRAM_ROW;
        commandBytes[BYTE_CMD_DATA_SIZE] = (byte) (data.length + COMMAND_DATA_SIZE);
        commandBytes[BYTE_CMD_DATA_SIZE_SHIFT] = (byte) ((int) ((data.length + COMMAND_DATA_SIZE) >> ADDITIVE_OP));
        commandBytes[BYTE_ARRAY_ID] = (byte) arrayID;
        commandBytes[BYTE_ROW] = (byte) rowMSB;
        commandBytes[6] = (byte) rowLSB;
        for (i = 0; i < data.length; i++)
            commandBytes[i + 7] = data[i];
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, RADIX),
                data.length + 7, commandBytes);
        commandBytes[totalSize - 3] = (byte) checksum;
        commandBytes[totalSize - 2] = (byte) (checksum >> ADDITIVE_OP);
        commandBytes[totalSize - 1] = (byte) BootLoaderCommands.PACKET_END;
        Log.d("OTAFirmwareWrite","OTAProgramRowCmd send size--->" + commandBytes.length);
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /*
   *
   * OTA Bootloader Verify row Command
   * */
    public void OTAVerifyRowCmd(long rowMSB, long rowLSB, OTAFlashRowModel model,
                                String checkSumType) {
        int COMMAND_DATA_SIZE = 3;
        int COMMAND_SIZE = BootLoaderCommands.BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        int checksum;
        byte[] commandBytes = new byte[COMMAND_SIZE];
        int startCommand = 0x01;

        commandBytes[BYTE_START_CMD] = (byte) startCommand;
        commandBytes[BYTE_CMD_TYPE] = (byte) BootLoaderCommands.VERIFY_ROW;
        commandBytes[BYTE_CMD_DATA_SIZE] = (byte) (COMMAND_DATA_SIZE);
        commandBytes[BYTE_CMD_DATA_SIZE_SHIFT] = (byte) (COMMAND_DATA_SIZE >> ADDITIVE_OP);
        commandBytes[BYTE_ARRAY_ID] = (byte) model.mArrayId;
        commandBytes[BYTE_ROW] = (byte) rowMSB;
        commandBytes[BYTE_ROW_SHIFT] = (byte) rowLSB;
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, RADIX),
                COMMAND_SIZE - 3, commandBytes);
        commandBytes[BYTE_CHECKSUM_VER_ROW] = (byte) checksum;
        commandBytes[BYTE_CHECKSUM_VER_ROW_SHIFT] = (byte) (checksum >> ADDITIVE_OP);
        commandBytes[BYTE_PACKET_END_VER_ROW] = (byte) BootLoaderCommands.PACKET_END;
        Log.d("OTAFirmwareWrite","OTAVerifyRowCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /*
   *
   * OTA Verify CheckSum Command
   * */
    public void OTAVerifyCheckSumCmd(String checkSumType) {

        int checksum;
        byte[] commandBytes = new byte[BootLoaderCommands.BASE_CMD_SIZE];
        int startCommand = 0x01;

        commandBytes[BYTE_START_CMD] = (byte) startCommand;
        commandBytes[BYTE_CMD_TYPE] = (byte) BootLoaderCommands.VERIFY_CHECK_SUM;
        commandBytes[BYTE_CMD_DATA_SIZE] = (byte) (0);
        commandBytes[BYTE_CMD_DATA_SIZE_SHIFT] = (byte) (0);
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, RADIX),
                BootLoaderCommands.BASE_CMD_SIZE - 3, commandBytes);
        commandBytes[BYTE_CHECKSUM] = (byte) checksum;
        commandBytes[BYTE_CHECKSUM_SHIFT] = (byte) (checksum >> ADDITIVE_OP);
        commandBytes[BYTE_PACKET_END] = (byte) BootLoaderCommands.PACKET_END;
        Log.d("OTAFirmwareWrite","OTAVerifyCheckSumCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /*
     *
     * Exit BootloaderCommand
     *
     * */
    public void OTAExitBootloaderCmd(String checkSumType) {

        int COMMAND_DATA_SIZE = 0x00;
        int COMMAND_SIZE = BootLoaderCommands.BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        int checksum;
        byte[] commandBytes = new byte[BootLoaderCommands.BASE_CMD_SIZE];
        int startCommand = 0x01;

        commandBytes[BYTE_START_CMD] = (byte) startCommand;
        commandBytes[BYTE_CMD_TYPE] = (byte) BootLoaderCommands.EXIT_BOOTLOADER;
        commandBytes[BYTE_CMD_DATA_SIZE] = (byte) (COMMAND_DATA_SIZE);
        commandBytes[BYTE_CMD_DATA_SIZE_SHIFT] = (byte) (COMMAND_DATA_SIZE >> ADDITIVE_OP);
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, RADIX),
                COMMAND_SIZE - 3, commandBytes);
        commandBytes[BYTE_CHECKSUM] = (byte) checksum;
        commandBytes[BYTE_CHECKSUM_SHIFT] = (byte) (checksum >> ADDITIVE_OP);
        commandBytes[BYTE_PACKET_END] = (byte) BootLoaderCommands.PACKET_END;
        Log.d("OTAFirmwareWrite","OTAExitBootloaderCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes, true);
    }

}
