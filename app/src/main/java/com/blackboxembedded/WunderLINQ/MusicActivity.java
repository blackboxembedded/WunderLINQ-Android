package com.blackboxembedded.WunderLINQ;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MusicActivity extends AppCompatActivity {

    public final static String TAG = "WunderLINQ";

    private ActionBar actionBar;
    private TextView navbarTitle;
    private ImageButton mPrevButton;
    private ImageButton mPlayPauseButton;
    private ImageButton mNextButton;
    private TextView mArtistText;
    private TextView mTitleText;
    private TextView mAlbumText;
    private ImageView mArtwork;

    private ImageButton backButton;
    private ImageButton forwardButton;

    private MediaController.TransportControls controls;
    private MediaController controller;
    private MediaMetadata metaData;

    private SharedPreferences sharedPrefs;

    SensorManager sensorManager;
    Sensor lightSensor;

    static boolean itsDark = false;
    private long darkTimer = 0;
    private long lightTimer = 0;

    private Handler mHandler = new Handler();

    private boolean alertDiagUp = true;

    private OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.prev_button:
                    if (controller != null) {
                        controls.skipToPrevious();
                        refreshMetaData();
                    }
                    break;
                case R.id.next_button:
                    if (controller != null) {
                        controls.skipToNext();
                        refreshMetaData();
                    }
                    break;
                case R.id.play_pause_button:
                    if (controller != null) {
                        PlaybackState playbackState = controller.getPlaybackState();
                        if (playbackState.getState() != PlaybackState.STATE_PLAYING) {
                            controls.play();
                            refreshMetaData();
                        } else {
                            controls.pause();
                            refreshMetaData();
                        }
                    }
                    break;
                case R.id.action_back:
                    Intent backIntent = new Intent(MusicActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_forward:
                    Intent forwardIntent = new Intent(MusicActivity.this, CompassActivity.class);
                    startActivity(forwardIntent);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_music);

        View view = findViewById(R.id.layout_music);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                Intent backIntent = new Intent(MusicActivity.this, CompassActivity.class);
                startActivity(backIntent);
            }
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(MusicActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
        });

        mPrevButton = (ImageButton)findViewById(R.id.prev_button);
        mPlayPauseButton = (ImageButton)findViewById(R.id.play_pause_button);
        mNextButton = (ImageButton)findViewById(R.id.next_button);

        mTitleText = (TextView)findViewById(R.id.title_text);
        mAlbumText = (TextView)findViewById(R.id.album_text);
        mArtistText = (TextView)findViewById(R.id.artist_text);

        mArtwork = (ImageView)findViewById(R.id.album_art);

        mPrevButton.setOnClickListener(mClickListener);
        mNextButton.setOnClickListener(mClickListener);
        mPlayPauseButton.setOnClickListener(mClickListener);

        showActionBar();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (((MyApplication) this.getApplication()).getitsDark() || sharedPrefs.getBoolean("prefNightMode", false)){
            updateColors(true);
        } else {
            updateColors(false);
        }

        // Check if we have permissions
        if (Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName()))
        {
            alertDiagUp = false;
            MediaSessionManager mm = (MediaSessionManager) this.getSystemService(
                    Context.MEDIA_SESSION_SERVICE);
            List<MediaController> controllers = mm.getActiveSessions(
                    new ComponentName(this, NotificationListener.class));
            if (controllers.size() != 0 ) {
                mHandler.post(mUpdateMetaData);
            } else {
                Toast.makeText(this, R.string.start_media_player, Toast.LENGTH_SHORT).show();
            }
        } else {
            // Need permissions to read notifications
            requestPermissions();
        }
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
        // Need permissions to read notifications
        if (Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
            alertDiagUp = false;
            mHandler.post(mUpdateMetaData);
        } else {
            // Need permissions to read notifications
            if (!alertDiagUp) {
                requestPermissions();
            }
        }
        if (sharedPrefs.getBoolean("prefAutoNightMode", false)) {
            sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateMetaData);
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mUpdateMetaData);
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mUpdateMetaData);
        sensorManager.unregisterListener(sensorEventListener, lightSensor);
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setCustomView(v);

        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.music_title);

        backButton = (ImageButton) findViewById(R.id.action_back);
        forwardButton = (ImageButton) findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
    }

    private Runnable mUpdateMetaData = new Runnable() {
        @Override
        public void run() {
            refreshMetaData();
            mHandler.postDelayed(this, 1000); //setting up update event after one second
        }
    };

    private void requestPermissions(){
        // Need permissions to read notifications
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.notification_alert_title));
        builder.setMessage(getString(R.string.notification_alert_body));
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                /*
                getApplicationContext().startActivity(new Intent(
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        */
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        builder.show();
    }

    protected void refreshMetaData(){
        // Check if we have permissions
        if (Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
            MediaSessionManager mm = (MediaSessionManager) this.getSystemService(
                    Context.MEDIA_SESSION_SERVICE);
            List<MediaController> controllers = mm.getActiveSessions(
                    new ComponentName(this, NotificationListener.class));
            if (controllers.size() != 0 ) {
                controller = controllers.get(0);
                controls = controller.getTransportControls();
                PlaybackState playbackState = controller.getPlaybackState();
                if (playbackState != null) {
                    if (playbackState.getState() != PlaybackState.STATE_PLAYING) {
                        mPlayPauseButton.setImageResource(R.drawable.ic_play);
                    } else {
                        mPlayPauseButton.setImageResource(R.drawable.ic_pause);
                    }
                }

                metaData = controller.getMetadata();

                String metadataArtist = getString(R.string.unknown);
                if (metaData.getString(MediaMetadata.METADATA_KEY_ARTIST) != null) {
                    metadataArtist = metaData.getString(MediaMetadata.METADATA_KEY_ARTIST);
                }
                mArtistText.setText(metadataArtist);

                String metadataAlbum = getString(R.string.unknown);
                if (metaData.getString(MediaMetadata.METADATA_KEY_ALBUM) != null) {
                    metadataAlbum = metaData.getString(MediaMetadata.METADATA_KEY_ALBUM);
                }
                mAlbumText.setText(metadataAlbum);

                String metadataTitle = getString(R.string.unknown);
                if (metaData.getString(MediaMetadata.METADATA_KEY_TITLE) != null) {
                    metadataTitle = metaData.getString(MediaMetadata.METADATA_KEY_TITLE);
                }
                mTitleText.setText(metadataTitle);

                if (metaData.getBitmap(MediaMetadata.METADATA_KEY_ART) != null) {
                    mArtwork.setImageBitmap(scaleBitmap(metaData.getBitmap(MediaMetadata.METADATA_KEY_ART), 800, 800));
                } else if (metaData.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null) {
                    mArtwork.setImageBitmap(scaleBitmap(metaData.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART), 800, 800));
                } else {
                    // Read your drawable from somewhere
                    Drawable dr = getResources().getDrawable(R.drawable.ic_music_note);
                    Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
                    mArtwork.setImageBitmap(scaleBitmap(bitmap, 800, 800));
                    //mArtwork.setImageResource(R.drawable.ic_music_note);
                }
            } else {
                Log.d(TAG, "No music player running");
            }
        } else {
            Log.d(TAG, "No permissions to control music player");
        }
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
        RelativeLayout lLayout = (RelativeLayout) findViewById(R.id.layout_music);
        if (itsDark) {
            lLayout.setBackgroundColor(getResources().getColor(R.color.black));
            mTitleText.setTextColor(getResources().getColor(R.color.white));
            mAlbumText.setTextColor(getResources().getColor(R.color.white));
            mArtistText.setTextColor(getResources().getColor(R.color.white));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.black)));
            navbarTitle.setTextColor(getResources().getColor(R.color.white));
            backButton.setColorFilter(getResources().getColor(R.color.white));
            forwardButton.setColorFilter(getResources().getColor(R.color.white));
        } else {
            lLayout.setBackgroundColor(getResources().getColor(R.color.white));
            mTitleText.setTextColor(getResources().getColor(R.color.black));
            mAlbumText.setTextColor(getResources().getColor(R.color.black));
            mArtistText.setTextColor(getResources().getColor(R.color.black));
            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.white)));
            navbarTitle.setTextColor(getResources().getColor(R.color.black));
            backButton.setColorFilter(getResources().getColor(R.color.black));
            forwardButton.setColorFilter(getResources().getColor(R.color.black));
        }
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int wantedWidth, int wantedHeight) {
        Bitmap output = Bitmap.createBitmap(wantedWidth, wantedHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Matrix m = new Matrix();
        m.setScale((float) wantedWidth / bitmap.getWidth(), (float) wantedHeight / bitmap.getHeight());
        canvas.drawBitmap(bitmap, m, new Paint());

        return output;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Intent backIntent = new Intent(MusicActivity.this, MainActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Intent forwardIntent = new Intent(MusicActivity.this, CompassActivity.class);
                startActivity(forwardIntent);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
