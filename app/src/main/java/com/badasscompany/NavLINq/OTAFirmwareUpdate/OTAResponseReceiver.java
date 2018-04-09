/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign),
 * United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate
 * license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 *
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 *
 *
 */

package com.badasscompany.NavLINq.OTAFirmwareUpdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.badasscompany.NavLINq.BluetoothLeService;

/**
 * Receiver class for OTA response
 */
public class OTAResponseReceiver extends BroadcastReceiver {

    private Context mContext;

    //Substring Constants
    private static final int RESPONSE_START = 2;
    private static final int RESPONSE_END = 4;

    private static final int STATUS_START = 4;
    private static final int STATUS_END = 6;
    private static final int CHECKSUM_START = 4;
    private static final int CHECKSUM_END = 6;

    private static final int SILICON_ID_START = 8;
    private static final int SILICON_ID_END = 16;
    private static final int SILICON_REV_START = 16;
    private static final int SILICON_REV_END = 18;

    private static final int START_ROW_START = 8;
    private static final int START_ROW_END = 12;
    private static final int END_ROW_START = 12;
    private static final int END_ROW_END = 16;

    private static final int DATA_START = 8;
    private static final int DATA_END = 10;

    private static final int RADIX = 16;

    //Switch case Constants
    private static final int CASE_SUCCESS = 0;
    private static final int CASE_ERR_FILE = 1;
    private static final int CASE_ERR_EOF = 2;
    private static final int CASE_ERR_LENGTH = 3;
    private static final int CASE_ERR_DATA = 4;
    private static final int CASE_ERR_CMD = 5;
    private static final int CASE_ERR_DEVICE = 6;
    private static final int CASE_ERR_VERSION = 7;
    private static final int CASE_ERR_CHECKSUM = 8;
    private static final int CASE_ERR_ARRAY = 9;
    private static final int CASE_ERR_ROW = 10;
    private static final int CASE_BTLDR = 11;
    private static final int CASE_ERR_APP = 12;
    private static final int CASE_ERR_ACTIVE = 13;
    private static final int CASE_ERR_UNK = 14;
    private static final int CASE_ABORT = 15;


