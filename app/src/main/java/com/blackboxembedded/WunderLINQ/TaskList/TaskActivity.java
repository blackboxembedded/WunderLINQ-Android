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
package com.blackboxembedded.WunderLINQ.TaskList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.blackboxembedded.WunderLINQ.AccessoryActivity;
import com.blackboxembedded.WunderLINQ.TaskList.Activities.AppListActivity;
import com.blackboxembedded.WunderLINQ.TaskList.Activities.ContactListActivity;
import com.blackboxembedded.WunderLINQ.LoggingService;
import com.blackboxembedded.WunderLINQ.MainActivity;
import com.blackboxembedded.WunderLINQ.MusicActivity;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.NavAppHelper;
import com.blackboxembedded.WunderLINQ.OsmAndHelper;
import com.blackboxembedded.WunderLINQ.PhotoService;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.SettingsActivity;
import com.blackboxembedded.WunderLINQ.VideoRecService;
import com.blackboxembedded.WunderLINQ.TaskList.Activities.VolumeActivity;
import com.blackboxembedded.WunderLINQ.WaypointDatasource;
import com.blackboxembedded.WunderLINQ.TaskList.Activities.WaypointNavActivity;
import com.blackboxembedded.WunderLINQ.WaypointRecord;
import com.blackboxembedded.WunderLINQ.TaskList.Activities.WeatherMapActivity;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.google.android.gms.maps.model.LatLng;
import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.transform.Pivot;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TaskActivity extends AppCompatActivity implements OsmAndHelper.OnOsmandMissingListener {

    private final static String TAG = "TaskActivity";
    private DiscreteScrollView taskListView;
    private TaskAdapter adapter;
    final ArrayList<TaskItem> taskItems = new ArrayList<>();

    private SharedPreferences sharedPrefs;

    private CountDownTimer cTimer = null;
    private boolean timerRunning = false;

    private List<Integer> mapping;

    private int selected = 0;

    private final DiscreteScrollView.ScrollStateChangeListener scrollListener = new DiscreteScrollView.ScrollStateChangeListener() {
        @Override
        public void onScrollStart(@NonNull RecyclerView.ViewHolder currentItemHolder, int adapterPosition) {
        }

        @Override
        public void onScrollEnd(@NonNull RecyclerView.ViewHolder currentItemHolder, int adapterPosition) {
            selected = adapterPosition;
            adapter.selected = selected;
            if (!taskListView.isComputingLayout()){
                updateTasks();
            }
            if (!getSupportActionBar().isShowing()){
                getSupportActionBar().show();
                startTimer();
            }
        }

        @Override
        public void onScroll(float scrollPosition, int currentPosition, int newPosition, @Nullable RecyclerView.ViewHolder currentHolder, @Nullable RecyclerView.ViewHolder newCurrent) {
        }
    };

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

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

        taskListView = findViewById(R.id.main_task_view);

        adapter = new TaskAdapter(this, taskItems, new TaskAdapter.AdapterCallback() {
            @Override
            public void onItemClicked(final Integer taskPosition) {
                selected = taskPosition;
                executeTask(taskPosition);
            }
        });

        taskListView.setAdapter(adapter);

        taskListView.setItemTransformer(new ScaleTransformer.Builder()
                .setMaxScale(1.00f)
                .setMinScale(0.75f)
                .setPivotX(Pivot.X.CENTER) // CENTER is a default one
                .setPivotY(Pivot.Y.BOTTOM) // CENTER is a default one
                .build());

        showActionBar();
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        updateTasks();
        getSupportActionBar().show();
        taskListView.addScrollStateChangeListener(scrollListener);
        startTimer();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        Log.d(TAG,"onPause");
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
        Log.d(TAG,"onStop");
        super.onStop();
        taskListView.removeScrollStateChangeListener(scrollListener);
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
        taskListView.removeScrollStateChangeListener(scrollListener);
        cancelTimer();
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            startService(new Intent(TaskActivity.this,
                    VideoRecService.class));
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                updateTasks();
                executeTask(adapter.selected);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                goBack();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                goForward();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                if ((adapter.selected != (mapping.size() - 1))){
                    adapter.selected = adapter.selected + 1;
                }
                selected = adapter.selected;
                updateTasks();
                taskListView.scrollToPosition(adapter.selected);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                if (adapter.selected != 0){
                    adapter.selected = adapter.selected - 1;
                }
                selected = adapter.selected;
                updateTasks();
                taskListView.scrollToPosition(adapter.selected);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void osmandMissing() {
        //OsmAndMissingDialogFragment().show(supportFragmentManager, null);
    }

    //Build ActionBar
    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.quicktask_title);

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

    //Start Timer to hide the ActionBar
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

    //Cancel Timer to hide the ActionBar
    void cancelTimer() {
        if(cTimer!=null)
            cTimer.cancel();
    }

    //Go to next screen
    private void goForward(){
        Intent forwardIntent = new Intent(this, MainActivity.class);
        if (Data.wlq != null) {
            if (Data.wlq.getStatus() != null) {
                forwardIntent = new Intent(this, AccessoryActivity.class);
            }
        }
        startActivity(forwardIntent);
    }

    //Go previous screen
    private void goBack(){
        Intent backIntent = new Intent(this, MusicActivity.class);
        startActivity(backIntent);
    }

    //Update Tasks
    public void updateTasks(){
        String videoTaskText = getResources().getString(R.string.task_title_start_record);
        String videoFrontTaskText = getResources().getString(R.string.task_title_front_start_record);
        if (((MyApplication) this.getApplication()).getVideoRecording()){
            videoTaskText = getResources().getString(R.string.task_title_stop_record);
            videoFrontTaskText = getResources().getString(R.string.task_title_stop_record);
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
                getResources().getString(R.string.task_title_selfie),
                videoTaskText,
                tripTaskText,
                getResources().getString(R.string.task_title_waypoint),
                getResources().getString(R.string.task_title_waypoint_nav),
                getResources().getString(R.string.task_title_voicecontrol),
                getResources().getString(R.string.task_title_settings),
                getResources().getString(R.string.task_title_homescreen),
                getResources().getString(R.string.task_title_gopro),
                getResources().getString(R.string.task_title_weathermap),
                getResources().getString(R.string.task_title_applauncher),
                getResources().getString(R.string.task_title_roadbook),
                getResources().getString(R.string.task_title_systemvolume),
                getResources().getString(R.string.task_title_insta360),
                videoFrontTaskText

        };
        int numTasks = taskTitles.length;
        int[] iconId = new int[numTasks];
        iconId[0] = R.drawable.ic_map;
        iconId[1] = R.drawable.ic_home;
        iconId[2] = R.drawable.ic_phone;
        iconId[3] = R.drawable.ic_address_book;
        iconId[4] = R.drawable.ic_camera;
        iconId[5] = R.drawable.ic_portrait;
        iconId[6] = R.drawable.ic_video_camera;
        iconId[7] = R.drawable.ic_road;
        iconId[8] = R.drawable.ic_map_marker;
        iconId[9] = R.drawable.ic_route;
        iconId[10] = R.drawable.ic_microphone;
        iconId[11] = R.drawable.ic_cog;
        iconId[12] = R.drawable.ic_android;
        iconId[13] = R.drawable.ic_action_camera;
        iconId[14] = R.drawable.ic_cloud_sun;
        iconId[15] = R.drawable.ic_android;
        iconId[16] = R.drawable.ic_roadbook;
        iconId[17] = R.drawable.ic_volume_up;
        iconId[18] = R.drawable.ic_spherical_camera;
        iconId[19] = R.drawable.ic_video_camera;

        mapping = new ArrayList<>();
        taskItems.clear();
        int x = 0;
        while (x < numTasks){
            switch (x){
                case 0:
                    int selectionOne = Integer.parseInt(sharedPrefs.getString("prefQuickTaskOne", "1"));
                    if (!(selectionOne >= numTasks)){
                        mapping.add(selectionOne);
                        taskItems.add(new TaskItem(iconId[selectionOne], taskTitles[selectionOne]));
                    }
                    break;
                case 1:
                    int selectionTwo = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTwo", "2"));
                    if (!(selectionTwo >= numTasks)){
                        mapping.add(selectionTwo);
                        taskItems.add(new TaskItem(iconId[selectionTwo], taskTitles[selectionTwo]));
                    }
                    break;
                case 2:
                    int selectionThree = Integer.parseInt(sharedPrefs.getString("prefQuickTaskThree", "3"));
                    if (!(selectionThree >= numTasks)){
                        mapping.add(selectionThree);
                        taskItems.add(new TaskItem(iconId[selectionThree], taskTitles[selectionThree]));
                    }
                    break;
                case 3:
                    int selectionFour = Integer.parseInt(sharedPrefs.getString("prefQuickTaskFour", "4"));
                    if (!(selectionFour >= numTasks)){
                        mapping.add(selectionFour);
                        taskItems.add(new TaskItem(iconId[selectionFour], taskTitles[selectionFour]));
                    }
                    break;
                case 4:
                    int selectionFive = Integer.parseInt(sharedPrefs.getString("prefQuickTaskFive", "5"));
                    if (!(selectionFive >= numTasks)){
                        mapping.add(selectionFive);
                        taskItems.add(new TaskItem(iconId[selectionFive], taskTitles[selectionFive]));
                    }
                    break;
                case 5:
                    int selectionSix = Integer.parseInt(sharedPrefs.getString("prefQuickTaskSix", "6"));
                    if (!(selectionSix >= numTasks)){
                        mapping.add(selectionSix);
                        taskItems.add(new TaskItem(iconId[selectionSix], taskTitles[selectionSix]));
                    }
                    break;
                case 6:
                    int selectionSeven = Integer.parseInt(sharedPrefs.getString("prefQuickTaskSeven", "7"));
                    if (!(selectionSeven >= numTasks)){
                        mapping.add(selectionSeven);
                        taskItems.add(new TaskItem(iconId[selectionSeven], taskTitles[selectionSeven]));
                    }
                    break;
                case 7:
                    int selectionEight = Integer.parseInt(sharedPrefs.getString("prefQuickTaskEight", "8"));
                    if (!(selectionEight >= numTasks)){
                        mapping.add(selectionEight);
                        taskItems.add(new TaskItem(iconId[selectionEight], taskTitles[selectionEight]));
                    }
                    break;
                case 8:
                    int selectionNine = Integer.parseInt(sharedPrefs.getString("prefQuickTaskNine", "9"));
                    if (!(selectionNine >= numTasks)){
                        mapping.add(selectionNine);
                        taskItems.add(new TaskItem(iconId[selectionNine], taskTitles[selectionNine]));
                    }
                    break;
                case 9:
                    int selectionTen = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTen", "10"));
                    if (!(selectionTen >= numTasks)){
                        mapping.add(selectionTen);
                        taskItems.add(new TaskItem(iconId[selectionTen], taskTitles[selectionTen]));
                    }
                    break;
                case 10:
                    int selectionEleven = Integer.parseInt(sharedPrefs.getString("prefQuickTaskEleven", "11"));
                    if (!(selectionEleven >= numTasks)){
                        mapping.add(selectionEleven);
                        taskItems.add(new TaskItem(iconId[selectionEleven], taskTitles[selectionEleven]));
                    }
                    break;
                case 11:
                    int selectionTwelve = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTwelve", "12"));
                    if (!(selectionTwelve >= numTasks)){
                        mapping.add(selectionTwelve);
                        taskItems.add(new TaskItem(iconId[selectionTwelve], taskTitles[selectionTwelve]));
                    }
                    break;
                case 12:
                    int selectionThirteen = Integer.parseInt(sharedPrefs.getString("prefQuickTaskThirteen", "13"));
                    if (!(selectionThirteen >= numTasks)){
                        mapping.add(selectionThirteen);
                        taskItems.add(new TaskItem(iconId[selectionThirteen], taskTitles[selectionThirteen]));
                    }
                    break;
                case 13:
                    int selectionFourteen = Integer.parseInt(sharedPrefs.getString("prefQuickTaskFourteen", "14"));
                    if (!(selectionFourteen >= numTasks)){
                        mapping.add(selectionFourteen);
                        taskItems.add(new TaskItem(iconId[selectionFourteen], taskTitles[selectionFourteen]));
                    }
                    break;
                case 14:
                    int selectionFifteen = Integer.parseInt(sharedPrefs.getString("prefQuickTaskFifteen", "15"));
                    if (!(selectionFifteen >= numTasks)){
                        mapping.add(selectionFifteen);
                        taskItems.add(new TaskItem(iconId[selectionFifteen], taskTitles[selectionFifteen]));
                    }
                    break;
                case 15:
                    int selectionSixteen = Integer.parseInt(sharedPrefs.getString("prefQuickTaskSixteen", "16"));
                    if (!(selectionSixteen >= numTasks)){
                        mapping.add(selectionSixteen);
                        taskItems.add(new TaskItem(iconId[selectionSixteen], taskTitles[selectionSixteen]));
                    }
                    break;
                case 16:
                    int selectionSeventeen = Integer.parseInt(sharedPrefs.getString("prefQuickTaskSeventeen", "17"));
                    if (!(selectionSeventeen >= numTasks)){
                        mapping.add(selectionSeventeen);
                        taskItems.add(new TaskItem(iconId[selectionSeventeen], taskTitles[selectionSeventeen]));
                    }
                    break;
                default:
                    break;
            }
            x = x + 1;
        }
        adapter.notifyDataSetChanged();
    }

    //Task Actions
    private void executeTask(int taskID){
        switch (mapping.get(taskID)){
            case 0:
                // Open Navigation App
                NavAppHelper.open(this);
                break;
            case 1:
                // Navigate Home
                String address = sharedPrefs.getString("prefHomeAddress","");
                if (!address.equals("")) {
                    LatLng location = getLocationFromAddress(TaskActivity.this, address);
                    if (location != null) {
                        Location destination = new Location(LocationManager.GPS_PROVIDER);
                        destination.setLatitude(location.latitude);
                        destination.setLongitude(location.longitude);
                        // Check Location permissions
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                // Get the location manager
                                LocationManager locationManager = (LocationManager)
                                        TaskActivity.this.getSystemService(LOCATION_SERVICE);
                                Criteria criteria = new Criteria();
                                try {
                                    String bestProvider = locationManager.getBestProvider(criteria, false);
                                    Log.d(TAG, "Trying Best Provider: " + bestProvider);
                                    Location currentLocation = locationManager.getLastKnownLocation(bestProvider);

                                    NavAppHelper.navigateTo(this, currentLocation, destination);
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
                // Call Favorite Number
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
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
                // Call Contact
                // Check Read Contacts permissions
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
                    // Android version is lesser than 6.0 or the permission is already granted.
                    Intent forwardIntent = new Intent(TaskActivity.this, ContactListActivity.class);
                    startActivity(forwardIntent);
                }
                break;
            case 4:
                // Take photo
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
                    Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                    photoIntent.putExtra("camera", CameraCharacteristics.LENS_FACING_BACK);
                    startService(photoIntent);
                }
                break;
            case 5:
                // Take selfie
                // Check Camera permissions
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
                    Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                    photoIntent.putExtra("camera", CameraCharacteristics.LENS_FACING_FRONT);
                    startService(photoIntent);
                }
                break;
            case 6:
                // Record Rear Camera Video
                // Check Camera permissions
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        || !canDrawOverlays()) {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
                    if (((MyApplication) TaskActivity.this.getApplication()).getVideoRecording()) {
                        stopService(new Intent(TaskActivity.this, VideoRecService.class));
                        ((MyApplication) this.getApplication()).setVideoRecording(false);
                    } else {
                        Intent videoIntent = new Intent(TaskActivity.this, VideoRecService.class);
                        videoIntent.putExtra("camera", 1);
                        startService(videoIntent);
                        ((MyApplication) this.getApplication()).setVideoRecording(true);
                    }
                    updateTasks();
                }
                break;
            case 7:
                // Trip Log
                // Check Write permissions
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
                    if (((MyApplication) TaskActivity.this.getApplication()).getTripRecording()) {
                        stopService(new Intent(TaskActivity.this, LoggingService.class));
                        ((MyApplication) this.getApplication()).setTripRecording(false);
                    } else {
                        startService(new Intent(TaskActivity.this, LoggingService.class));
                        ((MyApplication) this.getApplication()).setTripRecording(true);
                    }
                    updateTasks();
                }
                break;
            case 8:
                // Waypoint
                // Check Location permissions
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
                    try {
                        // Get the location manager
                        double lat;
                        double lon;
                        LocationManager locationManager = (LocationManager)
                                TaskActivity.this.getSystemService(LOCATION_SERVICE);
                        Criteria criteria = new Criteria();
                        try {
                            String bestProvider = locationManager.getBestProvider(criteria, false);
                            Log.d(TAG, "Trying Best Provider: " + bestProvider);
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
                //Open WunderLINQ GoPro Remote
                Intent WLQGoProIntent = new Intent(android.content.Intent.ACTION_VIEW);
                String url = "wunderlinqgp://";
                WLQGoProIntent.setData(Uri.parse(url));
                try {
                    startActivity(WLQGoProIntent);
                } catch ( ActivityNotFoundException ex  ) {
                    Log.d(TAG,"WunderLINQ GoPro Remote not found");
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.nogpremote_alert_title));
                    builder.setMessage(getString(R.string.nogpremote_alert_body));
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String appPackageName = "com.blackboxembedded.wunderlinqgopro";
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException exception) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.show();
                }
                break;
            case 14:
                //Weather Map
                Intent weatherIntent = new Intent(TaskActivity.this, WeatherMapActivity.class);
                startActivity(weatherIntent);
                break;
            case 15:
                //App Launcher
                Intent appListIntent = new Intent(TaskActivity.this, AppListActivity.class);
                startActivity(appListIntent);
                break;
            case 16:
                //Open Road Book App
                NavAppHelper.roadbook(this);
                break;
            case 17:
                //Open System Volume Control
                Intent volumeIntent = new Intent(TaskActivity.this, VolumeActivity.class);
                startActivity(volumeIntent);
                break;
            case 18:
                //Open WunderLINQ Insta360 Remote
                Intent WLQInsta360Intent = new Intent(android.content.Intent.ACTION_VIEW);
                WLQInsta360Intent.setData(Uri.parse("wunderlinqi360://"));
                try {
                    startActivity(WLQInsta360Intent);
                } catch ( ActivityNotFoundException ex  ) {
                    Log.d(TAG,"WunderLINQ Insta360 Remote not found");
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.no360remote_alert_title));
                    builder.setMessage(getString(R.string.no360remote_alert_body));
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String appPackageName = "com.blackboxembedded.wunderlinqinsta360";
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException exception) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.show();
                }
                break;
            case 19:
                // Front Camera Video
                // Check Camera permissions
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        || !canDrawOverlays()) {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
                    if (((MyApplication) TaskActivity.this.getApplication()).getVideoRecording()) {
                        stopService(new Intent(TaskActivity.this, VideoRecService.class));
                        ((MyApplication) this.getApplication()).setVideoRecording(false);
                    } else {
                        Intent videoIntent = new Intent(TaskActivity.this, VideoRecService.class);
                        videoIntent.putExtra("camera", 0);
                        startService(videoIntent);
                        ((MyApplication) this.getApplication()).setVideoRecording(true);
                    }
                    updateTasks();
                }
                break;

            default:
                break;
        }
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

    // Handles various events fired by the Service.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE.equals(action)) {
                Intent accessoryIntent = new Intent(TaskActivity.this, AccessoryActivity.class);
                startActivity(accessoryIntent);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE);
        return intentFilter;
    }

    private boolean canDrawOverlays(){
        boolean canDrawOverlays = false;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.canDrawOverlays(TaskActivity.this)){
                canDrawOverlays = true;
            }
        } else {
            canDrawOverlays = true;
        }
        return canDrawOverlays;
    }
}