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
package com.blackboxembedded.WunderLINQ.TaskList.Activities;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.SoundManager;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.blackboxembedded.WunderLINQ.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.android.gms.maps.MapsInitializer.Renderer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeatherMapActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapsSdkInitializedCallback {

    public final static String TAG = "WeatherActivity";
    private GoogleMap mMap;
    private Marker mMarker;
    private TileOverlay tileOverlay;
    private ValueAnimator animator;
    private SharedPreferences sharedPrefs;

    private Handler handler = new Handler();
    private int delay = 60 * 1000;

    private int currentZoom = 8;

    private String timestamp = "";

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_weather);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapsInitializer.initialize(getApplicationContext(), Renderer.LATEST, this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(30000); // 30 seconds
        animator.setRepeatCount(ValueAnimator.INFINITE); // repeat forever
        animator.setRepeatMode(ValueAnimator.RESTART); // repeat from the beginning
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float progress = (float) valueAnimator.getAnimatedValue();
                Date date = calculateDateForProgress(progress);
                long l = date.getTime();
                l -= l % (10*60*1000);
                long unixtime = l / 1000L;
                if (!timestamp.equals(String.valueOf(unixtime))) {
                    Log.d(TAG,"Updating Map");
                    timestamp = String.valueOf(unixtime);
                    if (tileOverlay != null) {
                        tileOverlay.clearTileCache();
                    }
                }
            }
        });
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        animator.end();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
    public void onMapsSdkInitialized(MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d(TAG, "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d(TAG, "The legacy version of the renderer is used.");
                break;
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG,"onMapReady()");
        // Move the camera
        if (Data.getLastLocation() != null) {
            LatLng location = new LatLng(Data.getLastLocation().getLatitude(), Data.getLastLocation().getLongitude());
            MarkerOptions mMarkerOptions= new MarkerOptions().position(location);
            mMarker = mMap.addMarker(mMarkerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, currentZoom));
        }
        long l = System.currentTimeMillis();
        l -= l % (10*60*1000);
        long unixtime = l / 1000L;
        timestamp = String.valueOf(unixtime);
        TileProvider tileProvider = new UrlTileProvider(256, 256) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {
                /* Define the URL pattern for the tile images */
                //https://www.rainviewer.com/api.html
                String s = String.format(Locale.US, "https://tilecache.rainviewer.com/v2/radar/%s/256/%d/%d/%d/4/1_1.png", timestamp, zoom, x, y);
                //Log.d(TAG,s);
                try {
                    return new URL(s);
                } catch (MalformedURLException e) {
                    throw new AssertionError(e);
                }
            }
        };

        tileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(tileProvider));


        animator.start();

        handler.postDelayed(new Runnable(){
            public void run(){
                Log.d(TAG,"Updating marker");
                // This portion of code runs each 10s.
                long l = System.currentTimeMillis();
                l -= l % (10*60*1000);
                long unixtime = l / 1000L;
                timestamp = String.valueOf(unixtime);
                if (Data.getLastLocation() != null) {
                    LatLng location = new LatLng(Data.getLastLocation().getLatitude(), Data.getLastLocation().getLongitude());
                    mMarker.setPosition(location);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
                    tileOverlay.clearTileCache();
                }
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private Date calculateDateForProgress(float progress) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());

        // Calculate the timestamp for one hour ago
        cal.add(Calendar.HOUR_OF_DAY, -2);
        Date startDate = cal.getTime();

        // Calculate the timestamp for the current frame
        long timeRange = 60 * 60 * 2000; // 1 hour in milliseconds
        long frameTime = (long) (timeRange * progress);
        Date frameDate = new Date(startDate.getTime() + frameTime);

        // Round down to the nearest 15-minute interval
        int minute = frameDate.getMinutes();
        int roundedMinute = minute / 15 * 15;
        cal.setTime(frameDate);
        cal.set(Calendar.MINUTE, roundedMinute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
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

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.weathermap_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private void goBack(){
        Intent backIntent = new Intent(WeatherMapActivity.this, com.blackboxembedded.WunderLINQ.TaskList.TaskActivity.class);
        startActivity(backIntent);
    }
    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    goBack();
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
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                SoundManager.playSound(this, R.raw.directional);
                //Zoom Out
                if (currentZoom > 3){
                    if (Data.getLastLocation() != null) {
                        currentZoom = currentZoom - 1;
                        LatLng location = new LatLng(Data.getLastLocation().getLatitude(), Data.getLastLocation().getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, currentZoom));
                    }
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                SoundManager.playSound(this, R.raw.directional);
                //Zoom In
                if (currentZoom < 16){
                    if (Data.getLastLocation() != null) {
                        currentZoom = currentZoom + 1;
                        LatLng location = new LatLng(Data.getLastLocation().getLatitude(), Data.getLastLocation().getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, currentZoom));
                    }
                }
                return true;
            case KeyEvent.KEYCODE_ENTER:
                //Center
                SoundManager.playSound(this, R.raw.enter);
                if (Data.getLastLocation() != null) {
                    LatLng location = new LatLng(Data.getLastLocation().getLatitude(), Data.getLastLocation().getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}