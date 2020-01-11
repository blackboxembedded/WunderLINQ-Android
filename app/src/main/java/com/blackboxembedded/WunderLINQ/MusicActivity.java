package com.blackboxembedded.WunderLINQ;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.Set;

public class MusicActivity extends AppCompatActivity implements View.OnTouchListener {

    public final static String TAG = "WunderLINQ";

    private ImageButton mPlayPauseButton;
    private TextView mArtistText;
    private TextView mTitleText;
    private TextView mAlbumText;
    private ImageView mArtwork;

    private MediaController.TransportControls controls;
    private MediaController controller;

    private SharedPreferences sharedPrefs;

    private Handler mHandler = new Handler();

    private boolean alertDiagUp = true;

    private GestureDetectorListener gestureDetector;

    private CountDownTimer cTimer = null;

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
                        try {
                            if (playbackState.getState() != PlaybackState.STATE_PLAYING) {
                                controls.play();
                                refreshMetaData();
                            } else {
                                controls.pause();
                                refreshMetaData();
                            }
                        } catch (NullPointerException e) {
                            // Testing
                            Log.d(TAG,"NullPointerException: " + e.toString());
                        }
                    }
                    break;
                case R.id.action_back:
                    Intent backIntent = new Intent(MusicActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_forward:
                    Intent forwardIntent = new Intent(MusicActivity.this, TaskActivity.class);
                    startActivity(forwardIntent);
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
                Intent backIntent = new Intent(MusicActivity.this, TaskActivity.class);
                startActivity(backIntent);
            }

            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(MusicActivity.this, MainActivity.class);
                startActivity(backIntent);
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

        showActionBar();

        // Check if we have permissions
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages (this);
        if (packageNames.contains(getApplicationContext().getPackageName()))
        {
            alertDiagUp = false;
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
            // Need permissions to read notifications
            requestPermissions();
        }
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

        // Need permissions to read notifications
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages (this);
        if (packageNames.contains(getApplicationContext().getPackageName())) {
            alertDiagUp = false;
            mHandler.post(mUpdateMetaData);
        } else {
            // Need permissions to read notifications
            if (!alertDiagUp) {
                requestPermissions();
            }
        }

        getSupportActionBar().show();
        startTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        cancelTimer();
        mHandler.removeCallbacks(mUpdateMetaData);
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelTimer();
        mHandler.removeCallbacks(mUpdateMetaData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimer();
        mHandler.removeCallbacks(mUpdateMetaData);
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
                        Drawable drawable = getResources().getDrawable(R.drawable.ic_music_note);
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
        } else {
            Log.d(TAG, "No permissions to control music player");
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
            case KeyEvent.KEYCODE_DPAD_UP:
                if (controller != null) {
                    controls.skipToNext();
                    refreshMetaData();
                }
                mPlayPauseButton.setFocusable(true);
                mPlayPauseButton.requestFocus();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
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
                Intent backIntent = new Intent(MusicActivity.this, MainActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mPlayPauseButton.setFocusable(true);
                mPlayPauseButton.requestFocus();
                Intent forwardIntent = new Intent(MusicActivity.this, TaskActivity.class);
                startActivity(forwardIntent);
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
}
