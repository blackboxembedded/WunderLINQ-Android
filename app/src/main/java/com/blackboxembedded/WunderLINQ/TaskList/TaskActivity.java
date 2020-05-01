package com.blackboxembedded.WunderLINQ.TaskList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.blackboxembedded.WunderLINQ.ContactListActivity;
import com.blackboxembedded.WunderLINQ.GridOnSwipeTouchListener;
import com.blackboxembedded.WunderLINQ.LoggingService;
import com.blackboxembedded.WunderLINQ.MainActivity;
import com.blackboxembedded.WunderLINQ.MusicActivity;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.OsmAndHelper;
import com.blackboxembedded.WunderLINQ.PhotoService;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.SettingsActivity;
import com.blackboxembedded.WunderLINQ.VideoRecService;
import com.blackboxembedded.WunderLINQ.WaypointDatasource;
import com.blackboxembedded.WunderLINQ.WaypointNavActivity;
import com.blackboxembedded.WunderLINQ.WaypointRecord;
import com.blackboxembedded.WunderLINQ.WeatherMapActivity;
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

public class TaskActivity extends AppCompatActivity implements OsmAndHelper.OnOsmandMissingListener {

    public final static String TAG = "TaskActivity";

    private ConstraintLayout clTasks;
    private WearableRecyclerView recyclerView;
    private TextView tvLabel;
    private TaskAdapter adapter;
    final ArrayList<MenuItem> menuItems = new ArrayList<>();

    private SharedPreferences sharedPrefs;

    private CountDownTimer cTimer = null;

    private boolean timerRunning = false;

    private List<Integer> mapping;

    private boolean actionCamOnline = false;
    private boolean actionCamRecording = false;

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

        clTasks = findViewById(R.id.layout_new_task);
        tvLabel = findViewById(R.id.tvLabel);
        recyclerView = findViewById(R.id.main_menu_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setCircularScrollingGestureEnabled(true);
        recyclerView.setBezelFraction(0.5f);
        recyclerView.setScrollDegreesPerScreen(90);
        recyclerView.setOnTouchListener(new GridOnSwipeTouchListener(this) {

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

        adapter = new TaskAdapter(this, menuItems, new TaskAdapter.AdapterCallback() {
            @Override
            public void onItemClicked(final Integer menuPosition) {
                Log.d(TAG,"Item selected position: " + menuPosition);
                updateTasks();
                executeTask(menuPosition);
            }
        });

        recyclerView.setAdapter(adapter);

        CustomScrollingLayoutCallback customScrollingLayoutCallback =
                new CustomScrollingLayoutCallback();
        recyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this, customScrollingLayoutCallback));

        showActionBar();
    }

