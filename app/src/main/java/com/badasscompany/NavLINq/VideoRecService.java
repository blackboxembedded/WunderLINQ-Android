package com.badasscompany.NavLINq;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class VideoRecService extends Service implements SurfaceHolder.Callback {

    private final static String TAG = "VideoRecService";

    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private Camera camera = null;
    private MediaRecorder mediaRecorder = null;

    private File recordingFile;

    @Override
    public void onCreate() {
        ((MyApplication) this.getApplication()).setVideoRecording(true);
        recordingFile = new File(Environment.getExternalStorageDirectory()+"/NavLINq/videos/"+
                DateFormat.format("NavLINq-yyyy-MM-dd_kk-mm-ss", new Date().getTime())+
                ".mp4");
        // Start foreground service to avoid unexpected kill
        String CHANNEL_ID = "NavLINq";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, this.getString(R.string.title_video_notification),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setContentTitle("Background Video Recorder")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_video_camera);
        Notification notification = builder.build();
        startForeground(1234, notification);

        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);
        LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        surfaceView.getHolder().addCallback(this);

    }

    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        camera = Camera.open();
        mediaRecorder = new MediaRecorder();
        camera.unlock();

        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        File root = new File(Environment.getExternalStorageDirectory(), "/NavLINq/videos/");
        if(!root.exists()){
            if(!root.mkdirs()){
                Log.d(TAG,"Unable to create directory: " + root);
            }
        }
        mediaRecorder.setOutputFile(recordingFile.getPath());

        try { mediaRecorder.prepare(); } catch (Exception e) {}
        mediaRecorder.start();

    }

    // Stop recording and remove SurfaceView
    @Override
    public void onDestroy() {

        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();

        camera.lock();
        camera.release();

        windowManager.removeView(surfaceView);

        ContentValues values = new ContentValues(3);
        values.put(MediaStore.Video.Media.TITLE, "NavLINq Video");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, recordingFile.getAbsolutePath());
        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        ((MyApplication) this.getApplication()).setVideoRecording(false);

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}

    @Override
    public IBinder onBind(Intent intent) { return null; }

}