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
package com.blackboxembedded.WunderLINQ;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = "BluetoothLeService";

    int mStartMode;       // indicates how to behave if the service is killed
    boolean mAllowRebind; // indicates whether onRebind should be used

    private static Logger debugLogger = null;

    private static boolean fuelAlertSent = false;
    private static int prevBrakeValue = 0;

    private static SharedPreferences sharedPrefs;

    private static Date holdStartTime;
    private static boolean holdStart = false;
    private static Date RTholdStartTime;
    private static boolean RTholdStart = false;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor rotationVector;
    private Sensor gravity;
    private Sensor barometer;
    private Sensor acceleration;
    private Sensor lightSensor;

    static boolean itsDark = false;
    private long darkTimer = 0;
    private long lightTimer = 0;

    /*
     * time smoothing constant for low-pass filter
     * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
     * was 0.15f
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    static final float ALPHA = 0.05f;
    float[] mGravity = new float[3];
    float[] mGeomagnetic = new float[3];
    float[] mAcceleration = new float[3];

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;

    private int lastDirection;

    private static byte[] lastMessage00 = new byte[8];
    private static byte[] lastMessage01 = new byte[8];
    private static byte[] lastMessage04 = new byte[8];
    private static byte[] lastMessage05 = new byte[8];
    private static byte[] lastMessage06 = new byte[8];
    private static byte[] lastMessage07 = new byte[8];
    private static byte[] lastMessage08 = new byte[8];
    private static byte[] lastMessage09 = new byte[8];
    private static byte[] lastMessage0A = new byte[8];
    private static byte[] lastMessage0B = new byte[8];
    private static byte[] lastMessage0C = new byte[8];

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
    public final static String ACTION_WRITE_FAILED =
            "android.bluetooth.device.action.ACTION_WRITE_FAILED";
    public final static String ACTION_WRITE_SUCCESS =
            "android.bluetooth.device.action.ACTION_WRITE_SUCCESS";
    private final static String ACTION_GATT_DISCONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTING";
    private final static String ACTION_PAIRING_REQUEST =
            "com.example.bluetooth.le.PAIRING_REQUEST";

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
    private static final int STATE_BONDED = 5;

    /**
     * BluetoothAdapter for handling connections
     */
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothGatt mBluetoothGatt;

    /**
     * Disable/enable notification
     */
    public static ArrayList<BluetoothGattCharacteristic> mEnabledCharacteristics = new ArrayList<>();

    public static boolean mDisableNotificationFlag = false;

    public static int mConnectionState = STATE_DISCONNECTED;
    /**
     * Device address
     */
    private static String mBluetoothDeviceAddress;
    private static String mBluetoothDeviceName;

    /**
     * Flag to check the mBound status
     */
    public boolean mBound;

    /**
     * BlueTooth manager for handling connections
     */
    private BluetoothManager mBluetoothManager;

    private final IBinder mBinder = new LocalBinder();

    /**
     * Local binder class
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public BluetoothLeService() {

    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Data.setLastLocation(location);
                if (sharedPrefs.getBoolean("prefBearingOverride", false) && location.hasBearing()) {
                    Data.setBearing((int)location.getBearing());
                }
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG,"In onCreate");
        // The service is being created
        // Initializing the service
        if (!initialize()) {
            Log.d(TAG,"Service not initialized");
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());

        // Sensor Stuff
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        acceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, rotationVector, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorEventListener, gravity, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(sensorEventListener, barometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, acceleration, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Location stuff
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /* Looper */);
        }

        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();
                Data.setTime(c.getTime());
                final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
                MyApplication.getContext().sendBroadcast(intent);

                //Send time to cluster
                if (MainActivity.gattCommandCharacteristic != null) {
                    if (Data.getFirmwareVersion() == null){
                        // Read config
                        byte[] getConfigCmd = {0x57,0x52,0x57,0x0D,0x0A};
                        Log.d(TAG, "Sending get config command");
                        MainActivity.gattCommandCharacteristic.setValue(getConfigCmd);
                        BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                    } else {
                        BluetoothGattCharacteristic characteristic = MainActivity.gattCommandCharacteristic;
                        //Get Current Time
                        Date date = new Date();
                        Calendar calendar = new GregorianCalendar();
                        calendar.setTime(date);
                        int year = calendar.get(Calendar.YEAR);
                        //Add one to month {0 - 11}
                        int month = calendar.get(Calendar.MONTH) + 1;
                        int day = calendar.get(Calendar.DAY_OF_MONTH);
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        int minute = calendar.get(Calendar.MINUTE);
                        int second = calendar.get(Calendar.SECOND);
                        int yearByte = (year >> 4);
                        byte yearLByte = (byte) year;
                        int yearNibble = (yearLByte & 0x0F);
                        byte monthNibble = (byte) month;
                        int monthYearByte = ((yearNibble & 0x0F) << 4 | (monthNibble & 0x0F));
                        byte[] setClusterClock = {0x57, 0x57, 0x44, 0x43, (byte) second, (byte) minute, (byte) hour, (byte) day, (byte) monthYearByte, (byte) yearByte};
                        characteristic.setValue(setClusterClock);
                        writeCharacteristic(characteristic);
                    }
                }

            }

        }, 1000, 1000); //Initial Delay and Period for update (in milliseconds)
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        Log.d(TAG,"onStartCommand");
        return mStartMode;
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        // A client is binding to the service with bindService()
        mBound = true;
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        // All clients have unbound with unbindService()
        mBound = false;
        return mAllowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind");
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        // The service is no longer used and is being destroyed
        clearNotifications();
        sensorManager.unregisterListener(sensorEventListener, magnetometer);
        sensorManager.unregisterListener(sensorEventListener, accelerometer);
        sensorManager.unregisterListener(sensorEventListener, rotationVector);
        sensorManager.unregisterListener(sensorEventListener, gravity);
        sensorManager.unregisterListener(sensorEventListener, barometer);
        sensorManager.unregisterListener(sensorEventListener, acceleration);
        sensorManager.unregisterListener(sensorEventListener, lightSensor);

        stopLocationUpdates();
    }

    // Listens for sensor events
    private final SensorEventListener sensorEventListener
            = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                //Get Rotation Vector Sensor Values
                float[] mRotationMatrix = new float[9];
                float[] mRotationFixMatrix = new float[9];
                float[] orientation = new float[3];
                double leanAngle = 0.0;
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                int rotation = MyApplication.getContext().getResources().getConfiguration().orientation;
                if(rotation == 1) { // Default display rotation is portrait
                    SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationFixMatrix);
                    SensorManager.getOrientation(mRotationFixMatrix, orientation);
                    leanAngle = (orientation[2] * 180) / Math.PI;
                } else {   // Default display rotation is landscape
                    SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationFixMatrix);
                    SensorManager.getOrientation(mRotationFixMatrix, orientation);
                    leanAngle = ((orientation[2] * 180) / Math.PI) + 90;
                }
                Data.setLeanAngle(leanAngle);
            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                mGravity = event.values.clone();
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic = Utils.lowPass(event.values.clone(), mGeomagnetic, ALPHA);
            } else if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                float[] mBarometricPressure = event.values;
                Data.setBarometricPressure((double)mBarometricPressure[0]);
            } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                mAcceleration = event.values.clone();
                double gforce = Math.sqrt(mAcceleration[0] * mAcceleration[0] + mAcceleration[1] * mAcceleration[1] + mAcceleration[2] * mAcceleration[2]);
                Data.setGForce(gforce);
            } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                if (sharedPrefs.getString("prefNightModeCombo", "0").equals("2")) {
                    int delay = (Integer.parseInt(sharedPrefs.getString("prefAutoNightModeDelay", "30")) * 1000);
                    float currentReading = event.values[0];
                    double darkThreshold = 20.0;  // Light level to determine darkness
                    if (currentReading < darkThreshold) {
                        lightTimer = 0;
                        if (darkTimer == 0) {
                            darkTimer = System.currentTimeMillis();
                        } else {
                            long currentTime = System.currentTimeMillis();
                            long duration = (currentTime - darkTimer);
                            if ((duration >= delay) && (!itsDark)) {
                                itsDark = true;
                                Log.d(TAG, "Its dark");
                                // Update Theme
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            }
                        }
                    } else {
                        darkTimer = 0;
                        if (lightTimer == 0) {
                            lightTimer = System.currentTimeMillis();
                        } else {
                            long currentTime = System.currentTimeMillis();
                            long duration = (currentTime - lightTimer);
                            if ((duration >= delay) && (itsDark)) {
                                itsDark = false;
                                Log.d(TAG, "Its light");
                                // Update Theme
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            }
                        }
                    }
                }
            }
            if (mGravity != null && mGeomagnetic != null) {
                float[] R = new float[9];
                float[] I = new float[9];
                float[] remappedR = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedR);
                    SensorManager.getOrientation(remappedR, orientation);
                    int direction = filterChange(Utils.normalizeDegrees(Math.toDegrees(orientation[0])));
                    if(direction != lastDirection) {
                        lastDirection = direction;
                        if (!sharedPrefs.getBoolean("prefBearingOverride", false)) {
                            Data.setBearing(lastDirection);
                        }
                    }
                }
            }
        }
    };

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

    /**
     * Implements callback methods for GATT events that the app cares about. For
     * example,connection change and services discovered.
     */
    private final static BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
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
                /*
                lastMessage00 = new byte[8];
                lastMessage01 = new byte[8];
                lastMessage04 = new byte[8];
                lastMessage05 = new byte[8];
                lastMessage06 = new byte[8];
                lastMessage07 = new byte[8];
                lastMessage08 = new byte[8];
                lastMessage09 = new byte[8];
                lastMessage0A = new byte[8];
                lastMessage0C = new byte[8];
                Data.clear();
                FaultStatus.clear();
                */
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Intent intent = new Intent(ACTION_WRITE_SUCCESS);
                MyApplication.getContext().sendBroadcast(intent);
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
                mDisableNotificationFlag = false;
                Intent intent = new Intent(ACTION_WRITE_FAILED);
                MyApplication.getContext().sendBroadcast(intent);
            }
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            String dataLog = "Read response received";
            Log.d(TAG,dataLog);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Intent intent = new Intent(ACTION_WRITE_SUCCESS);
                Bundle mBundle = new Bundle();
                // Putting the byte value read for GATT Db
                final byte[] data = characteristic.getValue();
                mBundle.putByteArray(EXTRA_BYTE_VALUE,
                        data);
                mBundle.putString(EXTRA_BYTE_UUID_VALUE,
                        characteristic.getUuid().toString());
                mBundle.putString("ACTION_WRITE_SUCCESS",
                        "" + status);
                intent.putExtras(mBundle);
                if (characteristic.getUuid().toString().contains(GattAttributes.WUNDERLINQ_COMMAND_CHARACTERISTIC)){
                    readCharacteristic(MainActivity.gattCommandCharacteristic);
                }

                MyApplication.getContext().sendBroadcast(intent);
            } else {
                Intent intent = new Intent(ACTION_GATT_CHARACTERISTIC_ERROR);
                intent.putExtra("EXTRA_CHARACTERISTIC_ERROR_MESSAGE", "" + status);
                MyApplication.getContext().sendBroadcast(intent);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            // GATT Characteristic read
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastNotifyUpdate(characteristic);
            } else {
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                    bondDevice();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastNotifyUpdate(characteristic);
        }
    };

    private static void broadcastConnectionUpdate(final String action) {
        Log.d(TAG,"Action: " + action);
        final Intent intent = new Intent(action);
        MyApplication.getContext().sendBroadcast(intent);
    }

    private static void broadcastNotifyUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle mBundle = new Bundle();
        // Putting the byte value read for GATT Db
        final byte[] data = characteristic.getValue();
        mBundle.putByteArray(EXTRA_BYTE_VALUE,
                data);
        mBundle.putString(EXTRA_BYTE_UUID_VALUE,
                characteristic.getUuid().toString());

        if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_MESSAGE_CHARACTERISTIC)) {
            if (data != null) {
                if (sharedPrefs.getBoolean("prefDebugLogging", false)) {
                    // Log data
                    if (debugLogger == null) {
                        debugLogger = new Logger();
                    }
                    debugLogger.write(Utils.ByteArraytoHexNoDelim(data));
                } else {
                    if (debugLogger != null) {
                        debugLogger.shutdown();
                        debugLogger = null;
                    }
                }
                //Check if message changed
                boolean process = false;
                switch (data[0]){
                    case 0x00:
                        if(!Arrays.equals(lastMessage00, data)){
                            lastMessage00 = data;
                            process = true;
                        }
                        break;
                    case 0x01:
                        if(!Arrays.equals(lastMessage01, data)){
                            lastMessage01 = data;
                            process = true;
                        }
                        break;
                    case 0x04:

                        if(!Arrays.equals(lastMessage04, data)){
                            lastMessage04 = data;
                            process = true;
                        }
                        break;
                    case 0x05:
                        if(!Arrays.equals(lastMessage05, data)){
                            lastMessage05 = data;
                            process = true;
                        }
                        break;
                    case 0x06:
                        if(!Arrays.equals(lastMessage06, data)){
                            lastMessage06 = data;
                            process = true;
                        }
                        break;
                    case 0x07:
                        if(!Arrays.equals(lastMessage07, data)){
                            lastMessage07 = data;
                            process = true;
                        }
                        break;
                    case 0x08:
                        if(!Arrays.equals(lastMessage08, data)){
                            lastMessage08 = data;
                            process = true;
                        }
                        break;
                    case 0x09:
                        if(!Arrays.equals(lastMessage09, data)){
                            lastMessage09 = data;
                            process = true;
                        }
                        break;
                    case 0x0a:
                        if(!Arrays.equals(lastMessage0A, data)){
                            lastMessage0A = data;
                            process = true;
                        }
                        break;
                    case 0x0b:
                        if(!Arrays.equals(lastMessage0B, data)){
                            lastMessage0B = data;
                            process = true;
                        }
                        break;
                    case 0x0c:
                        if(!Arrays.equals(lastMessage0C, data)){
                            lastMessage0C = data;
                            process = true;
                        }
                        break;
                }

                if(process) {
                    parseMessage(data);
                    /*
                     * Sending the broad cast so that it can be received on registered
                     * receivers
                     */
                    intent.putExtras(mBundle);
                    MyApplication.getContext().sendBroadcast(intent);
                }
            }
        } else if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_COMMAND_CHARACTERISTIC)){
            if (data != null) {
                //Read Config
                if ((data[0] == 0x57) && (data[1] == 0x52) && (data[2] == 0x57)){
                    String version = data[9] + "." + data[10];
                    Data.setFirmwareVersion(Double.parseDouble(version));
                    if(Data.getFirmwareMode() != data[26]
                    || Data.getFirmwareSensitivity() != data[34]) {
                        Data.setFirmwareMode(data[26]);
                        Data.setFirmwareSensitivity(data[34]);
                        intent.putExtras(mBundle);
                        MyApplication.getContext().sendBroadcast(intent);
                    }
                }
            }
        } else {
            /*
             * Sending the broad cast so that it can be received on registered
             * receivers
             */
            intent.putExtras(mBundle);
            MyApplication.getContext().sendBroadcast(intent);
        }
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

        String dataLog = "[" + devicename + "|" + address + "] " +
                "Connection request sent";
        Log.d(TAG,dataLog);
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
            mBluetoothGatt.disconnect();
            String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    "Disconnection request sent";
            Log.d(TAG,dataLog);
            //Data.clear();
            //FaultStatus.clear();
            clearNotifications();
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
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public static void close() {
        mBluetoothGatt.close();
        mBluetoothGatt = null;
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
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
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

            } else {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = "Start notification request sent";
            Log.d(TAG,dataLog);
        } else {
            String dataLog = "Stop notification request sent";
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
                Log.d(TAG,"Added notify characteristic");
                addEnabledCharacteristic(descriptor.getCharacteristic());
                break;
            case 2:
                //Enabled indication
                Log.d(TAG,"Added indicate characteristic");
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
                alertIntent.putExtra("TYPE", 1);
                alertIntent.putExtra("TITLE", MyApplication.getContext().getResources().getString(R.string.alert_title_fuel));
                alertIntent.putExtra("BODY", MyApplication.getContext().getResources().getString(R.string.alert_label_fuel));
                alertIntent.putExtra("BACKGROUND", "");
                MyApplication.getContext().startActivity(alertIntent);
            }

        } else {
            fuelAlertSent = false;
        }
    }

    private static void parseMessage(byte[] data){
        //Log.d(TAG,"MSG");
        Data.setLastMessage(data);
        int msgID = (data[0] & 0xFF) ;
        switch (msgID) {
            case 0x00:
                //Log.d(TAG, "Message ID 0");
                byte[] vinValue = new byte[7];
                int sum = 0;
                for (int x = 1; x <= 7; x++){
                    vinValue[x - 1] = data[x];
                    sum = sum + data[x];
                }
                if (sum > 0) {
                    String vin = new String(vinValue);
                    Data.setVin(vin);
                }

                break;
            case 0x01:
                //Log.d(TAG, "Message ID 1");
                if (sharedPrefs.getBoolean("prefTaskSwitcher",false)) {
                    if ((data[1] & 0xFF) == 0x65) {
                        if (RTholdStartTime == null) {
                            RTholdStartTime = new Date();
                        } else {
                            Date currentTime = new Date();
                            if (currentTime.getTime() - RTholdStartTime.getTime() >= 1500) {
                                if (!RTholdStart) {
                                    RTholdStart = true;
                                    Log.d("task", "RT Open Task Switcher");
                                    Intent accessibilityService = new Intent(MyApplication.getContext(), MyAccessibilityService.class);
                                    accessibilityService.putExtra("command", 2);
                                    MyApplication.getContext().startService(accessibilityService);
                                }
                            }
                        }

                    } else {
                        RTholdStart = false;
                        RTholdStartTime = null;
                    }
                }
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
                if (sharedPrefs.getBoolean("prefTaskSwitcher",false)) {
                    if (((data[4] & 0xFF) == 0xFD) || (data[4] & 0xFF) == 0x01) {
                        if (holdStartTime == null) {
                            holdStartTime = new Date();
                        } else {
                            Date currentTime = new Date();
                            if (currentTime.getTime() - holdStartTime.getTime() >= 1500) {
                                if (!holdStart) {
                                    holdStart = true;
                                    Log.d("task", "Open Task Switcher");
                                    Intent accessibilityService = new Intent(MyApplication.getContext(), MyAccessibilityService.class);
                                    accessibilityService.putExtra("command", 2);
                                    MyApplication.getContext().startService(accessibilityService);
                                }
                            }
                        }

                    } else {
                        holdStart = false;
                        holdStartTime = null;
                    }
                }
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
                    case 0x2: case 0x5: case 0x6: case 0x7: case 0xA: case 0xD: case 0xE:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0x3: case 0xB:
                        FaultStatus.setAbsSelfDiagActive(true);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(false);
                        break;
                    case 0x8:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(true);
                        FaultStatus.setabsErrorActive(false);
                        break;
                    case 0xF: default:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(false);
                        break;
                }

                // Tire Pressure
                if ((data[4] & 0xFF) != 0xFF) {
                    double rdcFront = (data[4] & 0xFF) / 50.0;
                    Data.setFrontTirePressure(rdcFront);
                    if (sharedPrefs.getBoolean("prefTPMSAlert", false)) {
                        int pressureThreshold = Integer.parseInt(sharedPrefs.getString("prefTPMSAlertThreshold", "-1"));
                        if (pressureThreshold >= 0) {
                            String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
                            if (pressureFormat.contains("1")) {
                                // KPa
                                if (pressureThreshold >= Utils.barTokPa(rdcFront)) {
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("2")) {
                                // Kg-f
                                if (pressureThreshold >= Utils.barTokgf(rdcFront)) {
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("3")) {
                                // Psi
                                if (pressureThreshold >= Utils.barToPsi(rdcFront)) {
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                }
                            }
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive())) {
                                    updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                                }
                            }
                        } else {
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                }
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
                                if (pressureThreshold >= Utils.barTokPa(rdcRear)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("2")) {
                                // Kg-f
                                if (pressureThreshold >= Utils.barTokgf(rdcRear)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("3")) {
                                // Psi
                                if (pressureThreshold >= Utils.barToPsi(rdcRear)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            }
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                    updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                                }
                            }
                        } else {
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                }
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
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                        case 0xCA:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(true);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                        case 0xCB:
                            FaultStatus.setfrontTirePressureWarningActive(true);
                            FaultStatus.setrearTirePressureWarningActive(true);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                        case 0xD1:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(true);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive())) {
                                    updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                        case 0xD2:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(true);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (!(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                    updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                                }
                            }
                            break;
                        case 0xD3:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(true);
                            FaultStatus.setrearTirePressureCriticalActive(true);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive()) && !(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                    updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                                }
                            }
                            break;
                        default:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
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
                if ((data[3] & 0xFF) != 0xFF) {
                    int minPosition = 36;
                    int maxPosition = 236;
                    double throttlePosition = (((data[3] & 0xFF) - minPosition) * 100.0) / (maxPosition - minPosition);
                    Data.setThrottlePosition(throttlePosition);
                }

                // Engine Temperature
                if ((data[4] & 0xFF) != 0xFF) {
                    double engineTemp = ((data[4] & 0xFF) * 0.75) - 25;
                    Data.setEngineTemperature(engineTemp);
                }

                // ASC Fault
                int ascValue = ((data[5] & 0xFF)  >> 4) & 0x0f; // the highest 4 bits.
                switch (ascValue){
                    case 0x1: case 0x9:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(true);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0x2: case 0x5: case 0x6: case 0x7: case 0xA: case 0xD: case 0xE:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    case 0x3: case 0xB:
                        FaultStatus.setAscSelfDiagActive(true);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0x8:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(true);
                        FaultStatus.setascErrorActive(false);
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
                    case 0x2: case 0x6: case 0xA: case 0xE:
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
                    double speed = (data[3] & 0xFF) * 2;
                    Data.setSpeed(speed);
                }

                //Voltage
                if ((data[4] & 0xFF) != 0xFF) {
                    double voltage = (data[4] & 0xFF) / 10.0;
                    Data.setvoltage(voltage);
                }

                // Fuel Fault
                int fuelValue = ((data[5] & 0xFF)  >> 4) & 0x0f; // the highest 4 bits.
                switch (fuelValue){
                    case 0x2: case 0x6: case 0xA: case 0xE:
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
                    case 0x1: case 0xD:
                        FaultStatus.setGeneralFlashingYellowActive(true);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x2: case 0xE:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x4: case 0x7:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(true);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x5:
                        FaultStatus.setGeneralFlashingYellowActive(true);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(true);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x6:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(true);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x8: case 0xB:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (!(FaultStatus.getgeneralShowsRedNotificationActive())) {
                                updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(true);
                            }
                        }
                        break;
                    case 0x9:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (!FaultStatus.getgeneralShowsRedNotificationActive() && !FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(true);
                                FaultStatus.setGeneralShowsRedNotificationActive(true);
                            }
                        }
                        break;
                    case 0xA:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (!(FaultStatus.getgeneralShowsRedNotificationActive())) {
                                updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(true);
                            }
                        }
                        break;
                    default:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                }
                break;
            case 0x08:
                //Log.d(TAG, "Message ID 8");
                if ((data[1] & 0xFF) != 0xFF) {
                    double ambientTemp = ((data[1] & 0xFF) * 0.50) - 40;
                    Data.setAmbientTemperature(ambientTemp);
                    if(ambientTemp <= 0.0){
                        FaultStatus.seticeWarnActive(true);
                    } else {
                        FaultStatus.seticeWarnActive(false);
                    }
                }

                // LAMP Faults
                if (((data[3] & 0xFF) != 0xFF) ) {
                    // LAMPF 1
                    int lampfOneValue = ((data[3] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfOneValue) {
                        case 0x1: case 0x5: case 0x9: case 0xD:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(false);
                            break;
                        case 0x2: case 0x6: case 0xA: case 0xE:
                            FaultStatus.setAddFrontLightOneActive(false);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        case 0x3: case 0xB:
                            FaultStatus.setAddFrontLightOneActive(true);
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
                        case 0x1: case 0x9:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x2: case 0xA:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x3: case 0xB:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x4: case 0xC:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x5: case 0xD:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x6: case 0xE:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x7: case 0xF:
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
                        case 0x1: case 0x3: case 0x5: case 0x7: case 0x9: case 0xB: case 0xD: case 0xF:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
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
                        case 0x5: case 0xC:
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
                        case 0x1: case 0x3: case 0x5: case 0x9: case 0xB: case 0xD: case 0xF:
                            FaultStatus.setRearFogLightActive(true);
                            break;
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
                } else {
                    Data.setFuelEconomyOne(null);
                }
                //Fuel Economy 2
                if ((data[3] & 0xFF) != 0xFF) {
                    double fuelEconomyTwo = ((((data[3] & 0xFF) >> 4) & 0x0f) * 1.6) + (((data[3] & 0xFF) & 0x0f) * 0.1);
                    Data.setFuelEconomyTwo(fuelEconomyTwo);
                } else {
                    Data.setFuelEconomyTwo(null);
                }
                //Current Consumption
                if ((data[4] & 0xFF) != 0xFF) {
                    double cConsumption = ((((data[4] & 0xFF) >> 4) & 0x0f) * 1.6) + (((data[4] & 0xFF) & 0x0f) * 0.1);
                    Data.setCurrentConsumption(cConsumption);
                } else {
                    Data.setCurrentConsumption(null);
                }
                break;
            case 0x0a:
                //Log.d(TAG, "Message ID 10");
                if ((data[3] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF && (data[1] & 0xFF) != 0xFF) {
                    double odometer = Utils.bytesToInt(data[3], data[2], data[1]);
                    Data.setOdometer(odometer);
                }

                if ((data[6] & 0xFF) != 0xFF && (data[5] & 0xFF) != 0xFF && (data[4] & 0xFF) != 0xFF) {
                    double tripAuto = Utils.bytesToInt(data[6], data[5], data[4]) / 10.0;
                    Data.setTripAuto(tripAuto);
                }
                break;
            case 0x0b:
                //Log.d(TAG, "Message ID 11");
                if ((data[3] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF && (data[1] & 0xFF) != 0xFF) {
                    int year = (((data[2] & 0xFF) & 0x0f) << 8) |(data[1] & 0xFF);
                    int month = ((data[2] & 0xFF) >> 4 & 0x0f) - 1;
                    int day = (data[3] & 0xFF);
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    Date nextServiceDate = cal.getTime();
                    Data.setNextServiceDate(nextServiceDate);
                }
                if ((data[4] & 0xFF) != 0xFF){
                    int nextService = data[4] * 100;
                    Data.setNextService(nextService);
                }
                break;
            case 0x0c:
                //Log.d(TAG, "Message ID 12");
                if ((data[3] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF && (data[1] & 0xFF) != 0xFF) {
                    double trip1 = Utils.bytesToInt(data[3], data[2], data[1]) / 10.0;
                    Data.setTripOne(trip1);
                }
                if ((data[6] & 0xFF) != 0xFF && (data[5] & 0xFF) != 0xFF && (data[4] & 0xFF) != 0xFF) {
                    double trip2 = Utils.bytesToInt(data[6], data[5], data[4]) / 10.0;
                    Data.setTripTwo(trip2);
                }
                break;
            default:
                //Log.d(TAG, "Unknown Message ID: " + String.format("%02x", msgID));
                break;
        }
    }

    private int filterChange(int newDir){
        int change = newDir - lastDirection;
        int smallestChange = Math.max(Math.min(change,3),-3);
        return lastDirection + smallestChange;
    }

    protected void stopLocationUpdates() {
        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
    }
}