    @Override
    public void onResume() {
        Log.d(TAG,"in onResume");
        super.onResume();

        updateActionCamStatus();

        updateTasks();

        updateOrientation(this.getResources().getConfiguration().orientation);

        getSupportActionBar().show();
        startTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelTimer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimer();
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
        updateOrientation(newConfig.orientation);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                updateTasks();
                executeTask(adapter.selected);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Intent backIntent = new Intent(TaskActivity.this, MusicActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Intent forwardIntent = new Intent(TaskActivity.this, MainActivity.class);
                startActivity(forwardIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if ((adapter.selected == (mapping.size() - 1))){
                    adapter.selected = 0;
                } else {
                    adapter.selected = adapter.selected + 1;
                }
                updateTasks();
                RecyclerView.SmoothScroller smoothScroller = new
                        LinearSmoothScroller(this) {
                            @Override protected int getVerticalSnapPreference() {
                                return LinearSmoothScroller.SNAP_TO_START;
                            }
                        };
                smoothScroller.setTargetPosition(adapter.selected);
                updateTasks();
                recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (adapter.selected == 0){
                    adapter.selected = mapping.size() - 1;
                } else {
                    adapter.selected = adapter.selected - 1;
                }
                updateTasks();
                RecyclerView.SmoothScroller smoothScroller2 = new
                        LinearSmoothScroller(this) {
                            @Override protected int getVerticalSnapPreference() {
                                return LinearSmoothScroller.SNAP_TO_START;
                            }
                        };
                smoothScroller2.setTargetPosition(adapter.selected);
                updateTasks();
                recyclerView.getLayoutManager().startSmoothScroll(smoothScroller2);
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

    //Update layout for current orientation
    private void updateOrientation(int orientation){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int h = displayMetrics.heightPixels;
        int w = displayMetrics.widthPixels;

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams)recyclerView.getLayoutParams();

        ViewGroup.LayoutParams lp = recyclerView.getLayoutParams();

        // Checks the orientation of the screen
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            recyclerView.setRotation(90.0f);
            if(h > w){
                lp.height = h;
                lp.width = w;
            } else {
                lp.height = w;
                lp.width = h;
            }
            if(getSupportActionBar().isShowing()) {
                params.setMargins(params.leftMargin, 300, params.rightMargin, params.bottomMargin);
            } else {
                params.setMargins(params.leftMargin, 50, params.rightMargin, params.bottomMargin);
            }
            tvLabel.setVisibility(View.VISIBLE);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT){
            recyclerView.setRotation(0.0f);
            if(h > w){
                lp.height = h;
                lp.width = w;
            } else {
                lp.height = w;
                lp.width = h;
            }
            params.setMargins(params.leftMargin, 0, params.rightMargin, params.bottomMargin);
            tvLabel.setVisibility(View.INVISIBLE);
        }

        recyclerView.requestLayout();
        adapter.notifyDataSetChanged();
    }

    //Start Timer to hide the ActionBar
    void startTimer() {
        if(!timerRunning) {
            cTimer = new CountDownTimer(10000, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    getSupportActionBar().hide();
                    updateOrientation(TaskActivity.this.getResources().getConfiguration().orientation);
                    timerRunning = false;
                }
            };
            timerRunning = true;
            cTimer.start();
        }
    }

    //Cancel Timer to hide the ActionBar
    void cancelTimer() {
        if(cTimer!=null)
            cTimer.cancel();
    }

