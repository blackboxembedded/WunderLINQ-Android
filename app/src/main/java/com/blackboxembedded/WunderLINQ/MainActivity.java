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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "MainActivity";

    private LayoutInflater layoutInflater;

    private ImageButton faultButton;
    private ImageButton btButton;
    private GridLayout gridLayout;

    private SharedPreferences sharedPrefs;

    private boolean gridChange = true;

    private Intent bluetoothLeService;
    public static BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    public static BluetoothGattCharacteristic gattCommandCharacteristic;
    List<BluetoothGattCharacteristic> gattCharacteristics;
    private String mDeviceAddress;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SETTINGS_CHECK = 10;

    public final static UUID UUID_MOTORCYCLE_SERVICE =
            UUID.fromString(GattAttributes.WUNDERLINQ_SERVICE);

    private PopupMenu mPopupMenu;
    private Menu mMenu;

    private GestureDetectorListener gestureDetector;

    private boolean inPIP = false;

    private boolean drawingComplete = true;

    private boolean timerRunning = false;

    private CountDownTimer cTimer = null;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "In onCreate");
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        gestureDetector = new GestureDetectorListener(this){

            @Override
            public void onPressLong() {
                if ( cell >= 1 && cell <= 15){
                    showCellSelector(cell);
                }
            }

            @Override
            public void onSwipeUp() {
                goUp();
            }

            @Override
            public void onSwipeDown() {
                goDown();
            }

            @Override
            public void onSwipeLeft() {
                goForward();
            }

            @Override
            public void onSwipeRight() {
                goBack();
            }
        };

        view.setOnTouchListener(this);

        showActionBar();

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

        bluetoothLeService = new Intent(MainActivity.this, BluetoothLeService.class);
        startService(bluetoothLeService);
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
            final ArrayAdapter<String> adp = new ArrayAdapter<String>(MainActivity.this, R.layout.item_gridspinner,
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
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setCustomView(v);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        ImageButton menuButton = findViewById(R.id.action_menu);
        faultButton = findViewById(R.id.action_faults);
        btButton = findViewById(R.id.action_connect);

        TextView navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.main_title);

        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
        faultButton.setOnClickListener(mClickListener);
        menuButton.setOnClickListener(mClickListener);
        btButton.setOnClickListener(mClickListener);

        faultButton.setVisibility(View.GONE);

        mPopupMenu = new PopupMenu(this, menuButton);
        MenuInflater menuOtherInflater = mPopupMenu.getMenuInflater();
        menuOtherInflater.inflate(R.menu.menu_main, mPopupMenu.getMenu());
        mMenu = mPopupMenu.getMenu();
        mMenu.findItem(R.id.action_hwsettings).setVisible(false);
        mPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.action_bike_info:
                        Intent bikeInfoIntent = new Intent(MainActivity.this, BikeInfoActivity.class);
                        startActivity(bikeInfoIntent);
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
                        Intent hwSettingsIntent = new Intent(MainActivity.this, HWSettingsActivity.class);
                        startActivity(hwSettingsIntent);
                        break;
                    case R.id.action_enter_splitscreen:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if (!isInMultiWindowMode()) {
                                if (isAccessibilityServiceEnabled(MainActivity.this, MyAccessibilityService.class)) {
                                    Intent accessibilityService = new Intent(MainActivity.this, MyAccessibilityService.class);
                                    accessibilityService.putExtra("command", 1);
                                    startService(accessibilityService);
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
                        BluetoothLeService.clearNotifications();
                        stopService(bluetoothLeService);
                        finishAffinity();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        getSupportActionBar().show();
        startTimer();
        gestureDetector.onTouch(v, event);
        return true;
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_connect:
                    Log.d(TAG,"Connect");
                    if(!(Build.BRAND.startsWith("Android") && Build.DEVICE.startsWith("generic"))) {
                        setupBLE();
                    } else {
                        Log.d(TAG,"Running in the emulator");
                    }
                    break;
                case R.id.action_back:
                    goBack();
                    break;
                case R.id.action_forward:
                    goForward();
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

    @Override
    public void recreate() {
        super.recreate();
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
        }

        getSupportActionBar().show();
        updateNightMode();
        updateDisplay();
        startTimer();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"In onDestroy");
        super.onDestroy();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
        }
        mBluetoothLeService = null;
    }

    @Override
    public void onStop() {
        Log.d(TAG,"In onStop");
        super.onStop();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG,"In onPause");
        super.onPause();
        cancelTimer();
        try {
            if (!sharedPrefs.getBoolean("prefPIP", false)) {
                unregisterReceiver(mGattUpdateReceiver);
            }
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
        }
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
        Log.d(TAG,"onActivityResult");
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else if (requestCode == SETTINGS_CHECK) {
            gridChange = true;
            updateNightMode();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "In onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.d(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            BluetoothLeService.connect(mDeviceAddress, getString(R.string.device_name));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "In onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };

    private void setupBLE() {
        Log.d(TAG,"In setupBLE()");
        int wlqCnt = 0;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (!pairedDevices.isEmpty()) {
            for (BluetoothDevice devices : pairedDevices) {
                if (devices.getName() != null) {
                    if (devices.getName().equals(getString(R.string.device_name))) {
                        wlqCnt = wlqCnt + 1;
                        Log.d(TAG, "Previously Paired WunderLINQ: " + devices.getAddress());
                        mDeviceAddress = devices.getAddress();
                    }
                }
            }
        }
        if (wlqCnt == 0){
            Log.d(TAG, "No paired WunderLINQ: " + mDeviceAddress);
            // Display dialog text here......
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.no_pairing_alert_title));
            builder.setMessage(getString(R.string.no_pairing_alert_body));
            builder.setPositiveButton(R.string.alert_btn_ok,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            builder.setNegativeButton(R.string.task_title_settings,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent settings_intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                            startActivityForResult(settings_intent, 0);
                        }
                    });
            builder.show();
        } else if (wlqCnt == 1){
            Log.d(TAG, "Connecting to Address: " + mDeviceAddress);
            bindService(bluetoothLeService, mServiceConnection, BIND_AUTO_CREATE);
        } else if (wlqCnt > 1){
            Log.d(TAG, "Too many WunderLINQ pairings: " + wlqCnt);
            // Display dialog text here......
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.too_many_pairings_alert_title));
            builder.setMessage(getString(R.string.too_many_pairings_alert_body));
            builder.setPositiveButton(R.string.alert_btn_ok,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            builder.setNegativeButton(R.string.task_title_settings,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent settings_intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                            startActivityForResult(settings_intent, 0);
                        }
                    });
            builder.show();
        }
    }

    BroadcastReceiver mBondingBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

            switch (state) {
                case BluetoothDevice.BOND_BONDING:
                    Log.d("Bonding Status:", " Bonding...");
                    break;

                case BluetoothDevice.BOND_BONDED:
                    Log.d("Bonding Status:", "Bonded!!");
                    setupBLE();
                    break;

                case BluetoothDevice.BOND_NONE:
                    Log.d("Bonding Status:", "Fail");
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
                BluetoothLeService.discoverServices();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG,"GATT_DISCONNECTED");
                //Data.clear();
                if (!sharedPrefs.getBoolean("prefMotorcycleData", false)){
                    updateDisplay();
                }
                btButton.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.motorrad_red));
                btButton.setEnabled(true);
                mMenu.findItem(R.id.action_hwsettings).setVisible(false);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG,"GATT_SERVICE_DISCOVERED");
                checkGattServices(BluetoothLeService.getSupportedGattServices());
                btButton.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.motorrad_blue));
                btButton.setEnabled(false);
                mMenu.findItem(R.id.action_hwsettings).setVisible(true);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Bundle bd = intent.getExtras();
                if(bd != null) {
                    if (bd.getString(BluetoothLeService.EXTRA_BYTE_UUID_VALUE).contains(GattAttributes.WUNDERLINQ_MESSAGE_CHARACTERISTIC)) {
                        btButton.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.motorrad_blue));
                        btButton.setEnabled(false);
                        mMenu.findItem(R.id.action_hwsettings).setVisible(true);
                    }
                }
                if(drawingComplete) {
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
        if (gattServices == null) return;
        String uuid;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
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
                                BluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            BluetoothLeService.readCharacteristic(gattCharacteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = gattCharacteristic;
                            BluetoothLeService.setCharacteristicNotification(
                                    gattCharacteristic, true);
                        }
                    } else if (UUID.fromString(GattAttributes.WUNDERLINQ_COMMAND_CHARACTERISTIC).equals(gattCharacteristic.getUuid())){
                        gattCommandCharacteristic = gattCharacteristic;
                        // Read config
                        BluetoothLeService.writeCharacteristic(gattCommandCharacteristic, WLQ.GET_CONFIG_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
                    }
                }
            }
        }
    }

    //Update Nightmode
    private void updateNightMode(){
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        int prefNightMode = Integer.parseInt(sharedPrefs.getString("prefNightModeCombo", "3"));

        switch(prefNightMode){
            case 0:
                //Off
                if(currentNightMode != Configuration.UI_MODE_NIGHT_NO){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    Log.d(TAG,"Setting NIGHT MODE OFF");
                }
                break;
            case 1:
                //On
                if(currentNightMode != Configuration.UI_MODE_NIGHT_YES){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    Log.d(TAG,"Setting NIGHT MODE ON");
                }
                break;
            case 3:
                //Android
                if(currentNightMode != Configuration.UI_MODE_NIGHT_UNDEFINED){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    Log.d(TAG,"Setting NIGHT MODE FOLLOW SYSTEM");
                }
                break;
            default:
                //
                break;
        }
    }

    // Update Display
    private void updateDisplay(){
        drawingComplete = false;
        gridLayout = findViewById(R.id.gridLayout);

        //Check for active faults
        ArrayList<String> faultListData = FaultStatus.getallActiveDesc();
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

        int count = Integer.parseInt(sharedPrefs.getString("CELL_COUNT", "15"));
        if (inPIP) {
            count = Integer.parseInt(sharedPrefs.getString("prefPIPCellCount", "4"));
        }
        boolean portrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        switch (count) {
            case 15:
                if (gridChange) {
                    gridLayout.removeAllViews();
                    if (portrait) {
                        gridLayout.setColumnCount(3);
                        gridLayout.setRowCount(5);
                    } else {
                        gridLayout.setColumnCount(5);
                        gridLayout.setRowCount(3);
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
                    if (portrait) {
                        gridLayout.setColumnCount(3);
                        gridLayout.setRowCount(4);
                    } else {
                        gridLayout.setColumnCount(4);
                        gridLayout.setRowCount(3);
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
            case 10:
                if (gridChange) {
                    gridLayout.removeAllViews();
                    if (portrait) {
                        gridLayout.setColumnCount(2);
                        gridLayout.setRowCount(5);
                    } else {
                        gridLayout.setColumnCount(5);
                        gridLayout.setRowCount(2);
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
                // Cell Nine
                setCellText(9, cell9Data);
                // Cell Ten
                setCellText(10, cell10Data);
                gridChange = false;
                break;
            case 8:
                if (gridChange) {
                    gridLayout.removeAllViews();
                    if (portrait) {
                        gridLayout.setColumnCount(2);
                        gridLayout.setRowCount(4);
                    } else {
                        gridLayout.setColumnCount(4);
                        gridLayout.setRowCount(2);
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
                    if (portrait) {
                        gridLayout.setColumnCount(1);
                        gridLayout.setRowCount(4);
                    } else {
                        gridLayout.setColumnCount(2);
                        gridLayout.setRowCount(2);
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
                    if (portrait) {
                        gridLayout.setColumnCount(1);
                        gridLayout.setRowCount(2);
                    } else {
                        gridLayout.setColumnCount(2);
                        gridLayout.setRowCount(1);
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
        String heightUnit = "m";
        String distanceTimeUnit = "kmh";
        String distanceFormat = sharedPrefs.getString("prefDistance", "0");
        if (distanceFormat.contains("1")) {
            distanceUnit = "mi";
            heightUnit = "ft";
            distanceTimeUnit = "mph";
        }
        String consumptionUnit = "L/100";
        String consumptionFormat = sharedPrefs.getString("prefConsumption", "0");
        if (consumptionFormat.contains("1")) {
            consumptionUnit = "mpg";
        } else if (consumptionFormat.contains("2")) {
            consumptionUnit = "mpg";
        } else if (consumptionFormat.contains("3")) {
            consumptionUnit = "km/L";
        }
        String voltageUnit = "V";
        String throttleUnit = "%";

        String label = "";
        String value = getString(R.string.blank_field);
        Drawable icon = null;

        switch (dataPoint){
            case 0:
                //Gear
                label = getString(R.string.gear_label);
                if(Data.getGear() != null){
                    value = Data.getGear();
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_cog);
                break;
            case 1:
                //Engine
                label = getString(R.string.engine_temp_label) + " (" + temperatureUnit + ")";
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_engine_temp);
                if(Data.getEngineTemperature() != null ){
                    double engineTemp = Data.getEngineTemperature();
                    if (engineTemp >= 104.0){
                        icon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                    }
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
                    double ambientTemp = Data.getAmbientTemperature();
                    if(ambientTemp <= 0){
                        icon = AppCompatResources.getDrawable(this, R.drawable.ic_snowflake);
                    } else {
                        icon = AppCompatResources.getDrawable(this, R.drawable.ic_thermometer_half);
                    }
                    if (temperatureFormat.contains("1")) {
                        // F
                        ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                    }
                    value = String.valueOf(Math.round(ambientTemp));
                } else {
                    icon = AppCompatResources.getDrawable(this, R.drawable.ic_thermometer_half);
                }

                break;
            case 3:
                //FrontTire
                label = getString(R.string.frontpressure_header) + " (" + pressureUnit + ")";
                if(Data.getFrontTirePressure() != null){
                    double rdcFront = Data.getFrontTirePressure();
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
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_tire);
                if (FaultStatus.getfrontTirePressureCriticalActive()){
                    icon = AppCompatResources.getDrawable(this, R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                } else if (FaultStatus.getfrontTirePressureWarningActive()){
                    icon = AppCompatResources.getDrawable(this, R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                }
                break;
            case 4:
                //RearTire
                label = getString(R.string.rearpressure_header) + " (" + pressureUnit + ")";
                if(Data.getRearTirePressure() != null){
                    double rdcRear = Data.getRearTirePressure();
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
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_tire);
                if (FaultStatus.getrearTirePressureCriticalActive()){
                    icon = AppCompatResources.getDrawable(this, R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                } else if (FaultStatus.getrearTirePressureWarningActive()){
                    icon = AppCompatResources.getDrawable(this, R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                }
                break;
            case 5:
                //Odometer
                label = getString(R.string.odometer_label) + " (" + distanceUnit + ")";
                if(Data.getOdometer() != null){
                    double odometer = Data.getOdometer();
                    if (distanceFormat.contains("1")) {
                        odometer = Utils.kmToMiles(odometer);
                    }
                    value = String.valueOf(Math.round(odometer));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_dashboard_meter);
                break;
            case 6:
                //Voltage
                label = getString(R.string.voltage_label) + " (" + voltageUnit + ")";
                if(Data.getvoltage() != null){
                    Double voltage = Data.getvoltage();
                    value = String.valueOf(Utils.oneDigit.format(voltage));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_car_battery);
                break;
            case 7:
                //Throttle
                label = getString(R.string.throttle_label) + " (" + throttleUnit + ")";
                if(Data.getThrottlePosition() != null){
                    Double throttlePosition = Data.getThrottlePosition();
                    value = String.valueOf(Math.round(throttlePosition));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_signature);
                break;
            case 8:
                //Front Brakes
                label = getString(R.string.frontbrakes_label);
                if((Data.getFrontBrake() != null) && (Data.getFrontBrake() != 0)){
                    Integer frontBrakes = Data.getFrontBrake();
                    value = String.valueOf(frontBrakes);
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_brakes);
                break;
            case 9:
                //Rear Brakes
                label = getString(R.string.rearbrakes_label);
                if((Data.getRearBrake() != null) && (Data.getRearBrake() != 0)){
                    Integer rearBrakes = Data.getRearBrake();
                    value = String.valueOf(rearBrakes);
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_brakes);
                break;
            case 10:
                //Ambient Light
                label = getString(R.string.ambientlight_label);
                if(Data.getAmbientLight() != null){
                    Integer ambientLight = Data.getAmbientLight();
                    value = String.valueOf(ambientLight);
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_lightbulb);
                break;
            case 11:
                //Trip 1
                label = getString(R.string.trip1_label) + " (" + distanceUnit + ")";
                if(Data.getTripOne() != null) {
                    double trip1 = Data.getTripOne();
                    if (distanceFormat.contains("1")) {
                        trip1 = Utils.kmToMiles(trip1);
                    }
                    value = Utils.oneDigit.format(trip1);
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_suitcase);
                break;
            case 12:
                //Trip 2
                label = getString(R.string.trip2_label) + " (" + distanceUnit + ")";
                if(Data.getTripTwo() != null){
                    double trip2 = Data.getTripTwo();
                    if (distanceFormat.contains("1")) {
                        trip2 = Utils.kmToMiles(trip2);
                    }
                    value = Utils.oneDigit.format(trip2);
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_suitcase);
                break;
            case 13:
                //Trip Auto
                label = getString(R.string.tripauto_label) + " (" + distanceUnit + ")";
                if(Data.getTripAuto() != null){
                    double tripauto = Data.getTripAuto();
                    if (distanceFormat.contains("1")) {
                        tripauto = Utils.kmToMiles(tripauto);
                    }
                    value = Utils.oneDigit.format(tripauto);
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_suitcase);
                break;
            case 14:
                //Speed
                label = getString(R.string.speed_label) + " (" + distanceTimeUnit + ")";
                if(Data.getSpeed() != null){
                    double speed = Data.getSpeed();
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    value = String.valueOf(Math.round(speed));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_tachometer_alt);
                break;
            case 15:
                //Average Speed
                label = getString(R.string.avgspeed_label) + " (" + distanceTimeUnit + ")";
                if(Data.getAvgSpeed() != null){
                    double avgspeed = Data.getAvgSpeed();
                    if (distanceFormat.contains("1")) {
                        avgspeed = Utils.kmToMiles(avgspeed);
                    }
                    value = String.valueOf(Utils.oneDigit.format(avgspeed));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_tachometer_alt);
                break;
            case 16:
                //Current Consumption
                label = getString(R.string.cconsumption_label) + " (" + consumptionUnit + ")";
                if(Data.getCurrentConsumption() != null){
                    double currentConsumption = Data.getCurrentConsumption();
                    if (consumptionFormat.contains("1")) {
                        currentConsumption = Utils.l100Tompg(currentConsumption);
                    } else if (consumptionFormat.contains("2")) {
                        currentConsumption = Utils.l100Tompgi(currentConsumption);
                    } else if (consumptionFormat.contains("3")) {
                        currentConsumption = Utils.l100Tokml(currentConsumption);
                    }
                    value = String.valueOf(Utils.oneDigit.format(currentConsumption));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_gas_pump);
                break;
            case 17:
                //Fuel Economy One
                label = getString(R.string.fueleconomyone_label) + " (" + consumptionUnit + ")";
                if(Data.getFuelEconomyOne() != null){
                    double fuelEconomyOne = Data.getFuelEconomyOne();
                    if (consumptionFormat.contains("1")) {
                        fuelEconomyOne = Utils.l100Tompg(fuelEconomyOne);
                    } else if (consumptionFormat.contains("2")) {
                        fuelEconomyOne = Utils.l100Tompgi(fuelEconomyOne);
                    } else if (consumptionFormat.contains("3")) {
                        fuelEconomyOne = Utils.l100Tokml(fuelEconomyOne);
                    }
                    value = String.valueOf(Utils.oneDigit.format(fuelEconomyOne));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_gas_pump);
                break;
            case 18:
                //Fuel Economy Two
                label = getString(R.string.fueleconomytwo_label) + " (" + consumptionUnit + ")";
                if(Data.getFuelEconomyTwo() != null){
                    double fuelEconomyTwo = Data.getFuelEconomyTwo();
                    if (consumptionFormat.contains("1")) {
                        fuelEconomyTwo = Utils.l100Tompg(fuelEconomyTwo);
                    } else if (consumptionFormat.contains("2")) {
                        fuelEconomyTwo  = Utils.l100Tompgi(fuelEconomyTwo);
                    } else if (consumptionFormat.contains("3")) {
                        fuelEconomyTwo  = Utils.l100Tokml(fuelEconomyTwo);
                    }
                    value = String.valueOf(Utils.oneDigit.format(fuelEconomyTwo));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_gas_pump);
                break;
            case 19:
                //Fuel Range
                label = getString(R.string.fuelrange_label) + " (" + distanceUnit + ")";
                if(Data.getFuelRange() != null){
                    double fuelrange = Data.getFuelRange();
                    if (distanceFormat.contains("1")) {
                        fuelrange = Utils.kmToMiles(fuelrange);
                    }
                    value = String.valueOf(Math.round(fuelrange));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_gas_pump);
                break;
            case 20:
                //Shifts
                label = getString(R.string.shifts_header);
                if(Data.getNumberOfShifts() != null){
                    int shifts = Data.getNumberOfShifts();
                    value = String.valueOf(shifts);
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_arrows_alt_v);
                break;
            case 21:
                //Lean Angle
                label = getString(R.string.leanangle_header);
                if(Data.getLeanAngle() != null){
                    Double leanAngle = Data.getLeanAngle();
                    value = String.valueOf(Math.round(leanAngle));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_angle);
                break;
            case 22:
                //g-force
                label = getString(R.string.gforce_header);
                if(Data.getGForce() != null){
                    Double gForce = Data.getGForce();
                    value = String.valueOf(Utils.oneDigit.format(gForce));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_accelerometer);
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
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_compass);
                break;
            case 24:
                //time
                label = getString(R.string.time_header);
                if (Data.getTime() != null) {
                    SimpleDateFormat dateformat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    if (!sharedPrefs.getString("prefTime", "0").equals("0")) {
                        dateformat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    }
                    value = dateformat.format(Data.getTime());
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_clock);
                break;
            case 25:
                //barometric pressure
                label = getString(R.string.barometricpressure_header) + " (mBar)";
                if (Data.getBarometricPressure() != null) {
                    value = String.valueOf(Math.round(Data.getBarometricPressure()));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_barometer);
                break;
            case 26:
                //GPS Speed
                label = getString(R.string.gpsspeed_header) + " (" + distanceTimeUnit + ")";
                String gpsSpeed = "No Fix";
                if (Data.getLastLocation() != null){
                    gpsSpeed = String.valueOf(Math.round(Data.getLastLocation().getSpeed() * 3.6));
                    if (distanceFormat.contains("1")) {
                        gpsSpeed = String.valueOf(Math.round(Utils.kmToMiles(Data.getLastLocation().getSpeed() * 3.6)));
                    }
                }
                value = gpsSpeed;
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_tachometer_alt);
                break;
            case 27:
                //Altitude
                label = getString(R.string.altitude_header) + " (" + heightUnit + ")";
                String altitude = "No Fix";
                if (Data.getLastLocation() != null){
                    altitude = String.valueOf(Math.round(Data.getLastLocation().getAltitude()));
                    if (distanceFormat.contains("1")) {
                        altitude = String.valueOf(Math.round(Utils.mToFeet(Data.getLastLocation().getAltitude())));
                    }
                }
                value = altitude;
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_mountain);
                break;
            case 28:
                //Sunrise/Sunset
                label = getString(R.string.sunrisesunset_header);
                if (Data.getLastLocation() != null) {
                    Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(Calendar.getInstance(), Data.getLastLocation().getLatitude(), Data.getLastLocation().getLongitude());
                    Date sunrise = sunriseSunset[0].getTime();
                    Date sunset = sunriseSunset[1].getTime();
                    Date current = new Date();
                    SimpleDateFormat dateformat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    if (!sharedPrefs.getString("prefTime", "0").equals("0")) {
                        dateformat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    }
                    String sunriseString = dateformat.format(sunrise);
                    String sunsetString = dateformat.format(sunset);
                    value = sunriseString + "/" + sunsetString;

                    if(current.compareTo(sunrise) > 0 && current.compareTo(sunset) < 0){
                        icon = AppCompatResources.getDrawable(this, R.drawable.ic_sun);
                    } else {
                        icon = AppCompatResources.getDrawable(this, R.drawable.ic_moon);
                    }

                } else {
                    value = "No Fix";
                    icon = AppCompatResources.getDrawable(this, R.drawable.ic_sun);
                }

                break;
            case 29:
                //RPM
                label = getString(R.string.rpm_header);
                if (Data.getRPM() > 0){
                    value = String.valueOf(Data.getRPM());
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_tachometer_alt);
                break;
            case 30:
                //Lean Angle Bike
                label = getString(R.string.leanangle_bike_header);
                if(Data.getLeanAngleBike() != null){
                    Double leanAngleBike = Data.getLeanAngleBike();
                    value = String.valueOf(Math.round(leanAngleBike));
                }
                icon = AppCompatResources.getDrawable(this, R.drawable.ic_angle);
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
                ConstraintLayout layout1 = findViewById(R.id.layout_1);
                TextView textView1 = findViewById(R.id.textView1);
                TextView textView1Label = findViewById(R.id.textView1label);
                ImageView imageView1 = findViewById(R.id.imageView1);
                layout1.setOnTouchListener(MainActivity.this);
                layout1.setTag(cellNumber);
                textView1.setTag(cellNumber);
                textView1Label.setTag(cellNumber);
                textView1Label.setText(label);
                textView1.setText(value);
                if (icon !=null) {
                    imageView1.setImageDrawable(icon);
                } else {
                    imageView1.setImageResource(android.R.color.transparent);
                }
                break;
            case 2:
                if (gridChange) {
                    View gridCell2 = layoutInflater.inflate(R.layout.layout_griditem2, gridLayout, false);
                    gridLayout.addView(gridCell2);
                }
                ConstraintLayout layout2 = findViewById(R.id.layout_2);
                TextView textView2 = findViewById(R.id.textView2);
                TextView textView2Label = findViewById(R.id.textView2label);
                ImageView imageView2 = findViewById(R.id.imageView2);
                layout2.setOnTouchListener(MainActivity.this);
                layout2.setTag(cellNumber);
                textView2.setTag(cellNumber);
                textView2Label.setTag(cellNumber);
                textView2Label.setText(label);
                textView2.setText(value);
                if (icon !=null) {
                    imageView2.setImageDrawable(icon);
                } else {
                    imageView2.setImageResource(android.R.color.transparent);
                }
                break;
            case 3:
                if (gridChange) {
                    View gridCell3 = layoutInflater.inflate(R.layout.layout_griditem3, gridLayout, false);
                    gridLayout.addView(gridCell3);
                }
                ConstraintLayout layout3 = findViewById(R.id.layout_3);
                TextView textView3 = findViewById(R.id.textView3);
                TextView textView3Label = findViewById(R.id.textView3label);
                ImageView imageView3 = findViewById(R.id.imageView3);
                layout3.setOnTouchListener(MainActivity.this);
                layout3.setTag(cellNumber);
                textView3.setTag(cellNumber);
                textView3Label.setTag(cellNumber);
                textView3Label.setText(label);
                textView3.setText(value);
                if (icon !=null) {
                    imageView3.setImageDrawable(icon);
                } else {
                    imageView3.setImageResource(android.R.color.transparent);
                }
                break;
            case 4:
                if (gridChange) {
                    View gridCell4 = layoutInflater.inflate(R.layout.layout_griditem4, gridLayout, false);
                    gridLayout.addView(gridCell4);
                }
                ConstraintLayout layout4 = findViewById(R.id.layout_4);
                TextView textView4 = findViewById(R.id.textView4);
                TextView textView4Label = findViewById(R.id.textView4label);
                ImageView imageView4 = findViewById(R.id.imageView4);
                layout4.setOnTouchListener(MainActivity.this);
                layout4.setTag(cellNumber);
                textView4.setTag(cellNumber);
                textView4Label.setTag(cellNumber);
                textView4Label.setText(label);
                textView4.setText(value);
                if (icon !=null) {
                    imageView4.setImageDrawable(icon);
                } else {
                    imageView4.setImageResource(android.R.color.transparent);
                }
                break;
            case 5:
                if (gridChange) {
                    View gridCell5 = layoutInflater.inflate(R.layout.layout_griditem5, gridLayout, false);
                    gridLayout.addView(gridCell5);
                }
                ConstraintLayout layout5 = findViewById(R.id.layout_5);
                TextView textView5 = findViewById(R.id.textView5);
                TextView textView5Label = findViewById(R.id.textView5label);
                ImageView imageView5 = findViewById(R.id.imageView5);
                layout5.setOnTouchListener(MainActivity.this);
                layout5.setTag(cellNumber);
                textView5.setTag(cellNumber);
                textView5Label.setTag(cellNumber);
                textView5Label.setText(label);
                textView5.setText(value);
                if (icon !=null) {
                    imageView5.setImageDrawable(icon);
                } else {
                    imageView5.setImageResource(android.R.color.transparent);
                }
                break;
            case 6:
                if (gridChange) {
                    View gridCell6 = layoutInflater.inflate(R.layout.layout_griditem6, gridLayout, false);
                    gridLayout.addView(gridCell6);
                }
                ConstraintLayout layout6 = findViewById(R.id.layout_6);
                TextView textView6 = findViewById(R.id.textView6);
                TextView textView6Label = findViewById(R.id.textView6label);
                ImageView imageView6 = findViewById(R.id.imageView6);
                layout6.setOnTouchListener(MainActivity.this);
                layout6.setTag(cellNumber);
                textView6.setTag(cellNumber);
                textView6Label.setTag(cellNumber);
                textView6Label.setText(label);
                textView6.setText(value);
                if (icon !=null) {
                    imageView6.setImageDrawable(icon);
                } else {
                    imageView6.setImageResource(android.R.color.transparent);
                }
                break;
            case 7:
                if (gridChange) {
                    View gridCell7 = layoutInflater.inflate(R.layout.layout_griditem7, gridLayout, false);
                    gridLayout.addView(gridCell7);
                }
                ConstraintLayout layout7 = findViewById(R.id.layout_7);
                TextView textView7 = findViewById(R.id.textView7);
                TextView textView7Label = findViewById(R.id.textView7label);
                ImageView imageView7 = findViewById(R.id.imageView7);
                layout7.setOnTouchListener(MainActivity.this);
                layout7.setTag(cellNumber);
                textView7.setTag(cellNumber);
                textView7Label.setTag(cellNumber);
                textView7Label.setText(label);
                textView7.setText(value);
                if (icon !=null) {
                    imageView7.setImageDrawable(icon);
                } else {
                    imageView7.setImageResource(android.R.color.transparent);
                }
                break;
            case 8:
                if (gridChange) {
                    View gridCell8 = layoutInflater.inflate(R.layout.layout_griditem8, gridLayout, false);
                    gridLayout.addView(gridCell8);
                }
                ConstraintLayout layout8 = findViewById(R.id.layout_8);
                TextView textView8 = findViewById(R.id.textView8);
                TextView textView8Label = findViewById(R.id.textView8label);
                ImageView imageView8 = findViewById(R.id.imageView8);
                layout8.setOnTouchListener(MainActivity.this);
                layout8.setTag(cellNumber);
                textView8.setTag(cellNumber);
                textView8Label.setTag(cellNumber);
                textView8Label.setText(label);
                textView8.setText(value);
                if (icon !=null) {
                    imageView8.setImageDrawable(icon);
                } else {
                    imageView8.setImageResource(android.R.color.transparent);
                }
                break;
            case 9:
                if (gridChange) {
                    View gridCell9 = layoutInflater.inflate(R.layout.layout_griditem9, gridLayout, false);
                    gridLayout.addView(gridCell9);
                }
                ConstraintLayout layout9 = findViewById(R.id.layout_9);
                TextView textView9 = findViewById(R.id.textView9);
                TextView textView9Label = findViewById(R.id.textView9label);
                ImageView imageView9 = findViewById(R.id.imageView9);
                layout9.setOnTouchListener(MainActivity.this);
                layout9.setTag(cellNumber);
                textView9.setTag(cellNumber);
                textView9Label.setTag(cellNumber);
                textView9Label.setText(label);
                textView9.setText(value);
                if (icon !=null) {
                    imageView9.setImageDrawable(icon);
                } else {
                    imageView9.setImageResource(android.R.color.transparent);
                }
                break;
            case 10:
                if (gridChange) {
                    View gridCell10 = layoutInflater.inflate(R.layout.layout_griditem10, gridLayout, false);
                    gridLayout.addView(gridCell10);
                }
                ConstraintLayout layout10 = findViewById(R.id.layout_10);
                TextView textView10 = findViewById(R.id.textView10);
                TextView textView10Label = findViewById(R.id.textView10label);
                ImageView imageView10 = findViewById(R.id.imageView10);
                layout10.setOnTouchListener(MainActivity.this);
                layout10.setTag(cellNumber);
                textView10.setTag(cellNumber);
                textView10Label.setTag(cellNumber);
                textView10Label.setText(label);
                textView10.setText(value);
                if (icon !=null) {
                    imageView10.setImageDrawable(icon);
                } else {
                    imageView10.setImageResource(android.R.color.transparent);
                }
                break;
            case 11:
                if (gridChange) {
                    View gridCell11 = layoutInflater.inflate(R.layout.layout_griditem11, gridLayout, false);
                    gridLayout.addView(gridCell11);
                }
                ConstraintLayout layout11 = findViewById(R.id.layout_11);
                TextView textView11 = findViewById(R.id.textView11);
                TextView textView11Label = findViewById(R.id.textView11label);
                ImageView imageView11 = findViewById(R.id.imageView11);
                layout11.setOnTouchListener(MainActivity.this);
                layout11.setTag(cellNumber);
                textView11.setTag(cellNumber);
                textView11Label.setTag(cellNumber);
                textView11Label.setText(label);
                textView11.setText(value);
                if (icon !=null) {
                    imageView11.setImageDrawable(icon);
                } else {
                    imageView11.setImageResource(android.R.color.transparent);
                }
                break;
            case 12:
                if (gridChange) {
                    View gridCell12 = layoutInflater.inflate(R.layout.layout_griditem12, gridLayout, false);
                    gridLayout.addView(gridCell12);
                }
                ConstraintLayout layout12 = findViewById(R.id.layout_12);
                TextView textView12 = findViewById(R.id.textView12);
                TextView textView12Label = findViewById(R.id.textView12label);
                ImageView imageView12 = findViewById(R.id.imageView12);
                layout12.setOnTouchListener(MainActivity.this);
                layout12.setTag(cellNumber);
                textView12.setTag(cellNumber);
                textView12Label.setTag(cellNumber);
                textView12Label.setText(label);
                textView12.setText(value);
                if (icon !=null) {
                    imageView12.setImageDrawable(icon);
                } else {
                    imageView12.setImageResource(android.R.color.transparent);
                }
                break;
            case 13:
                if (gridChange) {
                    View gridCell13 = layoutInflater.inflate(R.layout.layout_griditem13, gridLayout, false);
                    gridLayout.addView(gridCell13);
                }
                ConstraintLayout layout13 = findViewById(R.id.layout_13);
                TextView textView13 = findViewById(R.id.textView13);
                TextView textView13Label = findViewById(R.id.textView13label);
                ImageView imageView13 = findViewById(R.id.imageView13);
                layout13.setOnTouchListener(MainActivity.this);
                layout13.setTag(cellNumber);
                textView13.setTag(cellNumber);
                textView13Label.setTag(cellNumber);
                textView13Label.setText(label);
                textView13.setText(value);
                if (icon !=null) {
                    imageView13.setImageDrawable(icon);
                } else {
                    imageView13.setImageResource(android.R.color.transparent);
                }
                break;
            case 14:
                if (gridChange) {
                    View gridCell14 = layoutInflater.inflate(R.layout.layout_griditem14, gridLayout, false);
                    gridLayout.addView(gridCell14);
                }
                ConstraintLayout layout14 = findViewById(R.id.layout_14);
                TextView textView14 = findViewById(R.id.textView14);
                TextView textView14Label = findViewById(R.id.textView14label);
                ImageView imageView14 = findViewById(R.id.imageView14);
                layout14.setOnTouchListener(MainActivity.this);
                layout14.setTag(cellNumber);
                textView14.setTag(cellNumber);
                textView14Label.setTag(cellNumber);
                textView14Label.setText(label);
                textView14.setText(value);
                if (icon !=null) {
                    imageView14.setImageDrawable(icon);
                } else {
                    imageView14.setImageResource(android.R.color.transparent);
                }
                break;
            case 15:
                if (gridChange) {
                    View gridCell15 = layoutInflater.inflate(R.layout.layout_griditem15, gridLayout, false);
                    gridLayout.addView(gridCell15);
                }
                ConstraintLayout layout15 = findViewById(R.id.layout_15);
                TextView textView15 = findViewById(R.id.textView15);
                TextView textView15Label = findViewById(R.id.textView15label);
                ImageView imageView15 = findViewById(R.id.imageView15);
                layout15.setOnTouchListener(MainActivity.this);
                layout15.setTag(cellNumber);
                textView15.setTag(cellNumber);
                textView15Label.setTag(cellNumber);
                textView15Label.setText(label);
                textView15.setText(value);
                if (icon !=null) {
                    imageView15.setImageDrawable(icon);
                } else {
                    imageView15.setImageResource(android.R.color.transparent);
                }
                break;
            default:
                break;
        }
        drawingComplete = true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ESCAPE:
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                goBack();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                goForward();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                goUp();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                goDown();
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

    //start timer function
    void startTimer() {
        if(!timerRunning) {
            cTimer = new CountDownTimer(10000, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    getSupportActionBar().hide();
                    timerRunning = false;
                }
            };
            timerRunning = true;
            cTimer.start();
        }
    }

    //cancel timer
    void cancelTimer() {
        if(cTimer!=null)
            cTimer.cancel();
    }

    //Go to next screen - Quick Tasks
    private void goForward(){
        Intent backIntent = new Intent(this, MusicActivity.class);
        if (sharedPrefs.getBoolean("prefDisplayDash", false)) {
            backIntent = new Intent(this, DashActivity.class);
        }
        startActivity(backIntent);
    }

    //Go back to last screen - Motorcycle Data
    private void goBack(){
        Intent backIntent = new Intent(this, com.blackboxembedded.WunderLINQ.TaskList.TaskActivity.class);
        startActivity(backIntent);
    }

    //Go up - Change grid count
    private void goUp(){
        int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT","15"));
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
            case 10:
                nextCellCount = 12;
                break;
            case 8:
                nextCellCount = 10;
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

    //Go down - Change grid count
    private void goDown(){
        int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT","15"));
        int nextCellCount = 1;
        SharedPreferences.Editor editor = sharedPrefs.edit();
        gridChange = true;
        switch (currentCellCount){
            case 15:
                nextCellCount = 12;
                break;
            case 12:
                nextCellCount = 10;
                break;
            case 10:
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
}
