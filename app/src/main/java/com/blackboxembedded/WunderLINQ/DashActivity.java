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
import android.os.Bundle;
import android.os.CountDownTimer;
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

import com.blackboxembedded.WunderLINQ.SVGDashboards.ADVDashboard;
import com.blackboxembedded.WunderLINQ.SVGDashboards.SportDashboard;
import com.blackboxembedded.WunderLINQ.SVGDashboards.StandardDashboard;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;

public class DashActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "DashActivity";

    private SVGImageView dashboardView;
    private SVG svg;
    private SvgFileResolver svgFileResolver;
    private GestureDetectorListener gestureDetector;
    private CountDownTimer cTimer = null;
    private boolean timerRunning = false;
    private boolean dashUpdateRunning = false;

    private int currentDashboard = 1;
    private int currentInfoLine = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash);

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

        showActionBar();

        dashboardView = findViewById(R.id.mainView);
        svgFileResolver = new SvgFileResolver();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        getSupportActionBar().show();
        startTimer();
        updateDashboard();
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
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
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
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
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
            }
        }
    };

    //Go to next screen - Quick Tasks
    private void goForward(){
        Intent backIntent = new Intent(this, MusicActivity.class);
        startActivity(backIntent);
    }

    //Go back to last screen - Motorcycle Data
    private void goBack(){
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
            case KeyEvent.KEYCODE_DPAD_UP:
                nextInfoLine();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
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
        if (currentDashboard == 3){
            currentDashboard = 1;
        } else {
            currentDashboard = currentDashboard + 1;
        }
        updateDashboard();
    }

    void nextInfoLine(){
        if (currentInfoLine == 3){
            currentInfoLine = 1;
        } else {
            currentInfoLine = currentInfoLine + 1;
        }
        updateDashboard();
    }

    void prevInfoLine(){
        if (currentInfoLine == 1){
            currentInfoLine = 3;
        } else {
            currentInfoLine = currentInfoLine - 1;
        }
        updateDashboard();
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

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

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
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                updateDashboard();
            }
        }
    };

    public void updateDashboard(){
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
