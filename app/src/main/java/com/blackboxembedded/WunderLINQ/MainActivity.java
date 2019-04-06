package com.blackboxembedded.WunderLINQ;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.PictureInPictureParams;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.text.TextUtils;
import android.util.Log;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.os.Process.myUid;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "MainActivity";

    private LayoutInflater layoutInflater;

    private ActionBar actionBar;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ImageButton menuButton;
    private ImageButton faultButton;
    private ImageButton btButton;
    private TextView navbarTitle;

    private GridLayout gridLayout;
    private LinearLayout layout1;

    private SharedPreferences sharedPrefs;

    private boolean gridChange = false;

    static boolean itsDark = false;
    private long darkTimer = 0;
    private long lightTimer = 0;

    private SensorManager sensorManager;
    private Sensor lightSensor;

    private Intent gattServiceIntent;
    public static BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    public static BluetoothGattCharacteristic gattCommandCharacteristic;
    List<BluetoothGattCharacteristic> gattCharacteristics;
    private String mDeviceAddress;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SETTINGS_CHECK = 10;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    public final static UUID UUID_MOTORCYCLE_SERVICE =
            UUID.fromString(GattAttributes.WUNDERLINQ_SERVICE);

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int PERMISSION_REQUEST_CALL_PHONE = 101;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 102;
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 112;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 122;
    private PopupMenu mPopupMenu;
    private Menu mMenu;

    private GestureDetectorListener gestureDetector;

    private boolean inPIP = false;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"In onCreate");

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        int orientation = Integer.parseInt(sharedPrefs.getString("prefOrientation", "0"));
        switch (orientation){
            case 0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                break;
            case 1:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 2:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case 3:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                break;
        }

        layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setContentView(R.layout.activity_main);
        View view = findViewById(R.id.layout_main);
        gridLayout = findViewById(R.id.gridLayout);
        if (!sharedPrefs.getBoolean("prefMotorcycleData", false)){
            int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT","15"));
            switch(currentCellCount){
                case 15:
                    gridLayout.removeAllViews();
                    gridLayout.setColumnCount(3);
                    gridLayout.setRowCount(5);
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem1, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem2, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem3, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem4, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem5, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem6, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem7, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem8, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem9, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem10, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem11, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem12, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem13, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem14, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem15, gridLayout, false));
                    break;
                case 12:
                    gridLayout.removeAllViews();
                    gridLayout.setColumnCount(3);
                    gridLayout.setRowCount(4);
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem1, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem2, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem3, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem4, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem5, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem6, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem7, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem8, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem9, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem10, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem11, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem12, gridLayout, false));
                    break;
                case 8:
                    gridLayout.removeAllViews();
                    if (getResources().getConfiguration().orientation == 2) {
                        //Landscape
                        gridLayout.setColumnCount(4);
                        gridLayout.setRowCount(2);
                    } else {
                        gridLayout.setColumnCount(2);
                        gridLayout.setRowCount(4);
                    }
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem1, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem2, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem3, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem4, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem5, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem6, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem7, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem8, gridLayout, false));
                    break;
                case 4:
                    gridLayout.removeAllViews();
                    if(getResources().getConfiguration().orientation == 2) {
                        //Landscape
                        gridLayout.setColumnCount(2);
                        gridLayout.setRowCount(2);
                    } else {
                        gridLayout.setColumnCount(1);
                        gridLayout.setRowCount(4);
                    }
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem1, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem2, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem3, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem4, gridLayout, false));
                    break;
                case 2:
                    gridLayout.removeAllViews();
                    if(getResources().getConfiguration().orientation == 2) {
                        //Landscape
                        gridLayout.setColumnCount(2);
                        gridLayout.setRowCount(1);
                    } else {
                        gridLayout.setColumnCount(1);
                        gridLayout.setRowCount(2);
                    }
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem1, gridLayout, false));
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem2, gridLayout, false));
                    break;
                case 1:
                    gridLayout.removeAllViews();
                    gridLayout.setColumnCount(1);
                    gridLayout.setRowCount(1);
                    gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem1, gridLayout, false));
                    break;
            }

        } else {
            gridLayout.removeAllViews();
            gridLayout.setColumnCount(1);
            gridLayout.setRowCount(1);
            gridLayout.addView(layoutInflater.inflate(R.layout.layout_griditem_nodata, gridLayout, false));
            layout1 = findViewById(R.id.layout_no_data);
            ImageView logoImageView = findViewById(R.id.imageView);
            if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
                layout1.setBackgroundColor(getResources().getColor(R.color.black));
                logoImageView.setImageDrawable(getResources().getDrawable(R.drawable.wunderlinq_logo_white));
            } else {
                layout1.setBackgroundColor(getResources().getColor(R.color.white));
                logoImageView.setImageDrawable(getResources().getDrawable(R.drawable.wunderlinq_logo_black));
            }
        }

        gestureDetector = new GestureDetectorListener(this){

            @Override
            public void onPressLong() {
                if ( cell >= 1 && cell <= 15){
                    showCellSelector(cell);
                }
            }

            @Override
            public void onSwipeUp() {
                int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT","15"));
                //int maxCellCount = Integer.parseInt(sharedPrefs.getString("prefMaxCells","15"));
                int nextCellCount = 1;
                SharedPreferences.Editor editor = sharedPrefs.edit();
                gridChange = true;
                switch (currentCellCount){
                    case 15:
                        nextCellCount = 1;
                        break;
                    case 12:
                        nextCellCount = 15;
                        break;
                    case 8:
                        nextCellCount = 12;
                        break;
                    case 4:
                        nextCellCount = 8;
                        break;
                    case 2:
                        nextCellCount = 4;
                        break;
                    case 1:
                        nextCellCount = 2;
                        break;
                }
                editor.putString("CELL_COUNT", String.valueOf(nextCellCount));
                editor.apply();
                updateDisplay();
            }

            @Override
            public void onSwipeDown() {
                int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT","15"));
                //int maxCellCount = Integer.parseInt(sharedPrefs.getString("prefMaxCells","15"));
                int nextCellCount = 1;
                SharedPreferences.Editor editor = sharedPrefs.edit();
                gridChange = true;
                switch (currentCellCount){
                    case 15:
                        nextCellCount = 12;
                        break;
                    case 12:
                        nextCellCount = 8;
                        break;
                    case 8:
                        nextCellCount = 4;
                        break;
                    case 4:
                        nextCellCount = 2;
                        break;
                    case 2:
                        nextCellCount = 1;
                        break;
                    case 1:
                        nextCellCount = 15;
                        break;
                }
                editor.putString("CELL_COUNT", String.valueOf(nextCellCount));
                editor.apply();
                updateDisplay();
            }

            @Override
            public void onSwipeLeft() {
                Intent backIntent = new Intent(MainActivity.this, MusicActivity.class);
                startActivity(backIntent);
            }

            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(MainActivity.this, TaskActivity.class);
                startActivity(backIntent);
            }
        };

        view.setOnTouchListener(this);

        showActionBar();

        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
            updateColors(true);
        } else {
            updateColors(false);
        }
        mHandler = new Handler();

        // Sensor Stuff
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Brand: " + Build.BRAND);
            Log.d(TAG, "Device: " + Build.DEVICE);
            //Only quit on real device
            if(!(Build.BRAND.startsWith("Android") && Build.DEVICE.startsWith("generic"))) {
                finish();
            } else {
                Log.d(TAG,"Running in the emulator");
            }
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_LONG).show();

            Log.d(TAG, "Brand: " + Build.BRAND);
            Log.d(TAG, "Device: " + Build.DEVICE);
            //Only quit if on a real device
            if(!(Build.BRAND.startsWith("Android") && Build.DEVICE.startsWith("generic"))){
                finish();
            } else {
                Log.d(TAG,"Running in the emulator");
            }

            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check Read Contacts permissions
            if (this.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.contacts_alert_title));
                builder.setMessage(getString(R.string.contacts_alert_body));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
                    }
                });
                builder.show();
            }
            // Check Camera permissions
            if (this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.camera_alert_title));
                builder.setMessage(getString(R.string.camera_alert_body));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                    }
                });
                builder.show();
            }
            // Check Call phone permissions
            if (this.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.call_alert_title));
                builder.setMessage(getString(R.string.call_alert_body));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CALL_PHONE);
                    }
                });
                builder.show();
            }
            // Check Read Audio permissions
            if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.record_audio_alert_title));
                builder.setMessage(getString(R.string.record_audio_alert_body));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                    }
                });
                builder.show();
            }
            // Check Write permissions
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.write_alert_title));
                builder.setMessage(getString(R.string.write_alert_body));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_STORAGE);
                    }
                });
                builder.show();
            }
            // Check Location permissions
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.location_alert_title));
                builder.setMessage(getString(R.string.location_alert_body));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
            // Check overlay permissions
            if (!Settings.canDrawOverlays(this)) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.overlay_alert_title));
                builder.setMessage(getString(R.string.overlay_alert_body));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(23)
                    public void onDismiss(DialogInterface dialog) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                });
                builder.show();
            }
        }
        // Check read notification permissions
        if (Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners") == null
                || !Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.notification_alert_title));
            builder.setMessage(getString(R.string.notification_alert_body));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @TargetApi(23)
                public void onDismiss(DialogInterface dialog) {
                    startActivity(new Intent(
                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                }
            });
            builder.show();
        }
        //Check usage stats permissions
        AppOpsManager appOps = (AppOpsManager) this.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, myUid(), this.getPackageName());
        if (mode != MODE_ALLOWED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.usagestats_alert_title));
            builder.setMessage(getString(R.string.usagestats_alert_body));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @TargetApi(23)
                public void onDismiss(DialogInterface dialog) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                }
            });
            builder.show();
        }

        if (!isAccessibilityServiceEnabled(MainActivity.this, MyAccessibilityService.class)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.accessibilityservice_alert_title));
            builder.setMessage(getString(R.string.accessibilityservice_alert_body));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @TargetApi(23)
                public void onDismiss(DialogInterface dialog) {
                    Intent accessibilityIntent = new Intent();
                    accessibilityIntent.setAction(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(accessibilityIntent);
                }
            });
            builder.show();
        }

        // Daily Disclaimer Warning
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        final String currentDate = sdf.format(new Date());
        if (sharedPrefs.getString("LAST_LAUNCH_DATE","nodate").contains(currentDate)){
            // Date matches. User has already Launched the app once today. So do nothing.
        }
        else
        {
            // Display dialog text here......
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.disclaimer_alert_title));
            builder.setMessage(getString(R.string.disclaimer_alert_body));
            builder.setPositiveButton(R.string.disclaimer_ok,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Set the last Launched date to today.
                            SharedPreferences.Editor editor = sharedPrefs.edit();
                            editor.putString("LAST_LAUNCH_DATE", currentDate);
                            editor.apply();
                            dialog.cancel();
                        }
                    });
            builder.setNegativeButton(R.string.disclaimer_quit,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // End App
                            finishAndRemoveTask();
                        }
                    });
            builder.show();

        }

        registerReceiver(mBondingBroadcast,new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
        startService(new Intent(MainActivity.this, BluetoothLeService.class));

        if (!sharedPrefs.getBoolean("prefMotorcycleData", false)){
            gridChange = true;
            updateDisplay();
        }
    }

    private void showCellSelector(int cell){
        String prefStringKey = "";
        switch (cell){
            case 1:
                prefStringKey = "prefCellOne";
                break;
            case 2:
                prefStringKey = "prefCellTwo";
                break;
            case 3:
                prefStringKey = "prefCellThree";
                break;
            case 4:
                prefStringKey = "prefCellFour";
                break;
            case 5:
                prefStringKey = "prefCellFive";
                break;
            case 6:
                prefStringKey = "prefCellSix";
                break;
            case 7:
                prefStringKey = "prefCellSeven";
                break;
            case 8:
                prefStringKey = "prefCellEight";
                break;
            case 9:
                prefStringKey = "prefCellNine";
                break;
            case 10:
                prefStringKey = "prefCellTen";
                break;
            case 11:
                prefStringKey = "prefCellEleven";
                break;
            case 12:
                prefStringKey = "prefCellTwelve";
                break;
            case 13:
                prefStringKey = "prefCellThirteen";
                break;
            case 14:
                prefStringKey = "prefCellFourteen";
                break;
            case 15:
                prefStringKey = "prefCellFifteen";
                break;
        }
        final String selectedPrefStringKey = prefStringKey;
        if (!prefStringKey.equals("")) {
            final ArrayAdapter<String> adp = new ArrayAdapter<String>(MainActivity.this, R.layout.spinner_griditem,
                    R.id.textview, getResources().getStringArray(R.array.dataPoints_array));

            final Spinner sp1 = new Spinner(MainActivity.this);
            sp1.setLayoutParams(new LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
            sp1.setAdapter(adp);
            sp1.setSelection(Integer.parseInt(sharedPrefs.getString(prefStringKey, String.valueOf(cell))));
            sp1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent,
                                           View view, int pos, long id) {
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(selectedPrefStringKey, String.valueOf(pos));
                    editor.apply();
                    updateDisplay();
                }

                @Override
                public void onNothingSelected(AdapterView parent) {
                    // Do nothing.
                }

            });

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setView(sp1);
            builder.create().show();
        }
    }

    private void showActionBar(){
        View v = layoutInflater.inflate(R.layout.actionbar_nav_main, null);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setCustomView(v);

        backButton = findViewById(R.id.action_back);
        forwardButton = findViewById(R.id.action_forward);
        menuButton = findViewById(R.id.action_menu);
        faultButton = findViewById(R.id.action_faults);
        btButton = findViewById(R.id.action_connect);

        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.main_title);

        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
        faultButton.setOnClickListener(mClickListener);
        menuButton.setOnClickListener(mClickListener);
        btButton.setOnClickListener(mClickListener);

        faultButton.setVisibility(View.GONE);

        mPopupMenu = new PopupMenu(this, menuButton);
        MenuInflater menuOtherInflater = mPopupMenu.getMenuInflater();
        menuOtherInflater.inflate(R.menu.menu, mPopupMenu.getMenu());
        mMenu = mPopupMenu.getMenu();
        mMenu.findItem(R.id.action_hwsettings).setVisible(false);
        mPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.action_connect:
                        if(!(Build.BRAND.startsWith("Android") && Build.DEVICE.startsWith("generic"))) {
                            setupBLE();
                        } else {
                            Log.d(TAG,"Running in the emulator");
                        }
                        break;
                    case R.id.action_data:
                        Intent geoDataIntent = new Intent(MainActivity.this, GeoDataActivity.class);
                        startActivity(geoDataIntent);
                        break;
                    case R.id.action_settings:
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(settingsIntent, SETTINGS_CHECK);
                        break;
                    case R.id.action_hwsettings:
                        Intent hwSettingsIntent = new Intent(MainActivity.this, FWConfigActivity.class);
                        startActivity(hwSettingsIntent);
                        break;
                    case R.id.action_enter_splitscreen:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if (!isInMultiWindowMode()) {
                                if (isAccessibilityServiceEnabled(MainActivity.this, MyAccessibilityService.class)) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        Intent accessibilityService = new Intent(MainActivity.this, MyAccessibilityService.class);
                                        accessibilityService.putExtra("command", 1);
                                        startService(accessibilityService);
                                    }
                                } else {
                                    Intent accessibilityIntent = new Intent();
                                    accessibilityIntent.setAction(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                    startActivity(accessibilityIntent);
                                }
                            }
                        }
                        break;
                    case R.id.action_about:
                        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                        startActivity(aboutIntent);
                        break;
                    case R.id.action_exit:
                        finish();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouch(v, event);
        return true;
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(MainActivity.this, TaskActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_forward:
                    Intent forwardIntent = new Intent(MainActivity.this, MusicActivity.class);
                    startActivity(forwardIntent);
                    break;
                case R.id.action_faults:
                    Intent faultIntent = new Intent(MainActivity.this, FaultActivity.class);
                    startActivity(faultIntent);
                    break;
                case R.id.action_menu:
                    mPopupMenu.show();
                    break;
            }
        }
    };

    // Listens for light sensor events
    private final SensorEventListener sensorEventListener
            = new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (sharedPrefs.getString("prefNightModeCombo", "0").equals("2")) {
                int delay = (Integer.parseInt(sharedPrefs.getString("prefAutoNightModeDelay", "30")) * 1000);
                if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
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
                                // Update colors
                                updateColors(true);
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
                                // Update colors
                                updateColors(false);
                            }
                        }
                    }
                }
            }
        }
    };

    public void updateColors(boolean itsDark){
        ((MyApplication) this.getApplication()).setitsDark(itsDark);
        LinearLayout lLayout = findViewById(R.id.layout_main);
        if (lLayout != null){
            if (itsDark) {
                Log.d(TAG,"Settings things for dark");
                //Set Brightness to defaults
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = -1;
                getWindow().setAttributes(layoutParams);

                lLayout.setBackgroundColor(getResources().getColor(R.color.black));
                actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
                navbarTitle.setTextColor(getResources().getColor(R.color.white));
                backButton.setColorFilter(getResources().getColor(R.color.white));
                forwardButton.setColorFilter(getResources().getColor(R.color.white));
                menuButton.setColorFilter(getResources().getColor(R.color.white));
                updateDisplay();
            } else {
                Log.d(TAG, "Settings things for light");
                if (sharedPrefs.getBoolean("prefBrightnessOverride", false)) {
                    //Set Brightness to 100%
                    WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                    layoutParams.screenBrightness = 1;
                    getWindow().setAttributes(layoutParams);
                } else {
                    //Set Brightness to defaults
                    WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                    layoutParams.screenBrightness = -1;
                    getWindow().setAttributes(layoutParams);
                }

                lLayout.setBackgroundColor(getResources().getColor(R.color.white));
                actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
                navbarTitle.setTextColor(getResources().getColor(R.color.black));
                backButton.setColorFilter(getResources().getColor(R.color.black));
                forwardButton.setColorFilter(getResources().getColor(R.color.black));
                menuButton.setColorFilter(getResources().getColor(R.color.black));
                updateDisplay();
            }
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"In onResume");
        super.onResume();
        registerReceiver(mBondingBroadcast,new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService == null) {
            Log.d(TAG,"mBluetoothLeService is null");
            //Only use BLE if on a real device
            if(!(Build.BRAND.startsWith("Android") && Build.DEVICE.startsWith("generic"))) {
                setupBLE();
            } else {
                Log.d(TAG,"Running in the emulator");
            }
        } else {
            Log.d(TAG,"mBluetoothLeService is NOT null");
            //mBluetoothLeService.connect(mDeviceAddress,getString(R.string.device_name));
        }
        sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        if (((MyApplication) this.getApplication()).getitsDark()){
            updateColors(true);
        } else {
            updateColors(false);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"In onDestroy");
        super.onDestroy();
        try {
            unregisterReceiver(mGattUpdateReceiver);
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
        }
        mBluetoothLeService = null;
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onStop() {
        Log.d(TAG,"In onStop");
        super.onStop();
        try {
            unregisterReceiver(mGattUpdateReceiver);
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
        }
        mBluetoothLeService = null;
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"In onPause");
        super.onPause();
        try {
            if (!sharedPrefs.getBoolean("prefPIP", false)) {
                unregisterReceiver(mGattUpdateReceiver);
            }
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
        }
        mBluetoothLeService = null;
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onUserLeaveHint () {
        //Set Brightness to defaults
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = -1;
        getWindow().setAttributes(layoutParams);
        if (Build.VERSION.SDK_INT >= 26) {
            if (sharedPrefs.getBoolean("prefPIP", false) && (!isInMultiWindowMode())) {
                int width = getWindow().getDecorView().getWidth();
                int height = getWindow().getDecorView().getHeight();
                int pipWidth = width;
                int pipHeight = height;

                if (sharedPrefs.getString("prefPIPorientation", "0").equals("0")) {
                    if (height > width) {
                        pipWidth = height;
                        pipHeight = width;
                    }
                } else {
                    if (height < width) {
                        pipWidth = height;
                        pipHeight = width;
                    }
                }
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(pipWidth, pipHeight)).build();
                enterPictureInPictureMode(params);
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPIPMode) {
        gridChange = true;
        if (isInPIPMode) {
            inPIP = true;
            //Hide your clickable components
            getSupportActionBar().hide();
            updateDisplay();
        } else {
            inPIP = false;
            //Show your clickable components
            getSupportActionBar().show();
            updateDisplay();
        }
        super.onPictureInPictureModeChanged(isInPIPMode);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG,"In onConfigChange");
        gridChange = true;
        updateDisplay();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else if (requestCode == SETTINGS_CHECK) {
            Log.d(TAG,"onActivityResult");
            gridChange = true;
            if (sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
                updateColors(true);
            } else {
                updateColors(false);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (mBluetoothLeService.mConnectionState == BluetoothLeService.STATE_DISCONNECTED) {
                Log.e(TAG, "In onServiceCOnnected Disconnected,reconnected");
                mBluetoothLeService.connect(mDeviceAddress, getString(R.string.device_name));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void setupBLE() {
        Log.d(TAG,"In setupBLE()");
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice devices : pairedDevices) {
                if (devices.getName().equals(getString(R.string.device_name))){
                    Log.d(TAG,"WunderLINQ previously paired");
                    mDeviceAddress = devices.getAddress();
                    Log.d(TAG,"Address: " + mDeviceAddress);
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                    scanLeDevice(false);
                    return;
                }
            }
        }
        Log.d(TAG, "Previously Paired WunderLINQ not found");
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            Log.d(TAG,"In scanLeDevice() Scanning On");
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (bluetoothLeScanner != null) {
                        bluetoothLeScanner.stopScan(mLeScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            //scan specified devices only with ScanFilter
            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setDeviceName(getString(R.string.device_name))
                            .build();
            List<ScanFilter> scanFilters = new ArrayList<>();
            scanFilters.add(scanFilter);

            ScanSettings scanSettings =
                    new ScanSettings.Builder().build();

            try {
                bluetoothLeScanner.startScan(scanFilters, scanSettings, mLeScanCallback);
            } catch (NullPointerException e){
                //Testing
                Log.d(TAG,"NullPointerException: " + e.toString());
            }

        } else {
            Log.d(TAG,"In scanLeDevice() Scanning Off");
            bluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String device = result.getDevice().getName();
            if (device != null) {
                if (device.contains(getString(R.string.device_name))) {
                    Log.d(TAG, "WunderLINQ Device Found: " + device);
                    result.getDevice().createBond();
                    scanLeDevice(false);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    BroadcastReceiver mBondingBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

            switch (state) {
                case BluetoothDevice.BOND_BONDING:
                    Log.d("Bondind Status:", " Bonding...");
                    break;

                case BluetoothDevice.BOND_BONDED:
                    Log.d("Bondind Status:", "Bonded!!");
                    setupBLE();
                    break;

                case BluetoothDevice.BOND_NONE:
                    Log.d("Bondind Status:", "Fail");

                    break;
            }
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG,"GATT_CONNECTED");
                mBluetoothLeService.discoverServices();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG,"GATT_DISCONNECTED");
                Data.clear();
                if (!sharedPrefs.getBoolean("prefMotorcycleData", false)){
                    updateDisplay();
                }
                btButton.setColorFilter(getResources().getColor(R.color.motorrad_red));
                btButton.setEnabled(true);
                mMenu.findItem(R.id.action_hwsettings).setVisible(false);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG,"GATT_SERVICE_DISCOVERED");
                checkGattServices(mBluetoothLeService.getSupportedGattServices());
                btButton.setColorFilter(getResources().getColor(R.color.motorrad_blue));
                btButton.setEnabled(false);
                mMenu.findItem(R.id.action_hwsettings).setVisible(true);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                btButton.setColorFilter(getResources().getColor(R.color.motorrad_blue));
                btButton.setEnabled(false);
                mMenu.findItem(R.id.action_hwsettings).setVisible(true);
                if (!sharedPrefs.getBoolean("prefMotorcycleData", false)){
                    updateDisplay();
                }
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void checkGattServices(List<BluetoothGattService> gattServices) {
        Log.d(TAG,"In checkGattServices");
        if (gattServices == null) return;
        String uuid;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG,"In checkGattServices: for loop");
            if (UUID_MOTORCYCLE_SERVICE.equals(gattService.getUuid())){
                uuid = gattService.getUuid().toString();
                Log.d(TAG,"Motorcycle Service Found: " + uuid);
                gattCharacteristics = gattService.getCharacteristics();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    Log.d(TAG,"Characteristic Found: " + uuid);
                    if (UUID.fromString(GattAttributes.WUNDERLINQ_MESSAGE_CHARACTERISTIC).equals(gattCharacteristic.getUuid())) {
                        int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(gattCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gattCharacteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    gattCharacteristic, true);
                        }
                    } else if (UUID.fromString(GattAttributes.WUNDERLINQ_COMMAND_CHARACTERISTIC).equals(gattCharacteristic.getUuid())){
                        gattCommandCharacteristic = gattCharacteristic;
                    }
                }
            }
        }
    }

    // Update Display
    private void updateDisplay(){
        Log.d(TAG,"updateDisplay()");
        gridLayout = findViewById(R.id.gridLayout);

        if (!sharedPrefs.getBoolean("prefMotorcycleData", false)) {
            //Check for active faults
            FaultStatus faults;
            faults = (new FaultStatus(this));
            ArrayList<String> faultListData = faults.getallActiveDesc();
            if (!faultListData.isEmpty()) {
                faultButton.setVisibility(View.VISIBLE);
            } else {
                faultButton.setVisibility(View.GONE);
            }

            // Cell One
            Integer cell1Data = Integer.parseInt(sharedPrefs.getString("prefCellOne", "1"));
            // Cell Two
            Integer cell2Data = Integer.parseInt(sharedPrefs.getString("prefCellTwo", "2"));
            // Cell Three
            Integer cell3Data = Integer.parseInt(sharedPrefs.getString("prefCellThree", "3"));
            // Cell Four
            Integer cell4Data = Integer.parseInt(sharedPrefs.getString("prefCellFour", "4"));
            // Cell Five
            Integer cell5Data = Integer.parseInt(sharedPrefs.getString("prefCellFive", "5"));
            // Cell Six
            Integer cell6Data = Integer.parseInt(sharedPrefs.getString("prefCellSix", "6"));
            // Cell Seven
            Integer cell7Data = Integer.parseInt(sharedPrefs.getString("prefCellSeven", "7"));
            // Cell Eight
            Integer cell8Data = Integer.parseInt(sharedPrefs.getString("prefCellEight", "8"));
            // Cell Nine
            Integer cell9Data = Integer.parseInt(sharedPrefs.getString("prefCellNine", "9"));
            // Cell Ten
            Integer cell10Data = Integer.parseInt(sharedPrefs.getString("prefCellTen", "10"));
            // Cell Eleven
            Integer cell11Data = Integer.parseInt(sharedPrefs.getString("prefCellEleven", "11"));
            // Cell Twelve
            Integer cell12Data = Integer.parseInt(sharedPrefs.getString("prefCellTwelve", "12"));
            // Cell Thirteen
            Integer cell13Data = Integer.parseInt(sharedPrefs.getString("prefCellThirteen", "13"));
            // Cell Fourteen
            Integer cell14Data = Integer.parseInt(sharedPrefs.getString("prefCellFourteen", "14"));
            // Cell Fifteen
            Integer cell15Data = Integer.parseInt(sharedPrefs.getString("prefCellFifteen", "15"));

            int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT", "15"));
            int count = currentCellCount;
            if (inPIP) {
                count = Integer.parseInt(sharedPrefs.getString("prefPIPCellCount", "4"));
            }
            switch (count) {
                case 15:
                    if (gridChange) {
                        gridLayout.removeAllViews();
                        gridLayout.setColumnCount(3);
                        gridLayout.setRowCount(5);
                    }

                    // Cell One
                    setCellText(1, cell1Data);
                    // Cell Two
                    setCellText(2, cell2Data);
                    // Cell Three
                    setCellText(3, cell3Data);
                    // Cell Four
                    setCellText(4, cell4Data);
                    // Cell Five
                    setCellText(5, cell5Data);
                    // Cell Six
                    setCellText(6, cell6Data);
                    // Cell Seven
                    setCellText(7, cell7Data);
                    // Cell Eight
                    setCellText(8, cell8Data);
                    // Cell Nine
                    setCellText(9, cell9Data);
                    // Cell Ten
                    setCellText(10, cell10Data);
                    // Cell Eleven
                    setCellText(11, cell11Data);
                    // Cell Twelve
                    setCellText(12, cell12Data);
                    // Cell Thirteen
                    setCellText(13, cell13Data);
                    // Cell Fourteen
                    setCellText(14, cell14Data);
                    // Cell Fifteen
                    setCellText(15, cell15Data);
                    gridChange = false;
                    break;
                case 12:
                    if (gridChange) {
                        gridLayout.removeAllViews();
                        gridLayout.setColumnCount(3);
                        gridLayout.setRowCount(4);
                    }
                    // Cell One
                    setCellText(1, cell1Data);
                    // Cell Two
                    setCellText(2, cell2Data);
                    // Cell Three
                    setCellText(3, cell3Data);
                    // Cell Four
                    setCellText(4, cell4Data);
                    // Cell Five
                    setCellText(5, cell5Data);
                    // Cell Six
                    setCellText(6, cell6Data);
                    // Cell Seven
                    setCellText(7, cell7Data);
                    // Cell Eight
                    setCellText(8, cell8Data);
                    // Cell Nine
                    setCellText(9, cell9Data);
                    // Cell Ten
                    setCellText(10, cell10Data);
                    // Cell Eleven
                    setCellText(11, cell11Data);
                    // Cell Twelve
                    setCellText(12, cell12Data);
                    gridChange = false;
                    break;
                case 8:
                    if (gridChange) {
                        gridLayout.removeAllViews();
                        if (getResources().getConfiguration().orientation == 2) {
                            //Landscape
                            gridLayout.setColumnCount(4);
                            gridLayout.setRowCount(2);
                        } else {
                            gridLayout.setColumnCount(2);
                            gridLayout.setRowCount(4);
                        }
                    }
                    // Cell One
                    setCellText(1, cell1Data);
                    // Cell Two
                    setCellText(2, cell2Data);
                    // Cell Three
                    setCellText(3, cell3Data);
                    // Cell Four
                    setCellText(4, cell4Data);
                    // Cell Five
                    setCellText(5, cell5Data);
                    // Cell Six
                    setCellText(6, cell6Data);
                    // Cell Seven
                    setCellText(7, cell7Data);
                    // Cell Eight
                    setCellText(8, cell8Data);
                    gridChange = false;
                    break;
                case 4:
                    if (gridChange) {
                        gridLayout.removeAllViews();
                        if (getResources().getConfiguration().orientation == 2) {
                            //Landscape
                            gridLayout.setColumnCount(2);
                            gridLayout.setRowCount(2);
                        } else {
                            gridLayout.setColumnCount(1);
                            gridLayout.setRowCount(4);
                        }
                    }
                    // Cell One
                    setCellText(1, cell1Data);
                    // Cell Two
                    setCellText(2, cell2Data);
                    // Cell Three
                    setCellText(3, cell3Data);
                    // Cell Four
                    setCellText(4, cell4Data);
                    gridChange = false;
                    break;
                case 2:
                    if (gridChange) {
                        gridLayout.removeAllViews();
                        if (getResources().getConfiguration().orientation == 2) {
                            //Landscape
                            gridLayout.setColumnCount(2);
                            gridLayout.setRowCount(1);
                        } else {
                            gridLayout.setColumnCount(1);
                            gridLayout.setRowCount(2);
                        }
                    }
                    // Cell One
                    setCellText(1, cell1Data);
                    // Cell Two
                    setCellText(2, cell2Data);
                    gridChange = false;
                    break;
                case 1:
                    if (gridChange) {
                        gridLayout.removeAllViews();
                        gridLayout.setColumnCount(1);
                        gridLayout.setRowCount(1);
                    }
                    // Cell One
                    setCellText(1, cell1Data);
                    gridChange = false;
                    break;
            }
        } else {
            gridLayout.removeAllViews();
            gridLayout.setColumnCount(1);
            gridLayout.setRowCount(1);
            View gridCell1 = layoutInflater.inflate(R.layout.layout_griditem_nodata, gridLayout, false);
            gridLayout.addView(gridCell1);
            LinearLayout layout1 = findViewById(R.id.layout_no_data);
            ImageView logoImageView = findViewById(R.id.imageView);
            if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
                layout1.setBackgroundColor(getResources().getColor(R.color.black));
                logoImageView.setImageDrawable(getResources().getDrawable(R.drawable.wunderlinq_logo_white));
            } else {
                layout1.setBackgroundColor(getResources().getColor(R.color.white));
                logoImageView.setImageDrawable(getResources().getDrawable(R.drawable.wunderlinq_logo_black));
            }
        }
    }

    // Set Cell Text
    private void setCellText(Integer cellNumber, Integer dataPoint){
        String pressureUnit = "bar";
        String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
        if (pressureFormat.contains("1")) {
            // KPa
            pressureUnit = "KPa";
        } else if (pressureFormat.contains("2")) {
            // Kg-f
            pressureUnit = "Kg-f";
        } else if (pressureFormat.contains("3")) {
            // Psi
            pressureUnit = "psi";
        }
        String temperatureUnit = "C";
        String temperatureFormat = sharedPrefs.getString("prefTempF", "0");
        if (temperatureFormat.contains("1")) {
            // F
            temperatureUnit = "F";
        }
        String distanceUnit = "km";
        //String heightUnit = "m";
        String distanceTimeUnit = "kmh";
        String consumptionUnit = "L/100";
        String distanceFormat = sharedPrefs.getString("prefDistance", "0");
        if (distanceFormat.contains("1")) {
            distanceUnit = "mi";
            //heightUnit = "ft";
            distanceTimeUnit = "mph";
            consumptionUnit = "mpg";
        }
        String voltageUnit = "V";
        String throttleUnit = "%";

        String label = "";
        String value = getString(R.string.blank_field);
        boolean itsDark = ((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1");
        switch (dataPoint){
            case 0:
                //Gear
                label = getString(R.string.gear_label);
                if(Data.getGear() != null){
                    value = Data.getGear();
                }
                break;
            case 1:
                //Engine
                label = getString(R.string.engine_temp_label) + " (" + temperatureUnit + ")";
                if(Data.getEngineTemperature() != null ){
                    Double engineTemp = Data.getEngineTemperature();
                    if (temperatureFormat.contains("1")) {
                        // F
                        engineTemp = Utils.celsiusToFahrenheit(engineTemp);
                    }
                    value = String.valueOf(Math.round(engineTemp));
                }
                break;
            case 2:
                //Ambient
                label = getString(R.string.ambient_temp_label) + " (" + temperatureUnit + ")";
                if(Data.getAmbientTemperature() != null ){
                    Double ambientTemp = Data.getAmbientTemperature();
                    if (temperatureFormat.contains("1")) {
                        // F
                        ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                    }
                    value = String.valueOf(Math.round(ambientTemp));
                }
                break;
            case 3:
                //FrontTire
                label = getString(R.string.frontpressure_header) + " (" + pressureUnit + ")";
                if(Data.getFrontTirePressure() != null){
                    Double rdcFront = Data.getFrontTirePressure();
                    if (pressureFormat.contains("1")) {
                        // KPa
                        rdcFront = Utils.barTokPa(rdcFront);
                    } else if (pressureFormat.contains("2")) {
                        // Kg-f
                        rdcFront = Utils.barTokgf(rdcFront);
                    } else if (pressureFormat.contains("3")) {
                        // Psi
                        rdcFront = Utils.barToPsi(rdcFront);
                    }
                    value = String.valueOf(Utils.oneDigit.format(rdcFront));
                }
                break;
            case 4:
                //RearTire
                label = getString(R.string.rearpressure_header) + " (" + pressureUnit + ")";
                if(Data.getRearTirePressure() != null){
                    Double rdcRear = Data.getRearTirePressure();
                    if (pressureFormat.contains("1")) {
                        // KPa
                        rdcRear = Utils.barTokPa(rdcRear);
                    } else if (pressureFormat.contains("2")) {
                        // Kg-f
                        rdcRear = Utils.barTokgf(rdcRear);
                    } else if (pressureFormat.contains("3")) {
                        // Psi
                        rdcRear = Utils.barToPsi(rdcRear);
                    }
                    value = String.valueOf(Utils.oneDigit.format(rdcRear));
                }
                break;
            case 5:
                //Odometer
                label = getString(R.string.odometer_label) + " (" + distanceUnit + ")";
                if(Data.getOdometer() != null){
                    Double odometer = Data.getOdometer();
                    if (distanceFormat.contains("1")) {
                        odometer = Utils.kmToMiles(odometer);
                    }
                    value = String.valueOf(Math.round(odometer));
                }
                break;
            case 6:
                //Voltage
                label = getString(R.string.voltage_label) + " (" + voltageUnit + ")";
                if(Data.getvoltage() != null){
                    Double voltage = Data.getvoltage();
                    value = String.valueOf(Utils.oneDigit.format(voltage));
                }
                break;
            case 7:
                //Throttle
                label = getString(R.string.throttle_label) + " (" + throttleUnit + ")";
                if(Data.getThrottlePosition() != null){
                    Double throttlePosition = Data.getThrottlePosition();
                    value = String.valueOf(throttlePosition);
                }
                break;
            case 8:
                //Front Brakes
                label = getString(R.string.frontbrakes_label);
                if(Data.getFrontBrake() != null){
                    Integer frontBrakes = Data.getFrontBrake();
                    value = String.valueOf(frontBrakes);
                }
                break;
            case 9:
                //Rear Brakes
                label = getString(R.string.rearbrakes_label);
                if(Data.getRearBrake() != null){
                    Integer rearBrakes = Data.getRearBrake();
                    value = String.valueOf(rearBrakes);
                }
                break;
            case 10:
                //Ambient Light
                label = getString(R.string.ambientlight_label);
                if(Data.getAmbientLight() != null){
                    Integer ambientLight = Data.getAmbientLight();
                    value = String.valueOf(ambientLight);
                }
                break;
            case 11:
                //Trip 1
                label = getString(R.string.trip1_label) + " (" + distanceUnit + ")";
                if(Data.getTripOne() != null) {
                    Double trip1 = Data.getTripOne();
                    if (distanceFormat.contains("1")) {
                        trip1 = Utils.kmToMiles(trip1);
                    }
                    value = Utils.oneDigit.format(trip1);
                }
                break;
            case 12:
                //Trip 2
                label = getString(R.string.trip2_label) + " (" + distanceUnit + ")";
                if(Data.getTripTwo() != null){
                    Double trip2 = Data.getTripTwo();
                    if (distanceFormat.contains("1")) {
                        trip2 = Utils.kmToMiles(trip2);
                    }
                    value = Utils.oneDigit.format(trip2);
                }
                break;
            case 13:
                //Trip Auto
                label = getString(R.string.tripauto_label) + " (" + distanceUnit + ")";
                if(Data.getTripAuto() != null){
                    Double tripauto = Data.getTripAuto();
                    if (distanceFormat.contains("1")) {
                        tripauto = Utils.kmToMiles(tripauto);
                    }
                    value = Utils.oneDigit.format(tripauto);
                }
                break;
            case 14:
                //Speed
                label = getString(R.string.speed_label) + " (" + distanceTimeUnit + ")";
                if(Data.getSpeed() != null){
                    Double speed = Data.getSpeed();
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    value = String.valueOf(Math.round(speed));
                }
                break;
            case 15:
                //Average Speed
                label = getString(R.string.avgspeed_label) + " (" + distanceTimeUnit + ")";
                if(Data.getAvgSpeed() != null){
                    Double avgspeed = Data.getAvgSpeed();
                    if (distanceFormat.contains("1")) {
                        avgspeed = Utils.kmToMiles(avgspeed);
                    }
                    value = String.valueOf(Utils.oneDigit.format(avgspeed));
                }
                break;
            case 16:
                //Current Consumption
                label = getString(R.string.cconsumption_label) + " (" + consumptionUnit + ")";
                if(Data.getCurrentConsumption() != null){
                    Double currentConsumption = Data.getCurrentConsumption();
                    if (distanceFormat.contains("1")) {
                        currentConsumption = Utils.l100Tompg(currentConsumption);
                    }
                    value = String.valueOf(Utils.oneDigit.format(currentConsumption));
                }
                break;
            case 17:
                //Fuel Economy One
                label = getString(R.string.fueleconomyone_label) + " (" + consumptionUnit + ")";
                if(Data.getFuelEconomyOne() != null){
                    Double fuelEconomyOne = Data.getFuelEconomyOne();
                    if (distanceFormat.contains("1")) {
                        fuelEconomyOne = Utils.l100Tompg(fuelEconomyOne);
                    }
                    value = String.valueOf(Utils.oneDigit.format(fuelEconomyOne));
                }
                break;
            case 18:
                //Fuel Economy Two
                label = getString(R.string.fueleconomytwo_label) + " (" + consumptionUnit + ")";
                if(Data.getFuelEconomyTwo() != null){
                    Double fuelEconomyTwo = Data.getFuelEconomyTwo();
                    if (distanceFormat.contains("1")) {
                        fuelEconomyTwo = Utils.l100Tompg(fuelEconomyTwo);
                    }
                    value = String.valueOf(Utils.oneDigit.format(fuelEconomyTwo));
                }
                break;
            case 19:
                //Fuel Range
                label = getString(R.string.fuelrange_label) + " (" + distanceUnit + ")";
                if(Data.getFuelRange() != null){
                    Double fuelrange = Data.getFuelRange();
                    if (distanceFormat.contains("1")) {
                        fuelrange = Utils.kmToMiles(fuelrange);
                    }
                    value = String.valueOf(Math.round(fuelrange));
                }
                break;
            case 20:
                //Shifts
                label = getString(R.string.shifts_header);
                if(Data.getNumberOfShifts() != null){
                    int shifts = Data.getNumberOfShifts();
                    value = String.valueOf(shifts);
                }
                break;
            case 21:
                //Lean Angle
                label = getString(R.string.leanangle_header);
                if(Data.getLeanAngle() != null){
                    Double leanAngle = Data.getLeanAngle();
                    value = String.valueOf(Utils.oneDigit.format(leanAngle));
                }
                break;
            case 22:
                //g-force
                label = getString(R.string.gforce_header);
                if(Data.getGForce() != null){
                    Double gForce = Data.getGForce();
                    value = String.valueOf(Utils.oneDigit.format(gForce));
                }
                break;
            case 23:
                //bearing
                label = getString(R.string.bearing_header);
                if (Data.getBearing() != null) {
                    Integer bearingValue = Data.getBearing();
                    String bearing = bearingValue.toString() + "°";
                    if (!sharedPrefs.getString("prefBearing", "0").contains("0")) {
                        if (bearingValue > 331 || bearingValue <= 28) {
                            bearing = getString(R.string.north);
                        } else if (bearingValue > 28 && bearingValue <= 73) {
                            bearing = getString(R.string.north_east);
                        } else if (bearingValue > 73 && bearingValue <= 118) {
                            bearing = getString(R.string.east);
                        } else if (bearingValue > 118 && bearingValue <= 163) {
                            bearing = getString(R.string.south_east);
                        } else if (bearingValue > 163 && bearingValue <= 208) {
                            bearing = getString(R.string.south);
                        } else if (bearingValue > 208 && bearingValue <= 253) {
                            bearing = getString(R.string.south_west);
                        } else if (bearingValue > 253 && bearingValue <= 298) {
                            bearing = getString(R.string.west);
                        } else if (bearingValue > 298 && bearingValue <= 331) {
                            bearing = getString(R.string.north_west);
                        }
                    }
                    value = bearing;
                }
                break;
            default:

                break;
        }
        switch (cellNumber){
            case 1:
                if (gridChange) {
                    View gridCell1 = layoutInflater.inflate(R.layout.layout_griditem1, gridLayout, false);
                    gridLayout.addView(gridCell1);
                }
                layout1 = findViewById(R.id.layout_1);
                TextView textView1 = findViewById(R.id.textView1);
                TextView textView1Label = findViewById(R.id.textView1label);
                layout1.setOnTouchListener(MainActivity.this);
                layout1.setTag(cellNumber);
                textView1.setTag(cellNumber);
                textView1Label.setTag(cellNumber);
                if (itsDark){
                    textView1.setTextColor(getResources().getColor(R.color.white));
                    textView1Label.setTextColor(getResources().getColor(R.color.white));
                    layout1.setBackgroundColor(getResources().getColor(R.color.black));
                    layout1.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView1.setTextColor(getResources().getColor(R.color.black));
                    textView1Label.setTextColor(getResources().getColor(R.color.black));
                    layout1.setBackgroundColor(getResources().getColor(R.color.white));
                    layout1.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView1Label.setText(label);
                textView1.setText(value);
                break;
            case 2:
                if (gridChange) {
                    View gridCell2 = layoutInflater.inflate(R.layout.layout_griditem2, gridLayout, false);
                    gridLayout.addView(gridCell2);
                }
                LinearLayout layout2 = findViewById(R.id.layout_2);
                TextView textView2 = findViewById(R.id.textView2);
                TextView textView2Label = findViewById(R.id.textView2label);
                layout2.setOnTouchListener(MainActivity.this);
                layout2.setTag(cellNumber);
                textView2.setTag(cellNumber);
                textView2Label.setTag(cellNumber);
                if (itsDark){
                    textView2.setTextColor(getResources().getColor(R.color.white));
                    textView2Label.setTextColor(getResources().getColor(R.color.white));
                    layout2.setBackgroundColor(getResources().getColor(R.color.black));
                    layout2.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView2.setTextColor(getResources().getColor(R.color.black));
                    textView2Label.setTextColor(getResources().getColor(R.color.black));
                    layout2.setBackgroundColor(getResources().getColor(R.color.white));
                    layout2.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView2Label.setText(label);
                textView2.setText(value);
                break;
            case 3:
                if (gridChange) {
                    View gridCell3 = layoutInflater.inflate(R.layout.layout_griditem3, gridLayout, false);
                    gridLayout.addView(gridCell3);
                }
                LinearLayout layout3 = findViewById(R.id.layout_3);
                TextView textView3 = findViewById(R.id.textView3);
                TextView textView3Label = findViewById(R.id.textView3label);
                layout3.setOnTouchListener(MainActivity.this);
                layout3.setTag(cellNumber);
                textView3.setTag(cellNumber);
                textView3Label.setTag(cellNumber);
                if (itsDark){
                    textView3.setTextColor(getResources().getColor(R.color.white));
                    textView3Label.setTextColor(getResources().getColor(R.color.white));
                    layout3.setBackgroundColor(getResources().getColor(R.color.black));
                    layout3.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView3.setTextColor(getResources().getColor(R.color.black));
                    textView3Label.setTextColor(getResources().getColor(R.color.black));
                    layout3.setBackgroundColor(getResources().getColor(R.color.white));
                    layout3.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView3Label.setText(label);
                textView3.setText(value);
                break;
            case 4:
                if (gridChange) {
                    View gridCell4 = layoutInflater.inflate(R.layout.layout_griditem4, gridLayout, false);
                    gridLayout.addView(gridCell4);
                }
                LinearLayout layout4 = findViewById(R.id.layout_4);
                TextView textView4 = findViewById(R.id.textView4);
                TextView textView4Label = findViewById(R.id.textView4label);
                layout4.setOnTouchListener(MainActivity.this);
                layout4.setTag(cellNumber);
                textView4.setTag(cellNumber);
                textView4Label.setTag(cellNumber);
                if (itsDark){
                    textView4.setTextColor(getResources().getColor(R.color.white));
                    textView4Label.setTextColor(getResources().getColor(R.color.white));
                    layout4.setBackgroundColor(getResources().getColor(R.color.black));
                    layout4.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView4.setTextColor(getResources().getColor(R.color.black));
                    textView4Label.setTextColor(getResources().getColor(R.color.black));
                    layout4.setBackgroundColor(getResources().getColor(R.color.white));
                    layout4.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView4Label.setText(label);
                textView4.setText(value);
                break;
            case 5:
                if (gridChange) {
                    View gridCell5 = layoutInflater.inflate(R.layout.layout_griditem5, gridLayout, false);
                    gridLayout.addView(gridCell5);
                }
                LinearLayout layout5 = findViewById(R.id.layout_5);
                TextView textView5 = findViewById(R.id.textView5);
                TextView textView5Label = findViewById(R.id.textView5label);
                layout5.setOnTouchListener(MainActivity.this);
                layout5.setTag(cellNumber);
                textView5.setTag(cellNumber);
                textView5Label.setTag(cellNumber);
                if (itsDark){
                    textView5.setTextColor(getResources().getColor(R.color.white));
                    textView5Label.setTextColor(getResources().getColor(R.color.white));
                    layout5.setBackgroundColor(getResources().getColor(R.color.black));
                    layout5.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView5.setTextColor(getResources().getColor(R.color.black));
                    textView5Label.setTextColor(getResources().getColor(R.color.black));
                    layout5.setBackgroundColor(getResources().getColor(R.color.white));
                    layout5.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView5Label.setText(label);
                textView5.setText(value);
                break;
            case 6:
                if (gridChange) {
                    View gridCell6 = layoutInflater.inflate(R.layout.layout_griditem6, gridLayout, false);
                    gridLayout.addView(gridCell6);
                }
                LinearLayout layout6 = findViewById(R.id.layout_6);
                TextView textView6 = findViewById(R.id.textView6);
                TextView textView6Label = findViewById(R.id.textView6label);
                layout6.setOnTouchListener(MainActivity.this);
                layout6.setTag(cellNumber);
                textView6.setTag(cellNumber);
                textView6Label.setTag(cellNumber);
                if (itsDark){
                    textView6.setTextColor(getResources().getColor(R.color.white));
                    textView6Label.setTextColor(getResources().getColor(R.color.white));
                    layout6.setBackgroundColor(getResources().getColor(R.color.black));
                    layout6.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView6.setTextColor(getResources().getColor(R.color.black));
                    textView6Label.setTextColor(getResources().getColor(R.color.black));
                    layout6.setBackgroundColor(getResources().getColor(R.color.white));
                    layout6.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView6Label.setText(label);
                textView6.setText(value);
                break;
            case 7:
                if (gridChange) {
                    View gridCell7 = layoutInflater.inflate(R.layout.layout_griditem7, gridLayout, false);
                    gridLayout.addView(gridCell7);
                }
                LinearLayout layout7 = findViewById(R.id.layout_7);
                TextView textView7 = findViewById(R.id.textView7);
                TextView textView7Label = findViewById(R.id.textView7label);
                layout7.setOnTouchListener(MainActivity.this);
                layout7.setTag(cellNumber);
                textView7.setTag(cellNumber);
                textView7Label.setTag(cellNumber);
                if (itsDark){
                    textView7.setTextColor(getResources().getColor(R.color.white));
                    textView7Label.setTextColor(getResources().getColor(R.color.white));
                    layout7.setBackgroundColor(getResources().getColor(R.color.black));
                    layout7.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView7.setTextColor(getResources().getColor(R.color.black));
                    textView7Label.setTextColor(getResources().getColor(R.color.black));
                    layout7.setBackgroundColor(getResources().getColor(R.color.white));
                    layout7.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView7Label.setText(label);
                textView7.setText(value);
                break;
            case 8:
                if (gridChange) {
                    View gridCell8 = layoutInflater.inflate(R.layout.layout_griditem8, gridLayout, false);
                    gridLayout.addView(gridCell8);
                }
                LinearLayout layout8 = findViewById(R.id.layout_8);
                TextView textView8 = findViewById(R.id.textView8);
                TextView textView8Label = findViewById(R.id.textView8label);
                layout8.setOnTouchListener(MainActivity.this);
                layout8.setTag(cellNumber);
                textView8.setTag(cellNumber);
                textView8Label.setTag(cellNumber);
                if (itsDark){
                    textView8.setTextColor(getResources().getColor(R.color.white));
                    textView8Label.setTextColor(getResources().getColor(R.color.white));
                    layout8.setBackgroundColor(getResources().getColor(R.color.black));
                    layout8.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView8.setTextColor(getResources().getColor(R.color.black));
                    textView8Label.setTextColor(getResources().getColor(R.color.black));
                    layout8.setBackgroundColor(getResources().getColor(R.color.white));
                    layout8.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView8Label.setText(label);
                textView8.setText(value);
                break;
            case 9:
                if (gridChange) {
                    View gridCell9 = layoutInflater.inflate(R.layout.layout_griditem9, gridLayout, false);
                    gridLayout.addView(gridCell9);
                }
                LinearLayout layout9 = findViewById(R.id.layout_9);
                TextView textView9 = findViewById(R.id.textView9);
                TextView textView9Label = findViewById(R.id.textView9label);
                layout9.setOnTouchListener(MainActivity.this);
                layout9.setTag(cellNumber);
                textView9.setTag(cellNumber);
                textView9Label.setTag(cellNumber);
                if (itsDark){
                    textView9.setTextColor(getResources().getColor(R.color.white));
                    textView9Label.setTextColor(getResources().getColor(R.color.white));
                    layout9.setBackgroundColor(getResources().getColor(R.color.black));
                    layout9.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView9.setTextColor(getResources().getColor(R.color.black));
                    textView9Label.setTextColor(getResources().getColor(R.color.black));
                    layout9.setBackgroundColor(getResources().getColor(R.color.black));
                    layout9.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView9Label.setText(label);
                textView9.setText(value);
                break;
            case 10:
                if (gridChange) {
                    View gridCell10 = layoutInflater.inflate(R.layout.layout_griditem10, gridLayout, false);
                    gridLayout.addView(gridCell10);
                }
                LinearLayout layout10 = findViewById(R.id.layout_10);
                TextView textView10 = findViewById(R.id.textView10);
                TextView textView10Label = findViewById(R.id.textView10label);
                layout10.setOnTouchListener(MainActivity.this);
                layout10.setTag(cellNumber);
                textView10.setTag(cellNumber);
                textView10Label.setTag(cellNumber);
                if (itsDark){
                    textView10.setTextColor(getResources().getColor(R.color.white));
                    textView10Label.setTextColor(getResources().getColor(R.color.white));
                    layout10.setBackgroundColor(getResources().getColor(R.color.black));
                    layout10.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView10.setTextColor(getResources().getColor(R.color.black));
                    textView10Label.setTextColor(getResources().getColor(R.color.black));
                    layout10.setBackgroundColor(getResources().getColor(R.color.white));
                    layout10.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView10Label.setText(label);
                textView10.setText(value);
                break;
            case 11:
                if (gridChange) {
                    View gridCell11 = layoutInflater.inflate(R.layout.layout_griditem11, gridLayout, false);
                    gridLayout.addView(gridCell11);
                }
                LinearLayout layout11 = findViewById(R.id.layout_11);
                TextView textView11 = findViewById(R.id.textView11);
                TextView textView11Label = findViewById(R.id.textView11label);
                layout11.setOnTouchListener(MainActivity.this);
                layout11.setTag(cellNumber);
                textView11.setTag(cellNumber);
                textView11Label.setTag(cellNumber);
                if (itsDark){
                    textView11.setTextColor(getResources().getColor(R.color.white));
                    textView11Label.setTextColor(getResources().getColor(R.color.white));
                    layout11.setBackgroundColor(getResources().getColor(R.color.black));
                    layout11.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView11.setTextColor(getResources().getColor(R.color.black));
                    textView11Label.setTextColor(getResources().getColor(R.color.black));
                    layout11.setBackgroundColor(getResources().getColor(R.color.white));
                    layout11.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView11Label.setText(label);
                textView11.setText(value);
                break;
            case 12:
                if (gridChange) {
                    View gridCell12 = layoutInflater.inflate(R.layout.layout_griditem12, gridLayout, false);
                    gridLayout.addView(gridCell12);
                }
                LinearLayout layout12 = findViewById(R.id.layout_12);
                TextView textView12 = findViewById(R.id.textView12);
                TextView textView12Label = findViewById(R.id.textView12label);
                layout12.setOnTouchListener(MainActivity.this);
                layout12.setTag(cellNumber);
                textView12.setTag(cellNumber);
                textView12Label.setTag(cellNumber);
                if (itsDark){
                    textView12.setTextColor(getResources().getColor(R.color.white));
                    textView12Label.setTextColor(getResources().getColor(R.color.white));
                    layout12.setBackgroundColor(getResources().getColor(R.color.black));
                    layout12.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView12.setTextColor(getResources().getColor(R.color.black));
                    textView12Label.setTextColor(getResources().getColor(R.color.black));
                    layout12.setBackgroundColor(getResources().getColor(R.color.white));
                    layout12.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView12Label.setText(label);
                textView12.setText(value);
                break;
            case 13:
                if (gridChange) {
                    View gridCell13 = layoutInflater.inflate(R.layout.layout_griditem13, gridLayout, false);
                    gridLayout.addView(gridCell13);
                }
                LinearLayout layout13 = findViewById(R.id.layout_13);
                TextView textView13 = findViewById(R.id.textView13);
                TextView textView13Label = findViewById(R.id.textView13label);
                layout13.setOnTouchListener(MainActivity.this);
                layout13.setTag(cellNumber);
                textView13.setTag(cellNumber);
                textView13Label.setTag(cellNumber);
                if (itsDark){
                    textView13.setTextColor(getResources().getColor(R.color.white));
                    textView13Label.setTextColor(getResources().getColor(R.color.white));
                    layout13.setBackgroundColor(getResources().getColor(R.color.black));
                    layout13.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView13.setTextColor(getResources().getColor(R.color.black));
                    textView13Label.setTextColor(getResources().getColor(R.color.black));
                    layout13.setBackgroundColor(getResources().getColor(R.color.white));
                    layout13.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView13Label.setText(label);
                textView13.setText(value);
                break;
            case 14:
                if (gridChange) {
                    View gridCell14 = layoutInflater.inflate(R.layout.layout_griditem14, gridLayout, false);
                    gridLayout.addView(gridCell14);
                }
                LinearLayout layout14 = findViewById(R.id.layout_14);
                TextView textView14 = findViewById(R.id.textView14);
                TextView textView14Label = findViewById(R.id.textView14label);
                layout14.setOnTouchListener(MainActivity.this);
                layout14.setTag(cellNumber);
                textView14.setTag(cellNumber);
                textView14Label.setTag(cellNumber);

                if (itsDark){
                    textView14.setTextColor(getResources().getColor(R.color.white));
                    textView14Label.setTextColor(getResources().getColor(R.color.white));
                    layout14.setBackgroundColor(getResources().getColor(R.color.black));
                    layout14.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView14.setTextColor(getResources().getColor(R.color.black));
                    textView14Label.setTextColor(getResources().getColor(R.color.black));
                    layout14.setBackgroundColor(getResources().getColor(R.color.white));
                    layout14.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView14Label.setText(label);
                textView14.setText(value);
                break;
            case 15:
                if (gridChange) {
                    View gridCell15 = layoutInflater.inflate(R.layout.layout_griditem15, gridLayout, false);
                    gridLayout.addView(gridCell15);
                }
                LinearLayout layout15 = findViewById(R.id.layout_15);
                TextView textView15 = findViewById(R.id.textView15);
                TextView textView15Label = findViewById(R.id.textView15label);
                layout15.setOnTouchListener(MainActivity.this);
                layout15.setTag(cellNumber);
                textView15.setTag(cellNumber);
                textView15Label.setTag(cellNumber);
                if (itsDark){
                    textView15.setTextColor(getResources().getColor(R.color.white));
                    textView15Label.setTextColor(getResources().getColor(R.color.white));
                    layout15.setBackgroundColor(getResources().getColor(R.color.black));
                    layout15.setBackground(getResources().getDrawable(R.drawable.border_white));
                } else {
                    textView15.setTextColor(getResources().getColor(R.color.black));
                    textView15Label.setTextColor(getResources().getColor(R.color.black));
                    layout15.setBackgroundColor(getResources().getColor(R.color.white));
                    layout15.setBackground(getResources().getDrawable(R.drawable.border));
                }
                textView15Label.setText(label);
                textView15.setText(value);
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults != null) {
            switch (requestCode) {
                case PERMISSION_REQUEST_CAMERA: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Camera permission granted");
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.negative_alert_title));
                        builder.setMessage(getString(R.string.negative_camera_alert_body));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }
                        });
                        builder.show();
                    }
                }
                case PERMISSION_REQUEST_CALL_PHONE: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Call Phone permission granted");
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.negative_alert_title));
                        builder.setMessage(getString(R.string.negative_call_alert_body));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }
                        });
                        builder.show();
                    }
                }
                case PERMISSION_REQUEST_READ_CONTACTS: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Call Phone permission granted");
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.negative_alert_title));
                        builder.setMessage(getString(R.string.negative_contacts_alert_body));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }
                        });
                        builder.show();
                    }
                }
                case PERMISSION_REQUEST_RECORD_AUDIO: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Record Audio permission granted");
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.negative_alert_title));
                        builder.setMessage(getString(R.string.negative_record_audio_alert_body));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }
                        });
                        builder.show();
                    }
                }
                case PERMISSION_REQUEST_WRITE_STORAGE: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Write to storage permission granted");
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.negative_alert_title));
                        builder.setMessage(getString(R.string.negative_write_alert_body));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }
                        });
                        builder.show();
                    }
                }
                case PERMISSION_REQUEST_FINE_LOCATION: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "coarse location permission granted");
                    } else {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.negative_alert_title));
                        builder.setMessage(getString(R.string.negative_location_alert_body));
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                            }
                        });
                        builder.show();
                    }
                }
                default:
                    Log.d(TAG, "Unknown Permissions Request Code");
            }
        }

    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT","15"));
        //int maxCellCount = Integer.parseInt(sharedPrefs.getString("prefMaxCells","15"));
        int nextCellCount = 1;
        SharedPreferences.Editor editor = sharedPrefs.edit();

        switch (keyCode) {
            case KeyEvent.KEYCODE_ESCAPE:
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Intent backIntent = new Intent(MainActivity.this, TaskActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Intent forwardIntent = new Intent(MainActivity.this, MusicActivity.class);
                startActivity(forwardIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                gridChange = true;
                switch (currentCellCount){
                    case 15:
                        nextCellCount = 1;
                        break;
                    case 12:
                        nextCellCount = 15;
                        break;
                    case 8:
                        nextCellCount = 12;
                        break;
                    case 4:
                        nextCellCount = 8;
                        break;
                    case 2:
                        nextCellCount = 4;
                        break;
                    case 1:
                        nextCellCount = 2;
                        break;
                }
                editor.putString("CELL_COUNT", String.valueOf(nextCellCount));
                editor.apply();
                updateDisplay();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                gridChange = true;
                switch (currentCellCount){
                    case 15:
                        nextCellCount = 12;
                        break;
                    case 12:
                        nextCellCount = 8;
                        break;
                    case 8:
                        nextCellCount = 4;
                        break;
                    case 4:
                        nextCellCount = 2;
                        break;
                    case 2:
                        nextCellCount = 1;
                        break;
                    case 1:
                        nextCellCount = 15;
                        break;
                }
                editor.putString("CELL_COUNT", String.valueOf(nextCellCount));
                editor.apply();
                updateDisplay();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(),  Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }

        return false;
    }
}