    //Update Tasks
    public void updateTasks(){
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

        mapping = new ArrayList<>();
        menuItems.clear();
        int x = 0;
        while (x < numTasks){
            switch (x){
                case 0:
                    int selectionOne = Integer.parseInt(sharedPrefs.getString("prefQuickTaskOne", "0"));
                    if (!(selectionOne >= numTasks)){
                        mapping.add(selectionOne);
                        menuItems.add(new MenuItem(iconId[selectionOne], taskTitles[selectionOne]));
                    }
                    break;
                case 1:
                    int selectionTwo = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTwo", "1"));
                    if (!(selectionTwo >= numTasks)){
                        mapping.add(selectionTwo);
                        menuItems.add(new MenuItem(iconId[selectionTwo], taskTitles[selectionTwo]));
                    }
                    break;
                case 2:
                    int selectionThree = Integer.parseInt(sharedPrefs.getString("prefQuickTaskThree", "2"));
                    if (!(selectionThree >= numTasks)){
                        mapping.add(selectionThree);
                        menuItems.add(new MenuItem(iconId[selectionThree], taskTitles[selectionThree]));
                    }
                    break;
                case 3:
                    int selectionFour = Integer.parseInt(sharedPrefs.getString("prefQuickTaskFour", "3"));
                    if (!(selectionFour >= numTasks)){
                        mapping.add(selectionFour);
                        menuItems.add(new MenuItem(iconId[selectionFour], taskTitles[selectionFour]));
                    }
                    break;
                case 4:
                    int selectionFive = Integer.parseInt(sharedPrefs.getString("prefQuickTaskFive", "4"));
                    if (!(selectionFive >= numTasks)){
                        mapping.add(selectionFive);
                        menuItems.add(new MenuItem(iconId[selectionFive], taskTitles[selectionFive]));
                    }
                    break;
                case 5:
                    int selectionSix = Integer.parseInt(sharedPrefs.getString("prefQuickTaskSix", "5"));
                    if (!(selectionSix >= numTasks)){
                        mapping.add(selectionSix);
                        menuItems.add(new MenuItem(iconId[selectionSix], taskTitles[selectionSix]));
                    }
                    break;
                case 6:
                    int selectionSeven = Integer.parseInt(sharedPrefs.getString("prefQuickTaskSeven", "6"));
                    if (!(selectionSeven >= numTasks)){
                        mapping.add(selectionSeven);
                        menuItems.add(new MenuItem(iconId[selectionSeven], taskTitles[selectionSeven]));
                    }
                    break;
                case 7:
                    int selectionEight = Integer.parseInt(sharedPrefs.getString("prefQuickTaskEight", "7"));
                    if (!(selectionEight >= numTasks)){
                        mapping.add(selectionEight);
                        menuItems.add(new MenuItem(iconId[selectionEight], taskTitles[selectionEight]));
                    }
                    break;
                case 8:
                    int selectionNine = Integer.parseInt(sharedPrefs.getString("prefQuickTaskNine", "8"));
                    if (!(selectionNine >= numTasks)){
                        mapping.add(selectionNine);
                        menuItems.add(new MenuItem(iconId[selectionNine], taskTitles[selectionNine]));
                    }
                    break;
                case 9:
                    int selectionTen = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTen", "9"));
                    if (!(selectionTen >= numTasks)){
                        mapping.add(selectionTen);
                        menuItems.add(new MenuItem(iconId[selectionTen], taskTitles[selectionTen]));
                    }
                    break;
                case 10:
                    int selectionEleven = Integer.parseInt(sharedPrefs.getString("prefQuickTaskEleven", "10"));
                    if (!(selectionEleven >= numTasks)){
                        mapping.add(selectionEleven);
                        menuItems.add(new MenuItem(iconId[selectionEleven], taskTitles[selectionEleven]));
                    }
                    break;
                case 11:
                    int selectionTwelve = Integer.parseInt(sharedPrefs.getString("prefQuickTaskTwelve", "11"));
                    if (!(selectionTwelve >= numTasks)){
                        mapping.add(selectionTwelve);
                        menuItems.add(new MenuItem(iconId[selectionTwelve], taskTitles[selectionTwelve]));
                    }
                    break;
                default:
                    break;
            }
            x = x + 1;
        }
        tvLabel.setText(taskTitles[mapping.get(adapter.selected)]);
        adapter.notifyDataSetChanged();
    }

