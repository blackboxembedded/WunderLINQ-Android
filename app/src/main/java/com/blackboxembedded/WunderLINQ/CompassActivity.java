package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

public class CompassActivity extends AppCompatActivity {

    public final static String TAG = "WunderLINQ";

    private SharedPreferences sharedPrefs;

    private ImageButton backButton;
    private ImageButton forwardButton;

    private TextView compassTextView;

    SensorManager sensorManager;
    Sensor accelerometer;
    Sensor magnetometer;
    /*
    * time smoothing constant for low-pass filter
    * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
    * was 0.15f
    * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
    */
    static final float ALPHA = 0.05f;
    float[] gravity = new float[3];
    float[] geomagnetic = new float[3];
    private int lastDirection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_compass);

        showActionBar();

        compassTextView = (TextView) findViewById(R.id.compassTextView);
        compassTextView.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);

        // Sensor Stuff
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Compass
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI);
        Log.d(TAG," CompassActivity In onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener, magnetometer);
        sensorManager.unregisterListener(sensorEventListener, accelerometer);
        Log.d(TAG,"CompassActivity In onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(sensorEventListener, magnetometer);
        sensorManager.unregisterListener(sensorEventListener, accelerometer);
        Log.d(TAG,"CompassActivity In onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(sensorEventListener, magnetometer);
        sensorManager.unregisterListener(sensorEventListener, accelerometer);
        Log.d(TAG,"CompassActivity In onDestroy");
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle;
        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.compass_title);

        backButton = (ImageButton) findViewById(R.id.action_back);
        forwardButton = (ImageButton) findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(CompassActivity.this, MusicActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_forward:
                    Intent forwardIntent = new Intent(CompassActivity.this, TaskActivity.class);
                    startActivity(forwardIntent);
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
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                gravity = lowPass(event.values.clone(), gravity);
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                geomagnetic = lowPass(event.values.clone(), geomagnetic);
            }
            if (gravity != null && geomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                float remappedR[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    String bearing = "-";
                    SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedR);
                    SensorManager.getOrientation(remappedR, orientation);
                    int direction = filterChange(normalizeDegrees(Math.toDegrees(orientation[0])));
                    if((int)direction != (int)lastDirection) {
                        lastDirection = (int) direction;
                        bearing = String.valueOf(lastDirection);
                        if (sharedPrefs.getString("prefBearing", "0").contains("1")) {
                            if (lastDirection > 331 || lastDirection <= 28) {
                                bearing = "N";
                            } else if (lastDirection > 28 && lastDirection <= 73) {
                                bearing = "NE";
                            } else if (lastDirection > 73 && lastDirection <= 118) {
                                bearing = "E";
                            } else if (lastDirection > 118 && lastDirection <= 163) {
                                bearing = "SE";
                            } else if (lastDirection > 163 && lastDirection <= 208) {
                                bearing = "S";
                            } else if (lastDirection > 208 && lastDirection <= 253) {
                                bearing = "SW";
                            } else if (lastDirection > 253 && lastDirection <= 298) {
                                bearing = "W";
                            } else if (lastDirection > 298 && lastDirection <= 331) {
                                bearing = "NW";
                            }
                        }
                        compassTextView.setText(bearing);
                    }
                }
            }
        }
    };

    //Normalize a degree from 0 to 360 instead of -180 to 180
    private int normalizeDegrees(double rads){
        return (int)((rads+360)%360);
    }


    private int filterChange(int newDir){
        int change = newDir - lastDirection;
        int circularChange = newDir-(lastDirection+360);
        int smallestChange;
        if(Math.abs(change) < Math.abs(circularChange)){
            smallestChange = change;
        }
        else{
            smallestChange = circularChange;
        }
        smallestChange = Math.max(Math.min(change,3),-3);
        return lastDirection+smallestChange;
    }

    /*
     * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
     */
    // Lowpass filter
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Intent backIntent = new Intent(CompassActivity.this, MusicActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Intent forwardIntent = new Intent(CompassActivity.this, TaskActivity.class);
                startActivity(forwardIntent);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

}
