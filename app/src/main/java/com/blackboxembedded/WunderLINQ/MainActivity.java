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

import static com.blackboxembedded.WunderLINQ.R.*;

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
import android.widget.Button;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;

import com.blackboxembedded.WunderLINQ.TaskList.TaskActivity;
import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.SoundManager;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "MainActivity";

    private LayoutInflater layoutInflater;

    private ImageButton faultButton;
    private ImageButton btButton;
    private GridLayout gridLayout;

    private final static int maxNumCells = 15;

    private int[] layoutIDs = new int[maxNumCells];
    private int[] valueTextViewIDs = new int[maxNumCells];
    private int[] headerLabelViewIDs = new int[maxNumCells];
    private int[] iconImageViewIDs = new int[maxNumCells];
    private MotorcycleData.DataType[] cellDataPref = new MotorcycleData.DataType[maxNumCells];
    GridItem[] cellsData = new GridItem[maxNumCells];

    private SharedPreferences sharedPrefs;

    private boolean _gridChange = true;

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
                if (cell < maxNumCells) {
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

        dailyDisclaimerWarning();

        ContextCompat.registerReceiver(this, mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);

        bluetoothLeService = new Intent(MainActivity.this, BluetoothLeService.class);
    }


    private void dailyDisclaimerWarning() {
        final int DISCLAIMER_COUNTDOWN = 15000; //MS to auto accept
        // Daily Disclaimer Warning
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        final String currentDate = sdf.format(new Date());
        String lastLaunch = sharedPrefs.getString("LAST_LAUNCH_DATE", "nodate");

        // Check if the disclaimer has already been shown today
        if (currentDate.equals(lastLaunch)) {
            return;
        }

        // Display the disclaimer dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.disclaimer_alert_title);
        builder.setMessage(R.string.disclaimer_alert_body);
        builder.setPositiveButton(R.string.disclaimer_ok, null); // Set null initially to handle clicks programmatically
        builder.setNegativeButton(R.string.disclaimer_quit,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // End the app
                        finishAffinity();
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();

        final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        final CountDownTimer countDownTimer = new CountDownTimer(DISCLAIMER_COUNTDOWN, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update button text with the remaining time
                final String positiveButtonWithCountdown = getString(R.string.disclaimer_ok) + "  ( " + millisUntilFinished / 1000 + " )";
                positiveButton.setText(positiveButtonWithCountdown);
            }

            @Override
            public void onFinish() {
                // Automatically click the positive button when the countdown finishes
                if (dialog.isShowing()) {
                    positiveButton.performClick();
                }
            }
        }.start();

        // Set the positive button's click listener
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the last launched date to today
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("LAST_LAUNCH_DATE", currentDate);
                editor.apply();

                countDownTimer.cancel();  // Cancel the countdown timer if button is clicked manually
                dialog.dismiss();  // Use dismiss() instead of cancel() to properly close the dialog
            }
        });
    }

    private void showCellSelector(int cell) {
        String[] prefStringKeys = {
                "prefCellOne", "prefCellTwo", "prefCellThree", "prefCellFour",
                "prefCellFive", "prefCellSix", "prefCellSeven", "prefCellEight",
                "prefCellNine", "prefCellTen", "prefCellEleven", "prefCellTwelve",
                "prefCellThirteen", "prefCellFourteen", "prefCellFifteen"
        };
        String prefStringKey = "";
        if (cell < prefStringKeys.length) {
            prefStringKey = prefStringKeys[cell ];
        } else {
            Log.d(TAG, "Invalid cell in showCellSelector");
        }

        final String selectedPrefStringKey = prefStringKey;
        if (!prefStringKey.isEmpty()) {
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
                    gridChange(true);
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
            ContextCompat.registerReceiver(this, mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
            if (mBluetoothLeService == null) {
                Log.d(TAG, "mBluetoothLeService is null");
                setupBLE();
            }
        } else {
            Log.d(TAG, "Running in the emulator");
        }

        getSupportActionBar().show();
        updateNightMode();
        gridChange(true);
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

                if (sharedPrefs.getString("prefPIPOrientation", "0").equals("0")) {
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
        if (isInPIPMode) {
            inPIP = true;
            //Hide your clickable components
            getSupportActionBar().hide();
        } else {
            inPIP = false;
            //Show your clickable components
            getSupportActionBar().show();
        }
        gridChange(true);
        updateDisplay();
        super.onPictureInPictureModeChanged(isInPIPMode);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "In onConfigChange");
        gridChange(true);
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
            gridChange(true);
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
                try {
                    // code runs in a thread


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ( MotorcycleData.wlq != null) {
                                if ( MotorcycleData.wlq.getHardwareType() == WLQ.TYPE_NAVIGATOR) {
                                    mMenu.findItem(id.action_bike_info).setVisible(true);
                                }
                                mMenu.findItem(id.action_hwsettings).setVisible(true);
                            }
                            //Check for active faults
                            if (!Faults.getAllActiveDesc().isEmpty()) {
                                faultButton.setVisibility(View.VISIBLE);
                            } else {
                                faultButton.setVisibility(View.GONE);
                            }


                            //Layout visible grid
                            if (gridChange()) {
                                reloadGridLayout();
                            }

                            // Update grid for visible cellsData
                            for (int cellNumber = 0; cellNumber < gridLayout.getChildCount(); cellNumber++) {
                                String cellValue = getCellContents(cellNumber);
                                cellsData[cellNumber] = GridItem.getCellData(cellDataPref[cellNumber]);

                                //Only draw contents to screen when changes occur to underlying data
                                if (cellValue.isBlank() || !cellsData[cellNumber].getValue().equals(cellValue)) {
                                    setCellContents(cellNumber);
                                }
                            }
                        }
                    });
                } catch (final Exception ex) {
                    Log.i(TAG, "Exception in thread");
                }
                drawingComplete = true;
            }
        }.start();
    }


    public GridItem getCellData(int dataPoint){
        return new GridItem(MotorcycleData.getIcon(dataPoint),
                MotorcycleData.getLabel(dataPoint),
                (!MotorcycleData.getValue(dataPoint).equals("")) ? MotorcycleData.getValue(dataPoint) : getString(R.string.blank_field));
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
        SoundManager.playSound(this, R.raw.directional);
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
        SoundManager.playSound(this, R.raw.directional);
        Intent backIntent = new Intent(this, com.blackboxembedded.WunderLINQ.TaskList.TaskActivity.class);
        if (MotorcycleData.wlq != null) {
            if (MotorcycleData.wlq.getStatus() != null) {
                backIntent = new Intent(this, AccessoryActivity.class);
            }
        }
        startActivity(backIntent);
    }

    //Go up - Change grid count
    private void goUp(){
        SoundManager.playSound(this, R.raw.directional);
        int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT","15"));
        int nextCellCount = 1;
        SharedPreferences.Editor editor = sharedPrefs.edit();

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

        gridChange(true);
        updateDisplay();
    }

    //Go down - Change grid count
    private void goDown(){
        SoundManager.playSound(this, R.raw.directional);
        int currentCellCount = Integer.parseInt(sharedPrefs.getString("CELL_COUNT","15"));
        int nextCellCount = 1;
        SharedPreferences.Editor editor = sharedPrefs.edit();

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

        gridChange(true);
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


    private void gridChange(boolean gridHasBeenChanged) {
        if (gridHasBeenChanged){
            gridLayout.removeAllViews();

            int[] layoutIDs = new int[maxNumCells];
            int[] valueTextViewIDs = new int[maxNumCells];
            int[] headerLabelViewIDs = new int[maxNumCells];
            int[] iconImageViewIDs = new int[maxNumCells];
            MotorcycleData.DataType[] cellDataPref = new MotorcycleData.DataType[maxNumCells];

            //Probably unnecessary but why not
            MemCache.invalidate();
        }
        _gridChange = gridHasBeenChanged;
    }

    private boolean gridChange() {
        return _gridChange;
    }



    private void reloadGridLayout() {
        //Use layout grid item1 as template and copy as needed
        final int templateLayoutResource = R.layout.layout_griditem1;
        final int templateLayoutID = id.layout_1;
        final int templateTextViewID = id.textView1;
        final int templateTextViewLabelID = id.textView1label;
        final int templateImageViewID = id.imageView1;


        int[] defaultCellData = {
                14, 29, 3, 0, 1,
                2, 20, 8, 9, 7,
                24, 28, 27, 23, 22
        };
        String[] numberWords = {
                "One", "Two", "Three", "Four", "Five",
                "Six", "Seven", "Eight", "Nine", "Ten",
                "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen"
        };


        final int numCellsPIP = 4;

        int visibleCells;
        if (inPIP) {
            visibleCells = Integer.parseInt(sharedPrefs.getString("prefPIPCellCount", String.valueOf(numCellsPIP)));
        } else {
            visibleCells = Integer.parseInt(sharedPrefs.getString("CELL_COUNT", String.valueOf(maxNumCells)));
        }

        gridLayout.removeAllViews();
        boolean portrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        switch (visibleCells) {
            case 15:
                if (portrait) {
                    gridLayout.setColumnCount(3);
                    gridLayout.setRowCount(5);
                } else {
                    gridLayout.setColumnCount(5);
                    gridLayout.setRowCount(3);
                }
                break;
            case 12:
                if (portrait) {
                    gridLayout.setColumnCount(3);
                    gridLayout.setRowCount(4);
                } else {
                    gridLayout.setColumnCount(4);
                    gridLayout.setRowCount(3);
                }
                break;
            case 10:
                if (portrait) {
                    gridLayout.setColumnCount(2);
                    gridLayout.setRowCount(5);
                } else {
                    gridLayout.setColumnCount(5);
                    gridLayout.setRowCount(2);
                }
                break;
            case 8:
                if (portrait) {
                    gridLayout.setColumnCount(2);
                    gridLayout.setRowCount(4);
                } else {
                    gridLayout.setColumnCount(4);
                    gridLayout.setRowCount(2);
                }
                break;
            case 4:
                if (portrait) {
                    gridLayout.setColumnCount(1);
                    gridLayout.setRowCount(4);
                } else {
                    gridLayout.setColumnCount(2);
                    gridLayout.setRowCount(2);
                }
                break;
            case 2:
                if (portrait) {
                    gridLayout.setColumnCount(1);
                    gridLayout.setRowCount(2);
                } else {
                    gridLayout.setColumnCount(2);
                    gridLayout.setRowCount(1);
                }
                break;
            case 1:
                gridLayout.removeAllViews();
                gridLayout.setColumnCount(1);
                gridLayout.setRowCount(1);

                break;
        }




        for (int cellNumber = 0; cellNumber < maxNumCells; cellNumber++) {
            String prefKey = "prefCell" + numberWords[cellNumber];
            cellDataPref[cellNumber] = MotorcycleData.DataType.values()[ Integer.parseInt(sharedPrefs.getString(prefKey, String.valueOf(defaultCellData[cellNumber]))) ];
        }


        for (int cellNumber= 0; cellNumber < visibleCells; cellNumber++) {
            //Create new cell from template
            View gridCell = layoutInflater.inflate(templateLayoutResource, gridLayout, false);
            gridLayout.addView(gridCell);

            //Get contents of cell using template IDs
            ConstraintLayout layout = findViewById(templateLayoutID);
            TextView valueTextView = findViewById(templateTextViewID);
            TextView headerLabelView = findViewById(templateTextViewLabelID);
            ImageView iconImageView = findViewById(templateImageViewID);


            //Set dynamic tags and IDs
            gridCell.setId (View.generateViewId());
            layout.setId(View.generateViewId());
            valueTextView.setId(View.generateViewId());
            headerLabelView.setId(View.generateViewId());
            iconImageView.setId(View.generateViewId());

            gridCell.setTag(cellNumber);
            layout.setTag(cellNumber);
            valueTextView.setTag(cellNumber);
            headerLabelView.setTag(cellNumber);
            iconImageView.setTag(cellNumber);

            //cache generated IDs in an array for later use
            layoutIDs[cellNumber] = layout.getId();
            valueTextViewIDs[cellNumber] = valueTextView.getId();
            headerLabelViewIDs[cellNumber] = headerLabelView.getId();
            iconImageViewIDs[cellNumber] = iconImageView.getId();
        }

        gridChange( false);
    }



    private void setCellContents(Integer cellNumber) {
        ConstraintLayout layout = findViewById(layoutIDs[cellNumber]);
        if (layout != null) {
            TextView valueTextView = findViewById(valueTextViewIDs[cellNumber]);
            TextView headerLabelView = findViewById(headerLabelViewIDs[cellNumber]);
            ImageView iconImageView = findViewById(iconImageViewIDs[cellNumber]);

            String label = cellsData[cellNumber].getLabel();
            String value = cellsData[cellNumber].getValue();
            Integer valueColor = cellsData[cellNumber].getValueColor();
            Drawable icon = cellsData[cellNumber].getIcon();


            headerLabelView.setText(label);

            valueTextView.setText(value);
            valueTextView.setSingleLine(false);
            if (value.contains("\n")) {
                valueTextView.setMaxLines(2);
            } else {
                valueTextView.setMaxLines(1);
            }

            if (valueColor == null) valueColor = valueTextView.getTextColors().getDefaultColor();
            valueTextView.setTextColor(valueColor);

            if (icon != null) {
                iconImageView.setImageDrawable(icon);
            } else {
                iconImageView.setImageResource(android.R.color.transparent);
            }

            layout.setOnTouchListener(MainActivity.this);
        }

    }

    private String getCellContents(int cellNumber) {
        String retValue = "";

        if (gridLayout.getChildCount() > cellNumber) {
            retValue = (String)((TextView) findViewById(valueTextViewIDs[cellNumber])).getText();
        }

        return retValue;
    }
}
