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
package com.blackboxembedded.WunderLINQ;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.NotificationManagerCompat;

import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.Utils.SoundManager;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.comms.BLE.GattAttributes;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;

import java.util.List;
import java.util.Set;

public class MusicActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "MusicActivity";

    private ImageButton mPlayPauseButton;
    private TextView mArtistText;
    private TextView mTitleText;
    private TextView mAlbumText;
    private ImageView mArtwork;

    private SharedPreferences sharedPrefs;

    private MediaController.TransportControls controls;
    private MediaController controller;

    private Handler mHandler = new Handler();

    private GestureDetectorListener gestureDetector;

    private CountDownTimer cTimer = null;

    private boolean timerRunning = false;

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
                    playBack();
                    break;
                case R.id.action_back:
                    goBack();
                    break;
                case R.id.action_forward:
                    goForward();
                    break;
                case R.id.album_art:
                    if (controller != null) {
                        PackageManager packageManager = getPackageManager();
                        Intent intent = packageManager.getLaunchIntentForPackage(controller.getPackageName());
                        startActivity(intent);
                    }
                    break;
                case R.id.album_text:
                    if (Settings.Secure.getString(getApplication().getContentResolver(),"enabled_notification_listeners") == null
                        || !Settings.Secure.getString(getApplication().getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    }
                    break;
            }
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_music);

        View view = findViewById(R.id.layout_music);

        gestureDetector = new GestureDetectorListener(this){

            @Override
            public void onPressLong() {

            }

            @Override
            public void onSwipeUp() {

            }

            @Override
            public void onSwipeDown() {

            }

            @Override
            public void onSwipeLeft() {
                goForward();
            }

            @Override
            public void onSwipeRight() {
                goBack();
            }
        };

        view.setOnTouchListener(this);

        ImageButton mPrevButton = findViewById(R.id.prev_button);
        mPlayPauseButton = findViewById(R.id.play_pause_button);
        ImageButton mNextButton = findViewById(R.id.next_button);

        mTitleText = findViewById(R.id.title_text);
        mAlbumText = findViewById(R.id.album_text);
        mArtistText = findViewById(R.id.artist_text);

        mArtwork = findViewById(R.id.album_art);

        mPrevButton.setOnClickListener(mClickListener);
        mNextButton.setOnClickListener(mClickListener);
        mPlayPauseButton.setOnClickListener(mClickListener);
        mArtwork.setOnClickListener(mClickListener);
        mAlbumText.setOnClickListener(mClickListener);

        showActionBar();
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    public void onResume() {
        super.onResume();

        mPlayPauseButton.setFocusable(true);
        mPlayPauseButton.requestFocus();

        // Check if we have permissions to read notifications
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages (this);
        if (packageNames.contains(getApplicationContext().getPackageName())) {
            MediaSessionManager mm = (MediaSessionManager) this.getSystemService(
                    Context.MEDIA_SESSION_SERVICE);
            List<MediaController> controllers = mm.getActiveSessions(
                    new ComponentName(this, NotificationListener.class));
            if (controllers.size() != 0 ) {
                mHandler.post(mUpdateMetaData);
            } else {
                mTitleText.setText(R.string.not_found_media_player);
                mAlbumText.setText(R.string.start_media_player);
                mArtistText.setText("");
            }
        } else {
            mArtistText.setText(R.string.toast_permission_denied);
            mTitleText.setText("");
            mAlbumText.setText(R.string.touch_here);
        }

        getSupportActionBar().show();
        startTimer();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelTimer();
        mHandler.removeCallbacks(mUpdateMetaData);
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelTimer();
        mHandler.removeCallbacks(mUpdateMetaData);
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimer();
        mHandler.removeCallbacks(mUpdateMetaData);
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        getSupportActionBar().show();
        startTimer();
        gestureDetector.onTouch(v, event);
        return true;
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.music_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
    }

    //Go to next screen - Quick Tasks
    private void goForward(){
        SoundManager.playSound(this, R.raw.directional);
        Intent forwardIntent = new Intent(this, com.blackboxembedded.WunderLINQ.TaskList.TaskActivity.class);
        startActivity(forwardIntent);
    }

    //Go back to last screen - Motorcycle Data
    private void goBack(){
        SoundManager.playSound(this, R.raw.directional);
        Intent backIntent = new Intent(this, MainActivity.class);
        if (sharedPrefs.getBoolean("prefDisplayDash", false)) {
            backIntent = new Intent(this, DashActivity.class);
        }
        startActivity(backIntent);
    }

    void playBack(){
        if (controller != null) {
            PlaybackState playbackState = controller.getPlaybackState();
            try {
                if (playbackState.getState() != PlaybackState.STATE_PLAYING) {
                    controls.play();
                    refreshMetaData();
                } else {
                    controls.pause();
                    refreshMetaData();
                }
            } catch (NullPointerException e) {
                Log.d(TAG,"NullPointerException: " + e.toString());
            }
        }
    }

    private Runnable mUpdateMetaData = new Runnable() {
        @Override
        public void run() {
            refreshMetaData();
            mHandler.postDelayed(this, 1000); //setting up update event after one second
        }
    };

    //Refresh media metadata on screen and update button status
    protected void refreshMetaData(){
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

            try {
                MediaMetadata metaData = controller.getMetadata();
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
                    mArtwork.clearColorFilter();
                    mArtwork.setImageTintMode(null);
                } else if (metaData.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null) {
                    mArtwork.setImageBitmap(scaleBitmap(metaData.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART), 800, 800));
                    mArtwork.clearColorFilter();
                    mArtwork.setImageTintMode(null);
                } else {
                    Log.d(TAG,"No art");
                    Drawable drawable = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_music_note);
                    try {
                        Bitmap bitmap;

                        bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);

                        Canvas canvas = new Canvas(bitmap);
                        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        drawable.draw(canvas);

                        mArtwork.setImageBitmap(scaleBitmap(bitmap, 800, 800));

                        TypedValue typedValue = new TypedValue();
                        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
                        mArtwork.setColorFilter(typedValue.data);

                    } catch (OutOfMemoryError e) {
                        // Handle the error
                        Log.d(TAG,"Error converting drawable to bitmap");
                    }
                }
            } catch (NullPointerException e){
                Log.d(TAG,"Error: " + e.toString());
            }
        } else {
            mArtistText.setText(R.string.not_found_media_player);
            mTitleText.setText(R.string.start_media_player);
            mAlbumText.setText("");
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
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                playBack();
                mPlayPauseButton.setFocusable(true);
                mPlayPauseButton.requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                SoundManager.playSound(this, R.raw.enter);
                if (controller != null) {
                    controls.skipToNext();
                    refreshMetaData();
                }
                mPlayPauseButton.setFocusable(true);
                mPlayPauseButton.requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                SoundManager.playSound(this, R.raw.enter);
                if (controller != null) {
                    controls.skipToPrevious();
                    refreshMetaData();
                }
                mPlayPauseButton.setFocusable(true);
                mPlayPauseButton.requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mPlayPauseButton.setFocusable(true);
                mPlayPauseButton.requestFocus();
                goBack();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mPlayPauseButton.setFocusable(true);
                mPlayPauseButton.requestFocus();
                goForward();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    //start timer function
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

    //cancel timer
    void cancelTimer() {
        if(cTimer!=null)
            cTimer.cancel();
    }

    // Handles various events fired by the Service.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE.equals(action)) {
                Intent accessoryIntent = new Intent(MusicActivity.this, AccessoryActivity.class);
                startActivity(accessoryIntent);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_ACCSTATUS_AVAILABLE);
        return intentFilter;
    }
}
