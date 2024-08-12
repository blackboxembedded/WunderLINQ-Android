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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.CellInfo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.blackboxembedded.WunderLINQ.AlertActivity;
import com.blackboxembedded.WunderLINQ.FaultActivity;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_BASE;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_C;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_N;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_X;
import com.blackboxembedded.WunderLINQ.protocols.CANbus;
import com.blackboxembedded.WunderLINQ.protocols.LINbus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a WunderLINQ Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private final static String TAG = "BLE";

    int mStartMode;       // indicates how to behave if the service is killed
    boolean mAllowRebind; // indicates whether onRebind should be used

    private static final Queue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();
    private static Handler bleHandler = new Handler();
    private static volatile boolean commandQueueBusy = false;
    private static boolean isRetrying;
    private static int nrTries;
    // Maximum number of retries of commands
    private static final int MAX_TRIES = 2;

    public enum WriteType {
        WITH_RESPONSE,
        WITHOUT_RESPONSE,
        SIGNED
    }

    public static boolean fuelAlertSent = false;

    private static SharedPreferences sharedPrefs;

    private TelephonyManager telephonyManager;

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
    private LocationManager locationManager;
    private LocationListener locationListener;


    private int lastDirection;
    private static HashMap<Integer, byte[]> messages = new HashMap<>();

    private static BluetoothGattCharacteristic mNotifyCharacteristic;
    public static BluetoothGattCharacteristic gattCommandCharacteristic;
    public static BluetoothGattCharacteristic gattHWCharacteristic;

    /**
     * GATT Status constants
     */
    public final static String ACTION_GATT_CONNECTED =
            "com.blackboxembedded.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "com.blackboxembedded.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.blackboxembedded.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.blackboxembedded.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.blackboxembedded.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_GATT_CHARACTERISTIC_ERROR =
            "com.blackboxembedded.bluetooth.le.ACTION_GATT_CHARACTERISTIC_ERROR";
    public final static String ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL =
            "com.blackboxembedded.bluetooth.le.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL";
    public final static String ACTION_WRITE_FAILED =
            "android.bluetooth.device.action.ACTION_WRITE_FAILED";
    public final static String ACTION_WRITE_SUCCESS =
            "android.bluetooth.device.action.ACTION_WRITE_SUCCESS";
    private final static String ACTION_GATT_DISCONNECTING =
            "com.blackboxembedded.bluetooth.le.ACTION_GATT_DISCONNECTING";
    private final static String ACTION_PAIRING_REQUEST =
            "com.blackboxembedded.bluetooth.le.PAIRING_REQUEST";
    public final static String ACTION_PERFORMANCE_DATA_AVAILABLE =
            "com.blackboxembedded.wunderlinq.ACTION_PERFORMANCE_DATA_AVAILABLE";
    public final static String ACTION_CMDSTATUS_AVAILABLE =
            "com.blackboxembedded.wunderlinq.ACTION_CMDSTATUS_AVAILABLE";
    public final static String ACTION_ACCSTATUS_AVAILABLE =
            "com.blackboxembedded.wunderlinq.ACTION_ACCSTATUS_AVAILABLE";

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

    public static int connectedType = 0;

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
                updateLocationData(location);
            }
        }
    };

    private void updateLocationData(Location location) {
        Data.setLastLocation(location);
        if (sharedPrefs.getBoolean("prefBearingOverride", false) && location.hasBearing()) {
            Data.setBearing((int) location.getBearing());
        }
    }

    private final PhoneStateListener signalListener = new PhoneStateListener() {
        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfoList) {
            if (ActivityCompat.checkSelfPermission(BluetoothLeService.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(BluetoothLeService.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            super.onCellInfoChanged(cellInfoList);
            if (cellInfoList != null) {
                for (CellInfo cellInfo : cellInfoList) {
                    if (cellInfo instanceof CellInfoGsm) {
                        CellSignalStrengthGsm signalStrengthGsm = ((CellInfoGsm) cellInfo).getCellSignalStrength();
                        int dBm = signalStrengthGsm.getDbm();
                        Data.setCellularSignal(dBm);
                    } else if (cellInfo instanceof CellInfoLte) {
                        CellSignalStrengthLte signalStrengthLte = ((CellInfoLte) cellInfo).getCellSignalStrength();
                        int dBm = signalStrengthLte.getDbm();
                        Data.setCellularSignal(dBm);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (cellInfo instanceof CellInfoNr) {
                            CellSignalStrengthNr signalStrengthNr = (CellSignalStrengthNr) ((CellInfoNr) cellInfo).getCellSignalStrength();
                            int dBm = signalStrengthNr.getDbm();
                            Data.setCellularSignal(dBm);
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        // The service is being created
        // Initializing the service
        if (!initialize()) {
            Log.d(TAG, "Service not initialized");
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());

        // Get the TelephonyManager instance
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"No permission for telephonyManager");
        } else {
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_CELL_INFO);
            }
        }

        // Register the BroadcastReceiver to listen for battery status changes
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        // Sensor Stuff
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            startLocationUpdatesWithFusedLocationProvider();
        } else {
            startLocationUpdatesWithLocationManager();
        }

        // Update time Data field and send to the cluster if WLQ_N
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();
                Data.setTime(c.getTime());

                //Send time to cluster
                if (Data.wlq != null) {
                    if (Data.wlq.getHardwareType() == WLQ.TYPE_NAVIGATOR) {
                        if (gattCommandCharacteristic != null) {
                            BluetoothGattCharacteristic characteristic = gattCommandCharacteristic;
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
                            try {
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                outputStream.write(WLQ_N.SET_CLUSTER_CLOCK_CMD);
                                outputStream.write((byte) second);
                                outputStream.write((byte) minute);
                                outputStream.write((byte) hour);
                                outputStream.write((byte) day);
                                outputStream.write((byte) monthYearByte);
                                outputStream.write((byte) yearByte);
                                outputStream.write(WLQ_N.CMD_EOM);
                                byte[] setClusterClock = outputStream.toByteArray();
                                writeCharacteristic(characteristic, setClusterClock, WriteType.WITH_RESPONSE);
                            } catch (IOException e) {
                                Log.d(TAG, e.toString());
                            }
                        }
                    }
                }
            }
        }, 1000, 1000); //Initial Delay and Period for update (in milliseconds)
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        Log.d(TAG, "onStartCommand");
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
        unregisterReceiver(batteryReceiver);
        if (telephonyManager != null) {
            telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_NONE);
        }
        stopLocationUpdates();
    }

    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level != -1 && scale != -1) {
                float batteryPct = level * 100 / (float)scale;
                Data.setLocalBattery((double)batteryPct);
            }
        }
    };

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
                double leanAngle;
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                int rotation = MyApplication.getContext().getResources().getConfiguration().orientation;
                SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationFixMatrix);
                SensorManager.getOrientation(mRotationFixMatrix, orientation);
                if (rotation == 1) { // Default display rotation is portrait
                    leanAngle = (orientation[2] * 180) / Math.PI;
                } else {   // Default display rotation is landscape
                    leanAngle = ((orientation[2] * 180) / Math.PI) + 90;
                }
                //Filter out impossible values, max sport bike lean is +/-60
                if ((leanAngle >= -60.0) && (leanAngle <= 60.0)) {
                    Data.setLeanAngleBT(leanAngle);
                }
            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                mGravity = event.values.clone();
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic = Utils.lowPass(event.values.clone(), mGeomagnetic, ALPHA);
            } else if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                float[] mBarometricPressure = event.values;
                Data.setBarometricPressure((double) mBarometricPressure[0]);
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
                    float[] orientation = new float[3];
                    SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedR);
                    SensorManager.getOrientation(remappedR, orientation);
                    int direction = filterChange(Utils.normalizeDegrees(Math.toDegrees(orientation[0])));
                    if (direction != lastDirection) {
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
                if ((ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                        || (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
                    gatt.requestMtu(512);
                } else {
                    Log.d(TAG, "No BLUETOOTH_CONNECT permission granted");
                }
                String dataLog = "GATT Connected: [" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
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
                Log.d(TAG,"GATT: Services Discovered");
                broadcastConnectionUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                checkGattServices(getSupportedGattServices());
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
            Log.d(TAG, "onDescriptorRead");
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
                    if(data[0] == WLQ_N.SET_CLUSTER_CLOCK_CMD[0]
                            && data[1] == WLQ_N.SET_CLUSTER_CLOCK_CMD[1]
                            && data[2] == WLQ_N.SET_CLUSTER_CLOCK_CMD[2]
                            && data[3] == WLQ_N.SET_CLUSTER_CLOCK_CMD[3]) {
                        if (Data.wlq == null ) {
                            readCharacteristic(characteristic);
                        }
                    } else {
                        readCharacteristic(characteristic);
                    }
                }
                MyApplication.getContext().sendBroadcast(intent);
                completedCommand();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            // Perform some checks on the status field
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format(Locale.ENGLISH,"ERROR: Read failed for characteristic: %s, status %d", characteristic.getUuid(), status));
                completedCommand();
                return;
            }

            broadcastNotifyUpdate(characteristic);
            completedCommand();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastNotifyUpdate(characteristic);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG,"New MTU: " + mtu);
            }
            discoverServices();
        }
    };


    private static void broadcastConnectionUpdate(final String action) {
        final Intent intent = new Intent(action);
        MyApplication.getContext().sendBroadcast(intent);
    }

    private static void broadcastNotifyUpdate(final BluetoothGattCharacteristic characteristic) {
        Bundle mBundle = new Bundle();
        // Putting the byte value read for GATT Db
        final byte[] data = characteristic.getValue();

        mBundle.putByteArray(EXTRA_BYTE_VALUE,
                data);
        mBundle.putString(EXTRA_BYTE_UUID_VALUE,
                characteristic.getUuid().toString());

        if (data != null) {
            if (sharedPrefs.getBoolean("prefDebugLogging", false)) {
                // Log data
                Log.d(TAG, characteristic.getUuid().toString() + ": " + Utils.ByteArraytoHexNoDelim(data));
            }
        }

        if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_LINMESSAGE_CHARACTERISTIC)) {
            if (data != null) {
                //Check if message changed
                boolean process = false;
                int msgID = (data[0] & 0xFF);
                if(!messages.containsKey(msgID)){
                    messages.put(msgID ,data);
                    process = true;
                } else {
                    if(!Arrays.equals(messages.get(msgID), data)){
                        process = true;
                    }
                }
                //Process message
                if(process) {
                    LINbus.parseLINMessage(data);
                    /*
                     * Sending the broad cast so that it can be received on registered
                     * receivers
                     */
                    sendDataBroadcast();
                }
            }
        } else if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_CANMESSAGE_CHARACTERISTIC)) {
            if (data != null) {
                //Check if message changed
                boolean process = false;
                if (data.length > 2) {
                    int msgID = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
                    if (!messages.containsKey(msgID)) {
                        messages.put(msgID, data);
                        process = true;
                    } else {
                        if (!Arrays.equals(messages.get(msgID), data)) {
                            process = true;
                        }
                    }
                    //Process message
                    if (process) {
                        CANbus.parseCANMessage(data);
                        /*
                         * Sending the broad cast so that it can be received on registered
                         * receivers
                         */
                        sendDataBroadcast();
                    }
                }
            }
        } else if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_PERFORMANCE_CHARACTERISTIC)) {
            //TODO
        } else if (characteristic.getUuid().equals(UUIDDatabase.UUID_WUNDERLINQ_COMMAND_CHARACTERISTIC)) {
            if (data != null) {
                //Read Config
                if ((data[0] == 0x57) && (data[1] == 0x52) && (data[2] == 0x57)) {
                    if (connectedType == WLQ.TYPE_NAVIGATOR) {
                        Data.wlq = new WLQ_N(data);
                        if (Data.hardwareVersion != null) {
                            Data.wlq.setHardwareVersion(Data.hardwareVersion);
                        }
                    } else if (connectedType == WLQ.TYPE_COMMANDER) {
                        Data.wlq = new WLQ_C(data);
                        if (Data.hardwareVersion != null) {
                            Data.wlq.setHardwareVersion(Data.hardwareVersion);
                        }
                    } else if (connectedType == WLQ.TYPE_X) {
                        Data.wlq = new WLQ_X(data);
                        if (Data.hardwareVersion != null) {
                            Data.wlq.setHardwareVersion(Data.hardwareVersion);
                        }
                    }
                    final Intent intent = new Intent(ACTION_CMDSTATUS_AVAILABLE);
                    intent.putExtras(mBundle);
                    MyApplication.getContext().sendBroadcast(intent);
                } else if ((data[0] == 0x57) && (data[1] == 0x52) && (data[2] == 0x41) && (data[3] == 0x50)) {
                    if (sharedPrefs.getBoolean("prefDebugLogging", false)) {
                        Log.d(TAG,"ACC STATUS RECEIVED");
                        // Log data
                        Log.d(TAG,Utils.ByteArraytoHexNoDelim(data));
                    }
                    if(Data.wlq != null) {
                        Data.wlq.setStatus(data);
                        final Intent intent = new Intent(ACTION_ACCSTATUS_AVAILABLE);
                        intent.putExtras(mBundle);
                        MyApplication.getContext().sendBroadcast(intent);
                    } else {
                        // Request config
                        writeCharacteristic(gattCommandCharacteristic, WLQ_BASE.GET_CONFIG_CMD, WriteType.WITH_RESPONSE);
                    }
                }
            }
        } else if (characteristic.getUuid().equals(UUIDDatabase.UUID_HARDWARE_REVISION_STRING)) {
            if (data != null) {
                Data.hardwareVersion = characteristic.getStringValue(0);
                Log.d(TAG, "HW String Value: " + characteristic.getStringValue(0));
            }
        } else {
            /*
             * Sending the broad cast so that it can be received on registered
             * receivers
             */
            final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
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
        if ((ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
        || (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
            mBluetoothGatt = device.connectGatt(MyApplication.getContext(), false, mGattCallback);
            mBluetoothDeviceAddress = address;
            mBluetoothDeviceName = devicename;

            String dataLog = "[" + devicename + "|" + address + "] " +
                    "Connection request sent";
            Log.d(TAG, dataLog);
        } else {
            Log.d(TAG, "No BLUETOOTH_CONNECT permission granted");
        }
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
            if ((ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
                mBluetoothGatt.disconnect();
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        "Disconnection request sent";
                Log.d(TAG, dataLog);
            } else {
                //Request permission
                Log.d(TAG, "No BLUETOOTH_CONNECT permission granted");
            }
            clearNotifications();
            close();
        }
    }

    public static void discoverServices() {
        if (mBluetoothAdapter != null || mBluetoothGatt != null) {
            if ((ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                    || (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
                mBluetoothGatt.discoverServices();
                String dataLog = "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        "Service discovery request sent";
                Log.d(TAG, dataLog);
            } else {
                //Request permission
                Log.d(TAG, "No BLUETOOTH_CONNECT permission granted");
            }
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public static void close() {
        if ((ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
            }
        } else {
            //Request permission
            Log.d(TAG, "No BLUETOOTH_CONNECT permission granted");
        }
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
    public static boolean readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if(mBluetoothGatt == null) {
            Log.e(TAG, "ERROR: Gatt is 'null', ignoring read request");
            return false;
        }

        // Check if characteristic is valid
        if(characteristic == null) {
            Log.e(TAG, "ERROR: Characteristic is 'null', ignoring read request");
            return false;
        }

        // Check if this characteristic actually has READ property
        if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0 ) {
            Log.e(TAG, "ERROR: Characteristic cannot be read");
            return false;
        }

        // Enqueue the read command now that all checks have been passed
        boolean result = commandQueue.add(new Runnable() {
            @Override
            public void run() {
                if ((ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                        || (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
                    if (!mBluetoothGatt.readCharacteristic(characteristic)) {
                        Log.e(TAG, String.format("ERROR: readCharacteristic failed for characteristic: %s", characteristic.getUuid()));
                        completedCommand();
                    } else {
                        Log.d(TAG, String.format("Reading characteristic <%s>", characteristic.getUuid()));
                        nrTries++;
                    }
                } else {
                    //Request permission
                    Log.d(TAG, "No BLUETOOTH_CONNECT permission granted");
                }
            }
        });

        if(result) {
            nextCommand();
        } else {
            Log.e(TAG, "ERROR: Could not enqueue read characteristic command");
        }
        return result;
    }

    public static boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic, final byte[] value, final WriteType writeType) {

        if (!isConnected()) {
            Log.d(TAG, "Hardware Not Connected");
            return false;
        }

        // Copy the value to avoid race conditions
        final byte[] bytesToWrite = copyOf(value);

        // Check if this characteristic actually supports this writeType
        int writeProperty;
        final int writeTypeInternal;
        switch (writeType) {
            case WITH_RESPONSE:
                writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;
                writeTypeInternal = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
                break;
            case WITHOUT_RESPONSE:
                writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
                writeTypeInternal = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
                break;
            case SIGNED:
                writeProperty = BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
                writeTypeInternal = BluetoothGattCharacteristic.WRITE_TYPE_SIGNED;
                break;
            default:
                writeProperty = 0;
                writeTypeInternal = 0;
                break;
        }
        if ((characteristic.getProperties() & writeProperty) == 0) {
            Log.d(TAG, "Characteristic does not support writeType");
            return false;
        }

        boolean result = commandQueue.add(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    characteristic.setWriteType(writeTypeInternal);
                    characteristic.setValue(bytesToWrite);
                    if ((ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                            || (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
                        if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
                            Log.d(TAG, String.format("writeCharacteristic failed for characteristic: %s", characteristic.getUuid()));
                            completedCommand();
                        } else {
                            //Log.d(TAG, String.format("Writing <%s> to characteristic <%s>", Utils.ByteArraytoHex(bytesToWrite), characteristic.getUuid()));
                            nrTries++;
                        }
                    }
                } else {
                    completedCommand();
                }
            }
        });

        if (result) {
            nextCommand();
        } else {
            Log.d(TAG, "Could not enqueue write characteristic command");
        }
        return result;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public static void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if ((ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)) {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                return;
            }
            if (characteristic.getDescriptor(UUID
                    .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                if (enabled) {
                    descriptor
                            .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                } else {
                    descriptor
                            .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                mBluetoothGatt.writeDescriptor(descriptor);
            }
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            if (enabled) {
                String dataLog = "Start notification request sent";
                Log.d(TAG, dataLog);
            } else {
                String dataLog = "Stop notification request sent";
                Log.d(TAG, dataLog);
            }
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

    private static void checkGattServices(List<BluetoothGattService> gattServices) {
        List<BluetoothGattCharacteristic> gattCharacteristics;
        if (gattServices == null) return;
        String uuid;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            if (UUIDDatabase.UUID_DEVICE_INFORMATION_SERVICE.equals(gattService.getUuid())){
                uuid = gattService.getUuid().toString();
                Log.d(TAG,"Device Information Service Found: " + uuid);
                gattCharacteristics = gattService.getCharacteristics();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if (UUID.fromString(GattAttributes.HARDWARE_REVISION_STRING).equals(gattCharacteristic.getUuid())){
                        Log.d(TAG, "HW Revision Characteristic Found: " + uuid);
                        gattHWCharacteristic = gattCharacteristic;
                        readCharacteristic(gattHWCharacteristic);
                    }
                }
            } else if (UUIDDatabase.UUID_WUNDERLINQ_SERVICE.equals(gattService.getUuid())){
                uuid = gattService.getUuid().toString();
                Log.d(TAG,"WunderLINQ Service Found: " + uuid);
                gattCharacteristics = gattService.getCharacteristics();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    Log.d(TAG,"Characteristic Found: " + uuid);
                    if (UUID.fromString(GattAttributes.WUNDERLINQ_LINMESSAGE_CHARACTERISTIC).equals(gattCharacteristic.getUuid())) {
                        connectedType = WLQ.TYPE_NAVIGATOR;
                        int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            readCharacteristic(gattCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gattCharacteristic;
                            setCharacteristicNotification(
                                    gattCharacteristic, true);
                        }
                    } else if (UUID.fromString(GattAttributes.WUNDERLINQ_CANMESSAGE_CHARACTERISTIC).equals(gattCharacteristic.getUuid())) {
                        connectedType = WLQ.TYPE_COMMANDER;
                        int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            readCharacteristic(gattCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gattCharacteristic;
                            setCharacteristicNotification(
                                    gattCharacteristic, true);
                        }
                    } else if (UUID.fromString(GattAttributes.WUNDERLINQ_PERFORMANCE_CHARACTERISTIC).equals(gattCharacteristic.getUuid())) {
                        connectedType = WLQ.TYPE_X;
                        int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            readCharacteristic(gattCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gattCharacteristic;
                            setCharacteristicNotification(
                                    gattCharacteristic, true);
                        }
                    } else if (UUID.fromString(GattAttributes.WUNDERLINQ_COMMAND_CHARACTERISTIC).equals(gattCharacteristic.getUuid())){
                        int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (gattCommandCharacteristic != null) {
                                setCharacteristicNotification(
                                        gattCommandCharacteristic, false);
                                gattCommandCharacteristic = null;
                            }
                            readCharacteristic(gattCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            gattCommandCharacteristic = gattCharacteristic;
                            setCharacteristicNotification(
                                    gattCharacteristic, true);
                        }

                        gattCommandCharacteristic = gattCharacteristic;
                        // Request config
                        writeCharacteristic(gattCommandCharacteristic, WLQ_BASE.GET_CONFIG_CMD, WriteType.WITH_RESPONSE);
                    }
                }
            }
        }
    }

    static public void updateNotification(){
        StringBuilder body = new StringBuilder();
        body.append("");
        if(Faults.getfrontTirePressureCriticalActive()){
            body.append(MyApplication.getContext().getResources().getString(R.string.fault_TIREFCF)).append("\n");
        }
        if(Faults.getrearTirePressureCriticalActive()){
            body.append(MyApplication.getContext().getResources().getString(R.string.fault_TIRERCF)).append("\n");
        }
        if(Faults.getgeneralFlashingRedActive()){
            body.append(MyApplication.getContext().getResources().getString(R.string.fault_GENWARNFSRED)).append("\n");
        }
        if(Faults.getgeneralShowsRedActive()){
            body.append(MyApplication.getContext().getResources().getString(R.string.fault_GENWARNSHRED)).append("\n");
        }
        if(!body.toString().equals("")){
            showNotification(MyApplication.getContext(), MyApplication.getContext().getResources().getString(R.string.fault_title), body.toString());
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
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE
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

    static public void fuelAlert(){
        if (sharedPrefs.getBoolean("prefFuelAlert", false)) {
            if (!fuelAlertSent) {
                fuelAlertSent = true;
                Intent alertIntent = new Intent(MyApplication.getContext(), AlertActivity.class);
                alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                alertIntent.putExtra("TYPE", AlertActivity.ALERT_FUEL);
                alertIntent.putExtra("TITLE", MyApplication.getContext().getResources().getString(R.string.alert_title_fuel));
                alertIntent.putExtra("BODY", MyApplication.getContext().getResources().getString(R.string.alert_label_fuel));
                alertIntent.putExtra("BACKGROUND", "");
                MyApplication.getContext().startActivity(alertIntent);
            }

        } else {
            fuelAlertSent = false;
        }
    }

    static public void ignitionAlert(){
        if (sharedPrefs.getBoolean("prefIgnitionAlert", false)) {
            Intent alertIntent = new Intent(MyApplication.getContext(), AlertActivity.class);
            alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            alertIntent.putExtra("TYPE", AlertActivity.ALERT_IGNITION);
            alertIntent.putExtra("TITLE", MyApplication.getContext().getResources().getString(R.string.alert_title_ignition));
            alertIntent.putExtra("BODY", MyApplication.getContext().getResources().getString(R.string.alert_label_ignition));
            alertIntent.putExtra("BACKGROUND", "");
            MyApplication.getContext().startActivity(alertIntent);
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
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(500);
        mLocationRequest.setFastestInterval(500);
    }

    private void startLocationUpdatesWithFusedLocationProvider() {
        Log.d(TAG, "Starting location updates with FusedLocationProvider");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    private void startLocationUpdatesWithLocationManager() {
        Log.d(TAG, "Starting location updates using LocationManager");
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateLocationData(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

            @Override
            public void onProviderEnabled(@NonNull String provider) { }

            @Override
            public void onProviderDisabled(@NonNull String provider) { }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    public static boolean isConnected() {
        return mBluetoothGatt != null && mConnectionState == BluetoothProfile.STATE_CONNECTED;
    }

    private static byte[] copyOf(byte[] source) {
        return (source == null) ? new byte[0] : Arrays.copyOf(source, source.length);
    }

    private static void nextCommand() {
        // If there is still a command being executed then bail out
        if(commandQueueBusy) {
            return;
        }

        // Check if we still have a valid gatt object
        if (mBluetoothGatt == null) {
            Log.d(TAG, "ERROR: GATT is 'null' for peripheral, clearing command queue");
            commandQueue.clear();
            commandQueueBusy = false;
            return;
        }

        // Execute the next command in the queue
        if (commandQueue.size() > 0) {
            final Runnable bluetoothCommand = commandQueue.peek();
            commandQueueBusy = true;
            nrTries = 0;

            bleHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        bluetoothCommand.run();
                    } catch (Exception ex) {
                        Log.d(TAG, "ERROR: Command exception for device");
                    }
                }
            });
        }
    }

    private static void completedCommand() {
        commandQueueBusy = false;
        isRetrying = false;
        commandQueue.poll();
        nextCommand();
    }

    private static void retryCommand() {
        commandQueueBusy = false;
        Runnable currentCommand = commandQueue.peek();
        if(currentCommand != null) {
            if (nrTries >= MAX_TRIES) {
                // Max retries reached, give up on this one and proceed
                Log.v(TAG, "Max number of tries reached");
                commandQueue.poll();
            } else {
                isRetrying = true;
            }
        }
        nextCommand();
    }

    private static void sendDataBroadcast() {
        final Intent intent = new Intent(ACTION_PERFORMANCE_DATA_AVAILABLE);
        intent.putExtra(Data.getExtraKey(Data.DATA_GEAR), Data.getGear());
        intent.putExtra(Data.getExtraKey(Data.DATA_ENGINE_TEMP), Data.getEngineTemperature());
        intent.putExtra(Data.getExtraKey(Data.DATA_AIR_TEMP), Data.getAmbientTemperature());
        intent.putExtra(Data.getExtraKey(Data.DATA_FRONT_RDC), Data.getFrontTirePressure());
        intent.putExtra(Data.getExtraKey(Data.DATA_REAR_RDC), Data.getRearTirePressure());
        intent.putExtra(Data.getExtraKey(Data.DATA_ODOMETER), Data.getOdometer());
        intent.putExtra(Data.getExtraKey(Data.DATA_VOLTAGE), Data.getvoltage());
        intent.putExtra(Data.getExtraKey(Data.DATA_THROTTLE), Data.getThrottlePosition());
        intent.putExtra(Data.getExtraKey(Data.DATA_FRONT_BRAKE), Data.getFrontBrake());
        intent.putExtra(Data.getExtraKey(Data.DATA_REAR_BRAKE), Data.getRearBrake());
        intent.putExtra(Data.getExtraKey(Data.DATA_AMBIENT_LIGHT), Data.getAmbientLight());
        intent.putExtra(Data.getExtraKey(Data.DATA_TRIP_ONE), Data.getTripOne());
        intent.putExtra(Data.getExtraKey(Data.DATA_TRIP_TWO), Data.getTripTwo());
        intent.putExtra(Data.getExtraKey(Data.DATA_TRIP_AUTO), Data.getTripAuto());
        intent.putExtra(Data.getExtraKey(Data.DATA_SPEED), Data.getSpeed());
        intent.putExtra(Data.getExtraKey(Data.DATA_AVG_SPEED), Data.getAvgSpeed());
        intent.putExtra(Data.getExtraKey(Data.DATA_CURRENT_CONSUMPTION), Data.getCurrentConsumption());
        intent.putExtra(Data.getExtraKey(Data.DATA_ECONOMY_ONE), Data.getFuelEconomyOne());
        intent.putExtra(Data.getExtraKey(Data.DATA_ECONOMY_TWO), Data.getFuelEconomyTwo());
        intent.putExtra(Data.getExtraKey(Data.DATA_RANGE), Data.getFuelRange());
        intent.putExtra(Data.getExtraKey(Data.DATA_SHIFTS), Data.getNumberOfShifts());
        intent.putExtra(Data.getExtraKey(Data.DATA_RPM), Data.getRPM());
        intent.putExtra(Data.getExtraKey(Data.DATA_LEAN), Data.getLeanAngleBike());
        MyApplication.getContext().sendBroadcast(intent);
    }
}