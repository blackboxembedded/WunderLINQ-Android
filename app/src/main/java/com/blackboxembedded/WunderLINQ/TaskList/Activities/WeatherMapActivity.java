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
import android.os.Looper;
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
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.SoundManager;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherMapActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapsSdkInitializedCallback {

    public static final String TAG = "WeatherActivity";

    private static final String RAINVIEWER_API_URL = "https://api.rainviewer.com/public/weather-maps.json";
    private static final String DEFAULT_RAIN_HOST = "https://tilecache.rainviewer.com";

    private static final int RADAR_TILE_SIZE = 256;
    private static final int RADAR_COLOR_SCHEME = 2;   // Universal Blue
    private static final String RADAR_OPTIONS = "1_1"; // smooth + snow
    private static final int RADAR_MAX_ZOOM = 7;

    private static final long UI_UPDATE_DELAY_MS = 10_000L;
    private static final long FRAME_REFRESH_INTERVAL_MS = 5 * 60_000L;
    private static final long ANIMATION_DURATION_MS = 60_000L;
    private static final long ANIMATION_RESTART_DELAY_MS = 5_000L;

    private ImageButton faultButton;
    private GoogleMap mMap;
    private TextView tvDate;
    private Marker mMarker;
    private TileOverlay tileOverlay;
    private ValueAnimator animator;
    private SharedPreferences sharedPrefs;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private int currentZoom = RADAR_MAX_ZOOM;

    private String rainHost = DEFAULT_RAIN_HOST;
    private final List<RadarFrame> radarFrames = new ArrayList<>();
    private int currentFrameIndex = -1;

    private boolean allowAnimationRestart = true;

    private final Runnable faultAndLocationRunnable = new Runnable() {
        @Override
        public void run() {
            updateFaultButton();
            updateMarkerAndCamera(false);
            handler.postDelayed(this, UI_UPDATE_DELAY_MS);
        }
    };

    private final Runnable frameRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadRainViewerFrames();
            handler.postDelayed(this, FRAME_REFRESH_INTERVAL_MS);
        }
    };

    private final Runnable animationRestartRunnable = new Runnable() {
        @Override
        public void run() {
            if (!allowAnimationRestart) {
                return;
            }

            if (radarFrames.size() > 1 && animator != null && !animator.isRunning()) {
                animator.start();
            }
        }
    };

    private static class RadarFrame {
        final long time;
        final String path;

        RadarFrame(long time, String path) {
            this.time = time;
            this.path = path;
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_weather);

        tvDate = findViewById(R.id.tvDate);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String orientation = sharedPrefs.getString("prefOrientation", "0");
        if (!orientation.equals("0")) {
            if (orientation.equals("1")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if (orientation.equals("2")) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

        showActionBar();
        setupAnimator();

        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found.");
        }
    }

    private void setupAnimator() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setRepeatCount(0);

        animator.addUpdateListener(animation -> {
            if (radarFrames.isEmpty()) {
                return;
            }

            float progress = (float) animation.getAnimatedValue();
            int lastIndex = radarFrames.size() - 1;
            int newIndex = Math.min(lastIndex, Math.max(0, Math.round(progress * lastIndex)));

            if (newIndex != currentFrameIndex) {
                applyFrameIndex(newIndex, true);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!allowAnimationRestart) {
                    return;
                }

                scheduleAnimationRestart(ANIMATION_RESTART_DELAY_MS);
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
        allowAnimationRestart = true;

        if (mMap != null) {
            startRuntimeTasks();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        allowAnimationRestart = false;

        handler.removeCallbacksAndMessages(null);

        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);

        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "onMapReady()");

        updateMarkerAndCamera(true);
        setupRadarOverlay();

        tvDate.setText("Loading radar...");
        startRuntimeTasks();
    }

    private void startRuntimeTasks() {
        handler.removeCallbacksAndMessages(null);

        updateFaultButton();
        updateMarkerAndCamera(false);

        handler.post(faultAndLocationRunnable);
        handler.post(frameRefreshRunnable);
    }

    private void setupRadarOverlay() {
        TileProvider tileProvider = new UrlTileProvider(RADAR_TILE_SIZE, RADAR_TILE_SIZE) {
            @Override
            public URL getTileUrl(int x, int y, int zoom) {
                if (currentFrameIndex < 0 || currentFrameIndex >= radarFrames.size()) {
                    return null;
                }

                int radarZoom = Math.min(zoom, RADAR_MAX_ZOOM);
                RadarFrame frame = radarFrames.get(currentFrameIndex);

                String url = String.format(
                        Locale.US,
                        "%s%s/%d/%d/%d/%d/%d/%s.png",
                        rainHost,
                        frame.path,
                        RADAR_TILE_SIZE,
                        radarZoom,
                        x,
                        y,
                        RADAR_COLOR_SCHEME,
                        RADAR_OPTIONS
                );

                try {
                    return new URL(url);
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Invalid radar tile URL: " + url, e);
                    return null;
                }
            }
        };

        tileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
    }

    private void loadRainViewerFrames() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(RAINVIEWER_API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "RainViewer API HTTP error: " + responseCode);
                    return;
                }

                String json = readStream(connection.getInputStream());
                JSONObject root = new JSONObject(json);

                String newHost = root.optString("host", DEFAULT_RAIN_HOST);
                JSONObject radar = root.optJSONObject("radar");
                JSONArray past = radar != null ? radar.optJSONArray("past") : null;

                if (past == null || past.length() == 0) {
                    Log.w(TAG, "RainViewer returned no past radar frames.");
                    return;
                }

                ArrayList<RadarFrame> newFrames = new ArrayList<>();
                for (int i = 0; i < past.length(); i++) {
                    JSONObject frame = past.getJSONObject(i);
                    long time = frame.getLong("time");
                    String path = frame.getString("path");
                    newFrames.add(new RadarFrame(time, path));
                }

                runOnUiThread(() -> {
                    boolean changed = framesChanged(newHost, newFrames);

                    rainHost = newHost;
                    radarFrames.clear();
                    radarFrames.addAll(newFrames);

                    if (currentFrameIndex < 0 || currentFrameIndex >= radarFrames.size() || changed) {
                        applyFrameIndex(radarFrames.size() - 1, true);
                    } else {
                        updateDisplayedFrameTime();
                    }

                    if (tileOverlay != null && changed) {
                        tileOverlay.clearTileCache();
                    }

                    if (allowAnimationRestart && radarFrames.size() > 1 && (animator == null || !animator.isRunning())) {
                        scheduleAnimationRestart(ANIMATION_RESTART_DELAY_MS);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to load RainViewer frames", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private boolean framesChanged(String newHost, List<RadarFrame> newFrames) {
        if (!rainHost.equals(newHost)) {
            return true;
        }

        if (radarFrames.size() != newFrames.size()) {
            return true;
        }

        for (int i = 0; i < radarFrames.size(); i++) {
            RadarFrame oldFrame = radarFrames.get(i);
            RadarFrame newFrame = newFrames.get(i);

            if (oldFrame.time != newFrame.time || !oldFrame.path.equals(newFrame.path)) {
                return true;
            }
        }

        return false;
    }

    private String readStream(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();
        return builder.toString();
    }

    private void applyFrameIndex(int frameIndex, boolean clearCache) {
        if (frameIndex < 0 || frameIndex >= radarFrames.size()) {
            return;
        }

        currentFrameIndex = frameIndex;
        updateDisplayedFrameTime();

        if (clearCache && tileOverlay != null) {
            tileOverlay.clearTileCache();
        }
    }

    private void updateDisplayedFrameTime() {
        if (currentFrameIndex >= 0 && currentFrameIndex < radarFrames.size()) {
            long timeMs = radarFrames.get(currentFrameIndex).time * 1000L;
            tvDate.setText(new Date(timeMs).toString());
            Log.d(TAG, "Displaying radar frame: " + new Date(timeMs));
        }
    }

    private void scheduleAnimationRestart(long delayMs) {
        handler.removeCallbacks(animationRestartRunnable);
        handler.postDelayed(animationRestartRunnable, delayMs);
    }

    private void updateFaultButton() {
        if (faultButton == null) {
            return;
        }

        if (!Faults.getAllActiveDesc().isEmpty()) {
            faultButton.setVisibility(View.VISIBLE);
        } else {
            faultButton.setVisibility(View.GONE);
        }
    }

    private void updateMarkerAndCamera(boolean centerWithZoom) {
        if (mMap == null || MotorcycleData.getLastLocation() == null) {
            return;
        }

        LatLng loc = new LatLng(
                MotorcycleData.getLastLocation().getLatitude(),
                MotorcycleData.getLastLocation().getLongitude()
        );

        if (mMarker == null) {
            mMarker = mMap.addMarker(new MarkerOptions().position(loc));
        } else {
            mMarker.setPosition(loc);
        }

        if (centerWithZoom) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, currentZoom));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
        }
    }

    private void showActionBar() {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.actionbar_nav, null);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setCustomView(v);
        }

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.weathermap_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);

        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);

        faultButton = findViewById(R.id.action_faults);
        faultButton.setOnClickListener(mClickListener);

        updateFaultButton();
    }

    private void goBack() {
        Intent backIntent = new Intent(
                WeatherMapActivity.this,
                com.blackboxembedded.WunderLINQ.TaskList.TaskActivity.class
        );
        startActivity(backIntent);
    }

    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.action_back) {
                goBack();
            } else if (id == R.id.action_faults) {
                Intent faultIntent = new Intent(WeatherMapActivity.this, FaultActivity.class);
                startActivity(faultIntent);
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
                if (currentZoom > 3) {
                    currentZoom = currentZoom - 1;
                    updateMarkerAndCamera(true);
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                SoundManager.playSound(this, R.raw.directional);
                if (currentZoom < RADAR_MAX_ZOOM) {
                    currentZoom = currentZoom + 1;
                    updateMarkerAndCamera(true);
                }
                return true;

            case KeyEvent.KEYCODE_ENTER:
                SoundManager.playSound(this, R.raw.enter);
                if (MotorcycleData.getLastLocation() != null) {
                    LatLng location = new LatLng(
                            MotorcycleData.getLastLocation().getLatitude(),
                            MotorcycleData.getLastLocation().getLongitude()
                    );
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, currentZoom));
                }
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}