package com.blackboxembedded.WunderLINQ;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.blackboxembedded.WunderLINQ.OTAFirmwareUpdate.Constants;
import com.blackboxembedded.WunderLINQ.OTAFirmwareUpdate.DescriptorParser;
import com.blackboxembedded.WunderLINQ.OTAFirmwareUpdate.UUIDDatabase;
import com.blackboxembedded.WunderLINQ.OTAFirmwareUpdate.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given BlueTooth LE device.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = "BluetoothLeService";

    private static Logger logger = null;
    static FaultStatus faults = null;

    private static int prevBrakeValue = 0;

    /**
     * GATT Status constants
     */
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_DISCONNECTED_CAROUSEL =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_CAROUSEL";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_OTA_DATA_AVAILABLE =
            "com.blackboxembedded.wunderlinq.bluetooth.le.ACTION_OTA_DATA_AVAILABLE";
    public final static String ACTION_GATT_DISCONNECTED_OTA =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_OTA";
    public final static String ACTION_GATT_CONNECT_OTA =
            "com.example.bluetooth.le.ACTION_GATT_CONNECT_OTA";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_OTA =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_OTA";
    public final static String ACTION_GATT_CHARACTERISTIC_ERROR =
            "com.example.bluetooth.le.ACTION_GATT_CHARACTERISTIC_ERROR";
    public final static String ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL =
            "com.example.bluetooth.le.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL";
    public final static String ACTION_PAIR_REQUEST =
            "android.bluetooth.device.action.PAIRING_REQUEST";
    public final static String ACTION_WRITE_COMPLETED =
            "android.bluetooth.device.action.ACTION_WRITE_COMPLETED";
    public final static String ACTION_WRITE_FAILED =
            "android.bluetooth.device.action.ACTION_WRITE_FAILED";
    public final static String ACTION_WRITE_SUCCESS =
            "android.bluetooth.device.action.ACTION_WRITE_SUCCESS";
    /**
     * Connection status Constants
     */
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 4;
    private final static String ACTION_GATT_DISCONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTING";
    private final static String ACTION_PAIRING_REQUEST =
            "com.example.bluetooth.le.PAIRING_REQUEST";
    private static final int STATE_BONDED = 5;
    /**
     * BluetoothAdapter for handling connections
     */
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothGatt mBluetoothGatt;
    /**
     * Disable?enable notification
     */
    public static ArrayList<BluetoothGattCharacteristic> mEnabledCharacteristics =
            new ArrayList<BluetoothGattCharacteristic>();

    public static boolean mDisableNotificationFlag = false;


    private static int mConnectionState = STATE_DISCONNECTED;
    private static boolean mOtaExitBootloaderCmdInProgress = false;
    /**
     * Device address
     */
    private static String mBluetoothDeviceAddress;
    private static String mBluetoothDeviceName;
    private static Context mContext;

    /**
     * Implements callback methods for GATT events that the app cares about. For
     * example,connection change and services discovered.
     */
    private final static BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {

            Log.d(TAG,"onConnectionStateChange");
            String intentAction;
            // GATT Server connected
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTED;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_established);
                Log.d(TAG,dataLog);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_DISCONNECTED;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_disconnected);
                Log.d(TAG,dataLog);
                Data.clear();
                faults.clear();
            }
            // GATT Server Connecting
            if (newState == BluetoothProfile.STATE_CONNECTING) {
                intentAction = ACTION_GATT_CONNECTING;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTING;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_establishing);
                Log.d(TAG,dataLog);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                intentAction = ACTION_GATT_DISCONNECTING;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_DISCONNECTING;
                }
                broadcastConnectionUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // GATT Services discovered
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog2 = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_service_discovery_status) +
                        mContext.getResources().getString(R.string.dl_status_success);
                Log.d(TAG,dataLog2);
                broadcastConnectionUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION ||
                    status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                bondDevice();
                broadcastConnectionUpdate(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
            } else {
                String dataLog2 = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_service_discovery_status) +
                        mContext.getResources().getString(R.string.dl_status_failure) + status;
                Log.d(TAG,dataLog2);
                broadcastConnectionUpdate(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().
                    getService().getUuid(), serviceUUID);


            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().
                    getUuid(), characteristicUUID);

            String descriptorUUID = descriptor.getUuid().toString();
            String descriptorName = GattAttributes.lookupUUID(descriptor.getUuid(), descriptorUUID);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[00]";
                Intent intent = new Intent(ACTION_WRITE_SUCCESS);
                mContext.sendBroadcast(intent);
                Log.d(TAG,dataLog);
                if (descriptor.getValue() != null)
                    addRemoveData(descriptor);
                if (mDisableNotificationFlag) {
                    disableAllEnabledCharacteristics();
                }
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                    || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                bondDevice();
                Intent intent = new Intent(ACTION_WRITE_FAILED);
                mContext.sendBroadcast(intent);
            } else {
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_status_failure) +
                        +status;
                Log.d(TAG,dataLog);
                mDisableNotificationFlag = false;
                Intent intent = new Intent(ACTION_WRITE_FAILED);
                mContext.sendBroadcast(intent);
            }
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getService().getUuid(), serviceUUID);

            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getUuid(), characteristicUUID);

            String descriptorUUIDText = descriptor.getUuid().toString();
            String descriptorName = GattAttributes.lookupUUID(descriptor.getUuid(), descriptorUUIDText);

            String descriptorValue = " " + Utils.ByteArraytoHex(descriptor.getValue()) + " ";
            if (status == BluetoothGatt.GATT_SUCCESS) {
                UUID descriptorUUID = descriptor.getUuid();
                final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                Bundle mBundle = new Bundle();
                // Putting the byte value read for GATT Db
                mBundle.putByteArray(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE,
                        descriptor.getValue());
                mBundle.putInt(Constants.EXTRA_BYTE_DESCRIPTOR_INSTANCE_VALUE,
                        descriptor.getCharacteristic().getInstanceId());
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_response) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + descriptorValue + "]";
                Log.d(TAG,dataLog);
                mBundle.putString(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_UUID,
                        descriptor.getUuid().toString());
                mBundle.putString(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_CHARACTERISTIC_UUID,
                        descriptor.getCharacteristic().getUuid().toString());
                if (descriptorUUID.equals(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG)) {
                    String valueReceived = DescriptorParser
                            .getClientCharacteristicConfiguration(descriptor, mContext);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, valueReceived);
                } else if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_EXTENDED_PROPERTIES)) {
                    HashMap<String, String> receivedValuesMap = DescriptorParser
                            .getCharacteristicExtendedProperties(descriptor, mContext);
                    String reliableWriteStatus = receivedValuesMap.get(Constants.FIRST_BIT_KEY_VALUE);
                    String writeAuxillaryStatus = receivedValuesMap.get(Constants.SECOND_BIT_KEY_VALUE);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, reliableWriteStatus + "\n"
                            + writeAuxillaryStatus);
                } else if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_USER_DESCRIPTION)) {
                    String description = DescriptorParser
                            .getCharacteristicUserDescription(descriptor);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, description);
                } else if (descriptorUUID.equals(UUIDDatabase.UUID_SERVER_CHARACTERISTIC_CONFIGURATION)) {
                    String broadcastStatus = DescriptorParser.
                            getServerCharacteristicConfiguration(descriptor, mContext);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, broadcastStatus);
                }
                intent.putExtras(mBundle);
                /**
                 * Sending the broad cast so that it can be received on
                 * registered receivers
                 */

                mContext.sendBroadcast(intent);
            } else {
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Log.d(TAG,dataLog);
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String dataLog = "";
            if (status == BluetoothGatt.GATT_SUCCESS) {
                dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_status_success);
                if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_DFU_CHARACTERISTIC)){
                    Log.d(TAG,"DFU Write Success");
                    Set<BluetoothDevice> pairedDevices = MainActivity.mBluetoothAdapter.getBondedDevices();
                    if (!pairedDevices.isEmpty()) {
                        for (BluetoothDevice devices : pairedDevices) {
                            if (devices.getName().equals("WunderLINQ")) {
                                Log.d(TAG, "WunderLINQ previously paired");
                                //TODO: KEC playing around
                                /*
                                try {
                                    Log.d("unpairDevice()", "Start Un-Pairing...");
                                    Method m = devices.getClass().getMethod("removeBond", (Class[]) null);
                                    m.invoke(devices, (Object[]) null);
                                    Log.d("unpairDevice()", "Un-Pairing finished.");
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage());
                                }
                                */
                                //TODO: KEC playing around end
                            }
                        }
                    }
                }

                //timeStamp("OTA WRITE RESPONSE TIMESTAMP ");

                Log.d(TAG,dataLog);
            } else {
                dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Intent intent = new Intent(ACTION_GATT_CHARACTERISTIC_ERROR);
                intent.putExtra(Constants.EXTRA_CHARACTERISTIC_ERROR_MESSAGE, "" + status);
                mContext.sendBroadcast(intent);
                Log.d(TAG,dataLog);
            }

            Log.d(TAG, dataLog);
            boolean isExitBootloaderCmd = false;
            synchronized (mGattCallback) {
                isExitBootloaderCmd = mOtaExitBootloaderCmdInProgress;
                if (mOtaExitBootloaderCmdInProgress)
                    mOtaExitBootloaderCmdInProgress = false;
            }

            if (isExitBootloaderCmd)
                onOtaExitBootloaderComplete(status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String characteristicValue = " " + Utils.ByteArraytoHex(characteristic.getValue()) + " ";
            // GATT Characteristic read
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_response) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + characteristicValue + "]";
                Log.d(TAG,dataLog);
                broadcastNotifyUpdate(characteristic);
            } else {
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Log.d(TAG,dataLog);
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                    bondDevice();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String characteristicValue = Utils.ByteArraytoHex(characteristic.getValue());
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().
                            getString(R.string.dl_characteristic_notification_response) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            //Log.d(TAG,dataLog);
            broadcastNotifyUpdate(characteristic);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Resources res = mContext.getResources();
            String dataLog = String.format(
                    res.getString(R.string.exchange_mtu_rsp),
                    mBluetoothDeviceName,
                    mBluetoothDeviceAddress,
                    res.getString(R.string.exchange_mtu),
                    mtu,
                    status);

            Log.d(TAG,dataLog);
        }
    };

    public static void exchangeGattMtu(int mtu) {

        int retry = 5;
        boolean status = false;
        while (!status && retry > 0) {
            status = mBluetoothGatt.requestMtu(mtu);
            retry--;
        }

        Resources res = mContext.getResources();
        String dataLog = String.format(
                res.getString(R.string.exchange_mtu_request),
                mBluetoothDeviceName,
                mBluetoothDeviceAddress,
                res.getString(R.string.exchange_mtu),
                mtu,
                status ? 0x00 : 0x01);

        Log.d(TAG,dataLog);
    }

    private final IBinder mBinder = new LocalBinder();
    /**
     * Flag to check the mBound status
     */
    public boolean mBound;
    /**
     * BlueTooth manager for handling connections
     */
    private BluetoothManager mBluetoothManager;

    public static String getmBluetoothDeviceAddress() {
        return mBluetoothDeviceAddress;
    }

    public static String getmBluetoothDeviceName() {
        return mBluetoothDeviceName;
    }

    private static void broadcastConnectionUpdate(final String action) {
        Log.d(TAG,"Action: " + action);
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    private static void broadcastWritwStatusUpdate(final String action) {
        final Intent intent = new Intent((action));
        mContext.sendBroadcast(intent);
    }

    private static void broadcastNotifyUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle mBundle = new Bundle();
        // Putting the byte value read for GATT Db
        mBundle.putByteArray(Constants.EXTRA_BYTE_VALUE,
                characteristic.getValue());
        mBundle.putString(Constants.EXTRA_BYTE_UUID_VALUE,
                characteristic.getUuid().toString());
        mBundle.putInt(Constants.EXTRA_BYTE_INSTANCE_VALUE,
                characteristic.getInstanceId());
        mBundle.putString(Constants.EXTRA_BYTE_SERVICE_UUID_VALUE,
                characteristic.getService().getUuid().toString());
        mBundle.putInt(Constants.EXTRA_BYTE_SERVICE_INSTANCE_VALUE,
                characteristic.getService().getInstanceId());


        //case for OTA characteristic received
        if (characteristic.getUuid().equals(UUIDDatabase.UUID_OTA_UPDATE_CHARACTERISTIC)) {
            Intent mIntentOTA = new Intent(BluetoothLeService.ACTION_OTA_DATA_AVAILABLE);
            mIntentOTA.putExtras(mBundle);
            mContext.sendBroadcast(mIntentOTA);
        }
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_MESSAGE_CHARACTERISTIC)) {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            Data.setLastMessage(data);
            if (data != null) {
                //intent.putExtra(EXTRA_DATA, data);
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02x", byteChar));

                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (sharedPrefs.getBoolean("prefDataLogging", false)) {
                    // Log data
                    if (logger == null) {
                        logger = new Logger();
                    }
                    logger.write(stringBuilder.toString());
                }
                int msgID = (data[0] & 0xFF) ;
                switch (msgID) {
                    case 0x00:
                        //Log.d(TAG, "Message ID 0");
                        byte[] vinValue = new byte[7];
                        for (int x = 1; x <= 7; x++){
                            vinValue[x - 1] = data[x];
                        }
                        String vin = new String(vinValue);
                        Data.setVin(vin);

                        break;
                    case 0x01:
                        //Log.d(TAG, "Message ID 1");
                        // Ambient Light
                        int ambientLightValue = (data[6] & 0xFF) & 0x0f; // the lowest 4 bits
                        Data.setAmbientLight(ambientLightValue);
                        break;
                    case 0x02:
                        //Log.d(TAG, "Message ID 2");
                        break;
                    case 0x03:
                        //Log.d(TAG, "Message ID 3");
                        break;
                    case 0x04:
                        //Log.d(TAG, "Message ID 4");
                        break;
                    case 0x05:
                        //Log.d(TAG, "Message ID 5");
                        // Brakes
                        int brakes = ((data[2] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                        if(prevBrakeValue == 0){
                            prevBrakeValue = brakes;
                        }
                        if (prevBrakeValue != brakes) {
                            prevBrakeValue = brakes;
                            switch (brakes) {
                                case 0x6:
                                    //Front
                                    Data.setFrontBrake(Data.getFrontBrake() + 1);
                                    break;
                                case 0x9:
                                    //Back
                                    Data.setRearBrake(Data.getRearBrake() + 1);
                                    break;
                                case 0xA:
                                    //Both
                                    Data.setFrontBrake(Data.getFrontBrake() + 1);
                                    Data.setRearBrake(Data.getRearBrake() + 1);
                                    break;
                                default:
                                    break;
                            }
                        }
                        // ABS Fault
                        int absValue = (data[3] & 0xFF) & 0x0f; // the lowest 4 bits
                        switch (absValue){
                            case 0x2:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(true);
                                break;
                            case 0x3:
                                faults.setAbsSelfDiagActive(true);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(false);
                                break;
                            case 0x5:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(true);
                                break;
                            case 0x6:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(true);
                                break;
                            case 0x7:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(true);
                                break;
                            case 0x8:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(true);
                                faults.setabsErrorActive(false);
                                break;
                            case 0xA:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(true);
                                break;
                            case 0xB:
                                faults.setAbsSelfDiagActive(true);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(false);
                                break;
                            case 0xD:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(true);
                                break;
                            case 0xE:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(true);
                                break;
                            case 0xF:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(false);
                                break;
                            default:
                                faults.setAbsSelfDiagActive(false);
                                faults.setAbsDeactivatedActive(false);
                                faults.setabsErrorActive(false);
                                break;
                        }

                        // Tire Pressure
                        if ((data[4] & 0xFF) != 0xFF){
                            double rdcFront = (data[4] & 0xFF) / 50.0;
                            Data.setFrontTirePressure(rdcFront);
                        }
                        if ((data[5] & 0xFF) != 0xFF){
                            double rdcRear = (data[5] & 0xFF) / 50.0;
                            Data.setRearTirePressure(rdcRear);
                        }

                        // Tire Pressure Faults
                        switch (data[6] & 0xFF) {
                            case 0xC9:
                                faults.setfrontTirePressureWarningActive(true);
                                faults.setrearTirePressureWarningActive(false);
                                faults.setfrontTirePressureCriticalActive(false);
                                faults.setrearTirePressureCriticalActive(false);
                                if(faults.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if(faults.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setrearTirePressureCriticalNotificationActive(false);
                                }
                                break;
                            case 0xCA:
                                faults.setfrontTirePressureWarningActive(false);
                                faults.setrearTirePressureWarningActive(true);
                                faults.setfrontTirePressureCriticalActive(false);
                                faults.setrearTirePressureCriticalActive(false);
                                if(faults.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if(faults.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setrearTirePressureCriticalNotificationActive(false);
                                }
                                break;
                            case 0xCB:
                                faults.setfrontTirePressureWarningActive(true);
                                faults.setrearTirePressureWarningActive(true);
                                faults.setfrontTirePressureCriticalActive(false);
                                faults.setrearTirePressureCriticalActive(false);
                                if(faults.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if(faults.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setrearTirePressureCriticalNotificationActive(false);
                                }
                                break;
                            case 0xD1:
                                faults.setfrontTirePressureWarningActive(false);
                                faults.setrearTirePressureWarningActive(false);
                                faults.setfrontTirePressureCriticalActive(true);
                                faults.setrearTirePressureCriticalActive(false);
                                if(!(faults.getfrontTirePressureCriticalNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setfrontTirePressureCriticalNotificationActive(true);
                                }
                                if(faults.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setrearTirePressureCriticalNotificationActive(false);
                                }
                                break;
                            case 0xD2:
                                faults.setfrontTirePressureWarningActive(false);
                                faults.setrearTirePressureWarningActive(false);
                                faults.setfrontTirePressureCriticalActive(false);
                                faults.setrearTirePressureCriticalActive(true);
                                if(faults.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if(!(faults.getrearTirePressureCriticalNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setrearTirePressureCriticalNotificationActive(true);
                                }
                                break;
                            case 0xD3:
                                faults.setfrontTirePressureWarningActive(false);
                                faults.setrearTirePressureWarningActive(false);
                                faults.setfrontTirePressureCriticalActive(true);
                                faults.setrearTirePressureCriticalActive(true);
                                if(!(faults.getfrontTirePressureCriticalNotificationActive()) && !(faults.getrearTirePressureCriticalNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setfrontTirePressureCriticalNotificationActive(true);
                                    faults.setrearTirePressureCriticalNotificationActive(true);
                                }
                                break;
                            default:
                                faults.setfrontTirePressureWarningActive(false);
                                faults.setrearTirePressureWarningActive(false);
                                faults.setfrontTirePressureCriticalActive(false);
                                faults.setrearTirePressureCriticalActive(false);
                                if(faults.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if(faults.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setrearTirePressureCriticalNotificationActive(false);
                                }
                                break;
                        }

                        break;
                    case 0x06:
                        //Log.d(TAG, "Message ID 6");
                        String gear;
                        int gearValue = ((data[2] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                        switch (gearValue) {
                            case 0x1:
                                gear = "1";
                                break;
                            case 0x2:
                                gear = "N";
                                break;
                            case 0x4:
                                gear = "2";
                                break;
                            case 0x7:
                                gear = "3";
                                break;
                            case 0x8:
                                gear = "4";
                                break;
                            case 0xB:
                                gear = "5";
                                break;
                            case 0xD:
                                gear = "6";
                                break;
                            case 0xF:
                                // Inbetween Gears
                                gear = "-";
                                break;
                            default:
                                gear = "-";
                                Log.d(TAG, "Unknown gear value");
                        }
                        if(Data.getGear() != null) {
                            if (!Data.getGear().equals(gear) && !gear.equals("-")) {
                                Data.setNumberOfShifts(Data.getNumberOfShifts() + 1);
                            }
                        }
                        Data.setGear(gear);

                        // Throttle Position
                        int minPosition = 36;
                        int maxPosition = 236;
                        double throttlePosition = (((data[3] & 0xFF) - minPosition) * 100) / (maxPosition - minPosition);
                        Data.setThrottlePosition(throttlePosition);

                        // Engine Temperature
                        double engineTemp = ((data[4] & 0xFF) * 0.75) - 25;
                        Data.setEngineTemperature(engineTemp);

                        // ASC Fault
                        int ascValue = ((data[5] & 0xFF)  >> 4) & 0x0f; // the highest 4 bits.
                        switch (ascValue){
                            case 0x1:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(true);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(false);
                                break;
                            case 0x2:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(true);
                                break;
                            case 0x3:
                                faults.setAscSelfDiagActive(true);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(false);
                                break;
                            case 0x5:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(true);
                                break;
                            case 0x6:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(true);
                                break;
                            case 0x7:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(true);
                                break;
                            case 0x8:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(true);
                                faults.setascErrorActive(false);
                                break;
                            case 0x9:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(true);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(false);
                                break;
                            case 0xA:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(true);
                                break;
                            case 0xB:
                                faults.setAscSelfDiagActive(true);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(false);
                                break;
                            case 0xD:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(true);
                                break;
                            case 0xE:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(true);
                                break;
                            default:
                                faults.setAscSelfDiagActive(false);
                                faults.setAscInterventionActive(false);
                                faults.setAscDeactivatedActive(false);
                                faults.setascErrorActive(false);
                                break;
                        }

                        //Oil Fault
                        int oilValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                        switch (oilValue){
                            case 0x2:
                                faults.setOilLowActive(true);
                                break;
                            case 0x6:
                                faults.setOilLowActive(true);
                                break;
                            case 0xA:
                                faults.setOilLowActive(true);
                                break;
                            case 0xE:
                                faults.setOilLowActive(true);
                                break;
                            default:
                                faults.setOilLowActive(false);
                                break;
                        }

                        break;
                    case 0x07:
                        //Log.d(TAG, "Message ID 7");
                        //Voltage
                        double voltage = (data[4] & 0xFF) / 10;
                        Data.setvoltage(voltage);

                        // Fuel Fault
                        int fuelValue = ((data[5] & 0xFF)  >> 4) & 0x0f; // the highest 4 bits.
                        switch (fuelValue){
                            case 0x2:
                                faults.setfuelFaultActive(true);
                                break;
                            case 0x6:
                                faults.setfuelFaultActive(true);
                                break;
                            case 0xA:
                                faults.setfuelFaultActive(true);
                                break;
                            case 0xE:
                                faults.setfuelFaultActive(true);
                                break;
                            default:
                                faults.setfuelFaultActive(false);
                                break;
                        }
                        // General Fault
                        int generalFault = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                        switch (generalFault){
                            case 0x1:
                                faults.setGeneralFlashingYellowActive(true);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(false);
                                faults.setGeneralShowsRedActive(false);
                                if(faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0x2:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(true);
                                faults.setGeneralFlashingRedActive(false);
                                faults.setGeneralShowsRedActive(false);
                                if(faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0x4:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(true);
                                faults.setGeneralShowsRedActive(false);
                                if(!(faults.getgeneralFlashingRedNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0x5:
                                faults.setGeneralFlashingYellowActive(true);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(true);
                                faults.setGeneralShowsRedActive(false);
                                if(!(faults.getgeneralFlashingRedNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0x6:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(true);
                                faults.setGeneralFlashingRedActive(true);
                                faults.setGeneralShowsRedActive(false);
                                if(!(faults.getgeneralFlashingRedNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0x7:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(true);
                                faults.setGeneralShowsRedActive(false);
                                if(!(faults.getgeneralFlashingRedNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0x8:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(false);
                                faults.setGeneralShowsRedActive(true);
                                if(faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if(!(faults.getgeneralShowsRedNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0x9:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(true);
                                faults.setGeneralShowsRedActive(true);
                                if(faults.getgeneralShowsRedNotificationActive() && faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                }
                                break;
                            case 0xA:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(true);
                                faults.setGeneralFlashingRedActive(false);
                                faults.setGeneralShowsRedActive(true);
                                if(faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if(!(faults.getgeneralShowsRedNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0xB:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(false);
                                faults.setGeneralShowsRedActive(true);
                                if(faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if(!(faults.getgeneralShowsRedNotificationActive())) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0xD:
                                faults.setGeneralFlashingYellowActive(true);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(false);
                                faults.setGeneralShowsRedActive(false);
                                if(faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            case 0xE:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(true);
                                faults.setGeneralFlashingRedActive(false);
                                faults.setGeneralShowsRedActive(false);
                                if(faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                            default:
                                faults.setGeneralFlashingYellowActive(false);
                                faults.setGeneralShowsYellowActive(false);
                                faults.setGeneralFlashingRedActive(false);
                                faults.setGeneralShowsRedActive(false);
                                if(faults.getgeneralFlashingRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if(faults.getgeneralShowsRedNotificationActive()) {
                                    updateNotification(intent);
                                    faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                break;
                        }
                        break;
                    case 0x08:
                        //Log.d(TAG, "Message ID 8");
                        double ambientTemp = ((data[1] & 0xFF) * 0.50) - 40;
                        Data.setAmbientTemperature(ambientTemp);
                        if(ambientTemp <= 0.0){
                            faults.seticeWarnActive(true);
                        } else {
                            faults.seticeWarnActive(false);
                        }

                        // LAMP Faults
                        if (((data[3] & 0xFF) != 0xFF) ) {
                            // LAMPF 1
                            int lampfOneValue = ((data[3] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                            switch (lampfOneValue) {
                                case 0x1:
                                    faults.setAddFrontLightOneActive(true);
                                    faults.setAddFrontLightTwoActive(false);
                                    break;
                                case 0x2:
                                    faults.setAddFrontLightOneActive(false);
                                    faults.setAddFrontLightTwoActive(true);
                                    break;
                                case 0x3:
                                    faults.setAddFrontLightOneActive(true);
                                    faults.setAddFrontLightTwoActive(true);
                                    break;
                                case 0x5:
                                    faults.setAddFrontLightOneActive(true);
                                    faults.setAddFrontLightTwoActive(false);
                                    break;
                                case 0x6:
                                    faults.setAddFrontLightOneActive(false);
                                    faults.setAddFrontLightTwoActive(true);
                                    break;
                                case 0x9:
                                    faults.setAddFrontLightOneActive(true);
                                    faults.setAddFrontLightTwoActive(false);
                                    break;
                                case 0xA:
                                    faults.setAddFrontLightOneActive(false);
                                    faults.setAddFrontLightTwoActive(true);
                                    break;
                                case 0xB:
                                    faults.setAddFrontLightOneActive(true);
                                    faults.setAddFrontLightTwoActive(true);
                                    break;
                                case 0xD:
                                    faults.setAddFrontLightOneActive(true);
                                    faults.setAddFrontLightTwoActive(false);
                                    break;
                                case 0xE:
                                    faults.setAddFrontLightOneActive(false);
                                    faults.setAddFrontLightTwoActive(true);
                                    break;
                                default:
                                    faults.setAddFrontLightOneActive(false);
                                    faults.setAddFrontLightTwoActive(false);
                                    break;
                            }
                        }
                        // LAMPF 2
                        if (((data[4] & 0xFF) != 0xFF) ) {
                            int lampfTwoHighValue = ((data[4] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                            switch (lampfTwoHighValue) {
                                case 0x1:
                                    faults.setDaytimeRunningActive(true);
                                    faults.setfrontLeftSignalActive(false);
                                    faults.setfrontRightSignalActive(false);
                                    break;
                                case 0x2:
                                    faults.setDaytimeRunningActive(false);
                                    faults.setfrontLeftSignalActive(true);
                                    faults.setfrontRightSignalActive(false);
                                    break;
                                case 0x3:
                                    faults.setDaytimeRunningActive(true);
                                    faults.setfrontLeftSignalActive(true);
                                    faults.setfrontRightSignalActive(false);
                                    break;
                                case 0x4:
                                    faults.setDaytimeRunningActive(false);
                                    faults.setfrontLeftSignalActive(false);
                                    faults.setfrontRightSignalActive(true);
                                    break;
                                case 0x5:
                                    faults.setDaytimeRunningActive(true);
                                    faults.setfrontLeftSignalActive(false);
                                    faults.setfrontRightSignalActive(true);
                                    break;
                                case 0x6:
                                    faults.setDaytimeRunningActive(false);
                                    faults.setfrontLeftSignalActive(true);
                                    faults.setfrontRightSignalActive(true);
                                case 0x7:
                                    faults.setDaytimeRunningActive(true);
                                    faults.setfrontLeftSignalActive(true);
                                    faults.setfrontRightSignalActive(true);
                                case 0x9:
                                    faults.setDaytimeRunningActive(true);
                                    faults.setfrontLeftSignalActive(false);
                                    faults.setfrontRightSignalActive(false);
                                    break;
                                case 0xA:
                                    faults.setDaytimeRunningActive(false);
                                    faults.setfrontLeftSignalActive(true);
                                    faults.setfrontRightSignalActive(false);
                                    break;
                                case 0xB:
                                    faults.setDaytimeRunningActive(true);
                                    faults.setfrontLeftSignalActive(true);
                                    faults.setfrontRightSignalActive(false);
                                    break;
                                case 0xC:
                                    faults.setDaytimeRunningActive(false);
                                    faults.setfrontLeftSignalActive(false);
                                    faults.setfrontRightSignalActive(true);
                                    break;
                                case 0xD:
                                    faults.setDaytimeRunningActive(true);
                                    faults.setfrontLeftSignalActive(false);
                                    faults.setfrontRightSignalActive(true);
                                    break;
                                case 0xE:
                                    faults.setDaytimeRunningActive(false);
                                    faults.setfrontLeftSignalActive(true);
                                    faults.setfrontRightSignalActive(true);
                                case 0xF:
                                    faults.setDaytimeRunningActive(true);
                                    faults.setfrontLeftSignalActive(true);
                                    faults.setfrontRightSignalActive(true);
                                default:
                                    faults.setDaytimeRunningActive(false);
                                    faults.setfrontLeftSignalActive(false);
                                    faults.setfrontRightSignalActive(false);
                                    break;
                            }
                            int lampfTwoLowValue = (data[4] & 0xFF) & 0x0f; // the lowest 4 bits
                            switch (lampfTwoLowValue) {
                                case 0x1:
                                    faults.setFrontParkingLightOneActive(true);
                                    faults.setFrontParkingLightTwoActive(false);
                                    faults.setLowBeamActive(false);
                                    faults.setHighBeamActive(false);
                                    break;
                                case 0x2:
                                    faults.setFrontParkingLightOneActive(false);
                                    faults.setFrontParkingLightTwoActive(true);
                                    faults.setLowBeamActive(false);
                                    faults.setHighBeamActive(false);
                                    break;
                                case 0x3:
                                    faults.setFrontParkingLightOneActive(true);
                                    faults.setFrontParkingLightTwoActive(true);
                                    faults.setLowBeamActive(false);
                                    faults.setHighBeamActive(false);
                                    break;
                                case 0x4:
                                    faults.setFrontParkingLightOneActive(false);
                                    faults.setFrontParkingLightTwoActive(false);
                                    faults.setLowBeamActive(true);
                                    faults.setHighBeamActive(false);
                                    break;
                                case 0x5:
                                    faults.setFrontParkingLightOneActive(true);
                                    faults.setFrontParkingLightTwoActive(false);
                                    faults.setLowBeamActive(true);
                                    faults.setHighBeamActive(false);
                                    break;
                                case 0x6:
                                    faults.setFrontParkingLightOneActive(false);
                                    faults.setFrontParkingLightTwoActive(true);
                                    faults.setLowBeamActive(true);
                                    faults.setHighBeamActive(false);
                                    break;
                                case 0x7:
                                    faults.setFrontParkingLightOneActive(true);
                                    faults.setFrontParkingLightTwoActive(true);
                                    faults.setLowBeamActive(true);
                                    faults.setHighBeamActive(false);
                                    break;
                                case 0x8:
                                    faults.setFrontParkingLightOneActive(false);
                                    faults.setFrontParkingLightTwoActive(false);
                                    faults.setLowBeamActive(false);
                                    faults.setHighBeamActive(true);
                                    break;
                                case 0x9:
                                    faults.setFrontParkingLightOneActive(true);
                                    faults.setFrontParkingLightTwoActive(false);
                                    faults.setLowBeamActive(false);
                                    faults.setHighBeamActive(true);
                                    break;
                                case 0xA:
                                    faults.setFrontParkingLightOneActive(false);
                                    faults.setFrontParkingLightTwoActive(true);
                                    faults.setLowBeamActive(false);
                                    faults.setHighBeamActive(true);
                                    break;
                                case 0xB:
                                    faults.setFrontParkingLightOneActive(true);
                                    faults.setFrontParkingLightTwoActive(true);
                                    faults.setLowBeamActive(false);
                                    faults.setHighBeamActive(true);
                                    break;
                                case 0xC:
                                    faults.setFrontParkingLightOneActive(false);
                                    faults.setFrontParkingLightTwoActive(false);
                                    faults.setLowBeamActive(true);
                                    faults.setHighBeamActive(true);
                                    break;
                                case 0xD:
                                    faults.setFrontParkingLightOneActive(true);
                                    faults.setFrontParkingLightTwoActive(false);
                                    faults.setLowBeamActive(true);
                                    faults.setHighBeamActive(true);
                                    break;
                                case 0xE:
                                    faults.setFrontParkingLightOneActive(false);
                                    faults.setFrontParkingLightTwoActive(true);
                                    faults.setLowBeamActive(true);
                                    faults.setHighBeamActive(true);
                                    break;
                                case 0xF:
                                    faults.setFrontParkingLightOneActive(true);
                                    faults.setFrontParkingLightTwoActive(true);
                                    faults.setLowBeamActive(true);
                                    faults.setHighBeamActive(true);
                                    break;
                                default:
                                    faults.setFrontParkingLightOneActive(false);
                                    faults.setFrontParkingLightTwoActive(false);
                                    faults.setLowBeamActive(false);
                                    faults.setHighBeamActive(false);
                                    break;
                            }
                        }

                        // LAMPF 3
                        if (((data[5] & 0xFF) != 0xFF) ) {
                            int lampfThreeHighValue = ((data[5] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                            switch (lampfThreeHighValue) {
                                case 0x1:
                                    faults.setrearRightSignalActive(true);
                                    break;
                                case 0x3:
                                    faults.setrearRightSignalActive(true);
                                    break;
                                case 0x5:
                                    faults.setrearRightSignalActive(true);
                                    break;
                                case 0x7:
                                    faults.setrearRightSignalActive(true);
                                    break;
                                case 0x9:
                                    faults.setrearRightSignalActive(true);
                                    break;
                                case 0xB:
                                    faults.setrearRightSignalActive(true);
                                    break;
                                case 0xD:
                                    faults.setrearRightSignalActive(true);
                                    break;
                                case 0xF:
                                    faults.setrearRightSignalActive(true);
                                default:
                                    faults.setrearRightSignalActive(false);
                                    break;
                            }
                            int lampfThreeLowValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                            switch (lampfThreeLowValue) {
                                case 0x1:
                                    faults.setrearLeftSignalActive(false);
                                    faults.setRearLightActive(true);
                                    faults.setBrakeLightActive(false);
                                    faults.setLicenseLightActive(false);
                                    break;
                                case 0x2:
                                    faults.setrearLeftSignalActive(false);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(true);
                                    faults.setLicenseLightActive(false);
                                    break;
                                case 0x3:
                                    faults.setrearLeftSignalActive(false);
                                    faults.setRearLightActive(true);
                                    faults.setBrakeLightActive(true);
                                    faults.setLicenseLightActive(false);
                                    break;
                                case 0x4:
                                    faults.setrearLeftSignalActive(false);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(false);
                                    faults.setLicenseLightActive(true);
                                    break;
                                case 0x5:
                                    faults.setrearLeftSignalActive(true);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(false);
                                    faults.setLicenseLightActive(true);
                                    break;
                                case 0x6:
                                    faults.setrearLeftSignalActive(false);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(true);
                                    faults.setLicenseLightActive(true);
                                    break;
                                case 0x7:
                                    faults.setrearLeftSignalActive(false);
                                    faults.setRearLightActive(true);
                                    faults.setBrakeLightActive(true);
                                    faults.setLicenseLightActive(true);
                                    break;
                                case 0x8:
                                    faults.setrearLeftSignalActive(true);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(false);
                                    faults.setLicenseLightActive(false);
                                    break;
                                case 0x9:
                                    faults.setrearLeftSignalActive(true);
                                    faults.setRearLightActive(true);
                                    faults.setBrakeLightActive(false);
                                    faults.setLicenseLightActive(false);
                                    break;
                                case 0xA:
                                    faults.setrearLeftSignalActive(true);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(true);
                                    faults.setLicenseLightActive(false);
                                    break;
                                case 0xC:
                                    faults.setrearLeftSignalActive(true);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(false);
                                    faults.setLicenseLightActive(true);
                                    break;
                                case 0xD:
                                    faults.setrearLeftSignalActive(true);
                                    faults.setRearLightActive(true);
                                    faults.setBrakeLightActive(true);
                                    faults.setLicenseLightActive(false);
                                    break;
                                case 0xE:
                                    faults.setrearLeftSignalActive(true);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(true);
                                    faults.setLicenseLightActive(true);
                                    break;
                                case 0xF:
                                    faults.setrearLeftSignalActive(true);
                                    faults.setRearLightActive(true);
                                    faults.setBrakeLightActive(true);
                                    faults.setLicenseLightActive(true);
                                    break;
                                default:
                                    faults.setrearLeftSignalActive(false);
                                    faults.setRearLightActive(false);
                                    faults.setBrakeLightActive(false);
                                    faults.setLicenseLightActive(false);
                                    break;
                            }
                        }

                        // LAMPF 4
                        if (((data[6] & 0xFF) != 0xFF) ) {
                            int lampfFourHighValue = ((data[6] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                            switch (lampfFourHighValue) {
                                case 0x1:
                                    faults.setRearFogLightActive(true);
                                    break;
                                case 0x3:
                                    faults.setRearFogLightActive(true);
                                    break;
                                case 0x5:
                                    faults.setRearFogLightActive(true);
                                    break;
                                case 0x7:
                                    faults.setRearFogLightActive(true);
                                    break;
                                case 0x9:
                                    faults.setRearFogLightActive(true);
                                    break;
                                case 0xB:
                                    faults.setRearFogLightActive(true);
                                    break;
                                case 0xD:
                                    faults.setRearFogLightActive(true);
                                    break;
                                case 0xF:
                                    faults.setRearFogLightActive(true);
                                default:
                                    faults.setRearFogLightActive(false);
                                    break;
                            }
                            int lampfFourLowValue = (data[6] & 0xFF) & 0x0f; // the lowest 4 bits
                            switch (lampfFourLowValue) {
                                case 0x1:
                                    faults.setAddDippedLightActive(true);
                                    faults.setAddBrakeLightActive(false);
                                    faults.setFrontLampOneLightActive(false);
                                    faults.setFrontLampTwoLightActive(false);
                                    break;
                                case 0x2:
                                    faults.setAddDippedLightActive(false);
                                    faults.setAddBrakeLightActive(true);
                                    faults.setFrontLampOneLightActive(false);
                                    faults.setFrontLampTwoLightActive(false);
                                    break;
                                case 0x3:
                                    faults.setAddDippedLightActive(true);
                                    faults.setAddBrakeLightActive(true);
                                    faults.setFrontLampOneLightActive(false);
                                    faults.setFrontLampTwoLightActive(false);
                                    break;
                                case 0x4:
                                    faults.setAddDippedLightActive(false);
                                    faults.setAddBrakeLightActive(false);
                                    faults.setFrontLampOneLightActive(true);
                                    faults.setFrontLampTwoLightActive(false);
                                    break;
                                case 0x5:
                                    faults.setAddDippedLightActive(true);
                                    faults.setAddBrakeLightActive(false);
                                    faults.setFrontLampOneLightActive(true);
                                    faults.setFrontLampTwoLightActive(false);
                                    break;
                                case 0x6:
                                    faults.setAddDippedLightActive(false);
                                    faults.setAddBrakeLightActive(true);
                                    faults.setFrontLampOneLightActive(true);
                                    faults.setFrontLampTwoLightActive(false);
                                    break;
                                case 0x7:
                                    faults.setAddDippedLightActive(true);
                                    faults.setAddBrakeLightActive(true);
                                    faults.setFrontLampOneLightActive(true);
                                    faults.setFrontLampTwoLightActive(false);
                                    break;
                                case 0x8:
                                    faults.setAddDippedLightActive(false);
                                    faults.setAddBrakeLightActive(false);
                                    faults.setFrontLampOneLightActive(false);
                                    faults.setFrontLampTwoLightActive(true);
                                    break;
                                case 0x9:
                                    faults.setAddDippedLightActive(true);
                                    faults.setAddBrakeLightActive(false);
                                    faults.setFrontLampOneLightActive(false);
                                    faults.setFrontLampTwoLightActive(true);
                                    break;
                                case 0xA:
                                    faults.setAddDippedLightActive(false);
                                    faults.setAddBrakeLightActive(true);
                                    faults.setFrontLampOneLightActive(false);
                                    faults.setFrontLampTwoLightActive(true);
                                    break;
                                case 0xB:
                                    faults.setAddDippedLightActive(true);
                                    faults.setAddBrakeLightActive(true);
                                    faults.setFrontLampOneLightActive(false);
                                    faults.setFrontLampTwoLightActive(true);
                                    break;
                                case 0xC:
                                    faults.setAddDippedLightActive(false);
                                    faults.setAddBrakeLightActive(false);
                                    faults.setFrontLampOneLightActive(true);
                                    faults.setFrontLampTwoLightActive(true);
                                    break;
                                case 0xD:
                                    faults.setAddDippedLightActive(true);
                                    faults.setAddBrakeLightActive(false);
                                    faults.setFrontLampOneLightActive(true);
                                    faults.setFrontLampTwoLightActive(true);
                                    break;
                                case 0xE:
                                    faults.setAddDippedLightActive(false);
                                    faults.setAddBrakeLightActive(true);
                                    faults.setFrontLampOneLightActive(true);
                                    faults.setFrontLampTwoLightActive(true);
                                    break;
                                case 0xF:
                                    faults.setAddDippedLightActive(true);
                                    faults.setAddBrakeLightActive(true);
                                    faults.setFrontLampOneLightActive(true);
                                    faults.setFrontLampTwoLightActive(true);
                                    break;
                                default:
                                    faults.setAddDippedLightActive(false);
                                    faults.setAddBrakeLightActive(false);
                                    faults.setFrontLampOneLightActive(false);
                                    faults.setFrontLampTwoLightActive(false);
                                    break;
                            }
                        }
                        break;
                    case 0x09:
                        //Log.d(TAG, "Message ID 9");
                        break;
                    case 0x0a:
                        //Log.d(TAG, "Message ID 10");
                        double odometer = bytesToInt(data[3],data[2],data[1]);
                        double tripAuto = bytesToInt(data[6],data[5],data[4]) / 10;
                        Data.setOdometer(odometer);
                        Data.setTripAuto(tripAuto);
                        break;
                    case 0x0b:
                        //Log.d(TAG, "Message ID 11");
                        break;
                    case 0x0c:
                        //Log.d(TAG, "Message ID 12");
                        double trip1 = bytesToInt(data[3],data[2],data[1]) / 10;
                        double trip2 = bytesToInt(data[6],data[5],data[4]) / 10;
                        Data.setTripOne(trip1);
                        Data.setTripTwo(trip2);
                        break;
                    case 0xff:
                        Log.d(TAG,"Debug Message received: " + stringBuilder.toString());
                        //
                        if (sharedPrefs.getBoolean("prefShowUartFaults",false)) {
                            faults.setUartErrorActive(true);
                            if ((data[7] & 0xFF) == 0xf0){
                                faults.setUartCommTimeoutActive(true);
                            }
                        }
                        break;
                    default:
                        Log.d(TAG, "Unknown Message ID: " + String.format("%02x", msgID));
                }
            }
        }
        // Manufacture name read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_MANUFACTURE_NAME_STRING)) {
            mBundle.putString(Constants.EXTRA_MNS_VALUE,
                    Utils.getManufacturerNameString(characteristic));
        }
        // Model number read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_MODEL_NUMBER_STRING)) {
            mBundle.putString(Constants.EXTRA_MONS_VALUE,
                    Utils.getModelNumberString(characteristic));
        }
        // Serial number read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_SERIAL_NUMBER_STRING)) {
            mBundle.putString(Constants.EXTRA_SNS_VALUE,
                    Utils.getSerialNumberString(characteristic));
        }
        // Hardware revision read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_HARDWARE_REVISION_STRING)) {
            mBundle.putString(Constants.EXTRA_HRS_VALUE,
                    Utils.getHardwareRevisionString(characteristic));
        }
        // Firmware revision read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_FIRMWARE_REVISION_STRING)) {
            mBundle.putString(Constants.EXTRA_FRS_VALUE,
                    Utils.getFirmwareRevisionString(characteristic));
        }
        // Software revision read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_SOFTWARE_REVISION_STRING)) {
            mBundle.putString(Constants.EXTRA_SRS_VALUE,
                    Utils.getSoftwareRevisionString(characteristic));
        }
        // PNP ID read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_PNP_ID)) {
            mBundle.putString(Constants.EXTRA_PNP_VALUE,
                    Utils.getPNPID(characteristic));
        }
        // System ID read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_SYSTEM_ID)) {
            mBundle.putString(Constants.EXTRA_SID_VALUE,
                    Utils.getSYSID(characteristic));
        }
        // Regulatory data read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_IEEE)) {
            mBundle.putString(Constants.EXTRA_RCDL_VALUE,
                    Utils.ByteArraytoHex(characteristic.getValue()));
        }

        intent.putExtras(mBundle);
        /**
         * Sending the broad cast so that it can be received on registered
         * receivers
         */

        mContext.sendBroadcast(intent);
    }

    private static void onOtaExitBootloaderComplete(int status) {
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.EXTRA_BYTE_VALUE, new byte[]{(byte) status});
        Intent intentOTA = new Intent(BluetoothLeService.ACTION_OTA_DATA_AVAILABLE);
        intentOTA.putExtras(bundle);
        mContext.sendBroadcast(intentOTA);
    }

    /**
     * Connects to the GATT server hosted on the BlueTooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void connect(final String address, final String devicename, Context context) {
        faults = new FaultStatus(mContext);
        mContext = context;
        if (mBluetoothAdapter == null || address == null) {
            return;
        }

        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            return;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        //Clearing Bluetooth cache before disconnecting to the device
        if (Utils.getBooleanSharedPreference(mContext, Constants.PREF_PAIR_CACHE_STATUS)) {
            Log.d(TAG,"Cache cleared on disconnect!");
            BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
        }
        //TODO: KEC playing around
        BluetoothLeService.refreshDeviceCache(mBluetoothGatt);
        //TODO: KEC playing around end
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceName = devicename;
        /**
         * Adding data to the data logger
         */
        String dataLog = "[" + devicename + "|" + address + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Log.d(TAG,dataLog);
    }

    /**
     * Reconnect method to connect to already connected device
     */
    public static void reconnect() {
        Log.d(TAG,"<--Reconnecting device-->");
        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(mBluetoothDeviceAddress);
        if (device == null) {
            return;
        }
        mBluetoothGatt = null;//Creating a new instance of GATT before connect
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        /**
         * Adding data to the data logger
         */
        String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Log.d(TAG,dataLog);
    }

    /**
     * Reconnect method to connect to already connected device
     */
    public static void reDiscoverServices() {
        Log.d(TAG,"<--Rediscovering services-->");
        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(mBluetoothDeviceAddress);
        if (device == null) {
            return;
        }
        /**
         * Disconnecting the device
         */
        if (mBluetoothGatt != null)
            mBluetoothGatt.disconnect();

        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        /**
         * Adding data to the data logger
         */
        String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Log.d(TAG,dataLog);
    }

    /**
     * Method to clear the device cache
     *
     * @param gatt
     * @return boolean
     */
    public static boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh");
            if (localMethod != null) {
                Log.d(TAG,"In refreshDeviceCache");
                return (Boolean) localMethod.invoke(localBluetoothGatt);
            }
        } catch (Exception localException) {
            Log.d(TAG,"An exception occured while refreshing device");
        }
        return false;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void disconnect() {
        Log.d(TAG,"disconnect called");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            //Clearing Bluetooth cache before disconnecting to the device
            if (Utils.getBooleanSharedPreference(mContext, Constants.PREF_PAIR_CACHE_STATUS)) {
                Log.d(TAG, "Cache cleared on disconnect!");
                BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
            }
            BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
            mBluetoothGatt.disconnect();
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                    + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    mContext.getResources().getString(R.string.dl_disconnection_request);
            Log.d(TAG,dataLog);
            Data.clear();
            faults.clear();
            close();
        }

    }

    public static void discoverServices() {
        // Logger.datalog(mContext.getResources().getString(R.string.dl_service_discover_request));
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            mBluetoothGatt.discoverServices();
            String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    mContext.getResources().getString(R.string.dl_service_discovery_request);
            Log.d(TAG,dataLog);
        }

    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public static void readCharacteristic(
            BluetoothGattCharacteristic characteristic) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                mContext.getResources().getString(R.string.dl_characteristic_read_request);
        Log.d(TAG,dataLog);
    }

    /**
     * Request a read on a given {@code BluetoothGattDescriptor }.
     *
     * @param descriptor The descriptor to read from.
     */
    public static void readDescriptor(
            BluetoothGattDescriptor descriptor) {
        String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getService().getUuid(), serviceUUID);

        String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getUuid(), characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        //Logger.datalog(mContext.getResources().getString(R.string.dl_descriptor_read_request));
        mBluetoothGatt.readDescriptor(descriptor);
        String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                mContext.getResources().getString(R.string.dl_characteristic_read_request);
        Log.d(TAG,dataLog);
    }

    /**
     * Request a write with no response on a given
     * {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray      to write
     */
    public static void writeCharacteristicNoresponse(
            BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(byteArray);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = byteArray;
            characteristic.setValue(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Log.d(TAG,dataLog);

        }
    }

    public static void writeOTABootLoaderCommand(
            BluetoothGattCharacteristic characteristic,
            byte[] value,
            boolean isExitBootloaderCmd) {
        synchronized (mGattCallback) {
            writeOTABootLoaderCommand(characteristic, value);
            if (isExitBootloaderCmd)
                mOtaExitBootloaderCmdInProgress = true;
        }
    }

    public static void writeOTABootLoaderCommand(
            BluetoothGattCharacteristic characteristic, byte[] value) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(value);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = value;
            characteristic.setValue(valueByte);
            int counter = 20;
            boolean status;
            do {
                int i = 0;
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                status = mBluetoothGatt.writeCharacteristic(characteristic);
                if (!status) {
                    Log.d(TAG, "writeCharacteristic() status: False");
                    try {
                        Log.d(TAG, "" + i);
                        i++;
                        Thread.sleep(100, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (!status && (counter-- > 0));


            if (status) {
                String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[ " + characteristicValue + " ]";
                Log.d(TAG, dataLog);

            } else {
                Log.d(TAG, "writeOTABootLoaderCommand failed!");
            }
        }

    }

    private static String getHexValue(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (byte byteChar : array) {
            sb.append(String.format("%02x", byteChar));
        }
        return "" + sb;
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray
     */

    public static void writeCharacteristicGattDb(
            BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(byteArray);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = byteArray;
            characteristic.setValue(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Log.d(TAG,dataLog);
        }

    }

    /**
     * Writes the characteristic value to the given characteristic.
     *
     * @param characteristic the characteristic to write to
     * @return true if request has been sent
     */
    public static final boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
            return false;

        Log.d(TAG,"Writing characteristic " + characteristic.getUuid());
        Log.d(TAG,"gatt.writeCharacteristic(" + characteristic.getUuid() + ")");
        return gatt.writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public static void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookupUUID(UUIDDatabase.
                UUID_CLIENT_CHARACTERISTIC_CONFIG, descriptorUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        if (characteristic.getDescriptor(UUID
                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) + "]";
                Log.d(TAG,dataLog);

            } else {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) + "]";
                Log.d(TAG,dataLog);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_start_notification);
            Log.d(TAG,dataLog);
        } else {
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_stop_notification);
            Log.d(TAG,dataLog);
        }

    }

    /**
     * Enables or disables indications on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable indications. False otherwise.
     */
    public static void setCharacteristicIndication(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(),
                serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(),
                characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookupUUID(UUIDDatabase.
                UUID_CLIENT_CHARACTERISTIC_CONFIG, descriptorUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        if (characteristic.getDescriptor(UUID
                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" +
                        descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.
                        ENABLE_INDICATION_VALUE) + "]";
                Log.d(TAG,dataLog);
            } else {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.
                        DISABLE_NOTIFICATION_VALUE) + "]";
                Log.d(TAG,dataLog);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_start_indication);
            Log.d(TAG,dataLog);
        } else {
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_stop_indication);
            Log.d(TAG,dataLog);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public static List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    public static int getConnectionState() {
        synchronized (mGattCallback) {
            return mConnectionState;
        }
    }

    public static boolean getBondedState() {
        Boolean bonded;
        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(mBluetoothDeviceAddress);
        bonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
        return bonded;
    }

    public static void bondDevice() {
        try {
            Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
            Method createBondMethod = class1.getMethod("createBond");
            Boolean returnValue = (Boolean) createBondMethod.invoke(mBluetoothGatt.getDevice());
            Log.d(TAG,"Pair initates status-->" + returnValue);
        } catch (Exception e) {
            Log.d(TAG,"Exception Pair" + e.getMessage());
        }

    }

    public static void addRemoveData(BluetoothGattDescriptor descriptor) {
        switch (descriptor.getValue()[0]) {
            case 0:
                //Disabled notification and indication
                removeEnabledCharacteristic(descriptor.getCharacteristic());
                Log.d(TAG,"Removed characteristic");
                break;
            case 1:
                //Enabled notification
                Log.d(TAG,"added notify characteristic");
                addEnabledCharacteristic(descriptor.getCharacteristic());
                break;
            case 2:
                //Enabled indication
                Log.d(TAG,"added indicate characteristic");
                addEnabledCharacteristic(descriptor.getCharacteristic());
                break;
        }
    }

    public static void addEnabledCharacteristic(BluetoothGattCharacteristic
                                                        bluetoothGattCharacteristic) {
        if (!mEnabledCharacteristics.contains(bluetoothGattCharacteristic))
            mEnabledCharacteristics.add(bluetoothGattCharacteristic);
    }

    public static void removeEnabledCharacteristic(BluetoothGattCharacteristic
                                                           bluetoothGattCharacteristic) {
        if (mEnabledCharacteristics.contains(bluetoothGattCharacteristic))
            mEnabledCharacteristics.remove(bluetoothGattCharacteristic);
    }

    public static void disableAllEnabledCharacteristics() {
        if (mEnabledCharacteristics.size() > 0) {
            mDisableNotificationFlag = true;
            BluetoothGattCharacteristic bluetoothGattCharacteristic = mEnabledCharacteristics.
                    get(0);
            Log.d(TAG,"Disabling characteristic--" + bluetoothGattCharacteristic.getUuid());
            setCharacteristicNotification(bluetoothGattCharacteristic, false);
        } else {
            mDisableNotificationFlag = false;
        }

    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public static void close() {
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local BlueTooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        return mBluetoothAdapter != null;

    }

    @Override
    public void onCreate() {
        // Initializing the service
        if (!initialize()) {
            Log.d(TAG,"Service not initialized");
        }
    }

    /**
     * Local binder class
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    private static int bytesToInt(byte a, byte b, byte c) {
        return (a & 0xFF) << 16 | (b & 0xFF) << 8 | (c & 0xFF);
    }

    static public void updateNotification(Intent intent){
        StringBuilder body = new StringBuilder();
        body.append("");
        if(faults.getfrontTirePressureCriticalActive()){
            body.append(mContext.getResources().getString(R.string.fault_TIREFCF) + "\n");
        }
        if(faults.getrearTirePressureCriticalActive()){
            body.append(mContext.getResources().getString(R.string.fault_TIRERCF) + "\n");
        }
        if(faults.getgeneralFlashingRedNotificationActive()){
            body.append(mContext.getResources().getString(R.string.fault_GENWARNFSRED) + "\n");
        }
        if(faults.getgeneralShowsRedNotificationActive()){
            body.append(mContext.getResources().getString(R.string.fault_GENWARNSHRED) + "\n");
        }
        if(!body.toString().equals("")){
            showNotification(mContext,mContext.getResources().getString(R.string.fault_title),body.toString(),intent);
        } else {
            Log.d(TAG,"Clearing notification");
            clearNotifications();
        }
    }

    static public void showNotification(Context context, String title, String body, Intent intent) {

        Intent faultIntent=new Intent(mContext, FaultActivity.class);
        faultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "critical";
        String channelName = mContext.getString(R.string.notification_channel);
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            mChannel.shouldShowLights();
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigPictureStyle())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body))
                .setAutoCancel(false)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentText(body);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(faultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_INSISTENT|Notification.FLAG_NO_CLEAR;
        notificationManager.notify(notificationId, notification);
    }
    static public void clearNotifications(){
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        //notificationManager.cancel("myappnotif", i);
    }

}