    //Task Actions
    private void executeTask(int taskID){
        String navApp = sharedPrefs.getString("prefNavApp", "1");
        switch (mapping.get(taskID)){
            case 0:
                //Navigation
                Intent navIntent = new Intent(android.content.Intent.ACTION_VIEW);
                String url = "google.navigation://?free=1&mode=d&entry=fnls";
                if (navApp.equals("1")) {
                    // Android Default
                } else if (navApp.equals("2")){
                    //Google Maps
                    navIntent.setPackage("com.google.android.apps.maps");
                    url = "google.navigation://?free=1&mode=d&entry=fnls";
                } else if (navApp.equals("3")){
                    //Locus Maps
                    url = "http://link.locusmap.eu";
                    navIntent.setPackage("menion.android.locus.pro");
                    navIntent.setData(Uri.parse(url));
                    if(!isCallable(navIntent)){
                        Log.d(TAG,"Locus Maps Pro Not Installed");
                        navIntent.setPackage("menion.android.locus");
                    }
                } else if (navApp.equals("4")){
                    //Waze
                    url = "https://waze.com/ul";
                } else if (navApp.equals("5")){
                    //Maps.me
                    url = "https://dlink.maps.me/?back_url=wunderlinq://datagrid";
                } else if (navApp.equals("6")){
                    // OsmAnd
                    url = "http://osmand.net/go";
                } else if (navApp.equals("7")){
                    //Mapfactor Navigator
                    navIntent.setPackage("com.mapfactor.navigator");
                    url = "http://maps.google.com/maps";
                } else if (navApp.equals("8")) {
                    //Sygic
                    //https://www.sygic.com/developers/professional-navigation-sdk/android/api-examples/custom-url
                    url = "com.sygic.aura://";
                } else if (navApp.equals("9")) {
                    //Kurviger
                    navIntent = getPackageManager().getLaunchIntentForPackage("gr.talent.kurviger.pro");
                    if (navIntent == null) {
                        navIntent = getPackageManager().getLaunchIntentForPackage("gr.talent.kurviger");
                    }
                    url = "";
                }
                try {
                    navIntent.setData(Uri.parse(url));
                    if (android.os.Build.VERSION.SDK_INT >= 24) {
                        if (isInMultiWindowMode()) {
                            navIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                        }
                    }
                    startActivity(navIntent);
                } catch ( ActivityNotFoundException ex  ) {
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
                                Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
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
                                    Intent homeNavIntent = new Intent(android.content.Intent.ACTION_VIEW);
                                    String navUrl = "google.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                                    if (navApp.equals("1")){
                                        // Android Default or Google Maps
                                        // Nothing to do
                                    } else if (navApp.equals("2")){
                                        homeNavIntent.setPackage("com.google.android.apps.maps");
                                    } else if (navApp.equals("3")){
                                        //Locus Maps
                                        homeNavIntent.setPackage("menion.android.locus.pro");
                                        homeNavIntent.setData(Uri.parse(navUrl));
                                        if(!isCallable(homeNavIntent)){
                                            Log.d(TAG,"Locus Maps Pro Not Installed");
                                            homeNavIntent.setPackage("menion.android.locus");
                                        }
                                    } else if (navApp.equals("4")){
                                        //Waze
                                        navUrl = "https://www.waze.com/ul?ll=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes&zoom=17";
                                    } else if (navApp.equals("5")){
                                        //Maps.me
                                        navUrl = "https://dlink.maps.me/route?sll=" + String.valueOf(currentLocation.getLatitude()) + "," + String.valueOf(currentLocation.getLongitude()) + "&saddr=Start&dll=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&daddr=Home&type=vehicle&back_url=wunderlinq://datagrid";
                                    } else if (navApp.equals("6")){
                                        //OsmAnd
                                        //navUrl = "osmand.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                                        OsmAndHelper osmAndHelper = new OsmAndHelper(TaskActivity.this, OsmAndHelper.REQUEST_OSMAND_API, TaskActivity.this);
                                        osmAndHelper.navigate("Start",currentLocation.getLatitude(),currentLocation.getLongitude(),"Destination",location.latitude,location.longitude,"motorcycle", true);
                                    } else if (navApp.equals("7")){
                                        //Mapfactor Navigator
                                        homeNavIntent.setPackage("com.mapfactor.navigator");
                                        navUrl = "http://maps.google.com/maps?f=d&daddr=@"  + location.latitude + "," + location.longitude + "&navigate=yes";
                                    } else if (navApp.equals("8")) {
                                        //Sygic
                                        //https://www.sygic.com/developers/professional-navigation-sdk/android/api-examples/custom-url
                                        navUrl = "com.sygic.aura://coordinate|"  + location.longitude + "|" + location.latitude + "|drive";
                                    } else if (navApp.equals("9")) {
                                        //Kurviger
                                        navUrl = "https://kurviger.de/en?point="  + location.latitude + "," + location.longitude+"&vehicle=motorycycle"
                                                + "weighting=fastest";
                                    }
                                    Log.d(TAG,"NavURL: " + navUrl);
                                    if (!navApp.equals("6")) {
                                        try {
                                            homeNavIntent.setData(Uri.parse(navUrl));
                                            if (android.os.Build.VERSION.SDK_INT >= 24) {
                                                if (isInMultiWindowMode()) {
                                                    homeNavIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                                                }
                                            }
                                            startActivity(homeNavIntent);
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
                        } else {
                            Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
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
                        Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
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
                } else {
                    Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                }
                break;
            case 3:
                //Call Contact
                // Check Read Contacts permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (getApplication().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Check Camera permissions
                    if (getApplication().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                            || getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                    } else {
                        cameraPerms = true;
                    }
                } else {
                    cameraPerms = true;
                }
                if (cameraPerms){
                    Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                    photoIntent.putExtra("camera", CameraCharacteristics.LENS_FACING_BACK);
                    startService(photoIntent);
                }
                break;
            case 5:
                //Take selfie
                Log.d(TAG,"Take Front Photo");
                boolean selfieCameraPerms = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Check Camera permissions
                    if (getApplication().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                            || getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                    } else {
                        selfieCameraPerms = true;
                    }
                } else {
                    selfieCameraPerms = true;
                }
                if (selfieCameraPerms){
                    Intent photoIntent = new Intent(TaskActivity.this, PhotoService.class);
                    photoIntent.putExtra("camera",CameraCharacteristics.LENS_FACING_FRONT);
                    startService(photoIntent);
                }
                break;
            case 6:
                //Record Video
                boolean videoPerms = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Check Camera permissions
                    if (getApplication().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                            || getApplication().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                            || getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            || !Settings.canDrawOverlays(TaskActivity.this)){
                        Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                    } else {
                        videoPerms = true;
                    }
                } else {
                    videoPerms = true;
                }
                if(videoPerms){
                    if (((MyApplication) TaskActivity.this.getApplication()).getVideoRecording()){
                        stopService(new Intent(TaskActivity.this, VideoRecService.class));
                        ((MyApplication) this.getApplication()).setVideoRecording(false);
                    } else {
                        startService(new Intent(TaskActivity.this, VideoRecService.class));
                        ((MyApplication) this.getApplication()).setVideoRecording(true);
                    }
                    updateTasks();
                }
                break;
            case 7:
                //Trip Log
                boolean locationLogPerms = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Check Write permissions
                    if (getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            || getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                    } else {
                        locationLogPerms = true;
                    }
                } else {
                    locationLogPerms = true;
                }
                if (locationLogPerms){
                    if (((MyApplication) TaskActivity.this.getApplication()).getTripRecording()){
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
                //Waypoint
                // Get location
                boolean locationWPPerms = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Check Location permissions
                    if (getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(TaskActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
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
                                                updateTasks();
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
                                                updateTasks();
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
                                                                                updateTasks();
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
                                                                            updateTasks();
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
                                                            updateTasks();
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
                                            updateTasks();
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
                                                    updateTasks();
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
                                            updateTasks();
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
                                                    updateTasks();
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
                                            updateTasks();
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

    //Check ActionCam Status
    private void updateActionCamStatus(){
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
                                                updateTasks();
                                            }
                                        } else {
                                            if (actionCamRecording) {
                                                actionCamRecording = false;
                                                updateTasks();
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
                                                                                            updateTasks();
                                                                                        }
                                                                                    } else {
                                                                                        if (actionCamRecording) {
                                                                                            actionCamRecording = false;
                                                                                            updateTasks();
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
                                                                        updateTasks();
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
                                                updateTasks();
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
                            updateTasks();
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
                                updateTasks();
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
                            updateTasks();
                        }
                    }

                });

                break;
            default:
                break;
        }

        recyclerView.clearFocus();
        clTasks.clearFocus();
    }

    //Send WOL Packet
    public void WakeOnLan(String macAddress) {
        //From http://www.jibble.org/wake-on-lan/WakeOnLan.java

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

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
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
}

class CustomScrollingLayoutCallback extends WearableLinearLayoutManager.LayoutCallback {
    @Override
    public void onLayoutFinished(View child, RecyclerView parent) {
        final float MAX_ICON_PROGRESS = 0.65f;
        try {
            float centerOffset              = ((float) child.getHeight() / 2.0f) / (float) parent.getHeight();
            float yRelativeToCenterOffset   = (child.getY() / parent.getHeight()) + centerOffset;

            // Normalize for center, adjusting to the maximum scale
            float progressToCenter          = Math.min(Math.abs(0.5f - yRelativeToCenterOffset), MAX_ICON_PROGRESS);

            // Follow a curved path, rather than triangular!
            progressToCenter                = (float)(Math.cos(progressToCenter * Math.PI * 0.90f));

            child.setScaleX (progressToCenter);
            child.setScaleY (progressToCenter);
        } catch (Exception ignored) {}
    }
}