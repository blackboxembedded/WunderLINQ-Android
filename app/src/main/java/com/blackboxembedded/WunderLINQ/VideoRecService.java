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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class VideoRecService extends Service implements LifecycleOwner {

    private static final String TAG = "VideoRecService";
    private LifecycleRegistry mLifecycleRegistry;

    private int cameraArg = 0;
    private CameraManager cameraManager;
    private MediaRecorder mediaRecorder;
    private String cameraId;
    private Size videoSize;
    private ProcessCameraProvider cameraProvider;
    private File outputFile;
    private Location location;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        createNotification();

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == cameraArg) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to get camera ID", e);
            stopSelf();
        }

        if (cameraId == null) {
            Log.e(TAG, "No camera found");
            stopSelf();
        }

        mediaRecorder = new MediaRecorder();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        if (intent != null) {
            // Retrieve the Intent that started the Service
            Bundle extras = intent.getExtras();
            if (extras != null) {
                cameraArg = extras.getInt("camera");
                Log.d(TAG, "Camera Choice: " + cameraArg);
            } else {
                stopSelf();
            }
        }

        boolean locationWPPerms = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check Location permissions
            if (getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationWPPerms = true;
            }
        } else {
            locationWPPerms = true;
        }
        if (locationWPPerms) {
            LocationManager locationManager = (LocationManager)
                    this.getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String bestProvider = locationManager.getBestProvider(criteria, false);
            location = locationManager.getLastKnownLocation(bestProvider);
        }

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        // Get the camera instance
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                // Set up the video output file
                outputFile = createVideoFile();

                // Set up the MediaRecorder
                Size[] sizes = cameraManager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(MediaRecorder.class);

                videoSize = sizes[0];
                for (Size size : sizes) {
                    if (size.getWidth() * size.getHeight() > videoSize.getWidth() * videoSize.getHeight()) {
                        videoSize = size;
                    }
                }
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
                mediaRecorder.setVideoEncodingBitRate(10000000);
                mediaRecorder.setAudioSamplingRate(16000);
                mediaRecorder.setVideoFrameRate(30);
                mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mediaRecorder.prepare();

                // Get the camera provider instance
                cameraProvider = cameraProviderFuture.get();

                cameraProvider.unbindAll();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                if (cameraArg == 0 ){
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();
                }

                // Set up the video capture use case
                VideoCapture.Builder videoCaptureConfigBuilder = new VideoCapture.Builder();
                videoCaptureConfigBuilder.setTargetAspectRatio(AspectRatio.RATIO_16_9);
                VideoCapture videoCapture = videoCaptureConfigBuilder.build();

                // Bind the lifecycle of the camera to the lifecycle of the service
                cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture);

                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "WunderLINQ-" + DateFormat.format("yyyyMMdd_kkmmss", new Date().getTime()).toString());
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                contentValues.put(MediaStore.Video.Media.TITLE, "WunderLINQ Video");
                if (location != null) {
                    contentValues.put(MediaStore.Video.Media.LATITUDE, String.valueOf(location.getLatitude()));
                    contentValues.put(MediaStore.Video.Media.LONGITUDE, String.valueOf(location.getLongitude()));
                }

                VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(
                        this.getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, //Use this to save in normal Gallery
                        contentValues
                ).build();


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    videoCapture.startRecording(outputFileOptions, getMainExecutor(), new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            Log.i(TAG,"Recording ended");
                        }

                        @Override
                        public void onError(int videoCaptureError, String message, Throwable cause) {
                            // Error occurred while saving the video
                        }
                    });
                } else {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    Executor mainExecutor = new Executor() {
                        @Override
                        public void execute(Runnable command) {
                            mainHandler.post(command);
                        }
                    };

                    videoCapture.startRecording(outputFileOptions, mainExecutor, new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            Log.i(TAG,"Recording ended");
                        }

                        @Override
                        public void onError(int videoCaptureError, String message, Throwable cause) {
                            // Error occurred while saving the video
                        }
                    });
                }

                // Start recording
                mediaRecorder.start();
                isRecording = true;
                Log.d(TAG, "Recording started");
                ((MyApplication) this.getApplication()).setVideoRecording(true);

            } catch (ExecutionException | InterruptedException | IOException | CameraAccessException e) {
                Log.e(TAG, "Error setting up camera and media recorder", e);
                stopSelf();
            }
        }, ContextCompat.getMainExecutor(this));

        // Return START_STICKY to indicate that this service should be restarted if it's killed
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        if (isRecording) {
            // Stop recording
            try {
                mediaRecorder.stop();
            } catch(RuntimeException stopException) {
                // handle cleanup here
            }
            mediaRecorder.reset();
            mediaRecorder.release();
            Log.d(TAG, "Recording stopped");
        }

        // Unbind and release the camera
        cameraProvider.unbindAll();

        ((MyApplication) this.getApplication()).setVideoRecording(false);

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    private File createVideoFile() throws IOException {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM)+"/WunderLINQ/VID_"+
                DateFormat.format("yyyyMMdd_kkmmss", new Date().getTime())+
                ".mp4");
    }

    private void createNotification() {
        // Start foreground service to avoid unexpected kill
        String CHANNEL_ID = "WunderLINQ";
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
                .setContentTitle(getResources().getString(R.string.title_video_notification))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_video_camera);
        Notification notification = builder.build();
        startForeground(1234, notification);
    }
}