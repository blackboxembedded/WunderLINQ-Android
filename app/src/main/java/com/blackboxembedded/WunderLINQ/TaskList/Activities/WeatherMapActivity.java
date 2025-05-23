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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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

import com.blackboxembedded.WunderLINQ.FaultActivity;
import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.SoundManager;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
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
import java.util.Date;
import java.util.Locale;

public class WeatherMapActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapsSdkInitializedCallback {

    public final static String TAG = "WeatherActivity";
    private ImageButton faultButton;
    private GoogleMap mMap;
    private TextView tvDate;
    private Marker mMarker;
    private TileOverlay tileOverlay;
    private ValueAnimator animator;
    private SharedPreferences sharedPrefs;

    private final Handler handler = new Handler();
    private final int delay = 10 * 1000;

    private int currentZoom = 8;

    private String timestamp = "";

    private long windowStartMs;
    private final long windowDurationMs = 2 * 60L * 60L * 1000L;  // 2h in ms
    private static final long TILE_INTERVAL_MS = 10 * 60L * 1000L; // 10min

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_weather);

        tvDate = findViewById(R.id.tvDate);

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

        // ─── PREPARE THE ANIMATOR ────────────────────────────────────────────────
        // animate from 0→1 (windowStart → windowEnd) over 60s:
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(60_000);               // full 2h sweep in 60s
        animator.setRepeatCount(0);

        // on each frame, map progress→timestamp:
        animator.addUpdateListener(av -> {
            float progress = (float) av.getAnimatedValue();

            long frameMs;
            long tileMs;
            Date displayDate;
            String newTs;

            if (progress < 1f) {
                // intermediate frames: evenly interpolate over [windowStart → windowEnd]
                frameMs     = windowStartMs + (long)(windowDurationMs * progress);
                // round to the last 10min tick
                tileMs      = frameMs - (frameMs % TILE_INTERVAL_MS);
                displayDate = new Date(frameMs);
                newTs       = String.valueOf(tileMs / 1000L);
            } else {
                // final frame: snap to true now
                long nowMs  = System.currentTimeMillis();
                displayDate = new Date(nowMs);
                newTs       = String.valueOf(nowMs / 1000L);
            }

            if (! newTs.equals(timestamp)) {
                Log.d(TAG, "Updating Map → " + displayDate);
                tvDate.setText(displayDate.toString());
                timestamp = newTs;
                if (tileOverlay != null) {
                    tileOverlay.clearTileCache();
                }
            }
        });

        // on each cycle‑end, grab a fresh window ending at “now”:
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // 1) re‑anchor your 2h window ending at Now
                long nowMs      = System.currentTimeMillis();
                windowStartMs   = nowMs - windowDurationMs;
                timestamp       = "";                 // force tile & label refresh

                // show “now” label immediately:
                tvDate.setText(new Date().toString());
                if (tileOverlay != null) {
                    tileOverlay.clearTileCache();
                }
                Log.d(TAG, "Updating Map → " + "onAnimationEnd");
                // 2) after 5s, start the next 60s sweep
                handler.postDelayed(() -> animator.start(), 5_000);
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
        Log.d(TAG, "onMapReady()");

        // ─── camera & marker
        if (MotorcycleData.getLastLocation() != null) {
            LatLng loc = new LatLng(
                    MotorcycleData.getLastLocation().getLatitude(),
                    MotorcycleData.getLastLocation().getLongitude()
            );
            mMarker = mMap.addMarker(new MarkerOptions().position(loc));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, currentZoom));
        }

        // ─── compute our fixed 2h window
        long nowMs      = System.currentTimeMillis();
        windowStartMs   = nowMs - windowDurationMs;
        long rounded10m = nowMs - (nowMs % TILE_INTERVAL_MS);
        timestamp       = String.valueOf(rounded10m / 1000L);

        // ─── one‐time tileOverlay setup
        TileProvider tileProvider = new UrlTileProvider(256, 256) {
            @Override public URL getTileUrl(int x, int y, int zoom) {
                String url = String.format(Locale.US,
                        "https://tilecache.rainviewer.com/v2/radar/%s/256/%d/%d/%d/4/1_1.png",
                        timestamp, zoom, x, y
                );
                try { return new URL(url); }
                catch (MalformedURLException e) { throw new AssertionError(e); }
            }
        };
        tileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(tileProvider));

        // ─── show the “right now” date immediately
        tvDate.setText(new Date().toString());
        Log.d(TAG, "Start Updating Map → " + (new Date()));

        // ─── kick off the animator (which will start at progress=0 → windowStart) ─
        handler.postDelayed(animator::start, 5_000);

        // ─── (your existing) 10 s fault‐check & recenter loop
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!Faults.getAllActiveDesc().isEmpty()) {
                    faultButton.setVisibility(View.VISIBLE);
                } else {
                    faultButton.setVisibility(View.GONE);
                }
                long lm = System.currentTimeMillis();
                lm -= lm % (10 * 60 * 1000);
                timestamp = String.valueOf(lm / 1000L);
                if (MotorcycleData.getLastLocation() != null) {
                    LatLng loc2 = new LatLng(
                            MotorcycleData.getLastLocation().getLatitude(),
                            MotorcycleData.getLastLocation().getLongitude()
                    );
                    mMarker.setPosition(loc2);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(loc2));
                    tileOverlay.clearTileCache();
                }
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private void showActionBar(){
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.actionbar_nav, null);
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
        faultButton = findViewById(R.id.action_faults);
        faultButton.setOnClickListener(mClickListener);

        //Check for active faults
        if (!Faults.getAllActiveDesc().isEmpty()) {
            faultButton.setVisibility(View.VISIBLE);
        } else {
            faultButton.setVisibility(View.GONE);
        }
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
                case R.id.action_faults:
                    Intent faultIntent = new Intent(WeatherMapActivity.this, FaultActivity.class);
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
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                SoundManager.playSound(this, R.raw.directional);
                //Zoom Out
                if (currentZoom > 3){
                    if (MotorcycleData.getLastLocation() != null) {
                        currentZoom = currentZoom - 1;
                        LatLng location = new LatLng(MotorcycleData.getLastLocation().getLatitude(), MotorcycleData.getLastLocation().getLongitude());
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
                    if (MotorcycleData.getLastLocation() != null) {
                        currentZoom = currentZoom + 1;
                        LatLng location = new LatLng(MotorcycleData.getLastLocation().getLatitude(), MotorcycleData.getLastLocation().getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, currentZoom));
                    }
                }
                return true;
            case KeyEvent.KEYCODE_ENTER:
                //Center
                SoundManager.playSound(this, R.raw.enter);
                if (MotorcycleData.getLastLocation() != null) {
                    LatLng location = new LatLng(MotorcycleData.getLastLocation().getLatitude(), MotorcycleData.getLastLocation().getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}