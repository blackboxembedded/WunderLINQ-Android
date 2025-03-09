package com.blackboxembedded.WunderLINQ;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.SoundManager;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_BASE;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.WLQ_S;


public class AccessoryActivity extends AppCompatActivity implements View.OnTouchListener {

    private final static String TAG = "AccActivity";

    private SharedPreferences sharedPrefs;
    private ImageButton faultButton;
    private GestureDetectorListener gestureDetector;

    private ConstraintLayout channelOneCL;
    private TextView channelOneHeaderTV;
    private EditText channelOneHeaderET;
    private ProgressBar channelOneValuePB;
    private ConstraintLayout channelTwoCL;
    private TextView channelTwoHeaderTV;
    private EditText channelTwoHeaderET;
    private ProgressBar channelTwoValuePB;

    // class member variable to save the X,Y coordinates
    private float[] lastTouchDownXY = new float[2];

    private CountDownTimer cTimer = null;
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

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

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_acc);
        View view = findViewById(R.id.layout_acc);
        channelOneCL = findViewById(R.id.layout_channel1);
        channelOneHeaderTV = findViewById(R.id.tvChannel1Header);
        channelOneHeaderET = findViewById(R.id.etChannel1Header);
        channelOneValuePB = findViewById(R.id.pbChannel1Value);
        channelTwoCL = findViewById(R.id.layout_channel2);
        channelTwoHeaderTV = findViewById(R.id.tvChannel2Header);
        channelTwoHeaderET = findViewById(R.id.etChannel2Header);
        channelTwoValuePB = findViewById(R.id.pbChannel2Value);
        channelOneValuePB.setMax(4);
        channelTwoValuePB.setMax(4);

        gestureDetector = new GestureDetectorListener(this){
            @Override
            public void onPressLong() {
                // retrieve the stored coordinates
                float x = lastTouchDownXY[0];
                float y = lastTouchDownXY[1];

                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;
                if (y < (height / 2)){
                    channelOneHeaderTV.setVisibility(View.INVISIBLE);
                    channelOneHeaderET.setVisibility(View.VISIBLE);
                    if (channelOneHeaderET.requestFocus()) {
                        InputMethodManager imm = (InputMethodManager)
                                getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(channelOneHeaderET, InputMethodManager.SHOW_IMPLICIT);

                    }
                    channelOneHeaderTV.setText(channelOneHeaderET.getText().toString());
                    channelOneHeaderET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if(actionId== EditorInfo.IME_ACTION_DONE){
                                channelOneHeaderET.setVisibility(View.INVISIBLE);
                                channelOneHeaderTV.bringToFront();
                                channelOneHeaderTV.setVisibility(View.VISIBLE);
                                channelOneHeaderTV.setText(channelOneHeaderET.getText().toString());
                                SharedPreferences.Editor editor = sharedPrefs.edit();
                                editor.putString("ACC_CHAN_1", channelOneHeaderET.getText().toString());
                                editor.apply();
                            }
                            return false;
                        }
                    });
                } else {
                    channelTwoHeaderTV.setVisibility(View.INVISIBLE);
                    channelTwoHeaderET.setVisibility(View.VISIBLE);
                    if (channelTwoHeaderET.requestFocus()) {
                        InputMethodManager imm = (InputMethodManager)
                                getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(channelTwoHeaderET, InputMethodManager.SHOW_IMPLICIT);

                    }
                    channelTwoHeaderTV.setText(channelTwoHeaderET.getText().toString());
                    channelTwoHeaderET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if(actionId== EditorInfo.IME_ACTION_DONE){
                                channelTwoHeaderET.setVisibility(View.INVISIBLE);
                                channelTwoHeaderTV.bringToFront();
                                channelTwoHeaderTV.setVisibility(View.VISIBLE);
                                channelTwoHeaderTV.setText(channelTwoHeaderET.getText().toString());
                                SharedPreferences.Editor editor = sharedPrefs.edit();
                                editor.putString("ACC_CHAN_2", channelTwoHeaderET.getText().toString());
                                editor.apply();
                            }
                            return false;
                        }
                    });
                }
            }

            @Override
            public void onSwipeUp() {
            }

            @Override
            public void onSwipeDown() {
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        ContextCompat.registerReceiver(this, mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_EXPORTED);

        // Read status
        if (BluetoothLeService.gattCommandCharacteristic != null) {
            //BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, WLQ_S.GET_STATUS_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
        }

        updateDisplay();

        startTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
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
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
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
        navbarTitle.setText(R.string.accessory_title);

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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        getSupportActionBar().show();
        startTimer();
        // Save the X,Y coordinates
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastTouchDownXY[0] = event.getX();
            lastTouchDownXY[1] = event.getY();
        }
        gestureDetector.onTouch(v, event);
        return true;
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
                    Intent faultIntent = new Intent(AccessoryActivity.this, FaultActivity.class);
                    startActivity(faultIntent);
                    break;
            }
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
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

    //Go to next screen
    private void goForward(){
        SoundManager.playSound(this, R.raw.directional);
        Intent forwardIntent = new Intent(this, MainActivity.class);
        startActivity(forwardIntent);
    }

    //Go previous screen
    private void goBack(){
        SoundManager.playSound(this, R.raw.directional);
        Intent backIntent = new Intent(this, com.blackboxembedded.WunderLINQ.TaskList.TaskActivity.class);
        startActivity(backIntent);
    }

    private void updateDisplay(){
        // Set actionbar color based on focus
        if (sharedPrefs.getBoolean("prefFocusIndication", false)) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.backgroundColor, typedValue, true);
            int color = typedValue.data;
            if (MotorcycleData.getHasFocus()) {
                color = ContextCompat.getColor(AccessoryActivity.this, R.color.colorAccent);
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
        if (MotorcycleData.wlq != null){
            if (MotorcycleData.wlq.getStatus() != null) {
                channelOneHeaderTV.setText(sharedPrefs.getString("ACC_CHAN_1", getString(R.string.default_accessory_one_name)));
                channelOneHeaderET.setText(sharedPrefs.getString("ACC_CHAN_1", getString(R.string.default_accessory_one_name)));
                channelTwoHeaderTV.setText(sharedPrefs.getString("ACC_CHAN_2", getString(R.string.default_accessory_two_name)));
                channelTwoHeaderET.setText(sharedPrefs.getString("ACC_CHAN_2", getString(R.string.default_accessory_two_name)));
                int channelActive = (MotorcycleData.wlq.getStatus()[WLQ_S.ACTIVE_CHAN_INDEX] & 0xFF);
                int channel1ValueRaw = (MotorcycleData.wlq.getStatus()[WLQ_S.LIN_ACC_CHANNEL1_VAL_RAW_INDEX] & 0xFF);
                int channel2ValueRaw = (MotorcycleData.wlq.getStatus()[WLQ_S.LIN_ACC_CHANNEL2_VAL_RAW_INDEX] & 0xFF);

                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = this.getTheme();
                theme.resolveAttribute(R.attr.primaryTextColor, typedValue, true);
                @ColorInt int foregroundColor = typedValue.data;
                GradientDrawable drawable = (GradientDrawable) getDrawable(R.drawable.border_highlight);
                drawable.mutate(); // only change this instance of the xml, not all components using this xml
                drawable.setStroke(20, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getInt("prefHighlightColor", R.color.colorAccent)); // set stroke width and stroke color
                Log.d(TAG,"ACTIVE CHANNEL: " + channelActive);
                switch (channelActive) {
                    case 2:
                        channelOneCL.setBackground(drawable);
                        channelTwoCL.setBackgroundResource(0);
                        channelOneValuePB.setProgressTintList(ColorStateList.valueOf(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getInt("prefHighlightColor", R.color.colorAccent)));
                        channelTwoValuePB.setProgressTintList(ColorStateList.valueOf(foregroundColor));
                        break;
                    case 3:
                        channelOneCL.setBackgroundResource(0);
                        channelTwoCL.setBackground(drawable);
                        channelTwoValuePB.setProgressTintList(ColorStateList.valueOf(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getInt("prefHighlightColor", R.color.colorAccent)));
                        channelOneValuePB.setProgressTintList(ColorStateList.valueOf(foregroundColor));
                        break;
                    default:
                        channelOneCL.setBackgroundResource(0);
                        channelTwoCL.setBackgroundResource(0);
                        channelOneValuePB.setProgressTintList(ColorStateList.valueOf(foregroundColor));
                        channelTwoValuePB.setProgressTintList(ColorStateList.valueOf(foregroundColor));
                        break;
                }
                channelOneValuePB.setProgress(channel1ValueRaw);
                channelTwoValuePB.setProgress(channel2ValueRaw);
            } else {
                //BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, WLQ_S.GET_STATUS_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
            }
        } else {
            // Request config
            BluetoothLeService.writeCharacteristic(BluetoothLeService.gattCommandCharacteristic, WLQ_BASE.GET_CONFIG_CMD, BluetoothLeService.WriteType.WITH_RESPONSE);
        }
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

    // Handles various events fired by the Service.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE.equals(action)) {
                updateDisplay();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                goBack();
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }
}