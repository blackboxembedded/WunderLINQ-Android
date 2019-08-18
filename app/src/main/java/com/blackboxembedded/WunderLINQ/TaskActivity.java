package com.blackboxembedded.WunderLINQ;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.blackboxembedded.WunderLINQ.externalcamera.goproV1API.ApiBase;
import com.blackboxembedded.WunderLINQ.externalcamera.goproV1API.ApiClient;
import com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.model.GoProResponse;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;


public class TaskActivity extends AppCompatActivity implements OsmAndHelper.OnOsmandMissingListener{

    public final static String TAG = "TaskActivity";

    private ActionBar actionBar;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private TextView navbarTitle;

    GridView gridview;
    private int lastPosition = -1;

    private List<Integer> mapping;

    private SharedPreferences sharedPrefs;

    static boolean itsDark = false;
    private long darkTimer = 0;
    private long lightTimer = 0;

    SensorManager sensorManager;
    Sensor lightSensor;

    private CountDownTimer cTimer = null;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int PERMISSION_REQUEST_CALL_PHONE = 101;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 102;
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 112;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 122;

    private boolean actionCamOnline = false;
    private boolean actionCamRecording = false;

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
        Log.d(TAG,"In oncreate");
        super.onCreate(savedInstanceState);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_task);
        gridview = findViewById(R.id.gridview_tasks);
        gridview.setDrawSelectorOnTop(false);
        gridview.setOnTouchListener(new GridOnSwipeTouchListener(this) {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getSupportActionBar().show();
                startTimer();
                return super.onTouch(v,event);
            }
            @Override
            public void onSwipeLeft() {
                Intent backIntent = new Intent(TaskActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(TaskActivity.this, MusicActivity.class);
                startActivity(backIntent);
            }
        });


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
        Log.d(TAG,"in onResume");
        super.onResume();
        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getString("prefNightModeCombo", "0").equals("1")){
            updateColors(true);
        } else {
            updateColors(false);
        }
        displayTasks();
        if (sharedPrefs.getBoolean("prefAutoNightMode", false)) {
            sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        getSupportActionBar().show();
        startTimer();

        //Check ActionCam Status
        Integer actionCamEnabled = Integer.parseInt(sharedPrefs.getString("prefActionCam", "0"));
        switch (actionCamEnabled){
            case 1:
                //GoPro Hero3
                ApiClient GoProV1Api = com.blackboxembedded.WunderLINQ.externalcamera.goproV1API.ApiBase.getMainClient().create(ApiClient.class);
                Call<ResponseBody> setting;
                String actionCamPwd = sharedPrefs.getString("ACTIONCAM_GOPRO3_PWD","");
                if (actionCamPwd.equals("")){
                    setting = GoProV1Api.status();
                } else {
                    setting = GoProV1Api.statusPwd(actionCamPwd);
                }
                setting.clone().enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try{
                            int responseCode = response.code();
                            Log.d(TAG,"GoPro Hero3 Status Response Code: " + responseCode);
                            switch(responseCode){
                                case 200:
                                    //Success
                                    if (response.body() != null) {
                                        actionCamOnline = true;
                                        byte[] byteArr = response.body().bytes();
                                        Log.d(TAG, "GoPro Hero3 Status byte array: " + Arrays.toString(byteArr));
                                        Log.d(TAG, "GoPro Hero3 Recording Status byte: " + byteArr[33]);
                                        if (byteArr[29] == 0x01) {
                                            if (!actionCamRecording) {
                                                actionCamRecording = true;
                                                displayTasks();
                                            }
                                        } else {
                                            if (actionCamRecording) {
                                                actionCamRecording = false;
                                                displayTasks();
                                            }
                                        }
                                    }
                                    break;
                                case 403:
                                    //Bad Password
                                    //Get Password
                                    Log.d(TAG,"GoPro Hero3 Bad Password");
                                    ApiClient GoProV1Api = ApiBase.getMainClient().create(ApiClient.class);
                                    Call<ResponseBody> getPwd = GoProV1Api.getPwd();
                                    getPwd.clone().enqueue(new Callback<ResponseBody>() {
                                        @Override
                                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                            try{
                                                int responseCode = response.code();
                                                Log.d(TAG,"GoPro Hero3 Get Password Response Code: " + responseCode);
                                                switch(responseCode){
                                                    case 200:
                                                        //Success
                                                        if (response.body() != null) {
                                                            actionCamOnline = true;
                                                            String actionCamPass = response.body().string().substring(2);
                                                            Log.d(TAG,"Got GoPro Hero3 Password");
                                                            //Save Password
                                                            SharedPreferences.Editor editor = sharedPrefs.edit();
                                                            editor.putString("ACTIONCAM_GOPRO3_PWD", actionCamPass);
                                                            editor.apply();

                                                            ApiClient GoProV1Api = ApiBase.getMainClient().create(ApiClient.class);
                                                            Call<ResponseBody> setting = GoProV1Api.statusPwd(actionCamPass);
                                                            String actionCamPwd = sharedPrefs.getString("ACTIONCAM_GOPRO3_PWD","");
                                                            setting.clone().enqueue(new Callback<ResponseBody>() {
                                                                @Override
                                                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                                    try{
                                                                        int responseCode = response.code();
                                                                        Log.d(TAG,"GoPro Hero3 Status(ReAuth) Response Code: " + responseCode);
                                                                        switch(responseCode){
                                                                            case 200:
                                                                                //Success
                                                                                if (response.body() != null) {
                                                                                    actionCamOnline = true;
                                                                                    byte[] byteArr = response.body().bytes();
                                                                                    Log.d(TAG, "GoPro Status(ReAuth) byte array: " + Arrays.toString(byteArr));
                                                                                    Log.d(TAG, "GoPro Recording Status(ReAuth) byte: " + byteArr[33]);
                                                                                    if (byteArr[29] == 0x01) {
                                                                                        if (!actionCamRecording) {
                                                                                            actionCamRecording = true;
                                                                                            displayTasks();
                                                                                        }
                                                                                    } else {
                                                                                        if (actionCamRecording) {
                                                                                            actionCamRecording = false;
                                                                                            displayTasks();
                                                                                        }
                                                                                    }
                                                                                }
                                                                                break;
                                                                            case 403:
                                                                                //Bad Password
                                                                                Log.d(TAG,"GoPro Hero3 Bad Password(ReAuth)");
                                                                                break;
                                                                            default:
                                                                                break;
                                                                        }
                                                                    } catch (IOException e){

                                                                    }
                                                                }

                                                                @Override
                                                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                                    // handle failure
                                                                    Log.d(TAG,"onFailure() Get GoPro Hero3 Status(ReAuth)");
                                                                    actionCamOnline = false;
                                                                    if (actionCamRecording) {
                                                                        actionCamRecording = false;
                                                                        displayTasks();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    default:
                                                        break;

                                                }
                                            } catch (IOException e){

                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                                            // handle failure
                                            Log.d(TAG,"onFailure() Get GoPro Hero3 Password");
                                            actionCamOnline = false;
                                            if (actionCamRecording) {
                                                actionCamRecording = false;
                                                displayTasks();
                                            }
                                        }
                                    });
                                    break;
                                default:
                                    break;
                            }
                        } catch (IOException e){

                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // handle failure
                        Log.d(TAG,"onFailure() Get GoPro Hero3 Status");
                        actionCamOnline = false;
                        if (actionCamRecording) {
                            actionCamRecording = false;
                            displayTasks();
                        }
                    }
                });
                break;
            case 2:
                //GoPro Hero 4+
                final com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.ApiClient GoProV2Api = com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.ApiBase.getMainClient().create(com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.ApiClient.class);
                Call<ResponseBody> goProV2Status = GoProV2Api.status();
                goProV2Status.clone().enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String responseBody = response.body().string();
                            Log.d(TAG, responseBody);
                            try {
                                JSONObject mainObject = new JSONObject(responseBody);
                                JSONObject statusObject = mainObject.getJSONObject("status");
                                String recordingState = statusObject.getString("8");
                                String macAddress = statusObject.getString("40");
                                Log.d(TAG,"GoPro Hero4+ Recording State: " + recordingState);
                                actionCamOnline = true;
                                if(recordingState.equals("1")){
                                    actionCamRecording = true;
                                } else {
                                    actionCamRecording = false;
                                }
                                displayTasks();
                            } catch (JSONException e){

                            }

                        } catch (IOException e){

                        }
                        //Keep Wifi from going to sleep
                        //"http://10.5.5.9/gp/gpControl/execute?p1=gpStream&a1=proto_v2&c1=restart"
                        //"http://10.5.5.9/gp/gpControl/execute?p1=gpStream&a1=proto_v2&c1=stop"
                        Call<GoProResponse> restartCommand = com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.model.GPConstants.Commands.Stream.Restart;
                        restartCommand.clone().enqueue(new Callback<GoProResponse>() {
                            @Override
                            public void onResponse(Call<GoProResponse> call, Response<GoProResponse> response) {
                                switch (response.code()){
                                    case 200:
                                        //Success
                                        String responseMessage = response.message();
                                        Log.d(TAG,"GoPro Hero4+ Stream Restart Response: " + responseMessage);
                                        if(responseMessage.equals("OK")) {
                                            Call<GoProResponse> stopCommand = com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.model.GPConstants.Commands.Stream.Stop;
                                            stopCommand.clone().enqueue(new Callback<GoProResponse>() {
                                                @Override
                                                public void onResponse(Call<GoProResponse> call, Response<GoProResponse> response) {
                                                    switch (response.code()){
                                                        case 200:
                                                            //Success
                                                            String responseMessage = response.message();
                                                            Log.d(TAG,"GoPro Hero4+ Stream Stop Response: " + responseMessage);
                                                            if(responseMessage.equals("OK")) {
                                                                // Get MAC address
                                                                Call<ResponseBody> goProV2Info = GoProV2Api.info();
                                                                goProV2Info.clone().enqueue(new Callback<ResponseBody>() {
                                                                    @Override
                                                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                                        switch (response.code()) {
                                                                            case 200:
                                                                                //Success
                                                                                try {
                                                                                    String responseBody = response.body().string();
                                                                                    Log.d(TAG, responseBody);
                                                                                    try {
                                                                                        JSONObject mainObject = new JSONObject(responseBody);
                                                                                        JSONObject statusObject = mainObject.getJSONObject("info");
                                                                                        final String macAddress = statusObject.getString("ap_mac");
                                                                                        Log.d(TAG,"GoPro Hero4+ MAC Address: " + macAddress);
                                                                                        Thread thread = new Thread(new Runnable() {

                                                                                            @Override
                                                                                            public void run() {
                                                                                                try  {
                                                                                                    WakeOnLan(macAddress);
                                                                                                } catch (Exception e) {
                                                                                                    e.printStackTrace();
                                                                                                }
                                                                                            }
                                                                                        });

                                                                                        thread.start();
                                                                                    } catch (JSONException e){

                                                                                    }
                                                                                } catch (IOException e){

                                                                                }
                                                                            default:
                                                                                break;
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                                        // handle failure
                                                                        Log.d(TAG,"onFailure() Get GoPro Hero4+ Info");
                                                                        actionCamOnline = false;
                                                                    }
                                                                });
                                                            }
                                                            break;
                                                        default:
                                                            break;
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<GoProResponse> call, Throwable t) {
                                                    // handle failure
                                                    Log.d(TAG,"onFailure() Get GoPro Hero4+ Stream Stop");
                                                }
                                            });
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }

                            @Override
                            public void onFailure(Call<GoProResponse> call, Throwable t) {
                                // handle failure
                                Log.d(TAG,"onFailure() Get GoPro Hero4+ Stream Restart");
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // handle failure
                        Log.d(TAG,"onFailure() Get GoPro Hero4+ Status");
                        actionCamOnline = false;
                        if (actionCamRecording) {
                            actionCamRecording = false;
                            displayTasks();
                        }
                    }

                });

                break;
            default:
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelTimer();
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelTimer();
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimer();
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

        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.quicktask_title);

        backButton = findViewById(R.id.action_back);
        forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(TaskActivity.this, MusicActivity.class);
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

        String goProVideoTaskText = getResources().getString(R.string.task_title_actioncam_start_video);
        if (actionCamRecording){
            goProVideoTaskText = getResources().getString(R.string.task_title_actioncam_stop_video);
        }

        // Tasks
        // Order must match between text, icon and onItemClick case
        final String[] taskTitles = new String[] {
                getResources().getString(R.string.task_title_navigation),
                getResources().getString(R.string.task_title_gohome),
                getResources().getString(R.string.task_title_favnumber),
                getResources().getString(R.string.task_title_callcontact),
                getResources().getString(R.string.task_title_photo),
                getResources().getString(R.string.task_title_selfie),
                videoTaskText,
                tripTaskText,
                getResources().getString(R.string.task_title_waypoint),
                getResources().getString(R.string.task_title_waypoint_nav),
                getResources().getString(R.string.task_title_voicecontrol),
                getResources().getString(R.string.task_title_settings),
                getResources().getString(R.string.task_title_homescreen),
                goProVideoTaskText,
                getResources().getString(R.string.task_title_weathermap)
        };
        int numTasks = taskTitles.length;
        Drawable[] iconId = new Drawable[numTasks];
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
            iconId[5] = getResources().getDrawable(R.drawable.ic_camera, getTheme());
            iconId[5].setTint(Color.WHITE);
            iconId[6] = getResources().getDrawable(R.drawable.ic_video_camera, getTheme());
            iconId[6].setTint(Color.WHITE);
            iconId[7] = getResources().getDrawable(R.drawable.ic_road, getTheme());
            iconId[7].setTint(Color.WHITE);
            iconId[8] = getResources().getDrawable(R.drawable.ic_map_marker, getTheme());
            iconId[8].setTint(Color.WHITE);
            iconId[9] = getResources().getDrawable(R.drawable.ic_route, getTheme());
            iconId[9].setTint(Color.WHITE);
            iconId[10] = getResources().getDrawable(R.drawable.ic_microphone, getTheme());
            iconId[10].setTint(Color.WHITE);
            iconId[11] = getResources().getDrawable(R.drawable.ic_cog, getTheme());
            iconId[11].setTint(Color.WHITE);
            iconId[12] = getResources().getDrawable(R.drawable.ic_home, getTheme());
            iconId[12].setTint(Color.WHITE);
            iconId[13] = getResources().getDrawable(R.drawable.ic_video_camera, getTheme());
            iconId[13].setTint(Color.WHITE);
            iconId[14] = getResources().getDrawable(R.drawable.ic_cloud_sun, getTheme());
            iconId[14].setTint(Color.WHITE);
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
            iconId[5] = getResources().getDrawable(R.drawable.ic_camera, getTheme());
            iconId[5].setTint(Color.BLACK);
            iconId[6] = getResources().getDrawable(R.drawable.ic_video_camera, getTheme());
            iconId[6].setTint(Color.BLACK);
            iconId[7] = getResources().getDrawable(R.drawable.ic_road, getTheme());
            iconId[7].setTint(Color.BLACK);
            iconId[8] = getResources().getDrawable(R.drawable.ic_map_marker, getTheme());
            iconId[8].setTint(Color.BLACK);
            iconId[9] = getResources().getDrawable(R.drawable.ic_route, getTheme());
            iconId[9].setTint(Color.BLACK);
            iconId[10] = getResources().getDrawable(R.drawable.ic_microphone, getTheme());
            iconId[10].setTint(Color.BLACK);
            iconId[11] = getResources().getDrawable(R.drawable.ic_cog, getTheme());
            iconId[11].setTint(Color.BLACK);
            iconId[12] = getResources().getDrawable(R.drawable.ic_home, getTheme());
            iconId[12].setTint(Color.BLACK);
            iconId[13] = getResources().getDrawable(R.drawable.ic_video_camera, getTheme());
            iconId[13].setTint(Color.BLACK);
            iconId[14] = getResources().getDrawable(R.drawable.ic_cloud_sun, getTheme());
            iconId[14].setTint(Color.BLACK);
        }

        mapping = new ArrayList<>();
        List<String> taskTitle = new ArrayList<>();
        List<Drawable> taskIcon = new ArrayList<>();
        int x = 0;
        while (x < numTasks){
            switch (x){
                case 0:
                    int selectionOne = Integer.parseInt(sharedPrefs.getString("prefQuickTaskOne", "0"));
                    if (!(selectionOne >= numTasks)){
                        mapping.add(selectionOne);
                        taskTitle.add(taskTitles[selectionOne]);
                        taskIcon.add(iconId[selectionOne]);
                    }
                    break;
                case 1:
                    int selectionTwo = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTwo", "1"));
                    if (!(selectionTwo >= numTasks)){
                        mapping.add(selectionTwo);
                        taskTitle.add(taskTitles[selectionTwo]);
                        taskIcon.add(iconId[selectionTwo]);
                    }
                    break;
                case 2:
                    int selectionThree = Integer.parseInt(sharedPrefs.getString("prefQuickTaskThree", "2"));
                    if (!(selectionThree >= numTasks)){
                        mapping.add(selectionThree);
                        taskTitle.add(taskTitles[selectionThree]);
                        taskIcon.add(iconId[selectionThree]);
                    }
                    break;
                case 3:
                    int selectionFour = Integer.parseInt(sharedPrefs.getString("prefQuickTaskFour", "3"));
                    if (!(selectionFour >= numTasks)){
                        mapping.add(selectionFour);
                        taskTitle.add(taskTitles[selectionFour]);
                        taskIcon.add(iconId[selectionFour]);
                    }
                    break;
                case 4:
                    int selectionFive = Integer.parseInt(sharedPrefs.getString("prefQuickTaskFive", "4"));
                    if (!(selectionFive >= numTasks)){
                        mapping.add(selectionFive);
                        taskTitle.add(taskTitles[selectionFive]);
                        taskIcon.add(iconId[selectionFive]);
                    }
                    break;
                case 5:
                    int selectionSix = Integer.parseInt(sharedPrefs.getString("prefQuickTaskSix", "5"));
                    if (!(selectionSix >= numTasks)){
                        mapping.add(selectionSix);
                        taskTitle.add(taskTitles[selectionSix]);
                        taskIcon.add(iconId[selectionSix]);
                    }
                    break;
                case 6:
                    int selectionSeven = Integer.parseInt(sharedPrefs.getString("prefQuickTaskSeven", "6"));
                    if (!(selectionSeven >= numTasks)){
                        mapping.add(selectionSeven);
                        taskTitle.add(taskTitles[selectionSeven]);
                        taskIcon.add(iconId[selectionSeven]);
                    }
                    break;
                case 7:
                    int selectionEight = Integer.parseInt(sharedPrefs.getString("prefQuickTaskEight", "7"));
                    if (!(selectionEight >= numTasks)){
                        mapping.add(selectionEight);
                        taskTitle.add(taskTitles[selectionEight]);
                        taskIcon.add(iconId[selectionEight]);
                    }
                    break;
                case 8:
                    int selectionNine = Integer.parseInt(sharedPrefs.getString("prefQuickTaskNine", "8"));
                    if (!(selectionNine >= numTasks)){
                        mapping.add(selectionNine);
                        taskTitle.add(taskTitles[selectionNine]);
                        taskIcon.add(iconId[selectionNine]);
                    }
                    break;
                case 9:
                    int selectionTen = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTen", "9"));
                    Log.d(TAG,"Selection: " + selectionTen);
                    if (!(selectionTen >= numTasks)){
                        Log.d(TAG,"Mapping: " + selectionTen);
                        mapping.add(selectionTen);
                        taskTitle.add(taskTitles[selectionTen]);
                        taskIcon.add(iconId[selectionTen]);
                    }
                    break;
                case 10:
                    int selectionEleven = Integer.parseInt(sharedPrefs.getString("prefQuickTaskEleven", "10"));
                    if (!(selectionEleven >= numTasks)){
                        mapping.add(selectionEleven);
                        taskTitle.add(taskTitles[selectionEleven]);
                        taskIcon.add(iconId[selectionEleven]);
                    }
                    break;
                case 11:
                    int selectionTwelve = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTwelve", "11"));
                    if (!(selectionTwelve >= numTasks)){
                        mapping.add(selectionTwelve);
                        taskTitle.add(taskTitles[selectionTwelve]);
                        taskIcon.add(iconId[selectionTwelve]);
                    }
                    break;
                default:
                    break;
            }
            x = x + 1 ;
        }

        gridview.setDrawSelectorOnTop(false);
        gridview.setAdapter(new TaskAdapter(this,taskTitle,taskIcon,itsDark));
        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                lastPosition = position;
                String navApp = sharedPrefs.getString("prefNavApp", "1");
                switch (mapping.get(position)){
                    case 0:
                        //Navigation
                        String url = "google.navigation:/?free=1&mode=d&entry=fnls";
                        if (navApp.equals("1") || navApp.equals("2")){
                            // Android Default or Google Maps
                            url = "google.navigation:/?free=1&mode=d&entry=fnls";
                        } else if (navApp.equals("3")){
                            //Locus

                        } else if (navApp.equals("4")){
                            //Waze
                            url = "https://waze.com/ul";
                        } else if (navApp.equals("5")){
                            //Maps.me
                            url = "mapsme://?id=WunderLINQ&backurl=wunderlinq://&appname=WunderLINQ";
                        } else if (navApp.equals("6")){
                            // OsmAnd
                            url = "http://osmand.net/go";
                        }
                        try {
                            Intent navIntent = new Intent(android.content.Intent.ACTION_VIEW);
                            navIntent.setData(Uri.parse(url));
                            if (android.os.Build.VERSION.SDK_INT >= 24) {
                                navIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                            }
                            startActivity(navIntent);
                        } catch ( ActivityNotFoundException ex  ) {
                            // Add Alert
                        }
                        break;
                    case 1:
                        //Navigate Home
                        String address = sharedPrefs.getString("prefHomeAddress","");
                        if (!address.equals("")) {
                            LatLng location = getLocationFromAddress(TaskActivity.this, address);
                            if (location != null) {
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
                                    } else {
                                        locationWPPerms = true;
                                    }
                                } else {
                                    locationWPPerms = true;
                                }
                                if (locationWPPerms) {
                                    try {
                                        // Get the location manager
                                        LocationManager locationManager = (LocationManager)
                                                TaskActivity.this.getSystemService(LOCATION_SERVICE);
                                        Criteria criteria = new Criteria();
                                        try {
                                            String bestProvider = locationManager.getBestProvider(criteria, false);
                                            Log.d(TAG,"Trying Best Provider: " + bestProvider);
                                            Location currentLocation = locationManager.getLastKnownLocation(bestProvider);
                                            String navUrl = "google.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                                            if (navApp.equals("1") || navApp.equals("2")){
                                                // Android Default or Google Maps
                                                // Nothing to do
                                                navUrl = "google.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                                            } else if (navApp.equals("3")){
                                                //Locus

                                            } else if (navApp.equals("4")){
                                                //Waze
                                                navUrl = "https://www.waze.com/ul?ll=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes&zoom=17";
                                            } else if (navApp.equals("5")){
                                                //Maps.me
                                                navUrl = "mapsme://route?sll=" + String.valueOf(currentLocation.getLatitude()) + "," + String.valueOf(currentLocation.getLongitude()) + "&saddr=Start&dll=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&daddr=Home&type=vehicle";
                                            } else if (navApp.equals("6")){
                                                // OsmAnd
                                                //navUrl = "osmand.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                                                OsmAndHelper osmAndHelper = new OsmAndHelper(TaskActivity.this, OsmAndHelper.REQUEST_OSMAND_API, TaskActivity.this);
                                                osmAndHelper.navigate("Start",currentLocation.getLatitude(),currentLocation.getLongitude(),"Destination",location.latitude,location.longitude,"motorcycle", true);
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
                                        } catch (NullPointerException e) {
                                            e.printStackTrace();
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            } else {
                                Toast.makeText(TaskActivity.this, R.string.geocode_error, Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(TaskActivity.this, R.string.toast_address_not_set, Toast.LENGTH_LONG).show();
                        }
                        break;
                    case 2:
                        //Call Favorite Number
                        boolean callPerms = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(TaskActivity.this);
                                builder.setTitle(getString(R.string.call_alert_title));
                                builder.setMessage(getString(R.string.call_alert_body));
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
                                callPerms = true;
                            }
                        }  else {
                            // Android version is lesser than 6.0 or the permission is already granted.
                            callPerms = true;
                        }
                        if (callPerms) {
                            String phonenumber = sharedPrefs.getString("prefHomePhone", "");
                            if (!phonenumber.equals("")) {
                                Intent callHomeIntent = new Intent(Intent.ACTION_CALL);
                                callHomeIntent.setData(Uri.parse("tel:" + phonenumber));
                                startActivity(callHomeIntent);
                            } else {
                                Toast.makeText(TaskActivity.this, R.string.toast_phone_not_set, Toast.LENGTH_LONG).show();
                            }
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
                        break;
                    case 4:
                        //Take photo
                        Log.d(TAG,"Take Rear Photo");
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
                            } else {
                                writePerms = true;
                            }

                            if (cameraPerms && writePerms){
                                Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                                photoIntent.putExtra("camera",CameraCharacteristics.LENS_FACING_BACK);
                                startService(photoIntent);
                            }

                        } else {
                            Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                            photoIntent.putExtra("camera", CameraCharacteristics.LENS_FACING_BACK);
                            startService(photoIntent);
                        }
                        break;
                    case 5:
                        //Take selfie
                        Log.d(TAG,"Take Front Photo");
                        boolean selfieCameraPerms = false;
                        boolean selfieWritePerms = false;
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
                            } else {
                                selfieCameraPerms = true;
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
                            } else {
                                selfieWritePerms = true;
                            }

                            if (selfieCameraPerms && selfieWritePerms){
                                Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                                photoIntent.putExtra("camera",CameraCharacteristics.LENS_FACING_FRONT);
                                startService(photoIntent);
                            }

                        } else {
                            Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                            photoIntent.putExtra("camera", CameraCharacteristics.LENS_FACING_FRONT);
                            startService(photoIntent);
                        }
                        break;
                    case 6:
                        //Record Video
                        TextView taskText = view.findViewById(R.id.gridTextView);

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
                            } else {
                                overlayVidPerms = true;
                            }
                            if (cameraVidPerms && audioVidPerms && writeVidPerms && overlayVidPerms){
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
                    case 7:
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
                            } else {
                                locationLogPerms = true;
                            }
                            if (writeLogPerms && locationLogPerms){
                                TextView tripTaskText = view.findViewById(R.id.gridTextView);
                                if (tripTaskText.getText().equals(getResources().getString(R.string.task_title_start_trip))) {
                                    startService(new Intent(TaskActivity.this, LoggingService.class));
                                    tripTaskText.setText(getResources().getString(R.string.task_title_stop_trip));
                                } else {
                                    stopService(new Intent(TaskActivity.this, LoggingService.class));
                                    tripTaskText.setText(getResources().getString(R.string.task_title_start_trip));
                                }
                            }
                        } else {
                            TextView tripTaskText = view.findViewById(R.id.tv_label);
                            if (tripTaskText.getText().equals(getResources().getString(R.string.task_title_start_trip))) {
                                startService(new Intent(TaskActivity.this, LoggingService.class));
                                tripTaskText.setText(getResources().getString(R.string.task_title_stop_trip));
                            } else {
                                stopService(new Intent(TaskActivity.this, LoggingService.class));
                                tripTaskText.setText(getResources().getString(R.string.task_title_start_trip));
                            }
                        }
                        break;
                    case 8:
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
                            } else {
                                locationWPPerms = true;
                            }
                        } else {
                            locationWPPerms = true;
                        }
                        if (locationWPPerms) {
                            try {
                                // Get the location manager
                                double lat;
                                double lon;
                                LocationManager locationManager = (LocationManager)
                                        TaskActivity.this.getSystemService(LOCATION_SERVICE);
                                Criteria criteria = new Criteria();
                                try {
                                    String bestProvider = locationManager.getBestProvider(criteria, false);
                                    Log.d(TAG,"Trying Best Provider: " + bestProvider);
                                    Location location = locationManager.getLastKnownLocation(bestProvider);
                                    lat = location.getLatitude();
                                    lon = location.getLongitude();
                                    String waypoint = lat + "," + lon;
                                    // Get current date/time
                                    Calendar cal = Calendar.getInstance();
                                    Date date = cal.getTime();
                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
                                    String curdatetime = formatter.format(date);

                                    // Open database
                                    WaypointDatasource datasource = new WaypointDatasource(TaskActivity.this);
                                    datasource.open();

                                    WaypointRecord record = new WaypointRecord(curdatetime, waypoint, "");
                                    datasource.addRecord(record);
                                    datasource.close();

                                    Toast.makeText(TaskActivity.this, R.string.toast_waypoint_saved, Toast.LENGTH_LONG).show();

                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        break;
                    case 9:
                        //Navigate to Waypoint
                        Intent forwardIntent = new Intent(TaskActivity.this, WaypointNavActivity.class);
                        startActivity(forwardIntent);
                        break;
                    case 10:
                        //Voice Assistant
                        startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        break;
                    case 11:
                        //Settings
                        Intent settingsIntent = new Intent(TaskActivity.this, SettingsActivity.class);
                        startActivity(settingsIntent);
                        break;
                    case 12:
                        //Home Screen
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startActivity(startMain);
                        break;
                    case 13:
                        //ActionCam Video Control
                        if (actionCamOnline) {
                            final View txtView = view;
                            Integer actionCamEnabled = Integer.parseInt(sharedPrefs.getString("prefActionCam", "0"));
                            switch (actionCamEnabled) {
                                case 1:
                                    //GoPro Hero3
                                    if (actionCamRecording) {
                                        ApiClient GoProV1Api = ApiBase.getMainClient().create(ApiClient.class);
                                        Call<ResponseBody> shutterCommand;
                                        String actionCamPwd = sharedPrefs.getString("ACTIONCAM_GOPRO3_PWD", "");
                                        if (actionCamPwd.equals("")) {
                                            shutterCommand = GoProV1Api.command("bacpac", "SH", "%00");
                                        } else {
                                            shutterCommand = GoProV1Api.commandPwd("bacpac", "SH", actionCamPwd, "%00");
                                        }
                                        shutterCommand.clone().enqueue(new Callback<ResponseBody>() {
                                            @Override
                                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                int responseCode = response.code();
                                                Log.d(TAG, "GoPro Shutter Off Response Code: " + responseCode);
                                                switch (responseCode) {
                                                    case 200:
                                                        actionCamOnline = true;
                                                        actionCamRecording = false;
                                                        TextView tripTaskText = txtView.findViewById(R.id.gridTextView);
                                                        tripTaskText.setText(getResources().getString(R.string.task_title_actioncam_start_video));
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                Log.d(TAG, "onFailure(): GoPro Shutter Off");
                                            }

                                        });
                                    } else {
                                        final ApiClient GoProV1Api = ApiBase.getMainClient().create(ApiClient.class);
                                        Call<ResponseBody> shutterCommand;
                                        String actionCamPwd = sharedPrefs.getString("ACTIONCAM_GOPRO3_PWD", "");
                                        if (actionCamPwd.equals("")) {
                                            shutterCommand = GoProV1Api.command("bacpac", "SH", "%01");
                                        } else {
                                            shutterCommand = GoProV1Api.commandPwd("bacpac", "SH", actionCamPwd, "%01");
                                        }
                                        shutterCommand.clone().enqueue(new Callback<ResponseBody>() {
                                            @Override
                                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                int responseCode = response.code();
                                                Log.d(TAG, "GoPro Shutter On Response Code" + responseCode);
                                                switch (responseCode) {
                                                    case 200:
                                                        actionCamOnline = true;
                                                        actionCamRecording = true;
                                                        TextView tripTaskText = txtView.findViewById(R.id.gridTextView);
                                                        tripTaskText.setText(getResources().getString(R.string.task_title_actioncam_stop_video));
                                                        break;
                                                    case 410:
                                                        //Camera Off
                                                        //Turn Camera On
                                                        Call<ResponseBody> onCommand;
                                                        String actionCamPwd = sharedPrefs.getString("ACTIONCAM_GOPRO3_PWD", "");
                                                        if (actionCamPwd.equals("")) {
                                                            onCommand = GoProV1Api.command("bacpac", "PW", "%01");
                                                        } else {
                                                            onCommand = GoProV1Api.commandPwd("bacpac", "PW", actionCamPwd, "%01");
                                                        }
                                                        onCommand.clone().enqueue(new Callback<ResponseBody>() {
                                                            @Override
                                                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                                int responseCode = response.code();
                                                                Log.d(TAG, "GoPro Power On Response Code: " + responseCode);
                                                                switch (responseCode) {
                                                                    case 200:
                                                                        actionCamOnline = true;
                                                                        Call<ResponseBody> shutterCommand;
                                                                        String actionCamPwd = sharedPrefs.getString("ACTIONCAM_GOPRO3_PWD", "");
                                                                        if (actionCamPwd.equals("")) {
                                                                            shutterCommand = GoProV1Api.command("bacpac", "SH", "%01");
                                                                        } else {
                                                                            shutterCommand = GoProV1Api.commandPwd("bacpac", "SH", actionCamPwd, "%01");
                                                                        }
                                                                        shutterCommand.clone().enqueue(new Callback<ResponseBody>() {
                                                                            @Override
                                                                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                                                int responseCode = response.code();
                                                                                Log.d(TAG, "GoPro Shutter On Response Code" + responseCode);
                                                                                switch (responseCode) {
                                                                                    case 200:
                                                                                        actionCamOnline = true;
                                                                                        actionCamRecording = true;
                                                                                        TextView tripTaskText = txtView.findViewById(R.id.gridTextView);
                                                                                        tripTaskText.setText(getResources().getString(R.string.task_title_actioncam_stop_video));
                                                                                        break;
                                                                                    default:
                                                                                        break;
                                                                                }
                                                                            }

                                                                            @Override
                                                                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                                                Log.d(TAG, "onFailure() GoPro Hero3 Shutter On");
                                                                                actionCamOnline = false;
                                                                                if (actionCamRecording) {
                                                                                    actionCamRecording = false;
                                                                                    displayTasks();
                                                                                }
                                                                            }
                                                                        });

                                                                        break;
                                                                    default:
                                                                        break;
                                                                }

                                                            }

                                                            @Override
                                                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                                Log.d(TAG, "onFailure() GoPro Hero3 Power On");
                                                                actionCamOnline = false;
                                                                if (actionCamRecording) {
                                                                    actionCamRecording = false;
                                                                    displayTasks();
                                                                }
                                                            }
                                                        });
                                                        break;
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                                Log.d(TAG, "onFailure() GoPro Hero3 Shutter On");
                                                actionCamOnline = false;
                                                if (actionCamRecording) {
                                                    actionCamRecording = false;
                                                    displayTasks();
                                                }
                                            }
                                        });
                                    }
                                    break;
                                case 2:
                                    //GoPro Hero4+
                                    if (actionCamRecording) {
                                        Call<GoProResponse> shutterCommand = com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.model.GPConstants.Commands.Shutter.stop;
                                        shutterCommand.clone().enqueue(new Callback<GoProResponse>() {
                                            @Override
                                            public void onResponse(Call<GoProResponse> call, Response<GoProResponse> response) {
                                                switch (response.code()) {
                                                    case 200:
                                                        //Success
                                                        String responseMessage = response.message();
                                                        Log.d(TAG, "GoPro Hero4+ Off Response: " + responseMessage);
                                                        if (responseMessage.equals("OK")) {
                                                            actionCamOnline = true;
                                                            actionCamRecording = true;
                                                            TextView tripTaskText = txtView.findViewById(R.id.gridTextView);
                                                            tripTaskText.setText(getResources().getString(R.string.task_title_actioncam_start_video));
                                                        }
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<GoProResponse> call, Throwable t) {
                                                // handle failure
                                                Log.d(TAG, "onFailure() GoPro Hero4+ Off");
                                                actionCamOnline = false;
                                                if (actionCamRecording) {
                                                    actionCamRecording = false;
                                                    displayTasks();
                                                }
                                            }
                                        });

                                    } else {
                                        Call<GoProResponse> shutterCommand = com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.model.GPConstants.Commands.Shutter.shutter;
                                        shutterCommand.clone().enqueue(new Callback<GoProResponse>() {
                                            @Override
                                            public void onResponse(Call<GoProResponse> call, Response<GoProResponse> response) {
                                                switch (response.code()) {
                                                    case 200:
                                                        //Success
                                                        String responseMessage = response.message();
                                                        Log.d(TAG, "GoPro Hero4+ On Response: " + responseMessage);
                                                        if (responseMessage.equals("OK")) {
                                                            actionCamOnline = true;
                                                            actionCamRecording = true;
                                                            TextView tripTaskText = txtView.findViewById(R.id.gridTextView);
                                                            tripTaskText.setText(getResources().getString(R.string.task_title_actioncam_stop_video));
                                                        }
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<GoProResponse> call, Throwable t) {
                                                // handle failure
                                                Log.d(TAG, "onFailure() GoPro Hero4+ On");
                                                actionCamOnline = false;
                                                if (actionCamRecording) {
                                                    actionCamRecording = false;
                                                    displayTasks();
                                                }
                                            }
                                        });
                                    }
                                    break;
                                default:
                                    //ActionCam Not Set
                                    Toast.makeText(TaskActivity.this, R.string.toast_actioncam_notset, Toast.LENGTH_LONG).show();
                                    break;

                            }
                        } else {
                            //ActionCam Not Detected
                            Toast.makeText(TaskActivity.this, R.string.toast_actioncam_notconnected, Toast.LENGTH_LONG).show();
                        }
                        break;
                    case 14:
                        //Weather Map
                        Intent weatherIntent = new Intent(TaskActivity.this, WeatherMapActivity.class);
                        startActivity(weatherIntent);
                        break;
                    default:
                        break;
                }
            }
        });
        // End of Tasks
    }

    public LatLng getLocationFromAddress(Context context, String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;
        LatLng p1 = null;

        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            if (address.size() > 0) {
                Address location = address.get(0);
                p1 = new LatLng(location.getLatitude(), location.getLongitude());
            }

        } catch (IOException ex) {

            ex.printStackTrace();
        }

        return p1;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case PERMISSION_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Camera permission granted");
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
            }
            case PERMISSION_REQUEST_CALL_PHONE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Call Phone permission granted");
                } else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_call_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
            case PERMISSION_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Call Phone permission granted");
                } else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_contacts_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
            case PERMISSION_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Record Audio permission granted");
                } else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_record_audio_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
            case PERMISSION_REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Write to storage permission granted");
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
            }
        }
    }

    public void updateColors(boolean itsDark){
        ((MyApplication) this.getApplication()).setitsDark(itsDark);
        if (itsDark) {
            //Set Brightness to defaults
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = -1;
            getWindow().setAttributes(layoutParams);

            gridview.setBackgroundColor(getResources().getColor(R.color.black));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
            navbarTitle.setTextColor(getResources().getColor(R.color.white));
            backButton.setColorFilter(getResources().getColor(R.color.white));
            forwardButton.setColorFilter(getResources().getColor(R.color.white));
        } else {
            if (sharedPrefs.getBoolean("prefBrightnessOverride", false)) {
                //Set Brightness to 100%
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = 1;
                getWindow().setAttributes(layoutParams);
            }

            gridview.setBackgroundColor(getResources().getColor(R.color.white));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
            navbarTitle.setTextColor(getResources().getColor(R.color.black));
            backButton.setColorFilter(getResources().getColor(R.color.black));
            forwardButton.setColorFilter(getResources().getColor(R.color.black));
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                gridview.setSelection(lastPosition);
                Intent backIntent = new Intent(TaskActivity.this, MusicActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                gridview.setSelection(lastPosition);
                Intent forwardIntent = new Intent(TaskActivity.this, MainActivity.class);
                startActivity(forwardIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if ((gridview.getSelectedItemPosition() == (mapping.size() - 1)) && lastPosition == (mapping.size()  - 1) ){
                    lastPosition = 0;
                    gridview.setSelection(lastPosition);
                } else {
                    lastPosition = lastPosition + 1;
                    gridview.setSelection(lastPosition);
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (lastPosition == -1){
                    lastPosition = lastPosition + 1;
                    gridview.setSelection(lastPosition);
                } else if (gridview.getSelectedItemPosition() == 0 && lastPosition == 0){
                    lastPosition = mapping.size() - 1;
                    gridview.setSelection(lastPosition);
                } else {
                    lastPosition = lastPosition - 1;
                    gridview.setSelection(lastPosition);
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    //start timer function
    void startTimer() {
        cTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                getSupportActionBar().hide();
            }
        };
        cTimer.start();
    }

    //cancel timer
    void cancelTimer() {
        if(cTimer!=null)
            cTimer.cancel();
    }

    public void WakeOnLan(String macAddress) {
        // from http://www.jibble.org/wake-on-lan/WakeOnLan.java

        String ipStr = "10.5.5.9";
        String macStr = macAddress;
        int port = 80;

        try {
            byte[] macBytes = getMacBytes(macStr);
            byte[] bytes = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xff;
            }
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }

            InetAddress address = InetAddress.getByName(ipStr);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
            Thread.sleep(3000);
            Log.d(TAG,"Wake-on-LAN packet sent.");
        }
        catch (Exception e) {
            Log.d(TAG,"Failed to send Wake-on-LAN packet: " + e);
        }

    }

    private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        Log.d(TAG,"getMacBytes()");
        byte[] bytes = new byte[6];
        //String[] hex = macStr.split("(\\:|\\-)");
        //String[] hex = macStr.split("(?<=\\G.{" + 2 + "})");
        String[] hex = splitEqually(macStr,2).toArray(new String[6]);
        Log.d(TAG,"MAC array: " + java.util.Arrays.toString(hex));
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }

    public static List<String> splitEqually(String text, int size) {
        // Give the list the right capacity to start with. You could use an array
        // instead if you wanted.
        List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);

        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }
}

