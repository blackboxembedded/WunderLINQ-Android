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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Quality;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class VideoRecService extends Service implements LifecycleOwner {
    private static final String TAG = "VideoRecService";

    // Foreground notification
    private static final String CHANNEL_ID = "wlq-video";
    private static final int NOTIF_ID = 1234;

    // Intent extras (match your existing usage)
    // CAMERA: 0=front, 1=back (default back)
    private int cameraArg = CameraSelector.LENS_FACING_BACK;

    private LifecycleRegistry lifecycleRegistry;
    private ProcessCameraProvider cameraProvider;

    // CameraX 1.4.0 video API
    private Recorder recorder;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;

    // Optional: last known location (for MediaStore LAT/LON columns)
    @Nullable private Location location;

    @Override
    public void onCreate() {
        super.onCreate();
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        createNotification();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        if (intent != null) {
            cameraArg = intent.getIntExtra("CAMERA", CameraSelector.LENS_FACING_BACK);
        }

        // Try to fetch a last-known location if we have permission.
        fetchLastKnownLocation();

        // Spin up CameraX and start recording
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                startCameraAndRecord();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraProvider error", e);
                stopSelf();
            }
        }, ContextCompat.getMainExecutor(this));

        return START_STICKY;
    }

    private void startCameraAndRecord() {
        if (cameraProvider == null) {
            Log.e(TAG, "cameraProvider null");
            stopSelf();
            return;
        }

        cameraProvider.unbindAll();

        CameraSelector selector = new CameraSelector.Builder()
                .requireLensFacing(cameraArg == 0
                        ? CameraSelector.LENS_FACING_FRONT
                        : CameraSelector.LENS_FACING_BACK)
                .build();

        // Prefer FHD, then HD, then SD (fallbacks are important across devices)
        QualitySelector qualitySelector = QualitySelector.fromOrderedList(
                java.util.Arrays.asList(Quality.FHD, Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD));

        recorder = new Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build();

        videoCapture = VideoCapture.withOutput(recorder);

        // Bind to this Service's lifecycle
        cameraProvider.bindToLifecycle(this, selector, videoCapture);

        // Choose output: MediaStore (scoped storage) for API 29+; else a file in Movies/WunderLINQ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startRecordingToMediaStore();
        } else {
            startRecordingToFile();
        }
    }

    private void startRecordingToMediaStore() {
        String displayName = "WLQ_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.TITLE, "WunderLINQ Video");
        if (location != null) {
            // These columns are respected by some OEM galleries and Google Photos for videos.
            values.put(MediaStore.Video.Media.LATITUDE, location.getLatitude());
            values.put(MediaStore.Video.Media.LONGITUDE, location.getLongitude());
        }

        MediaStoreOutputOptions outputOptions =
                new MediaStoreOutputOptions.Builder(getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        .setContentValues(values)
                        .build();

        beginRecording(outputOptions);
    }

    private void startRecordingToFile() {
        File movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File appDir = new File(movies, "WunderLINQ");
        if (!appDir.exists() && !appDir.mkdirs()) {
            Log.e(TAG, "Failed to create dir: " + appDir);
            stopSelf();
            return;
        }
        String ts = DateFormat.format("yyyyMMdd_HHmmss", System.currentTimeMillis()).toString();
        File out = new File(appDir, "WLQ_" + ts + ".mp4");

        FileOutputOptions outputOptions =
                new FileOutputOptions.Builder(out).build();

        beginRecording(outputOptions);
    }

    @SuppressLint("MissingPermission")
    private void beginRecording(@NonNull Object outputOptions) {
        if (videoCapture == null || recorder == null) {
            Log.e(TAG, "Video components not ready");
            stopSelf();
            return;
        }

        PendingRecording pending;
        if (outputOptions instanceof MediaStoreOutputOptions) {
            pending = recorder.prepareRecording(this, (MediaStoreOutputOptions) outputOptions);
        } else if (outputOptions instanceof FileOutputOptions) {
            pending = recorder.prepareRecording(this, (FileOutputOptions) outputOptions);
        } else {
            Log.e(TAG, "Unsupported output options");
            stopSelf();
            return;
        }

        // Enable audio if we have permission
        boolean hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        if (hasAudio) pending = pending.withAudioEnabled();

        activeRecording = pending.start(ContextCompat.getMainExecutor(this), event -> {
            if (event instanceof VideoRecordEvent.Start) {
                Log.d(TAG, "Recording started");
                ((MyApplication) getApplication()).setVideoRecording(true);
            } else if (event instanceof VideoRecordEvent.Finalize finalizeEvent) {
                Uri uri = finalizeEvent.getOutputResults().getOutputUri();
                Log.d(TAG, "Recording finalized: " + uri + " error=" + finalizeEvent.getError());
                ((MyApplication) getApplication()).setVideoRecording(false);
                // Stop the service once finalized (adjust if you want continuous)
                stopSelf();
            } else if (event instanceof VideoRecordEvent.Status status) {
                // Optional: bitrate, duration, etc.
                // Log.v(TAG, "Status: " + status.getRecordedDurationNanos());
            } else if (event instanceof VideoRecordEvent.Pause) {
                Log.d(TAG, "Recording paused");
            } else if (event instanceof VideoRecordEvent.Resume) {
                Log.d(TAG, "Recording resumed");
            }
        });
    }

    private void fetchLastKnownLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return;

        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm != null) {
                Criteria c = new Criteria();
                String provider = lm.getBestProvider(c, false);
                if (provider != null) location = lm.getLastKnownLocation(provider);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Location fetch failed", t);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // Stop recording if active
        if (activeRecording != null) {
            try {
                activeRecording.stop();
            } catch (Throwable t) {
                Log.w(TAG, "Error stopping recording", t);
            }
            try {
                activeRecording.close();
            } catch (Throwable ignored) {}
            activeRecording = null;
        }

        // Unbind and release the camera
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        ((MyApplication) getApplication()).setVideoRecording(false);

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }

    // --- Foreground notification (unchanged style) ---
    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.title_video_notification),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setShowBadge(false);
            ch.setSound(null, null);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.title_video_notification))
                .setContentText("")
                .setSmallIcon(R.drawable.ic_video_camera)
                .build();
        startForeground(NOTIF_ID, notif);
    }

    // --- LifecycleOwner for binding ---
    @NonNull @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }
}
