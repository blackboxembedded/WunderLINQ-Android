package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class AlertActivity extends AppCompatActivity {

    public final static String TAG = "AlertActivity";

    int type = 1;
    String title = "";
    String body = "";
    String backgroundPath = "";

    TextView tvAlertbody;
    Button btnOK;
    Button btnClose;
    TextView navbarTitle;
    ActionBar actionBar;

    private SharedPreferences sharedPrefs;

    static boolean itsDark = false;
    private long darkTimer = 0;
    private long lightTimer = 0;

    SensorManager sensorManager;
    Sensor lightSensor;

    private Handler handler;
    private Runnable runnable;

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
        setContentView(R.layout.activity_alert);
        View view = findViewById(R.id.layout_alert);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                finish();
            }
        });

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String orientation = sharedPrefs.getString("prefOrientation", "0");
        if (!orientation.equals("0")){
            if(orientation.equals("1")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        tvAlertbody = findViewById(R.id.tvAlertBody);
        btnClose = findViewById(R.id.btnClose);
        btnOK = findViewById(R.id.btnOK);
        btnClose.setOnClickListener(mClickListener);
        btnOK.setOnClickListener(mClickListener);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            type = extras.getInt("TYPE");
            title = extras.getString("TITLE");
            body = extras.getString("BODY");
            backgroundPath = extras.getString("BACKGROUND");
            Log.d(TAG,"Background Image: " + backgroundPath);
        }
        tvAlertbody.setText(body);

        if (type == 2){
            btnOK.setVisibility(View.INVISIBLE);
        }
        switch (type){
            case 2:
                btnOK.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
        if(!backgroundPath.equals("")){
            Log.d(TAG,"Setting Background Image");
            view.setBackground(Drawable.createFromPath(backgroundPath));
        }
        showActionBar();
        // Sensor Stuff
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Close after some seconds
        handler  = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                finish();;
            }
        };

        handler.postDelayed(runnable, 10000);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
            itsDark = true;
        } else {
            itsDark = false;
        }
        updateColors(itsDark);
        if (sharedPrefs.getBoolean("prefAutoNightMode", false)) {
            sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btnClose:
                    finish();
                    break;
                case R.id.btnOK:
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=fuel+station"));
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                    break;
            }
        }
    };

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);


        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setVisibility(View.INVISIBLE);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    // Listens for light sensor events
    private final SensorEventListener sensorEventListener
            = new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (sharedPrefs.getBoolean("prefAutoNightMode", false) && (!sharedPrefs.getBoolean("prefNightMode", false))) {
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
                                Log.d(TAG, "Sensor Setting: Its dark");
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
                                Log.d(TAG, "Sensor Setting: Its NOT dark");
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
        android.support.constraint.ConstraintLayout lLayout = findViewById(R.id.layout_alert);
        if (itsDark) {
            Log.d(TAG, "updateColors: Its dark");
            //Set Brightness to default
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = -1;
            getWindow().setAttributes(layoutParams);

            if(backgroundPath.equals("")) {
               lLayout.setBackgroundColor(getResources().getColor(R.color.black));
            }
            btnOK.setBackgroundColor(getResources().getColor(R.color.black));
            btnOK.setTextColor(getResources().getColor(R.color.white));
            btnClose.setBackgroundColor(getResources().getColor(R.color.black));
            btnClose.setTextColor(getResources().getColor(R.color.white));
            tvAlertbody.setTextColor(getResources().getColor(R.color.white));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
            navbarTitle.setTextColor(getResources().getColor(R.color.white));
        } else {
            Log.d(TAG, "updateColors: Its NOT dark");
            if (sharedPrefs.getBoolean("prefBrightnessOverride", false)) {
                //Set Brightness to 100%
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = 1;
                getWindow().setAttributes(layoutParams);
            }

            if(backgroundPath.equals("")) {
                lLayout.setBackgroundColor(getResources().getColor(R.color.white));
            }
            btnOK.setBackgroundColor(getResources().getColor(R.color.white));
            btnOK.setTextColor(getResources().getColor(R.color.black));
            btnClose.setBackgroundColor(getResources().getColor(R.color.white));
            btnClose.setTextColor(getResources().getColor(R.color.black));
            tvAlertbody.setTextColor(getResources().getColor(R.color.black));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
            navbarTitle.setTextColor(getResources().getColor(R.color.black));
        }
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                finish();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (type != 2) {
                    btnOK.performClick();
                }
                switch (type){
                    case 2:
                        finish();
                        break;
                    default:
                        btnOK.performClick();
                        break;
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:

                return true;
            case KeyEvent.KEYCODE_DPAD_UP:

                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
