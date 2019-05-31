package com.blackboxembedded.WunderLINQ;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;

public class WaypointNavActivity extends AppCompatActivity implements OsmAndHelper.OnOsmandMissingListener {

    public final static String TAG = "WaypointNav";

    private ImageButton backButton;
    private ActionBar actionBar;
    private TextView navbarTitle;

    private ListView waypointList;
    List<WaypointRecord> listValues;
    ArrayAdapter<WaypointRecord> adapter;

    private int lastPosition = 0;

    private SharedPreferences sharedPrefs;

    static boolean itsDark = false;
    private long darkTimer = 0;
    private long lightTimer = 0;

    SensorManager sensorManager;
    Sensor lightSensor;

    @Override
    public void osmandMissing() {
        //OsmAndMissingDialogFragment().show(supportFragmentManager, null);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_waypoint_nav);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        showActionBar();

        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
            itsDark = true;
        } else {
            itsDark = false;
        }

        waypointList = findViewById(R.id.lv_waypoints);
        waypointList.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(WaypointNavActivity.this, TaskActivity.class);
                startActivity(backIntent);
            }
        });

        // Open database
        WaypointDatasource datasource = new WaypointDatasource(this);
        datasource.open();

        listValues = datasource.getAllRecords();
        adapter = new
                WaypointListView(this, listValues, itsDark);

        waypointList.setAdapter(adapter);

        waypointList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView < ? > adapter, View view, int position, long arg){
                lastPosition = position;
                WaypointRecord record = (WaypointRecord) waypointList.getItemAtPosition(position);
                String navApp = sharedPrefs.getString("prefNavApp", "1");
                String navUrl = "google.navigation:q=" + record.getData() + "&navigate=yes";
                // Get location
                // Get the location manager
                LocationManager locationManager = (LocationManager)
                        WaypointNavActivity.this.getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                try {
                    String bestProvider = locationManager.getBestProvider(criteria, false);
                    Log.d(TAG,"Trying Best Provider: " + bestProvider);
                    Location currentLocation = locationManager.getLastKnownLocation(bestProvider);
                    if (navApp.equals("1") || navApp.equals("2")){
                        // Android Default or Google Maps
                        // Nothing to do
                    } else if (navApp.equals("3")){
                        //Locus

                    } else if (navApp.equals("4")){
                        //Waze
                        navUrl = "https://www.waze.com/ul?ll=" + record.getData() + "&navigate=yes&zoom=17";
                    } else if (navApp.equals("5")){
                        //Maps.me
                        navUrl = "mapsme://route?sll=" + String.valueOf(currentLocation.getLatitude()) + ","
                                + String.valueOf(currentLocation.getLongitude()) + "&saddr="
                                + getString(R.string.trip_view_waypoint_start_label) + "&dll="
                                + record.getData() + "&daddr=" + record.getLabel() + "&type=vehicle";
                    } else if (navApp.equals("6")){
                        // OsmAnd
                        String location[] = record.getData().split(",");
                        Double latitude =  Double.parseDouble(location[0]);
                        Double longitude =  Double.parseDouble(location[1]);
                        //navUrl = "osmand.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                        OsmAndHelper osmAndHelper = new OsmAndHelper(WaypointNavActivity.this, OsmAndHelper.REQUEST_OSMAND_API, WaypointNavActivity.this);
                        osmAndHelper.navigate("Start",currentLocation.getLatitude(),currentLocation.getLongitude(),"Destination",latitude,longitude,"motorcycle", true);
                    }
                    if (!navApp.equals("6")) {
                        try {
                            Intent navIntent = new Intent(android.content.Intent.ACTION_VIEW);
                            navIntent.setData(Uri.parse(navUrl));
                            if (android.os.Build.VERSION.SDK_INT >= 24) {
                                navIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                            }
                            startActivity(navIntent);
                        } catch (ActivityNotFoundException ex) {
                            // Add Alert
                        }
                    }
                } catch (SecurityException|NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

        updateColors(itsDark);

        // Sensor Stuff
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
            updateColors(true);
        } else {
            updateColors(false);
        }
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
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

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
        navbarTitle.setText(R.string.waypoints_nav_title);

        backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(WaypointNavActivity.this, TaskActivity.class);
                    startActivity(backIntent);
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
        LinearLayout lLayout = findViewById(R.id.layout_waypoint_nav);
        if (itsDark) {
            //Set Brightness to default
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = -1;
            getWindow().setAttributes(layoutParams);

            lLayout.setBackgroundColor(getResources().getColor(R.color.black));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
            navbarTitle.setTextColor(getResources().getColor(R.color.white));
            backButton.setColorFilter(getResources().getColor(R.color.white));
        } else {
            if (sharedPrefs.getBoolean("prefBrightnessOverride", false)) {
                //Set Brightness to 100%
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = 1;
                getWindow().setAttributes(layoutParams);
            }

            lLayout.setBackgroundColor(getResources().getColor(R.color.white));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
            navbarTitle.setTextColor(getResources().getColor(R.color.black));
            backButton.setColorFilter(getResources().getColor(R.color.black));
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Intent backIntent = new Intent(WaypointNavActivity.this, TaskActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if ((waypointList.getSelectedItemPosition() == (listValues.size() - 1)) && lastPosition == (listValues.size() - 1) ){
                    waypointList.setSelection(0);
                }
                lastPosition = waypointList.getSelectedItemPosition();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (waypointList.getSelectedItemPosition() == 0 && lastPosition == 0){
                    waypointList.setSelection(listValues.size() - 1);
                }
                lastPosition = waypointList.getSelectedItemPosition();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