    //Error Constants
    private static final String CYRET_ERR_FILE = "CYRET_ERR_FILE";
    private static final String CYRET_ERR_EOF = "CYRET_ERR_EOF";
    private static final String CYRET_ERR_LENGTH = "CYRET_ERR_LENGTH";
    private static final String CYRET_ERR_DATA = "CYRET_ERR_DATA";
    private static final String CYRET_ERR_CMD = "CYRET_ERR_CMD";
    private static final String CYRET_ERR_DEVICE = "CYRET_ERR_DEVICE";
    private static final String CYRET_ERR_VERSION = "CYRET_ERR_VERSION";
    private static final String CYRET_ERR_CHECKSUM = "CYRET_ERR_CHECKSUM";
    private static final String CYRET_ERR_ARRAY = "CYRET_ERR_ARRAY";
    private static final String CYRET_BTLDR = "CYRET_BTLDR";
    private static final String CYRET_ERR_APP = "CYRET_ERR_APP";
    private static final String CYRET_ERR_ACTIVE = "CYRET_ERR_ACTIVE";
    private static final String CYRET_ERR_UNK = "CYRET_ERR_UNK";
    private static final String CYRET_ERR_ROW = "CYRET_ERR_ROW";
    private static final String CYRET_ABORT = "CYRET_ABORT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        this.mContext = context;
        Log.d("OTAResponseReceiver","In onRecieve");
        /**
         * Condition to execute the next command to execute
         * Checks the Shared preferences for the currently executing command
         */
        if (BluetoothLeService.ACTION_OTA_DATA_AVAILABLE.equals(action)) {
            Log.d("OTAResponseReceiver","In onRecieve ota_data_available");
            byte[] responseArray = intent
                    .getByteArrayExtra(Constants.EXTRA_BYTE_VALUE);
            String hexValue = Utils.ByteArraytoHex(responseArray);
            if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.ENTER_BOOTLOADER)) {
                parseEnterBootLoaderAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.GET_FLASH_SIZE)) {
                parseGetFlashSizeAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.SEND_DATA)) {
                parseParseSendDataAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.PROGRAM_ROW)) {
                parseParseRowAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.VERIFY_ROW)) {
                parseVerifyRowAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.VERIFY_CHECK_SUM)) {
                parseVerifyCheckSum(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.EXIT_BOOTLOADER)) {
                parseExitBootloader(hexValue);
            } else {
                Log.d("OTAResponseReceiver","In Receiver No case " + Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE));
            }
        }
    }

    private void parseParseSendDataAcknowledgement(String hexValue) {
        String result = hexValue.trim().replace(" ", "");
        String response = result.substring(RESPONSE_START, RESPONSE_END);
        String status = result.substring(STATUS_START, STATUS_END);
        int reponseBytes = Integer.parseInt(response, RADIX);
        switch (reponseBytes) {
            case CASE_SUCCESS:
                Log.d("OTAResponseReceiver","CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_SEND_DATA_ROW_STATUS,
                        status);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            default:
                broadCastErrors(reponseBytes);
                Log.d("OTAResponseReceiver","CYRET ERROR");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseEnterBootLoaderAcknowledgement(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(RESPONSE_START, RESPONSE_END);
        Log.d("OTAResponseReceiver","Response>>>>>" + result);
        int reponseBytes = Integer.parseInt(response, RADIX);
        switch (reponseBytes) {
            case CASE_SUCCESS:
                Log.d("OTAResponseReceiver","CYRET_SUCCESS");
                String siliconID = result.substring(SILICON_ID_START, SILICON_ID_END);
                String siliconRev = result.substring(SILICON_REV_START, SILICON_REV_END);
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_SILICON_ID,
                        siliconID);
                mBundle.putString(Constants.EXTRA_SILICON_REV, siliconRev);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            default:
                broadCastErrors(reponseBytes);
                Log.d("OTAResponseReceiver","CYRET ERROR");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseGetFlashSizeAcknowledgement(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(RESPONSE_START, RESPONSE_END);
        Log.d("OTAResponseReceiver","Get flash size Response>>>>>" + result);
        int reponseBytes = Integer.parseInt(response, RADIX);
        switch (reponseBytes) {
            case CASE_SUCCESS:
                Log.d("OTAResponseReceiver","CYRET_SUCCESS");
                int startRow = BootLoaderUtils.swap(Integer.parseInt(result.substring(START_ROW_START, START_ROW_END), RADIX));
                int endRow = BootLoaderUtils.swap(Integer.parseInt(result.substring(END_ROW_START, END_ROW_END), RADIX));
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_START_ROW,
                        "" + startRow);
                mBundle.putString(Constants.EXTRA_END_ROW, "" + endRow);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            default:
                broadCastErrors(reponseBytes);
                Log.d("OTAResponseReceiver","CYRET ERROR");
                break;
        }
    }


    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseParseRowAcknowledgement(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(RESPONSE_START, RESPONSE_END);
        String status = result.substring(STATUS_START, STATUS_END);
        int reponseBytes = Integer.parseInt(response, RADIX);
        switch (reponseBytes) {
            case CASE_SUCCESS:
                Log.d("OTAResponseReceiver","CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_PROGRAM_ROW_STATUS,
                        status);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            default:
                broadCastErrors(reponseBytes);
                Log.d("OTAResponseReceiver","CYRET ERROR");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseVerifyRowAcknowledgement(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(RESPONSE_START, RESPONSE_END);
        String data = result.substring(DATA_START, DATA_END);
        int reponseBytes = Integer.parseInt(response, RADIX);
        switch (reponseBytes) {
            case CASE_SUCCESS:
                Log.d("OTAResponseReceiver","CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_VERIFY_ROW_STATUS,
                        response);
                mBundle.putString(Constants.EXTRA_VERIFY_ROW_CHECKSUM,
                        data);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            default:
                broadCastErrors(reponseBytes);
                Log.d("OTAResponseReceiver","CYRET ERROR");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseVerifyCheckSum(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(RESPONSE_START, RESPONSE_END);
        String checkSumStatus = result.substring(CHECKSUM_START, CHECKSUM_END);
        int reponseBytes = Integer.parseInt(response, RADIX);
        switch (reponseBytes) {
            case CASE_SUCCESS:
                Log.d("OTAResponseReceiver","CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_VERIFY_CHECKSUM_STATUS,
                        checkSumStatus);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            default:
                broadCastErrors(reponseBytes);
                Log.d("OTAResponseReceiver","CYRET ERROR");
                break;
        }
    }


    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseExitBootloader(String parse) {
        String response = parse.trim().replace(" ", "");
        //int responseBytes = Integer.parseInt(response, RADIX);
        Log.d("OTAResponseReceiver","Reponse Byte Exit>>" + response);
        Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
        Bundle mBundle = new Bundle();
        mBundle.putString(Constants.EXTRA_VERIFY_EXIT_BOOTLOADER,
                response);
        intent.putExtras(mBundle);
        mContext.sendBroadcast(intent);
    }

    public void broadCastErrorMessage(String errorMessage) {
        Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
        Bundle mBundle = new Bundle();
        mBundle.putString(Constants.EXTRA_ERROR_OTA,
                errorMessage);
        intent.putExtras(mBundle);
        mContext.sendBroadcast(intent);
    }

    public void broadCastErrors(int errorkey) {
        switch (errorkey) {
            case CASE_ERR_FILE:
                Log.d("OTAResponseReceiver","CYRET_ERR_FILE");
                broadCastErrorMessage(CYRET_ERR_FILE);
                break;
            case CASE_ERR_EOF:
                Log.d("OTAResponseReceiver","CYRET_ERR_EOF");
                broadCastErrorMessage(CYRET_ERR_EOF);
                break;
            case CASE_ERR_LENGTH:
                Log.d("OTAResponseReceiver","CYRET_ERR_LENGTH");
                broadCastErrorMessage(CYRET_ERR_LENGTH);
                break;
            case CASE_ERR_DATA:
                Log.d("OTAResponseReceiver","CYRET_ERR_DATA");
                broadCastErrorMessage(CYRET_ERR_DATA);
                break;
            case CASE_ERR_CMD:
                Log.d("OTAResponseReceiver","CYRET_ERR_CMD");
                broadCastErrorMessage(CYRET_ERR_CMD);
                break;
            case CASE_ERR_DEVICE:
                Log.d("OTAResponseReceiver","CYRET_ERR_DEVICE");
                broadCastErrorMessage(CYRET_ERR_DEVICE);
                break;
            case CASE_ERR_VERSION:
                Log.d("OTAResponseReceiver","CYRET_ERR_VERSION");
                broadCastErrorMessage(CYRET_ERR_VERSION);
                break;
            case CASE_ERR_CHECKSUM:
                Log.d("OTAResponseReceiver","CYRET_ERR_CHECKSUM");
                broadCastErrorMessage(CYRET_ERR_CHECKSUM);
                break;
            case CASE_ERR_ARRAY:
                Log.d("OTAResponseReceiver","CYRET_ERR_ARRAY");
                broadCastErrorMessage(CYRET_ERR_ARRAY);
                break;
            case CASE_ERR_ROW:
                Log.d("OTAResponseReceiver","CYRET_ERR_ROW");
                broadCastErrorMessage(CYRET_ERR_ROW);
                break;
            case CASE_BTLDR:
                Log.d("OTAResponseReceiver","CYRET_BTLDR");
                broadCastErrorMessage(CYRET_BTLDR);
                break;
            case CASE_ERR_APP:
                Log.d("OTAResponseReceiver","CYRET_ERR_APP");
                broadCastErrorMessage(CYRET_ERR_APP);
                break;
            case CASE_ERR_ACTIVE:
                Log.d("OTAResponseReceiver","CYRET_ERR_ACTIVE");
                broadCastErrorMessage(CYRET_ERR_ACTIVE);
                break;
            case CASE_ERR_UNK:
                Log.d("OTAResponseReceiver","CYRET_ERR_UNK");
                broadCastErrorMessage(CYRET_ERR_UNK);
                break;
            case CASE_ABORT:
                Log.d("OTAResponseReceiver","CYRET_ABORT");
                broadCastErrorMessage(CYRET_ABORT);
                break;
            default:
                Log.d("OTAResponseReceiver","CYRET DEFAULT");
                break;
        }
    }
}
