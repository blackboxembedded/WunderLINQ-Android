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
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
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
import com.blackboxembedded.WunderLINQ.TaskList.TaskActivity;
import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.SoundManager;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;

public class DashActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "DashActivity";

    private SharedPreferences sharedPrefs;
    private ImageButton faultButton;

    private SVGImageView dashboardView;
    private SVG svg;
    private SvgFileResolver svgFileResolver;
    private GestureDetectorListener gestureDetector;
    private CountDownTimer cTimer = null;
    private boolean timerRunning = false;
    private boolean dashUpdateRunning = false;
    private long lastUpdate = 0;

    private int numDashboard = 3;
    private int numInfoLine = 4;
    private int currentDashboard = 1;
    private int currentInfoLine = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

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
    }

    @Override
    public void onResume() {
        super.onResume();
        getSupportActionBar().show();
        currentDashboard = sharedPrefs.getInt("lastDashboard",1);
        updateDashboard();
        startTimer();
        ContextCompat.registerReceiver(this, mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("lastDashboard", currentDashboard);
        editor.apply();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e){
            Log.d(TAG,e.toString());
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

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    goBack();
                    break;
                case R.id.action_forward:
                    goForward();
                    break;
                case R.id.action_faults:
                    Intent faultIntent = new Intent(DashActivity.this, FaultActivity.class);
                    startActivity(faultIntent);
                    break;
            }
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
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                nextDashboard();
                return true;
            case KeyEvent.KEYCODE_ESCAPE:
                prevDashboard();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                nextInfoLine();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                prevInfoLine();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                goBack();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                goForward();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    void nextDashboard(){
        SoundManager.playSound(this, R.raw.enter);
        if (currentDashboard == numDashboard){
            currentDashboard = 1;
        } else {
            currentDashboard = currentDashboard + 1;
        }
        updateDashboard();
    }

    void prevDashboard(){
        SoundManager.playSound(this, R.raw.enter);
        if (currentDashboard == 1){
            currentDashboard = numDashboard;
        } else {
            currentDashboard = currentDashboard - 1;
        }
        updateDashboard();
    }

    void nextInfoLine(){
        SoundManager.playSound(this, R.raw.directional);
        if (currentInfoLine == numInfoLine){
            currentInfoLine = 1;
        } else {
            currentInfoLine = currentInfoLine + 1;
        }
        updateDashboard();
    }

    void prevInfoLine(){
        SoundManager.playSound(this, R.raw.directional);
        if (currentInfoLine == 1){
            currentInfoLine = numInfoLine;
        } else {
            currentInfoLine = currentInfoLine - 1;
        }
        updateDashboard();
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
                    updateDashboard();
                }
            } else if (BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE.equals(action)) {
                Intent accessoryIntent = new Intent(DashActivity.this, AccessoryActivity.class);
                startActivity(accessoryIntent);
            }
        }
    };

    private void updateDashboard(){
        //Check for active faults
        if (!Faults.getAllActiveDesc().isEmpty()) {
            faultButton.setVisibility(View.VISIBLE);
        } else {
            faultButton.setVisibility(View.GONE);
        }
        if (!dashUpdateRunning) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    dashUpdateRunning = true;

                    if (currentDashboard == 1){
                        svg = StandardDashboard.updateDashboard(currentInfoLine);
                    } else if (currentDashboard == 2){
                        svg = SportDashboard.updateDashboard(currentInfoLine);
                    } else if (currentDashboard == 3){
                        svg = ADVDashboard.updateDashboard(currentInfoLine);
                    }
                    svg.registerExternalFileResolver(svgFileResolver);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //your code or your request that you want to run on uiThread
                            if(svg != null) {
                                dashboardView.setSVG(svg);
                            }
                        }
                    });
                    dashUpdateRunning = false;
                }
            }).start();
        }
    }
}
