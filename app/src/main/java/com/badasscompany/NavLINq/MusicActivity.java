package com.badasscompany.NavLINq;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MusicActivity extends AppCompatActivity {

    public final static String TAG = "NavLINq";

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

    private Handler mHandler = new Handler();

    private boolean alertDiagUp = true;

    private OnClickListener mClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.prev_button:
                    controls.skipToPrevious();
                    refreshMetaData();
                    break;
                case R.id.next_button:
                    controls.skipToNext();
                    refreshMetaData();
                    break;
                case R.id.play_pause_button:
                    PlaybackState playbackState = controller.getPlaybackState();
                    if(playbackState.getState() != PlaybackState.STATE_PLAYING){
                        controls.play();
                        refreshMetaData();
                    } else {
                        controls.pause();
                        refreshMetaData();
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
        Log.d("Musicacvitity","oncreate");
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_music);

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
    }

    @Override
    public void onResume() {
        super.onResume();
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
    }

    @Override
    public void onPause() {
        Log.d("Musicacvitity","onpause");
        super.onPause();
        mHandler.removeCallbacks(mUpdateMetaData);
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mUpdateMetaData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mUpdateMetaData);
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
            @TargetApi(23)
            public void onDismiss(DialogInterface dialog) {
                getApplicationContext().startActivity(new Intent(
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
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
                if (playbackState.getState() != PlaybackState.STATE_PLAYING) {
                    mPlayPauseButton.setImageResource(R.drawable.ic_play);
                } else {
                    mPlayPauseButton.setImageResource(R.drawable.ic_pause);
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
                    mArtwork.setImageResource(R.drawable.ic_music_note);
                }
            } else {
                Log.d(TAG, "No music player running");
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

}
