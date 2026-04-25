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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blackboxembedded.WunderLINQ.SVGDashboards.ADVDashboard;
import com.blackboxembedded.WunderLINQ.SVGDashboards.SportDashboard;
import com.blackboxembedded.WunderLINQ.SVGDashboards.StandardDashboard;
import com.blackboxembedded.WunderLINQ.SVGDashboards.SvgFileResolver;
import com.blackboxembedded.WunderLINQ.TaskList.TaskActivity;
import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.SoundManager;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;

public class DashActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "DashActivity";

    private SharedPreferences sharedPrefs;
    private ImageButton faultButton;

    private SVGImageView dashboardView;
    private SvgFileResolver svgFileResolver;
    private GestureDetectorListener gestureDetector;
    private CountDownTimer cTimer = null;
    private boolean timerRunning = false;
    private boolean dashUpdateRunning = false;
    private long lastUpdate = 0;

    private final int numDashboard = 3;
    private final int numInfoLine = 4;
    private int currentDashboard = 1;
    private int currentInfoLine = 1;

    private HandlerThread dashUpdateThread;
    private Handler dashUpdateHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentDashboard = sharedPrefs.getInt("lastDashboard",1);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());

        View view = findViewById(R.id.layout_dash);

        gestureDetector = new GestureDetectorListener(this){
            @Override
            public void onPressLong() {
                nextDashboard();
            }

            @Override
            public void onSwipeUp() {
                nextInfoLine();
            }

            @Override
            public void onSwipeDown() {
                prevInfoLine();
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

        String orientation = sharedPrefs.getString("prefOrientation", "0");
        if (!orientation.equals("0")){
            if(orientation.equals("1")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if (orientation.equals("2")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

        showActionBar();

        dashboardView = findViewById(R.id.mainView);
        svgFileResolver = new SvgFileResolver();

        dashUpdateThread = new HandlerThread("DashUpdateThread");
        dashUpdateThread.start();
        dashUpdateHandler = new Handler(dashUpdateThread.getLooper());

        updateDisplay();
    }

    private boolean isReceiverRegistered = false;

    @Override
    public void onResume() {
        super.onResume();
        getSupportActionBar().show();
        currentDashboard = sharedPrefs.getInt("lastDashboard", 1);
        updateDisplay();
        startTimer();

        if (!isReceiverRegistered) {
            ContextCompat.registerReceiver(this, mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
            isReceiverRegistered = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("lastDashboard", currentDashboard);
        editor.apply();
        cleanup();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
        if (dashUpdateThread != null) {
            dashUpdateThread.quitSafely();
        }
    }

    private void cleanup() {
        cancelTimer();
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(mGattUpdateReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Receiver not registered", e);
            }
            isReceiverRegistered = false;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        getSupportActionBar().show();
        startTimer();
        gestureDetector.onTouch(v, event);
        return true;
    }

    private void showActionBar(){
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.dash_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        faultButton = findViewById(R.id.action_faults);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
        faultButton.setOnClickListener(mClickListener);

        //Check for active faults
        if (!Faults.getAllActiveDesc().isEmpty()) {
            faultButton.setVisibility(View.VISIBLE);
        } else {
            faultButton.setVisibility(View.GONE);
        }
    }

    private final View.OnClickListener mClickListener = v -> {
        int id = v.getId();
        if (id == R.id.action_back) {
            goBack();
        } else if (id == R.id.action_forward) {
            goForward();
        } else if (id == R.id.action_faults) {
            Intent faultIntent = new Intent(DashActivity.this, FaultActivity.class);
            startActivity(faultIntent);
        }
    };

    //Go to next screen - Quick Tasks
    private void goForward(){
        SoundManager.playSound(this, R.raw.directional);
        Intent forwardIntent = new Intent(this, TaskActivity.class);
        if (sharedPrefs.getBoolean("prefDisplayMusic", false)) {
            forwardIntent = new Intent(this, MusicActivity.class);
        }
        startActivity(forwardIntent);
    }

    //Go back to last screen - Motorcycle Data
    private void goBack(){
        SoundManager.playSound(this, R.raw.directional);
        Intent backIntent = new Intent(this, MainActivity.class);
        startActivity(backIntent);
    }

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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER -> {
                nextDashboard();
                yield true;
            }
            case KeyEvent.KEYCODE_ESCAPE -> {
                prevDashboard();
                yield true;
            }
            case KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD -> {
                nextInfoLine();
                yield true;
            }
            case KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MINUS,
                 KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> {
                prevInfoLine();
                yield true;
            }
            case KeyEvent.KEYCODE_DPAD_LEFT -> {
                goBack();
                yield true;
            }
            case KeyEvent.KEYCODE_DPAD_RIGHT -> {
                goForward();
                yield true;
            }
            default -> super.onKeyUp(keyCode, event);
        };
    }

    void nextDashboard(){
        SoundManager.playSound(this, R.raw.enter);
        if (currentDashboard == numDashboard){
            currentDashboard = 1;
        } else {
            currentDashboard = currentDashboard + 1;
        }
        updateDisplay();
    }

    void prevDashboard(){
        SoundManager.playSound(this, R.raw.enter);
        if (currentDashboard == 1){
            currentDashboard = numDashboard;
        } else {
            currentDashboard = currentDashboard - 1;
        }
        updateDisplay();
    }

    void nextInfoLine(){
        SoundManager.playSound(this, R.raw.directional);
        if (currentInfoLine == numInfoLine){
            currentInfoLine = 1;
        } else {
            currentInfoLine = currentInfoLine + 1;
        }
        updateDisplay();
    }

    void prevInfoLine(){
        SoundManager.playSound(this, R.raw.directional);
        if (currentInfoLine == 1){
            currentInfoLine = numInfoLine;
        } else {
            currentInfoLine = currentInfoLine - 1;
        }
        updateDisplay();
    }

    //start timer function
    void startTimer() {
        if (sharedPrefs.getBoolean("prefHideNavBar", false)) {
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

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_PERFORMANCE_DATA_AVAILABLE.equals(action)) {
                if ((System.currentTimeMillis()) - lastUpdate > 500) {
                    lastUpdate = System.currentTimeMillis();
                    updateDisplay();
                }
            } else if (BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE.equals(action)) {
                Intent accessoryIntent = new Intent(DashActivity.this, AccessoryActivity.class);
                startActivity(accessoryIntent);
            }
        }
    };

    private void updateDisplay(){
        // Set actionbar color based on focus
        if (sharedPrefs.getBoolean("prefFocusIndication", false)) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.backgroundColor, typedValue, true);
            int color = typedValue.data;
            if (MotorcycleData.getHasFocus()) {
                color = ContextCompat.getColor(DashActivity.this, R.color.colorAccent);
            }
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setBackgroundDrawable(new ColorDrawable(color));
            }
        }
        //Check for active faults
        if (!Faults.getAllActiveDesc().isEmpty()) {
            faultButton.setVisibility(View.VISIBLE);
        } else {
            faultButton.setVisibility(View.GONE);
        }

        if (dashUpdateHandler != null && !dashUpdateRunning) {
            dashUpdateHandler.post(() -> {
                dashUpdateRunning = true;
                try {
                    SVG newSvg = null;
                    if (currentDashboard == 1) {
                        newSvg = StandardDashboard.updateDashboard(currentInfoLine);
                    } else if (currentDashboard == 2) {
                        newSvg = SportDashboard.updateDashboard(currentInfoLine);
                    } else if (currentDashboard == 3) {
                        newSvg = ADVDashboard.updateDashboard(currentInfoLine);
                    }

                    if (newSvg != null) {
                        SVG.registerExternalFileResolver(svgFileResolver);
                        final SVG finalSvg = newSvg;
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                dashboardView.setSVG(finalSvg);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating dashboard", e);
                } finally {
                    dashUpdateRunning = false;
                }
            });
        }
    }
}
