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
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given BlueTooth LE device.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = "BluetoothLeService";

    private static Logger logger = null;

    private static boolean fuelAlertSent = false;
    private static int prevBrakeValue = 0;

    private static SharedPreferences sharedPrefs;


    /**
     * GATT Status constants
     */
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
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

    public static final String EXTRA_BYTE_VALUE = "com.blackboxembedded.wunderlinq.backgroundservices." +
            "EXTRA_BYTE_VALUE";
    public static final String EXTRA_BYTE_UUID_VALUE = "com.blackboxembedded.wunderlinq.backgroundservices." +
            "EXTRA_BYTE_UUID_VALUE";

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
     * Disable/enable notification
     */
    public static ArrayList<BluetoothGattCharacteristic> mEnabledCharacteristics =
            new ArrayList<BluetoothGattCharacteristic>();

    public static boolean mDisableNotificationFlag = false;

    private static int mConnectionState = STATE_DISCONNECTED;
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
                        "Connection established";
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
                        "Disconnected";
                Log.d(TAG,dataLog);
                Data.clear();
                FaultStatus.clear();
            }
            // GATT Server Connecting
            if (newState == BluetoothProfile.STATE_CONNECTING) {
                intentAction = ACTION_GATT_CONNECTING;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTING;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        "Connection establishing";
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
                broadcastConnectionUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION ||
                    status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                bondDevice();
                broadcastConnectionUpdate(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
            } else {
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
                        "Write request status"
                        + "," +
                        "[00]";
                Intent intent = new Intent(ACTION_WRITE_SUCCESS);
                MyApplication.getContext().sendBroadcast(intent);
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
                MyApplication.getContext().sendBroadcast(intent);
            } else {
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        "Write request status failed with error code: " +
                        +status;
                Log.d(TAG,dataLog);
                mDisableNotificationFlag = false;
                Intent intent = new Intent(ACTION_WRITE_FAILED);
                MyApplication.getContext().sendBroadcast(intent);
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

            String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                    "Read response recieved with value ," +
                    "[" + descriptorValue + "]";
            Log.d(TAG,dataLog);
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
                        "write request status  - Success";
                Log.d(TAG,dataLog);
            } else {
                dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                        "Write request status - Failed with error code: " + status;
                Intent intent = new Intent(ACTION_GATT_CHARACTERISTIC_ERROR);
                intent.putExtra("EXTRA_CHARACTERISTIC_ERROR_MESSAGE", "" + status);
                MyApplication.getContext().sendBroadcast(intent);
                Log.d(TAG,dataLog);
            }
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
                        "Read response received with value , " +
                        "[" + characteristicValue + "]";
                Log.d(TAG,dataLog);
                broadcastNotifyUpdate(characteristic);
            } else {
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        "Read request status - failed with error code: " + status;
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
            broadcastNotifyUpdate(characteristic);
        }
    };

    private final IBinder mBinder = new LocalBinder();
    /**
     * Flag to check the mBound status
     */
    public boolean mBound;
    /**
     * BlueTooth manager for handling connections
     */
    private BluetoothManager mBluetoothManager;

    private static void broadcastConnectionUpdate(final String action) {
        Log.d(TAG,"Action: " + action);
        final Intent intent = new Intent(action);
        MyApplication.getContext().sendBroadcast(intent);
    }

    private static void broadcastNotifyUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle mBundle = new Bundle();
        // Putting the byte value read for GATT Db
        mBundle.putByteArray(EXTRA_BYTE_VALUE,
                characteristic.getValue());
        mBundle.putString(EXTRA_BYTE_UUID_VALUE,
                characteristic.getUuid().toString());

        if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_MESSAGE_CHARACTERISTIC)) {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null) {
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
                if (sharedPrefs.getBoolean("prefDataLogging", false)) {
                    // Log data
                    if (logger == null) {
                        logger = new Logger();
                    }
                    logger.write(Utils.ByteArraytoHex(data));
                }
                parseMessage(data);
            }
        }

        /*
         * Sending the broad cast so that it can be received on registered
         * receivers
         */
        intent.putExtras(mBundle);
        MyApplication.getContext().sendBroadcast(intent);
    }

    /**
     * Connects to the GATT server hosted on the BlueTooth LE device.
     *
     * @param address The device address of the destination device.
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void connect(final String address, final String devicename) {
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
        mBluetoothGatt = device.connectGatt(MyApplication.getContext(), false, mGattCallback);
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceName = devicename;
        /*
         * Adding data to the data logger
         */
        String dataLog = "[" + devicename + "|" + address + "] " +
                "Connection request sent";
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
            Method localMethod = gatt.getClass().getMethod("refresh");
            if (localMethod != null) {
                Log.d(TAG,"In refreshDeviceCache");
                return (Boolean) localMethod.invoke(gatt);
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
        if (mBluetoothAdapter != null || mBluetoothGatt != null) {
            BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
            mBluetoothGatt.disconnect();
            String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    "Disconnection request sent";
            Log.d(TAG,dataLog);
            Data.clear();
            FaultStatus.clear();
            close();
        }
    }

    public static void discoverServices() {
        if (mBluetoothAdapter != null || mBluetoothGatt != null) {
            mBluetoothGatt.discoverServices();
            String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    "Service discovery request sent";
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
                "Read Request Sent";
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
        mBluetoothGatt.readDescriptor(descriptor);
        String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                "Read Request Sent";
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
        if (mBluetoothAdapter != null || mBluetoothGatt != null) {
            characteristic.setValue(byteArray);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    "Write Request Sent With Value , " +
                    "[ " + characteristicValue + " ]";
            Log.d(TAG,dataLog);
        }
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
        if (mBluetoothAdapter != null || mBluetoothGatt != null) {
            characteristic.setValue(byteArray);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    "Write Request Sent With Value , " +
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
            if (enabled) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        "Write Request Sent With Value , " +
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
                        "Write Request Sent With Value , " +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) + "]";
                Log.d(TAG,dataLog);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    "Start notification request sent";
            Log.d(TAG,dataLog);
        } else {
            String dataLog = "[" + serviceName + "|" + characteristicName + "] " +
                    "Stop notification request sent";
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
            Log.d(TAG,"Disabling characteristic" + bluetoothGattCharacteristic.getUuid());
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

    static public void updateNotification(){
        StringBuilder body = new StringBuilder();
        body.append("");
        if(FaultStatus.getfrontTirePressureCriticalActive()){
            body.append(MyApplication.getContext().getResources().getString(R.string.fault_TIREFCF)).append("\n");
        }
        if(FaultStatus.getrearTirePressureCriticalActive()){
            body.append(MyApplication.getContext().getResources().getString(R.string.fault_TIRERCF)).append("\n");
        }
        if(FaultStatus.getgeneralFlashingRedActive()){
            body.append(MyApplication.getContext().getResources().getString(R.string.fault_GENWARNFSRED)).append("\n");
        }
        if(FaultStatus.getgeneralShowsRedActive()){
            body.append(MyApplication.getContext().getResources().getString(R.string.fault_GENWARNSHRED)).append("\n");
        }
        if(!body.toString().equals("")){
            showNotification(MyApplication.getContext(),MyApplication.getContext().getResources().getString(R.string.fault_title),body.toString());
        } else {
            Log.d(TAG,"Clearing notification");
            clearNotifications();
        }
    }

    static public void showNotification(Context context, String title, String body) {

        Intent faultIntent=new Intent(MyApplication.getContext(), FaultActivity.class);
        faultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "critical";
        String channelName = MyApplication.getContext().getString(R.string.notification_channel);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            mChannel.shouldShowLights();
            try {
                notificationManager.createNotificationChannel(mChannel);
            } catch (NullPointerException e){
                Log.d(TAG, "Error creating notification channel: " + e.toString());
            }
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
        NotificationManager notificationManager = (NotificationManager) MyApplication.getContext().getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    static private void fuelAlert(){
        if (sharedPrefs.getBoolean("prefFuelAlert", false)) {
            if (!fuelAlertSent) {
                fuelAlertSent = true;
                Intent alertIntent = new Intent(MyApplication.getContext(), AlertActivity.class);
                alertIntent.putExtra("TITLE", MyApplication.getContext().getResources().getString(R.string.alertLowFuelTitle));
                alertIntent.putExtra("BODY", MyApplication.getContext().getResources().getString(R.string.alertLowFuelBody));
                MyApplication.getContext().startActivity(alertIntent);
            }

        } else {
            fuelAlertSent = false;
        }
    }

    private static void parseMessage(byte[] data){
        Data.setLastMessage(data);
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
                //Fuel Range
                if ((data[4] & 0xFF) != 0xFF && (data[5] & 0xFF) != 0xFF) {
                    double fuelRange = (((data[4] & 0xFF) >> 4) & 0x0f) + (((data[5] & 0xFF) & 0x0f) * 16) + ((((data[5] & 0xFF) >> 4) & 0x0f) * 256);
                    Data.setFuelRange(fuelRange);
                }
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
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0x3:
                        FaultStatus.setAbsSelfDiagActive(true);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(false);
                        break;
                    case 0x5:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0x6:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0x7:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0x8:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(true);
                        FaultStatus.setabsErrorActive(false);
                        break;
                    case 0xA:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0xB:
                        FaultStatus.setAbsSelfDiagActive(true);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(false);
                        break;
                    case 0xD:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0xE:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0xF:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(false);
                        break;
                    default:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(false);
                        break;
                }

                // Tire Pressure
                if ((data[4] & 0xFF) != 0xFF){
                    double rdcFront = (data[4] & 0xFF) / 50.0;
                    Data.setFrontTirePressure(rdcFront);
                    if (sharedPrefs.getBoolean("prefTPMSAlert",false)) {
                        int pressureThreshold = Integer.parseInt(sharedPrefs.getString("prefTPMSAlertThreshold","-1"));
                        if (pressureThreshold >= 0) {
                            String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
                            if (pressureFormat.contains("1")) {
                                // KPa
                                if (pressureThreshold >= (rdcFront * 100)){
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                    updateNotification();
                                }
                            } else if (pressureFormat.contains("2")) {
                                // Kg-f
                                if (pressureThreshold >= (rdcFront * 1.0197162129779)){
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                    updateNotification();
                                }
                            } else if (pressureFormat.contains("3")) {
                                // Psi
                                if (pressureThreshold >= (rdcFront * 14.5037738)){
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                    updateNotification();
                                }
                            }
                            if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive())) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                            }
                        } else {
                            if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                            }
                        }
                    }
                }
                if ((data[5] & 0xFF) != 0xFF){
                    double rdcRear = (data[5] & 0xFF) / 50.0;
                    Data.setRearTirePressure(rdcRear);
                    if (sharedPrefs.getBoolean("prefTPMSAlert",false)) {
                        int pressureThreshold = Integer.parseInt(sharedPrefs.getString("prefTPMSAlertThreshold","-1"));
                        if (pressureThreshold >= 0) {
                            String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
                            if (pressureFormat.contains("1")) {
                                // KPa
                                if (pressureThreshold >= (rdcRear * 100)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("2")) {
                                // Kg-f
                                if (pressureThreshold >= (rdcRear * 1.0197162129779)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("3")) {
                                // Psi
                                if (pressureThreshold >= (rdcRear * 14.5037738)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            }
                            if (!(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                updateNotification();
                                FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                            }
                        } else {
                            if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                            }
                        }
                    }
                }

                if (!sharedPrefs.getBoolean("prefTPMSAlert",false)) {
                    // Tire Pressure Faults
                    switch (data[6] & 0xFF) {
                        case 0xC9:
                            FaultStatus.setfrontTirePressureWarningActive(true);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                            }
                            if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                            }
                            break;
                        case 0xCA:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(true);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                            }
                            if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                            }
                            break;
                        case 0xCB:
                            FaultStatus.setfrontTirePressureWarningActive(true);
                            FaultStatus.setrearTirePressureWarningActive(true);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                            }
                            if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                            }
                            break;
                        case 0xD1:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(true);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive())) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                            }
                            if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                            }
                            break;
                        case 0xD2:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(true);
                            if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                            }
                            if (!(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                updateNotification();
                                FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                            }
                            break;
                        case 0xD3:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(true);
                            FaultStatus.setrearTirePressureCriticalActive(true);
                            if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive()) && !(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                                FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                            }
                            break;
                        default:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                            }
                            if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                updateNotification();
                                FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                            }
                            break;
                    }
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
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(true);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0x2:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    case 0x3:
                        FaultStatus.setAscSelfDiagActive(true);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0x5:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    case 0x6:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    case 0x7:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    case 0x8:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(true);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0x9:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(true);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0xA:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    case 0xB:
                        FaultStatus.setAscSelfDiagActive(true);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0xD:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    case 0xE:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    default:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                }

                //Oil Fault
                int oilValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                switch (oilValue){
                    case 0x2:
                        FaultStatus.setOilLowActive(true);
                        break;
                    case 0x6:
                        FaultStatus.setOilLowActive(true);
                        break;
                    case 0xA:
                        FaultStatus.setOilLowActive(true);
                        break;
                    case 0xE:
                        FaultStatus.setOilLowActive(true);
                        break;
                    default:
                        FaultStatus.setOilLowActive(false);
                        break;
                }

                break;
            case 0x07:
                //Log.d(TAG, "Message ID 7");
                //Average Speed
                if ((data[1] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF) {
                    double avgSpeed = ((((data[1] & 0xFF) >> 4) & 0x0f) * 2) + (((data[1] & 0xFF) & 0x0f) * 0.125) + (((data[2] & 0xFF) & 0x0f) * 32);
                    Data.setAvgSpeed(avgSpeed);
                }

                //Speed
                if ((data[3] & 0xFF) != 0xFF) {
                    int speed = (data[3] & 0xFF) * 2;
                    Data.setSpeed(speed);
                }

                //Voltage
                double voltage = (data[4] & 0xFF) / 10;
                Data.setvoltage(voltage);

                // Fuel Fault
                int fuelValue = ((data[5] & 0xFF)  >> 4) & 0x0f; // the highest 4 bits.
                switch (fuelValue){
                    case 0x2:
                        FaultStatus.setfuelFaultActive(true);
                        fuelAlert();
                        break;
                    case 0x6:
                        FaultStatus.setfuelFaultActive(true);
                        fuelAlert();
                        break;
                    case 0xA:
                        FaultStatus.setfuelFaultActive(true);
                        fuelAlert();
                        break;
                    case 0xE:
                        FaultStatus.setfuelFaultActive(true);
                        fuelAlert();
                        break;
                    default:
                        FaultStatus.setfuelFaultActive(false);
                        fuelAlertSent = false;
                        break;
                }
                // General Fault
                int generalFault = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                switch (generalFault){
                    case 0x1:
                        FaultStatus.setGeneralFlashingYellowActive(true);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(false);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                    case 0x2:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(false);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                    case 0x4:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(true);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                    case 0x5:
                        FaultStatus.setGeneralFlashingYellowActive(true);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(true);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                    case 0x6:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(true);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                    case 0x7:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(true);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                    case 0x8:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if(FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(false);
                        }
                        if(!(FaultStatus.getgeneralShowsRedNotificationActive())) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(true);
                        }
                        break;
                    case 0x9:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if(!FaultStatus.getgeneralShowsRedNotificationActive() && !FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(true);
                            FaultStatus.setGeneralShowsRedNotificationActive(true);
                        }
                        break;
                    case 0xA:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if(FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(false);
                        }
                        if(!(FaultStatus.getgeneralShowsRedNotificationActive())) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(true);
                        }
                        break;
                    case 0xB:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if(FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(false);
                        }
                        if(!(FaultStatus.getgeneralShowsRedNotificationActive())) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(true);
                        }
                        break;
                    case 0xD:
                        FaultStatus.setGeneralFlashingYellowActive(true);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(false);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                    case 0xE:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(false);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                    default:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if(FaultStatus.getgeneralFlashingRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralFlashingRedNotificationActive(false);
                        }
                        if(FaultStatus.getgeneralShowsRedNotificationActive()) {
                            updateNotification();
                            FaultStatus.setGeneralShowsRedNotificationActive(false);
                        }
                        break;
                }
                break;
            case 0x08:
                //Log.d(TAG, "Message ID 8");
                double ambientTemp = ((data[1] & 0xFF) * 0.50) - 40;
                Data.setAmbientTemperature(ambientTemp);
                if(ambientTemp <= 0.0){
                    FaultStatus.seticeWarnActive(true);
                } else {
                    FaultStatus.seticeWarnActive(false);
                }

                // LAMP Faults
                if (((data[3] & 0xFF) != 0xFF) ) {
                    // LAMPF 1
                    int lampfOneValue = ((data[3] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfOneValue) {
                        case 0x1:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(false);
                            break;
                        case 0x2:
                            FaultStatus.setAddFrontLightOneActive(false);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        case 0x3:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        case 0x5:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(false);
                            break;
                        case 0x6:
                            FaultStatus.setAddFrontLightOneActive(false);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        case 0x9:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(false);
                            break;
                        case 0xA:
                            FaultStatus.setAddFrontLightOneActive(false);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        case 0xB:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        case 0xD:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(false);
                            break;
                        case 0xE:
                            FaultStatus.setAddFrontLightOneActive(false);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        default:
                            FaultStatus.setAddFrontLightOneActive(false);
                            FaultStatus.setAddFrontLightTwoActive(false);
                            break;
                    }
                }
                // LAMPF 2
                if (((data[4] & 0xFF) != 0xFF) ) {
                    int lampfTwoHighValue = ((data[4] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfTwoHighValue) {
                        case 0x1:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x2:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x3:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x4:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x5:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x6:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x7:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x9:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0xA:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0xB:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0xC:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0xD:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0xE:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0xF:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        default:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                    }
                    int lampfTwoLowValue = (data[4] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfTwoLowValue) {
                        case 0x1:
                            FaultStatus.setFrontParkingLightOneActive(true);
                            FaultStatus.setFrontParkingLightTwoActive(false);
                            FaultStatus.setLowBeamActive(false);
                            FaultStatus.setHighBeamActive(false);
                            break;
                        case 0x2:
                            FaultStatus.setFrontParkingLightOneActive(false);
                            FaultStatus.setFrontParkingLightTwoActive(true);
                            FaultStatus.setLowBeamActive(false);
                            FaultStatus.setHighBeamActive(false);
                            break;
                        case 0x3:
                            FaultStatus.setFrontParkingLightOneActive(true);
                            FaultStatus.setFrontParkingLightTwoActive(true);
                            FaultStatus.setLowBeamActive(false);
                            FaultStatus.setHighBeamActive(false);
                            break;
                        case 0x4:
                            FaultStatus.setFrontParkingLightOneActive(false);
                            FaultStatus.setFrontParkingLightTwoActive(false);
                            FaultStatus.setLowBeamActive(true);
                            FaultStatus.setHighBeamActive(false);
                            break;
                        case 0x5:
                            FaultStatus.setFrontParkingLightOneActive(true);
                            FaultStatus.setFrontParkingLightTwoActive(false);
                            FaultStatus.setLowBeamActive(true);
                            FaultStatus.setHighBeamActive(false);
                            break;
                        case 0x6:
                            FaultStatus.setFrontParkingLightOneActive(false);
                            FaultStatus.setFrontParkingLightTwoActive(true);
                            FaultStatus.setLowBeamActive(true);
                            FaultStatus.setHighBeamActive(false);
                            break;
                        case 0x7:
                            FaultStatus.setFrontParkingLightOneActive(true);
                            FaultStatus.setFrontParkingLightTwoActive(true);
                            FaultStatus.setLowBeamActive(true);
                            FaultStatus.setHighBeamActive(false);
                            break;
                        case 0x8:
                            FaultStatus.setFrontParkingLightOneActive(false);
                            FaultStatus.setFrontParkingLightTwoActive(false);
                            FaultStatus.setLowBeamActive(false);
                            FaultStatus.setHighBeamActive(true);
                            break;
                        case 0x9:
                            FaultStatus.setFrontParkingLightOneActive(true);
                            FaultStatus.setFrontParkingLightTwoActive(false);
                            FaultStatus.setLowBeamActive(false);
                            FaultStatus.setHighBeamActive(true);
                            break;
                        case 0xA:
                            FaultStatus.setFrontParkingLightOneActive(false);
                            FaultStatus.setFrontParkingLightTwoActive(true);
                            FaultStatus.setLowBeamActive(false);
                            FaultStatus.setHighBeamActive(true);
                            break;
                        case 0xB:
                            FaultStatus.setFrontParkingLightOneActive(true);
                            FaultStatus.setFrontParkingLightTwoActive(true);
                            FaultStatus.setLowBeamActive(false);
                            FaultStatus.setHighBeamActive(true);
                            break;
                        case 0xC:
                            FaultStatus.setFrontParkingLightOneActive(false);
                            FaultStatus.setFrontParkingLightTwoActive(false);
                            FaultStatus.setLowBeamActive(true);
                            FaultStatus.setHighBeamActive(true);
                            break;
                        case 0xD:
                            FaultStatus.setFrontParkingLightOneActive(true);
                            FaultStatus.setFrontParkingLightTwoActive(false);
                            FaultStatus.setLowBeamActive(true);
                            FaultStatus.setHighBeamActive(true);
                            break;
                        case 0xE:
                            FaultStatus.setFrontParkingLightOneActive(false);
                            FaultStatus.setFrontParkingLightTwoActive(true);
                            FaultStatus.setLowBeamActive(true);
                            FaultStatus.setHighBeamActive(true);
                            break;
                        case 0xF:
                            FaultStatus.setFrontParkingLightOneActive(true);
                            FaultStatus.setFrontParkingLightTwoActive(true);
                            FaultStatus.setLowBeamActive(true);
                            FaultStatus.setHighBeamActive(true);
                            break;
                        default:
                            FaultStatus.setFrontParkingLightOneActive(false);
                            FaultStatus.setFrontParkingLightTwoActive(false);
                            FaultStatus.setLowBeamActive(false);
                            FaultStatus.setHighBeamActive(false);
                            break;
                    }
                }

                // LAMPF 3
                if (((data[5] & 0xFF) != 0xFF) ) {
                    int lampfThreeHighValue = ((data[5] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfThreeHighValue) {
                        case 0x1:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
                        case 0x3:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
                        case 0x5:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
                        case 0x7:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
                        case 0x9:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
                        case 0xB:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
                        case 0xD:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
                        case 0xF:
                            FaultStatus.setrearRightSignalActive(true);
                        default:
                            FaultStatus.setrearRightSignalActive(false);
                            break;
                    }
                    int lampfThreeLowValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfThreeLowValue) {
                        case 0x1:
                            FaultStatus.setrearLeftSignalActive(false);
                            FaultStatus.setRearLightActive(true);
                            FaultStatus.setBrakeLightActive(false);
                            FaultStatus.setLicenseLightActive(false);
                            break;
                        case 0x2:
                            FaultStatus.setrearLeftSignalActive(false);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(true);
                            FaultStatus.setLicenseLightActive(false);
                            break;
                        case 0x3:
                            FaultStatus.setrearLeftSignalActive(false);
                            FaultStatus.setRearLightActive(true);
                            FaultStatus.setBrakeLightActive(true);
                            FaultStatus.setLicenseLightActive(false);
                            break;
                        case 0x4:
                            FaultStatus.setrearLeftSignalActive(false);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(false);
                            FaultStatus.setLicenseLightActive(true);
                            break;
                        case 0x5:
                            FaultStatus.setrearLeftSignalActive(true);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(false);
                            FaultStatus.setLicenseLightActive(true);
                            break;
                        case 0x6:
                            FaultStatus.setrearLeftSignalActive(false);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(true);
                            FaultStatus.setLicenseLightActive(true);
                            break;
                        case 0x7:
                            FaultStatus.setrearLeftSignalActive(false);
                            FaultStatus.setRearLightActive(true);
                            FaultStatus.setBrakeLightActive(true);
                            FaultStatus.setLicenseLightActive(true);
                            break;
                        case 0x8:
                            FaultStatus.setrearLeftSignalActive(true);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(false);
                            FaultStatus.setLicenseLightActive(false);
                            break;
                        case 0x9:
                            FaultStatus.setrearLeftSignalActive(true);
                            FaultStatus.setRearLightActive(true);
                            FaultStatus.setBrakeLightActive(false);
                            FaultStatus.setLicenseLightActive(false);
                            break;
                        case 0xA:
                            FaultStatus.setrearLeftSignalActive(true);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(true);
                            FaultStatus.setLicenseLightActive(false);
                            break;
                        case 0xC:
                            FaultStatus.setrearLeftSignalActive(true);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(false);
                            FaultStatus.setLicenseLightActive(true);
                            break;
                        case 0xD:
                            FaultStatus.setrearLeftSignalActive(true);
                            FaultStatus.setRearLightActive(true);
                            FaultStatus.setBrakeLightActive(true);
                            FaultStatus.setLicenseLightActive(false);
                            break;
                        case 0xE:
                            FaultStatus.setrearLeftSignalActive(true);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(true);
                            FaultStatus.setLicenseLightActive(true);
                            break;
                        case 0xF:
                            FaultStatus.setrearLeftSignalActive(true);
                            FaultStatus.setRearLightActive(true);
                            FaultStatus.setBrakeLightActive(true);
                            FaultStatus.setLicenseLightActive(true);
                            break;
                        default:
                            FaultStatus.setrearLeftSignalActive(false);
                            FaultStatus.setRearLightActive(false);
                            FaultStatus.setBrakeLightActive(false);
                            FaultStatus.setLicenseLightActive(false);
                            break;
                    }
                }

                // LAMPF 4
                if (((data[6] & 0xFF) != 0xFF) ) {
                    int lampfFourHighValue = ((data[6] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfFourHighValue) {
                        case 0x1:
                            FaultStatus.setRearFogLightActive(true);
                            break;
                        case 0x3:
                            FaultStatus.setRearFogLightActive(true);
                            break;
                        case 0x5:
                            FaultStatus.setRearFogLightActive(true);
                            break;
                        case 0x7:
                            FaultStatus.setRearFogLightActive(true);
                            break;
                        case 0x9:
                            FaultStatus.setRearFogLightActive(true);
                            break;
                        case 0xB:
                            FaultStatus.setRearFogLightActive(true);
                            break;
                        case 0xD:
                            FaultStatus.setRearFogLightActive(true);
                            break;
                        case 0xF:
                            FaultStatus.setRearFogLightActive(true);
                        default:
                            FaultStatus.setRearFogLightActive(false);
                            break;
                    }
                    int lampfFourLowValue = (data[6] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfFourLowValue) {
                        case 0x1:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x2:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x3:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x4:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x5:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x6:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x7:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x8:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0x9:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xA:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xB:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xC:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xD:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xE:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xF:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        default:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                    }
                }
                break;
            case 0x09:
                //Log.d(TAG, "Message ID 9");
                //Fuel Economy 1
                if ((data[2] & 0xFF) != 0xFF) {
                    double fuelEconomyOne = ((((data[2] & 0xFF) >> 4) & 0x0f) * 1.6) + (((data[2] & 0xFF) & 0x0f) * 0.1);
                    Data.setFuelEconomyOne(fuelEconomyOne);
                }
                //Fuel Economy 2
                if ((data[3] & 0xFF) != 0xFF) {
                    double fuelEconomyTwo = ((((data[3] & 0xFF) >> 4) & 0x0f) * 1.6) + (((data[3] & 0xFF) & 0x0f) * 0.1);
                    Data.setFuelEconomyTwo(fuelEconomyTwo);
                }
                //Current Consumption
                if ((data[4] & 0xFF) != 0xFF) {
                    double cConsumption = ((((data[4] & 0xFF) >> 4) & 0x0f) * 1.6) + (((data[4] & 0xFF) & 0x0f) * 0.1);
                    Data.setCurrentConsumption(cConsumption);
                }
                break;
            case 0x0a:
                //Log.d(TAG, "Message ID 10");
                double odometer = Utils.bytesToInt(data[3],data[2],data[1]);
                Data.setOdometer(odometer);

                if ((data[6] & 0xFF) != 0xFF && (data[5] & 0xFF) != 0xFF && (data[4] & 0xFF) != 0xFF) {
                    double tripAuto = Utils.bytesToInt(data[6], data[5], data[4]) / 10;
                    Data.setTripAuto(tripAuto);
                }
                break;
            case 0x0b:
                //Log.d(TAG, "Message ID 11");
                break;
            case 0x0c:
                //Log.d(TAG, "Message ID 12");
                if ((data[3] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF && (data[1] & 0xFF) != 0xFF) {
                    double trip1 = Utils.bytesToInt(data[3], data[2], data[1]) / 10;
                    Data.setTripOne(trip1);
                }
                if ((data[6] & 0xFF) != 0xFF && (data[5] & 0xFF) != 0xFF && (data[4] & 0xFF) != 0xFF) {
                    double trip2 = Utils.bytesToInt(data[6], data[5], data[4]) / 10;
                    Data.setTripTwo(trip2);
                }
                break;
            case 0xff:
                Log.d(TAG,"Debug Message received: " + Utils.ByteArraytoHex(data));
                //
                if (sharedPrefs.getBoolean("prefShowUartFaults",false)) {
                    FaultStatus.setUartErrorActive(true);
                    if ((data[7] & 0xFF) == 0xf0){
                        FaultStatus.setUartCommTimeoutActive(true);
                    }
                }
                break;
            default:
                Log.d(TAG, "Unknown Message ID: " + String.format("%02x", msgID));
        }
    }
}
