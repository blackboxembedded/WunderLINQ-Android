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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;

import com.blackboxembedded.WunderLINQ.TaskList.TaskActivity;
import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;


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

    private String mDeviceAddress;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_BLUETOOTH_CONNECT = 106;
    private static final int SETTINGS_CHECK = 10;

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
        switch (orientation) {
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

        gestureDetector = new GestureDetectorListener(this) {

            @Override
            public void onPressLong() {
                if (cell >= 1 && cell <= 15) {
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
            Log.d(TAG, "BLE Not supported, Brand: " + Build.BRAND + ", Device: " + Build.DEVICE);
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "mBluetoothAdapter == null");
            return;
        }

        // Daily Disclaimer Warning
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        final String currentDate = sdf.format(new Date());
        if (sharedPrefs.getString("LAST_LAUNCH_DATE", "nodate").contains(currentDate)) {
            // Date matches. User has already Launched the app once today. So do nothing.
        } else {
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

        registerReceiver(mBondingBroadcast, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        bluetoothLeService = new Intent(MainActivity.this, BluetoothLeService.class);
    }

    private void showCellSelector(int cell) {
        String prefStringKey = "";
        switch (cell) {
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

    private void showActionBar() {
        View v = layoutInflater.inflate(R.layout.actionbar_nav_main, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
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
        mMenu.findItem(R.id.action_bike_info).setVisible(false);
        mMenu.findItem(R.id.action_hwsettings).setVisible(false);
        mPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
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
            switch (v.getId()) {
                case R.id.action_connect:
                    Log.d(TAG, "Connect");
                    if (!(Build.BRAND.startsWith("google") && Build.DEVICE.startsWith("generic"))) {
                        setupBLE();
                    } else {
                        Log.d(TAG, "Running in the emulator");
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
        Log.d(TAG, "In onResume");
        super.onResume();
        //Only use BLE if on a real device
        if (!(Build.BRAND.startsWith("google") && Build.DEVICE.startsWith("generic"))) {
            startService(bluetoothLeService);
            registerReceiver(mBondingBroadcast, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            if (mBluetoothLeService == null) {
                Log.d(TAG, "mBluetoothLeService is null");
                setupBLE();
            }
        } else {
            Log.d(TAG, "Running in the emulator");
        }

        getSupportActionBar().show();
        updateNightMode();
        updateDisplay();
        startTimer();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "In onDestroy");
        super.onDestroy();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.toString());
        }
        mBluetoothLeService = null;
    }

    @Override
    public void onStop() {
        Log.d(TAG, "In onStop");
        super.onStop();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.toString());
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "In onPause");
        super.onPause();
        cancelTimer();
        try {
            if (!sharedPrefs.getBoolean("prefPIP", false)) {
                unregisterReceiver(mGattUpdateReceiver);
            }
            unregisterReceiver(mBondingBroadcast);
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void onUserLeaveHint() {
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
                try {
                    PictureInPictureParams params = new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(pipWidth, pipHeight)).build();
                    enterPictureInPictureMode(params);
                } catch (IllegalStateException e){
                    Log.d(TAG,"PiP Not Supported at this time: " + e);
                }
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
        Log.d(TAG, "In onConfigChange");
        gridChange = true;
        updateDisplay();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
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
        Log.d(TAG, "In setupBLE()");
        int wlqCnt = 0;
        boolean blePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                blePermission = false;
                //TODO Request Permission
                Log.d(TAG, "NO BLUETOOTH_CONNECT Permission");
            }
        }
        if (blePermission) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (!pairedDevices.isEmpty()) {
                for (BluetoothDevice devices : pairedDevices) {
                    if (devices.getName() != null) {
                        if (devices.getName().contains(getString(R.string.device_name))) {
                            wlqCnt = wlqCnt + 1;
                            Log.d(TAG, "Previously Paired WunderLINQ: " + devices.getAddress());
                            mDeviceAddress = devices.getAddress();
                        }
                    }
                }
            }
            if (wlqCnt == 0) {
                Log.d(TAG, "No paired WunderLINQ");
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
            } else if (wlqCnt == 1) {
                Log.d(TAG, "Connecting to Address: " + mDeviceAddress);
                bindService(bluetoothLeService, mServiceConnection, BIND_AUTO_CREATE);
            } else if (wlqCnt > 1) {
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
        } else {
            // Display A message about the need of Bluetooth Connect Permissions
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.negative_alert_title));
            builder.setMessage(getString(R.string.btconnect_alert_body));
            builder.setPositiveButton(R.string.alert_btn_ok,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    Log.d(TAG, "Requesting BT_CONNECT permission");
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_BLUETOOTH_CONNECT);
                                }
                            }
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
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                btButton.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.motorrad_red));
                btButton.setEnabled(true);
                mMenu.findItem(R.id.action_bike_info).setVisible(false);
                mMenu.findItem(R.id.action_hwsettings).setVisible(false);
                updateDisplay();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                btButton.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.motorrad_blue));
                btButton.setEnabled(false);
                mMenu.findItem(R.id.action_bike_info).setVisible(true);
                mMenu.findItem(R.id.action_hwsettings).setVisible(true);
            } else if (BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE.equals(action)) {
                if (drawingComplete) {
                    updateDisplay();
                }
            } else if (BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE.equals(action)) {
                Intent accessoryIntent = new Intent(MainActivity.this, AccessoryActivity.class);
                startActivity(accessoryIntent);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE);
        return intentFilter;
    }

    //Update Night Mode
    private void updateNightMode() {
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        int prefNightMode = Integer.parseInt(sharedPrefs.getString("prefNightModeCombo", "3"));

        switch (prefNightMode) {
            case 0:
                //Off
                if (currentNightMode != Configuration.UI_MODE_NIGHT_NO) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                break;
            case 1:
                //On
                if (currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                break;
            case 3:
                //Android
                if (currentNightMode != Configuration.UI_MODE_NIGHT_UNDEFINED) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                break;
            default:
                //
                break;
        }
    }

    // Update Display
    private void updateDisplay() {
        drawingComplete = false;
        new Thread() {
            @Override
            public void run() {
                // Cell One
                int cell1Data = Integer.parseInt(sharedPrefs.getString("prefCellOne", "14"));//Default:Speed
                GridItem cell1 = getCellData(cell1Data);
                // Cell Two
                int cell2Data = Integer.parseInt(sharedPrefs.getString("prefCellTwo", "29"));//Default:RPM
                GridItem cell2 = getCellData(cell2Data);
                // Cell Three
                int cell3Data = Integer.parseInt(sharedPrefs.getString("prefCellThree", "3"));//Default:Speed
                GridItem cell3 = getCellData(cell3Data);
                // Cell Four
                int cell4Data = Integer.parseInt(sharedPrefs.getString("prefCellFour", "0"));//Default:Gear
                GridItem cell4 = getCellData(cell4Data);
                // Cell Five
                int cell5Data = Integer.parseInt(sharedPrefs.getString("prefCellFive", "1"));//Default:Engine Temp
                GridItem cell5 = getCellData(cell5Data);
                // Cell Six
                int cell6Data = Integer.parseInt(sharedPrefs.getString("prefCellSix", "2"));//Default:Air Temp
                GridItem cell6 = getCellData(cell6Data);
                // Cell Seven
                int cell7Data = Integer.parseInt(sharedPrefs.getString("prefCellSeven", "20"));//Default:Shifts
                GridItem cell7 = getCellData(cell7Data);
                // Cell Eight
                int cell8Data = Integer.parseInt(sharedPrefs.getString("prefCellEight", "8"));//Default:Front Brakes
                GridItem cell8 = getCellData(cell8Data);
                // Cell Nine
                int cell9Data = Integer.parseInt(sharedPrefs.getString("prefCellNine", "9"));//Default:Rear Brakes
                GridItem cell9 = getCellData(cell9Data);
                // Cell Ten
                int cell10Data = Integer.parseInt(sharedPrefs.getString("prefCellTen", "7"));//Default:Throttle
                GridItem cell10 = getCellData(cell10Data);
                // Cell Eleven
                int cell11Data = Integer.parseInt(sharedPrefs.getString("prefCellEleven", "24"));//Default:time
                GridItem cell11 = getCellData(cell11Data);
                // Cell Twelve
                int cell12Data = Integer.parseInt(sharedPrefs.getString("prefCellTwelve", "28"));//Default:Sunrise/Sunset
                GridItem cell12 = getCellData(cell12Data);
                // Cell Thirteen
                int cell13Data = Integer.parseInt(sharedPrefs.getString("prefCellThirteen", "27"));//Default:Altitude
                GridItem cell13 = getCellData(cell13Data);
                // Cell Fourteen
                int cell14Data = Integer.parseInt(sharedPrefs.getString("prefCellFourteen", "23"));//Default:Bearing
                GridItem cell14 = getCellData(cell14Data);
                // Cell Fifteen
                int cell15Data = Integer.parseInt(sharedPrefs.getString("prefCellFifteen", "22"));//Default:g-force
                GridItem cell15 = getCellData(cell15Data);

                int count = Integer.parseInt(sharedPrefs.getString("CELL_COUNT", "15"));
                if (inPIP) {
                    count = Integer.parseInt(sharedPrefs.getString("prefPIPCellCount", "4"));
                }
                boolean portrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
                try {
                    // code runs in a thread
                    int finalCount = count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(Data.wlq != null) {
                                if(Data.wlq.getHardwareType() == WLQ.TYPE_NAVIGATOR) {
                                    mMenu.findItem(R.id.action_bike_info).setVisible(true);
                                }
                                mMenu.findItem(R.id.action_hwsettings).setVisible(true);
                            }
                            //Check for active faults
                            if (!FaultStatus.getallActiveDesc().isEmpty()) {
                                faultButton.setVisibility(View.VISIBLE);
                            } else {
                                faultButton.setVisibility(View.GONE);
                            }
                            switch (finalCount) {
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
                                    setCellText(1);
                                    // Cell Two
                                    setCellText(2);
                                    // Cell Three
                                    setCellText(3);
                                    // Cell Four
                                    setCellText(4);
                                    // Cell Five
                                    setCellText(5);
                                    // Cell Six
                                    setCellText(6);
                                    // Cell Seven
                                    setCellText(7);
                                    // Cell Eight
                                    setCellText(8);
                                    // Cell Nine
                                    setCellText(9);
                                    // Cell Ten
                                    setCellText(10);
                                    // Cell Eleven
                                    setCellText(11);
                                    // Cell Twelve
                                    setCellText(12);
                                    // Cell Thirteen
                                    setCellText(13);
                                    // Cell Fourteen
                                    setCellText(14);
                                    // Cell Fifteen
                                    setCellText(15);
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
                                    setCellText(1);
                                    // Cell Two
                                    setCellText(2);
                                    // Cell Three
                                    setCellText(3);
                                    // Cell Four
                                    setCellText(4);
                                    // Cell Five
                                    setCellText(5);
                                    // Cell Six
                                    setCellText(6);
                                    // Cell Seven
                                    setCellText(7);
                                    // Cell Eight
                                    setCellText(8);
                                    // Cell Nine
                                    setCellText(9);
                                    // Cell Ten
                                    setCellText(10);
                                    // Cell Eleven
                                    setCellText(11);
                                    // Cell Twelve
                                    setCellText(12);
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
                                    setCellText(1);
                                    // Cell Two
                                    setCellText(2);
                                    // Cell Three
                                    setCellText(3);
                                    // Cell Four
                                    setCellText(4);
                                    // Cell Five
                                    setCellText(5);
                                    // Cell Six
                                    setCellText(6);
                                    // Cell Seven
                                    setCellText(7);
                                    // Cell Eight
                                    setCellText(8);
                                    // Cell Nine
                                    setCellText(9);
                                    // Cell Ten
                                    setCellText(10);
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
                                    setCellText(1);
                                    // Cell Two
                                    setCellText(2);
                                    // Cell Three
                                    setCellText(3);
                                    // Cell Four
                                    setCellText(4);
                                    // Cell Five
                                    setCellText(5);
                                    // Cell Six
                                    setCellText(6);
                                    // Cell Seven
                                    setCellText(7);
                                    // Cell Eight
                                    setCellText(8);
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
                                    setCellText(1);
                                    // Cell Two
                                    setCellText(2);
                                    // Cell Three
                                    setCellText(3);
                                    // Cell Four
                                    setCellText(4);
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
                                    setCellText(1);
                                    // Cell Two
                                    setCellText(2);
                                    gridChange = false;
                                    break;
                                case 1:
                                    if (gridChange) {
                                        gridLayout.removeAllViews();
                                        gridLayout.setColumnCount(1);
                                        gridLayout.setRowCount(1);
                                    }
                                    // Cell One
                                    setCellText(1);
                                    gridChange = false;
                                    break;
                            }
                        }

                        // Set Cell Text and icon
                        private void setCellText(Integer cellNumber) {
                            switch (cellNumber) {
                                case 1:
                                    if (gridChange) {
                                        View gridCell1 = layoutInflater.inflate(R.layout.layout_griditem1, gridLayout, false);
                                        gridLayout.addView(gridCell1);
                                    }
                                    ConstraintLayout layout1 = findViewById(R.id.layout_1);
                                    if (layout1 != null) {
                                        TextView textView1 = findViewById(R.id.textView1);
                                        TextView textView1Label = findViewById(R.id.textView1label);
                                        ImageView imageView1 = findViewById(R.id.imageView1);
                                        layout1.setOnTouchListener(MainActivity.this);
                                        layout1.setTag(cellNumber);
                                        textView1.setTag(cellNumber);
                                        textView1Label.setTag(cellNumber);
                                        textView1Label.setText(cell1.getLabel());
                                        textView1.setText(cell1.getValue());
                                        if (cell1.getIcon() != null) {
                                            imageView1.setImageDrawable(cell1.getIcon());
                                        } else {
                                            imageView1.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 2:
                                    if (gridChange) {
                                        View gridCell2 = layoutInflater.inflate(R.layout.layout_griditem2, gridLayout, false);
                                        gridLayout.addView(gridCell2);
                                    }
                                    ConstraintLayout layout2 = findViewById(R.id.layout_2);
                                    if (layout2 != null) {
                                        TextView textView2 = findViewById(R.id.textView2);
                                        TextView textView2Label = findViewById(R.id.textView2label);
                                        ImageView imageView2 = findViewById(R.id.imageView2);
                                        layout2.setOnTouchListener(MainActivity.this);
                                        layout2.setTag(cellNumber);
                                        textView2.setTag(cellNumber);
                                        textView2Label.setTag(cellNumber);
                                        textView2Label.setText(cell2.getLabel());
                                        textView2.setText(cell2.getValue());
                                        if (cell2.getIcon() != null) {
                                            imageView2.setImageDrawable(cell2.getIcon());
                                        } else {
                                            imageView2.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 3:
                                    if (gridChange) {
                                        View gridCell3 = layoutInflater.inflate(R.layout.layout_griditem3, gridLayout, false);
                                        gridLayout.addView(gridCell3);
                                    }
                                    ConstraintLayout layout3 = findViewById(R.id.layout_3);
                                    if (layout3 != null) {
                                        TextView textView3 = findViewById(R.id.textView3);
                                        TextView textView3Label = findViewById(R.id.textView3label);
                                        ImageView imageView3 = findViewById(R.id.imageView3);
                                        layout3.setOnTouchListener(MainActivity.this);
                                        layout3.setTag(cellNumber);
                                        textView3.setTag(cellNumber);
                                        textView3Label.setTag(cellNumber);
                                        textView3Label.setText(cell3.getLabel());
                                        textView3.setText(cell3.getValue());
                                        if (cell3.getIcon() != null) {
                                            imageView3.setImageDrawable(cell3.getIcon());
                                        } else {
                                            imageView3.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 4:
                                    if (gridChange) {
                                        View gridCell4 = layoutInflater.inflate(R.layout.layout_griditem4, gridLayout, false);
                                        gridLayout.addView(gridCell4);
                                    }
                                    ConstraintLayout layout4 = findViewById(R.id.layout_4);
                                    if (layout4 != null) {
                                        TextView textView4 = findViewById(R.id.textView4);
                                        TextView textView4Label = findViewById(R.id.textView4label);
                                        ImageView imageView4 = findViewById(R.id.imageView4);
                                        layout4.setOnTouchListener(MainActivity.this);
                                        layout4.setTag(cellNumber);
                                        textView4.setTag(cellNumber);
                                        textView4Label.setTag(cellNumber);
                                        textView4Label.setText(cell4.getLabel());
                                        textView4.setText(cell4.getValue());
                                        if (cell4.getIcon() != null) {
                                            imageView4.setImageDrawable(cell4.getIcon());
                                        } else {
                                            imageView4.setImageResource(android.R.color.transparent);
                                        }
                                        break;
                                    }
                                case 5:
                                    if (gridChange) {
                                        View gridCell5 = layoutInflater.inflate(R.layout.layout_griditem5, gridLayout, false);
                                        gridLayout.addView(gridCell5);
                                    }
                                    ConstraintLayout layout5 = findViewById(R.id.layout_5);
                                    if (layout5 != null) {
                                        TextView textView5 = findViewById(R.id.textView5);
                                        TextView textView5Label = findViewById(R.id.textView5label);
                                        ImageView imageView5 = findViewById(R.id.imageView5);
                                        layout5.setOnTouchListener(MainActivity.this);
                                        layout5.setTag(cellNumber);
                                        textView5.setTag(cellNumber);
                                        textView5Label.setTag(cellNumber);
                                        textView5Label.setText(cell5.getLabel());
                                        textView5.setText(cell5.getValue());
                                        if (cell5.getIcon() != null) {
                                            imageView5.setImageDrawable(cell5.getIcon());
                                        } else {
                                            imageView5.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 6:
                                    if (gridChange) {
                                        View gridCell6 = layoutInflater.inflate(R.layout.layout_griditem6, gridLayout, false);
                                        gridLayout.addView(gridCell6);
                                    }
                                    ConstraintLayout layout6 = findViewById(R.id.layout_6);
                                    if (layout6 != null) {
                                        TextView textView6 = findViewById(R.id.textView6);
                                        TextView textView6Label = findViewById(R.id.textView6label);
                                        ImageView imageView6 = findViewById(R.id.imageView6);
                                        layout6.setOnTouchListener(MainActivity.this);
                                        layout6.setTag(cellNumber);
                                        textView6.setTag(cellNumber);
                                        textView6Label.setTag(cellNumber);
                                        textView6Label.setText(cell6.getLabel());
                                        textView6.setText(cell6.getValue());
                                        if (cell6.getIcon() != null) {
                                            imageView6.setImageDrawable(cell6.getIcon());
                                        } else {
                                            imageView6.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 7:
                                    if (gridChange) {
                                        View gridCell7 = layoutInflater.inflate(R.layout.layout_griditem7, gridLayout, false);
                                        gridLayout.addView(gridCell7);
                                    }
                                    ConstraintLayout layout7 = findViewById(R.id.layout_7);
                                    if (layout7 != null) {
                                        TextView textView7 = findViewById(R.id.textView7);
                                        TextView textView7Label = findViewById(R.id.textView7label);
                                        ImageView imageView7 = findViewById(R.id.imageView7);
                                        layout7.setOnTouchListener(MainActivity.this);
                                        layout7.setTag(cellNumber);
                                        textView7.setTag(cellNumber);
                                        textView7Label.setTag(cellNumber);
                                        textView7Label.setText(cell7.getLabel());
                                        textView7.setText(cell7.getValue());
                                        if (cell7.getIcon() != null) {
                                            imageView7.setImageDrawable(cell7.getIcon());
                                        } else {
                                            imageView7.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 8:
                                    if (gridChange) {
                                        View gridCell8 = layoutInflater.inflate(R.layout.layout_griditem8, gridLayout, false);
                                        gridLayout.addView(gridCell8);
                                    }
                                    ConstraintLayout layout8 = findViewById(R.id.layout_8);
                                    if (layout8 != null) {
                                        TextView textView8 = findViewById(R.id.textView8);
                                        TextView textView8Label = findViewById(R.id.textView8label);
                                        ImageView imageView8 = findViewById(R.id.imageView8);
                                        layout8.setOnTouchListener(MainActivity.this);
                                        layout8.setTag(cellNumber);
                                        textView8.setTag(cellNumber);
                                        textView8Label.setTag(cellNumber);
                                        textView8Label.setText(cell8.getLabel());
                                        textView8.setText(cell8.getValue());
                                        if (cell8.getIcon() != null) {
                                            imageView8.setImageDrawable(cell8.getIcon());
                                        } else {
                                            imageView8.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 9:
                                    if (gridChange) {
                                        View gridCell9 = layoutInflater.inflate(R.layout.layout_griditem9, gridLayout, false);
                                        gridLayout.addView(gridCell9);
                                    }
                                    ConstraintLayout layout9 = findViewById(R.id.layout_9);
                                    if (layout9 != null) {
                                        TextView textView9 = findViewById(R.id.textView9);
                                        TextView textView9Label = findViewById(R.id.textView9label);
                                        ImageView imageView9 = findViewById(R.id.imageView9);
                                        layout9.setOnTouchListener(MainActivity.this);
                                        layout9.setTag(cellNumber);
                                        textView9.setTag(cellNumber);
                                        textView9Label.setTag(cellNumber);
                                        textView9Label.setText(cell9.getLabel());
                                        textView9.setText(cell9.getValue());
                                        if (cell9.getIcon() != null) {
                                            imageView9.setImageDrawable(cell9.getIcon());
                                        } else {
                                            imageView9.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 10:
                                    if (gridChange) {
                                        View gridCell10 = layoutInflater.inflate(R.layout.layout_griditem10, gridLayout, false);
                                        gridLayout.addView(gridCell10);
                                    }
                                    ConstraintLayout layout10 = findViewById(R.id.layout_10);
                                    if (layout10 != null) {
                                        TextView textView10 = findViewById(R.id.textView10);
                                        TextView textView10Label = findViewById(R.id.textView10label);
                                        ImageView imageView10 = findViewById(R.id.imageView10);
                                        layout10.setOnTouchListener(MainActivity.this);
                                        layout10.setTag(cellNumber);
                                        textView10.setTag(cellNumber);
                                        textView10Label.setTag(cellNumber);
                                        textView10Label.setText(cell10.getLabel());
                                        textView10.setText(cell10.getValue());
                                        if (cell10.getIcon() != null) {
                                            imageView10.setImageDrawable(cell10.getIcon());
                                        } else {
                                            imageView10.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 11:
                                    if (gridChange) {
                                        View gridCell11 = layoutInflater.inflate(R.layout.layout_griditem11, gridLayout, false);
                                        gridLayout.addView(gridCell11);
                                    }
                                    ConstraintLayout layout11 = findViewById(R.id.layout_11);
                                    if (layout11 != null) {
                                        TextView textView11 = findViewById(R.id.textView11);
                                        TextView textView11Label = findViewById(R.id.textView11label);
                                        ImageView imageView11 = findViewById(R.id.imageView11);
                                        layout11.setOnTouchListener(MainActivity.this);
                                        layout11.setTag(cellNumber);
                                        textView11.setTag(cellNumber);
                                        textView11Label.setTag(cellNumber);
                                        textView11Label.setText(cell11.getLabel());
                                        textView11.setText(cell11.getValue());
                                        if (cell11.getIcon() != null) {
                                            imageView11.setImageDrawable(cell11.getIcon());
                                        } else {
                                            imageView11.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 12:
                                    if (gridChange) {
                                        View gridCell12 = layoutInflater.inflate(R.layout.layout_griditem12, gridLayout, false);
                                        gridLayout.addView(gridCell12);
                                    }
                                    ConstraintLayout layout12 = findViewById(R.id.layout_12);
                                    if (layout12 != null) {
                                        TextView textView12 = findViewById(R.id.textView12);
                                        TextView textView12Label = findViewById(R.id.textView12label);
                                        ImageView imageView12 = findViewById(R.id.imageView12);
                                        layout12.setOnTouchListener(MainActivity.this);
                                        layout12.setTag(cellNumber);
                                        textView12.setTag(cellNumber);
                                        textView12Label.setTag(cellNumber);
                                        textView12Label.setText(cell12.getLabel());
                                        textView12.setText(cell12.getValue());
                                        if (cell12.getIcon() != null) {
                                            imageView12.setImageDrawable(cell12.getIcon());
                                        } else {
                                            imageView12.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 13:
                                    if (gridChange) {
                                        View gridCell13 = layoutInflater.inflate(R.layout.layout_griditem13, gridLayout, false);
                                        gridLayout.addView(gridCell13);
                                    }
                                    ConstraintLayout layout13 = findViewById(R.id.layout_13);
                                    if (layout13 != null) {
                                        TextView textView13 = findViewById(R.id.textView13);
                                        TextView textView13Label = findViewById(R.id.textView13label);
                                        ImageView imageView13 = findViewById(R.id.imageView13);
                                        layout13.setOnTouchListener(MainActivity.this);
                                        layout13.setTag(cellNumber);
                                        textView13.setTag(cellNumber);
                                        textView13Label.setTag(cellNumber);
                                        textView13Label.setText(cell13.getLabel());
                                        textView13.setText(cell13.getValue());
                                        if (cell13.getIcon() != null) {
                                            imageView13.setImageDrawable(cell13.getIcon());
                                        } else {
                                            imageView13.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 14:
                                    if (gridChange) {
                                        View gridCell14 = layoutInflater.inflate(R.layout.layout_griditem14, gridLayout, false);
                                        gridLayout.addView(gridCell14);
                                    }
                                    ConstraintLayout layout14 = findViewById(R.id.layout_14);
                                    if (layout14 != null) {
                                        TextView textView14 = findViewById(R.id.textView14);
                                        TextView textView14Label = findViewById(R.id.textView14label);
                                        ImageView imageView14 = findViewById(R.id.imageView14);
                                        layout14.setOnTouchListener(MainActivity.this);
                                        layout14.setTag(cellNumber);
                                        textView14.setTag(cellNumber);
                                        textView14Label.setTag(cellNumber);
                                        textView14Label.setText(cell14.getLabel());
                                        textView14.setText(cell14.getValue());
                                        if (cell14.getIcon() != null) {
                                            imageView14.setImageDrawable(cell14.getIcon());
                                        } else {
                                            imageView14.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                case 15:
                                    if (gridChange) {
                                        View gridCell15 = layoutInflater.inflate(R.layout.layout_griditem15, gridLayout, false);
                                        gridLayout.addView(gridCell15);
                                    }
                                    ConstraintLayout layout15 = findViewById(R.id.layout_15);
                                    if (layout15 != null) {
                                        TextView textView15 = findViewById(R.id.textView15);
                                        TextView textView15Label = findViewById(R.id.textView15label);
                                        ImageView imageView15 = findViewById(R.id.imageView15);
                                        layout15.setOnTouchListener(MainActivity.this);
                                        layout15.setTag(cellNumber);
                                        textView15.setTag(cellNumber);
                                        textView15Label.setTag(cellNumber);
                                        textView15Label.setText(cell15.getLabel());
                                        textView15.setText(cell15.getValue());
                                        if (cell15.getIcon() != null) {
                                            imageView15.setImageDrawable(cell15.getIcon());
                                        } else {
                                            imageView15.setImageResource(android.R.color.transparent);
                                        }
                                    }
                                    break;
                                default:
                                    break;
                            }
                            drawingComplete = true;
                        }
                    });
                } catch (final Exception ex) {
                    Log.i(TAG, "Exception in thread");
                }
            }
        }.start();
    }

    public GridItem getCellData(int dataPoint){
        return new GridItem(Data.getIcon(dataPoint),
                Data.getLabel(dataPoint),
                (!Data.getValue(dataPoint).equals("")) ? Data.getValue(dataPoint) : getString(R.string.blank_field));
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
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                goUp();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
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
        if (sharedPrefs.getBoolean("prefHideNavBar", true)) {
            if (!timerRunning) {
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
    }

    //cancel timer
    void cancelTimer() {
        if(cTimer!=null)
            cTimer.cancel();
    }

    //Go to next screen
    private void goForward(){
        Intent forwardIntent = new Intent(this, TaskActivity.class);
        if (sharedPrefs.getBoolean("prefDisplayDash", false)) {
            forwardIntent = new Intent(this, DashActivity.class);
        } else if (sharedPrefs.getBoolean("prefDisplayMusic", false)) {
            forwardIntent = new Intent(this, MusicActivity.class);
        }
        startActivity(forwardIntent);
    }

    //Go previous screen
    private void goBack(){
        Intent backIntent = new Intent(this, com.blackboxembedded.WunderLINQ.TaskList.TaskActivity.class);
        if (Data.wlq != null) {
            if (Data.wlq.getStatus() != null) {
                backIntent = new Intent(this, AccessoryActivity.class);
            }
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_BLUETOOTH_CONNECT: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "BLUETOOTH_CONNECT permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_btconnect_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                break;
            }
            default:
                Log.d(TAG, "Unknown Permissions Request Code");
                break;
        }
    }
}

class GridItem {
    private String label;
    private String value;
    private Drawable icon;

    public GridItem(Drawable icon, String label, String value) {
        this.icon = icon;
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }
    public String getValue() {
        return value;
    }
    public Drawable getIcon() {
        return icon;
    }
}