package com.blackboxembedded.WunderLINQ;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TaskActivity extends AppCompatActivity {

    public final static String TAG = "WunderLINQ";

    private ActionBar actionBar;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private TextView navbarTitle;

    private ListView taskList;

    private SharedPreferences sharedPrefs;

    static boolean itsDark = false;
    private long darkTimer = 0;
    private long lightTimer = 0;

    SensorManager sensorManager;
    Sensor lightSensor;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 102;
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 112;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 122;

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
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_task);

        View view = findViewById(R.id.lv_tasks);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                Intent backIntent = new Intent(TaskActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(TaskActivity.this, CompassActivity.class);
                startActivity(backIntent);
            }
        });

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        taskList = (ListView) findViewById(R.id.lv_tasks);

        showActionBar();

        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getBoolean("prefNightMode", false)){
            itsDark = true;
        } else {
            itsDark = false;
        }
        updateColors(itsDark);

        displayTasks();

        // Sensor Stuff
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (sharedPrefs.getBoolean("prefAutoNightMode", false)) {
            sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getBoolean("prefNightMode", false)){
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            startService(new Intent(TaskActivity.this,
                    VideoRecService.class));

        }
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

        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.quicktask_title);

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
                    Intent backIntent = new Intent(TaskActivity.this, CompassActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_forward:
                    Intent forwardIntent = new Intent(TaskActivity.this, MainActivity.class);
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
                                // Update colors
                                updateColors(true);
                                displayTasks();
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
                                // Update colors
                                updateColors(false);
                                displayTasks();
                            }
                        }
                    }
                }
            }
        }
    };

    public void displayTasks(){
        //TODO: Hide options if permissions aren't granted, ie. video, camera, microphone, contacts
        String videoTaskText = getResources().getString(R.string.task_title_start_record);
        if (((MyApplication) this.getApplication()).getVideoRecording()){
            videoTaskText = getResources().getString(R.string.task_title_stop_record);
        }

        String tripTaskText = getResources().getString(R.string.task_title_start_trip);
        if (((MyApplication) this.getApplication()).getTripRecording()){
            tripTaskText = getResources().getString(R.string.task_title_stop_trip);
        }

        // Tasks
        // Order must match between text, icon and onItemClick case
        final String[] taskTitles = new String[] {
                getResources().getString(R.string.task_title_navigation),
                getResources().getString(R.string.task_title_gohome),
                getResources().getString(R.string.task_title_favnumber),
                getResources().getString(R.string.task_title_callcontact),
                getResources().getString(R.string.task_title_photo),
                videoTaskText,
                tripTaskText,
                getResources().getString(R.string.task_title_waypoint),
                getResources().getString(R.string.task_title_voicecontrol)
        };

        Drawable[] iconId = new Drawable[9];
        if (itsDark) {
            iconId[0] = getResources().getDrawable(R.drawable.ic_map, getTheme());
            iconId[0].setTint(Color.WHITE);
            iconId[1] = getResources().getDrawable(R.drawable.ic_home, getTheme());
            iconId[1].setTint(Color.WHITE);
            iconId[2] = getResources().getDrawable(R.drawable.ic_phone, getTheme());
            iconId[2].setTint(Color.WHITE);
            iconId[3] = getResources().getDrawable(R.drawable.ic_address_book, getTheme());
            iconId[3].setTint(Color.WHITE);
            iconId[4] = getResources().getDrawable(R.drawable.ic_camera, getTheme());
            iconId[4].setTint(Color.WHITE);
            iconId[5] = getResources().getDrawable(R.drawable.ic_video_camera, getTheme());
            iconId[5].setTint(Color.WHITE);
            iconId[6] = getResources().getDrawable(R.drawable.ic_road, getTheme());
            iconId[6].setTint(Color.WHITE);
            iconId[7] = getResources().getDrawable(R.drawable.ic_map_marker, getTheme());
            iconId[7].setTint(Color.WHITE);
            iconId[8] = getResources().getDrawable(R.drawable.ic_microphone, getTheme());
            iconId[8].setTint(Color.WHITE);
        } else  {
            iconId[0] = getResources().getDrawable(R.drawable.ic_map, getTheme());
            iconId[0].setTint(Color.BLACK);
            iconId[1] = getResources().getDrawable(R.drawable.ic_home, getTheme());
            iconId[1].setTint(Color.BLACK);
            iconId[2] = getResources().getDrawable(R.drawable.ic_phone, getTheme());
            iconId[2].setTint(Color.BLACK);
            iconId[3] = getResources().getDrawable(R.drawable.ic_address_book, getTheme());
            iconId[3].setTint(Color.BLACK);
            iconId[4] = getResources().getDrawable(R.drawable.ic_camera, getTheme());
            iconId[4].setTint(Color.BLACK);
            iconId[5] = getResources().getDrawable(R.drawable.ic_video_camera, getTheme());
            iconId[5].setTint(Color.BLACK);
            iconId[6] = getResources().getDrawable(R.drawable.ic_road, getTheme());
            iconId[6].setTint(Color.BLACK);
            iconId[7] = getResources().getDrawable(R.drawable.ic_map_marker, getTheme());
            iconId[7].setTint(Color.BLACK);
            iconId[8] = getResources().getDrawable(R.drawable.ic_microphone, getTheme());
            iconId[8].setTint(Color.BLACK);
        }

        TaskListView adapter = new
                TaskListView(this, taskTitles, iconId, itsDark);
        taskList=(ListView)findViewById(R.id.lv_tasks);
        taskList.setAdapter(adapter);
        taskList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                switch (position){
                    case 0:
                        //Navigation
                        Intent navIntent = new Intent(android.content.Intent.ACTION_VIEW);
                        navIntent.setData(Uri.parse("google.navigation:/?free=1&mode=d&entry=fnls"));
                        startActivity(navIntent);
                        break;
                    case 1:
                        //Navigate Home
                        String address = sharedPrefs.getString("prefHomeAddress","");
                        if ( address != "" ) {
                            Intent goHomeIntent = new Intent(android.content.Intent.ACTION_VIEW);
                            goHomeIntent.setData(Uri.parse("google.navigation:q=" + address));
                            startActivity(goHomeIntent);
                        } else {
                            Toast.makeText(TaskActivity.this, R.string.toast_address_not_set, Toast.LENGTH_LONG).show();
                        }
                        break;
                    case 2:
                        //Call Favorite Number
                        String phonenumber = sharedPrefs.getString("prefHomePhone","");
                        if (phonenumber != "") {
                            Intent callHomeIntent = new Intent(Intent.ACTION_DIAL);
                            callHomeIntent.setData(Uri.parse("tel:" + phonenumber));
                            startActivity(callHomeIntent);
                        } else {
                            Toast.makeText(TaskActivity.this, R.string.toast_phone_not_set, Toast.LENGTH_LONG).show();
                        }
                        break;
                    case 3:
                        //Call Contact
                        // Check Read Contacts permissions
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (getApplication().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.contacts_alert_title));
                                builder.setMessage(getString(R.string.contacts_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
                                    }
                                });
                                builder.show();
                            } else {
                                // Android version is lesser than 6.0 or the permission is already granted.
                                Intent forwardIntent = new Intent(TaskActivity.this, ContactListActivity.class);
                                startActivity(forwardIntent);
                            }
                        }  else {
                            // Android version is lesser than 6.0 or the permission is already granted.
                            Intent forwardIntent = new Intent(TaskActivity.this, ContactListActivity.class);
                            startActivity(forwardIntent);
                        }

                        //Intent forwardIntent = new Intent(TaskActivity.this, ContactListActivity.class);
                        //startActivity(forwardIntent);
                        break;
                    case 4:
                        //Take photo
                        boolean cameraPerms = false;
                        boolean writePerms = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Check Camera permissions
                            if (getApplication().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.camera_alert_title));
                                builder.setMessage(getString(R.string.camera_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                                    }
                                });
                                builder.show();
                                cameraPerms = false;
                            } else {
                                cameraPerms = true;
                            }
                            // Check Write permissions
                            if (getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.write_alert_title));
                                builder.setMessage(getString(R.string.write_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_STORAGE);
                                    }
                                });
                                builder.show();
                                writePerms = false;
                            } else {
                                writePerms = true;
                            }

                            if (cameraPerms == true && writePerms == true){
                                Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                                photoIntent.putExtra("camera",0);
                                startService(photoIntent);
                            }

                        } else {
                            Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                            photoIntent.putExtra("camera", 0);
                            startService(photoIntent);
                        }
                        break;
                    case 5:
                        //Record Video

                        TextView taskText=(TextView)view.findViewById(R.id.tv_label);

                        boolean cameraVidPerms = false;
                        boolean writeVidPerms = false;
                        boolean audioVidPerms = false;
                        boolean overlayVidPerms = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Check Camera permissions
                            if (getApplication().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.camera_alert_title));
                                builder.setMessage(getString(R.string.camera_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                                    }
                                });
                                builder.show();
                                cameraVidPerms = false;
                            } else {
                                cameraVidPerms = true;
                            }
                            // Check Read Audio permissions
                            if (getApplication().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.record_audio_alert_title));
                                builder.setMessage(getString(R.string.record_audio_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                                    }
                                });
                                builder.show();
                                audioVidPerms = false;
                            } else {
                                audioVidPerms = true;
                            }
                            // Check Write permissions
                            if (getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.write_alert_title));
                                builder.setMessage(getString(R.string.write_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_STORAGE);
                                    }
                                });
                                builder.show();
                                writeVidPerms = false;
                            } else {
                                writeVidPerms = true;
                            }
                            // Check overlay permissions
                            if (!Settings.canDrawOverlays(TaskActivity.this)) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.overlay_alert_title));
                                builder.setMessage(getString(R.string.overlay_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getPackageName()));
                                        startActivity(intent);
                                    }
                                });
                                builder.show();
                                overlayVidPerms = false;
                            } else {
                                overlayVidPerms = true;
                            }
                            if (cameraVidPerms == true && audioVidPerms == true && writeVidPerms == true && overlayVidPerms == true){
                                if (taskText.getText().equals(getResources().getString(R.string.task_title_start_record))) {
                                    startService(new Intent(TaskActivity.this, VideoRecService.class));
                                    taskText.setText(getResources().getString(R.string.task_title_stop_record));
                                } else {
                                    stopService(new Intent(TaskActivity.this, VideoRecService.class));
                                    taskText.setText(getResources().getString(R.string.task_title_start_record));
                                }
                            }
                        } else {
                            if (taskText.getText().equals(getResources().getString(R.string.task_title_start_record))) {
                                startService(new Intent(TaskActivity.this, VideoRecService.class));
                                taskText.setText(getResources().getString(R.string.task_title_stop_record));
                            } else {
                                stopService(new Intent(TaskActivity.this, VideoRecService.class));
                                taskText.setText(getResources().getString(R.string.task_title_start_record));
                            }
                        }
                        break;
                    case 6:
                        //Trip Log
                        boolean writeLogPerms = false;
                        boolean locationLogPerms = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Check Write permissions
                            if (getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.write_alert_title));
                                builder.setMessage(getString(R.string.write_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_STORAGE);
                                    }
                                });
                                builder.show();
                                writeLogPerms = false;
                            } else {
                                writeLogPerms = true;
                            }
                            // Check Location permissions
                            if (getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.location_alert_title));
                                builder.setMessage(getString(R.string.location_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                                    }
                                });
                                builder.show();
                                locationLogPerms = false;
                            } else {
                                locationLogPerms = true;
                            }
                            if (writeLogPerms == true && locationLogPerms == true){
                                TextView tripTaskText=(TextView)view.findViewById(R.id.tv_label);
                                if (tripTaskText.getText().equals(getResources().getString(R.string.task_title_start_trip))) {
                                    startService(new Intent(TaskActivity.this, LoggingService.class));
                                    tripTaskText.setText(getResources().getString(R.string.task_title_stop_trip));
                                } else {
                                    stopService(new Intent(TaskActivity.this, LoggingService.class));
                                    tripTaskText.setText(getResources().getString(R.string.task_title_start_trip));
                                }
                            }
                        } else {
                            TextView tripTaskText = (TextView) view.findViewById(R.id.tv_label);
                            if (tripTaskText.getText().equals(getResources().getString(R.string.task_title_start_trip))) {
                                startService(new Intent(TaskActivity.this, LoggingService.class));
                                tripTaskText.setText(getResources().getString(R.string.task_title_stop_trip));
                            } else {
                                stopService(new Intent(TaskActivity.this, LoggingService.class));
                                tripTaskText.setText(getResources().getString(R.string.task_title_start_trip));
                            }
                        }
                        break;
                    case 7:
                        //Waypoint
                        // Get location
                        boolean locationWPPerms = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Check Location permissions
                            if (getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.location_alert_title));
                                builder.setMessage(getString(R.string.location_alert_body));
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @TargetApi(23)
                                    public void onDismiss(DialogInterface dialog) {
                                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                                    }
                                });
                                builder.show();
                                locationWPPerms = false;
                            } else {
                                locationWPPerms = true;
                            }
                        } else {
                            locationWPPerms = true;
                        }
                        if (locationWPPerms == true) {
                            try {
                                // Get the location manager
                                double lat;
                                double lon;
                                double speed = 0;
                                String waypoint = "";
                                LocationManager locationManager = (LocationManager)
                                        TaskActivity.this.getSystemService(LOCATION_SERVICE);
                                Criteria criteria = new Criteria();
                                String bestProvider = locationManager.getBestProvider(criteria, false);
                                Log.d(TAG,"trying bestprovider: " + bestProvider);
                                Location location = locationManager.getLastKnownLocation(bestProvider);
                                try {
                                    lat = location.getLatitude();
                                    lon = location.getLongitude();
                                    waypoint = lat + "," + lon;
                                    // Get current date/time
                                    Calendar cal = Calendar.getInstance();
                                    Date date = cal.getTime();
                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                    String curdatetime = formatter.format(date);

                                    // Open database
                                    WaypointDatasource datasource = new WaypointDatasource(TaskActivity.this);
                                    datasource.open();

                                    WaypointRecord record = new WaypointRecord(curdatetime, waypoint);
                                    datasource.addRecord(record);
                                    datasource.close();


                                } catch (NullPointerException e) {
                                    lat = -1.0;
                                    lon = -1.0;
                                }

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        break;
                    case 8:
                        //Voice Assistant
                        startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        break;
                }
            }

        });
        // End of Tasks
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case PERMISSION_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Camera permission granted");
                    //setupBLE();
                } else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_camera_alert_body));
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
            case PERMISSION_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Microphone permission granted");
                    //setupBLE();
                } else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_microphone_alert_body));
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
            case PERMISSION_REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Write to storage permission granted");
                    //setupBLE();
                } else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_write_alert_body));
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
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_location_alert_body));
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
        }
    }

    public void updateColors(boolean itsDark){
        ((MyApplication) this.getApplication()).setitsDark(itsDark);
        LinearLayout lLayout = (LinearLayout) findViewById(R.id.layout_task);
        if (itsDark) {
            lLayout.setBackgroundColor(getResources().getColor(R.color.black));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
            navbarTitle.setTextColor(getResources().getColor(R.color.white));
            backButton.setColorFilter(getResources().getColor(R.color.white));
            forwardButton.setColorFilter(getResources().getColor(R.color.white));
        } else {
            lLayout.setBackgroundColor(getResources().getColor(R.color.white));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
            navbarTitle.setTextColor(getResources().getColor(R.color.black));
            backButton.setColorFilter(getResources().getColor(R.color.black));
            forwardButton.setColorFilter(getResources().getColor(R.color.black));
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Intent backIntent = new Intent(TaskActivity.this, CompassActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Intent forwardIntent = new Intent(TaskActivity.this, MainActivity.class);
                startActivity(forwardIntent);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
