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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class VideoRecService extends Service {
    private static final String TAG = "VideoRecService";

    // Foreground notification
    private static final String CHANNEL_ID = "wlq-video";
    private static final int NOTIF_ID = 1234;

    // Intent extra: CAMERA 0=front, 1=back (default back)
    private int cameraArg = CameraCharacteristics.LENS_FACING_BACK;

    // Camera2 plumbing
    private HandlerThread        cameraThread;
    private Handler              cameraHandler;
    private CameraDevice         cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder recordRequestBuilder;

    // MediaRecorder + outputs
    private MediaRecorder mediaRecorder;
    private Uri           outputUri;            // API 29+
    private android.os.ParcelFileDescriptor pfd;// API 29+
    private File          outputFile;           // <29

    // Chosen camera info
    private String cameraId;
    private int lensFacing = CameraCharacteristics.LENS_FACING_BACK;
    private int sensorOrientation = 0;
    private Size videoSize;

    // Optional: last known location (for MediaStore LAT/LON)
    @Nullable private Location location;

    // Bluetooth mic router
    private BluetoothMicRouter btRouter;

    // Display rotation → degrees for MediaRecorder orientation hint
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0,   90);
        ORIENTATIONS.append(Surface.ROTATION_90,  0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override public void onCreate() {
        super.onCreate();
        createNotification();
        btRouter = new BluetoothMicRouter(this);
        fetchLastKnownLocation();
        startCameraThread();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            cameraArg = intent.getIntExtra("CAMERA", CameraCharacteristics.LENS_FACING_BACK);
        }
        // Route BT mic (if present) first, then start camera+recording
        btRouter.routeToBluetoothIfPresentThen(this::startFlow);
        return START_STICKY;
    }

    private void startFlow() {
        try {
            if (!pickCamera()) {
                Log.e(TAG, "No matching camera found");
                stopSelf();
                return;
            }
            if (!prepareMediaRecorder()) {
                Log.e(TAG, "MediaRecorder prepare failed");
                stopSelf();
                return;
            }
            openCameraThenRecord();
        } catch (Throwable t) {
            Log.e(TAG, "startFlow error", t);
            stopSelf();
        }
    }

    // ---------------- Camera thread ----------------

    private void startCameraThread() {
        cameraThread = new HandlerThread("WLQ-Cam2");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void stopCameraThread() {
        if (cameraThread != null) {
            cameraThread.quitSafely();
            try { cameraThread.join(); } catch (InterruptedException ignored) {}
            cameraThread = null;
            cameraHandler = null;
        }
    }

    // ---------------- Camera selection ----------------

    private boolean pickCamera() throws CameraAccessException {
        CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (cm == null) return false;

        int desired = (cameraArg == 0)
                ? CameraCharacteristics.LENS_FACING_FRONT
                : CameraCharacteristics.LENS_FACING_BACK;

        // First pass: try desired lens
        for (String id : cm.getCameraIdList()) {
            CameraCharacteristics cc = cm.getCameraCharacteristics(id);
            Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
            if (facing == null || facing != desired) continue;

            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) continue;

            Size[] recorderSizes = map.getOutputSizes(MediaRecorder.class);
            if (recorderSizes == null || recorderSizes.length == 0) continue;

            Size best = chooseVideoSize(recorderSizes);
            if (best == null) continue;

            cameraId = id;
            lensFacing = facing;
            Integer so = cc.get(CameraCharacteristics.SENSOR_ORIENTATION);
            sensorOrientation = (so != null) ? so : 0;
            videoSize = best;
            return true;
        }

        // Fallback: pick any with MediaRecorder output
        for (String id : cm.getCameraIdList()) {
            CameraCharacteristics cc = cm.getCameraCharacteristics(id);
            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) continue;
            Size[] recorderSizes = map.getOutputSizes(MediaRecorder.class);
            if (recorderSizes == null || recorderSizes.length == 0) continue;

            cameraId = id;
            Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
            lensFacing = (facing != null) ? facing : CameraCharacteristics.LENS_FACING_BACK;
            Integer so = cc.get(CameraCharacteristics.SENSOR_ORIENTATION);
            sensorOrientation = (so != null) ? so : 0;
            videoSize = chooseVideoSize(recorderSizes);
            return (videoSize != null);
        }

        return false;
    }

    private static Size chooseVideoSize(Size[] choices) {
        // Prefer 1080p, else 720p, else largest ~16:9, else largest overall
        Size pick1080 = null, pick720 = null, best169 = null;
        for (Size s : choices) {
            if (s.getWidth() == 1920 && s.getHeight() == 1080) pick1080 = s;
            if (s.getWidth() == 1280 && s.getHeight() == 720)  pick720  = s;
            float r = (float) s.getWidth() / s.getHeight();
            if (Math.abs(r - 16f/9f) < 0.05f) {
                if (best169 == null ||
                        (s.getWidth()*s.getHeight() > best169.getWidth()*best169.getHeight())) {
                    best169 = s;
                }
            }
        }
        if (pick1080 != null) return pick1080;
        if (pick720  != null) return pick720;
        if (best169  != null) return best169;
        return Collections.max(Arrays.asList(choices),
                Comparator.comparingInt(a -> a.getWidth() * a.getHeight()));
    }

    // ---------------- MediaRecorder ----------------

    private int computeOrientationHint() {
        int rotation = Surface.ROTATION_0;
        try {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (wm != null && wm.getDefaultDisplay() != null) {
                rotation = wm.getDefaultDisplay().getRotation();
            }
        } catch (Throwable ignored) {}

        int deviceDeg = ORIENTATIONS.get(rotation, 0);
        // Typical recorder mapping (front gets the +180 mirror compensation)
        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            return (sensorOrientation + deviceDeg + 180) % 360;
        } else {
            return (sensorOrientation + deviceDeg) % 360;
        }
    }

    @SuppressLint("MissingPermission")
    private boolean prepareMediaRecorder() {
        releaseMediaRecorder();

        mediaRecorder = new MediaRecorder();

        // Audio source that respects BT SCO routing; change to MIC if you prefer less processing.
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        CamcorderProfile prof = bestProfileForCamera(cameraId);
        if (prof != null) {
            mediaRecorder.setOutputFormat(prof.fileFormat);
            mediaRecorder.setVideoEncoder(prof.videoCodec);
            mediaRecorder.setAudioEncoder(prof.audioCodec);
            mediaRecorder.setVideoEncodingBitRate(prof.videoBitRate);
            mediaRecorder.setVideoFrameRate(prof.videoFrameRate);
            mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            mediaRecorder.setAudioEncodingBitRate(prof.audioBitRate);
            mediaRecorder.setAudioSamplingRate(prof.audioSampleRate);
            // Channels from profile are fine; SCO is typically mono and will be upmixed as needed.
        } else {
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncodingBitRate(10_000_000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            mediaRecorder.setAudioEncodingBitRate(128_000);
            mediaRecorder.setAudioSamplingRate(48_000);
            mediaRecorder.setAudioChannels(1);
        }

        // Output target
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String name = "WLQ_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Video.Media.TITLE, "WunderLINQ Video");
                if (location != null) {
                    values.put(MediaStore.Video.Media.LATITUDE,  location.getLatitude());
                    values.put(MediaStore.Video.Media.LONGITUDE, location.getLongitude());
                }

                ContentResolver cr = getContentResolver();
                outputUri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                if (outputUri == null) throw new IOException("MediaStore insert failed");
                pfd = cr.openFileDescriptor(outputUri, "rw");
                if (pfd == null) throw new IOException("openFileDescriptor null");
                mediaRecorder.setOutputFile(pfd.getFileDescriptor());
            } else {
                File movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                File appDir = new File(movies, "WunderLINQ");
                if (!appDir.exists() && !appDir.mkdirs()) throw new IOException("mkdirs failed: " + appDir);
                String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                outputFile = new File(appDir, "WLQ_" + ts + ".mp4");
                mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Output setup failed", ioe);
            return false;
        }

        mediaRecorder.setOrientationHint(computeOrientationHint());

        try {
            mediaRecorder.prepare();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "MediaRecorder.prepare failed", e);
            return false;
        }
    }

    private @Nullable CamcorderProfile bestProfileForCamera(@Nullable String id) {
        int cid = -1;
        if (id != null) {
            try { cid = Integer.parseInt(id); } catch (Exception ignored) {}
        }
        int[] quals = new int[] {
                CamcorderProfile.QUALITY_1080P,
                CamcorderProfile.QUALITY_720P,
                CamcorderProfile.QUALITY_480P,
                CamcorderProfile.QUALITY_HIGH
        };
        for (int q : quals) {
            try {
                if (cid >= 0 && CamcorderProfile.hasProfile(cid, q)) return CamcorderProfile.get(cid, q);
                if (cid < 0 && CamcorderProfile.hasProfile(q))        return CamcorderProfile.get(q);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    // ---------------- Open camera & start recording ----------------

    @SuppressLint("MissingPermission")
    private void openCameraThenRecord() throws CameraAccessException {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "CAMERA permission not granted");
            stopSelf();
            return;
        }
        CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (cm == null) { stopSelf(); return; }

        cm.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                try {
                    createRecordSessionAndStart();
                } catch (Exception e) {
                    Log.e(TAG, "createRecordSession failed", e);
                    stopSelf();
                }
            }
            @Override public void onDisconnected(@NonNull CameraDevice camera) {
                Log.w(TAG, "Camera disconnected");
                camera.close();
                cameraDevice = null;
                stopSelf();
            }
            @Override public void onError(@NonNull CameraDevice camera, int error) {
                Log.e(TAG, "Camera error: " + error);
                camera.close();
                cameraDevice = null;
                stopSelf();
            }
        }, cameraHandler);
    }

    private void createRecordSessionAndStart() throws CameraAccessException {
        if (cameraDevice == null || mediaRecorder == null) throw new IllegalStateException("Not ready");

        final Surface recorderSurface = mediaRecorder.getSurface();

        recordRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        recordRequestBuilder.addTarget(recorderSurface);
        recordRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        recordRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        recordRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

        cameraDevice.createCaptureSession(
                Collections.singletonList(recorderSurface),
                new CameraCaptureSession.StateCallback() {
                    @Override public void onConfigured(@NonNull CameraCaptureSession session) {
                        captureSession = session;
                        try {
                            session.setRepeatingRequest(recordRequestBuilder.build(), null, cameraHandler);
                            mediaRecorder.start();
                            Log.d(TAG, "Recording started: " + (outputUri != null ? outputUri :
                                    (outputFile != null ? outputFile.getAbsolutePath() : "unknown")));
                            if (getApplication() instanceof MyApplication) {
                                ((MyApplication) getApplication()).setVideoRecording(true);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Recorder start failed", e);
                            stopSelf();
                        }
                    }
                    @Override public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "CaptureSession configure failed");
                        stopSelf();
                    }
                },
                cameraHandler
        );
    }

    // ---------------- Location helper ----------------

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

    // ---------------- Foreground notification ----------------

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

    // ---------------- Teardown ----------------

    @Override public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // Stop camera repeating and recording first (order matters on some OEMs)
        try {
            if (captureSession != null) {
                try { captureSession.stopRepeating(); } catch (Throwable ignored) {}
                try { captureSession.abortCaptures(); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            if (mediaRecorder != null) {
                try { mediaRecorder.stop(); } catch (Throwable t) { Log.w(TAG, "Recorder stop", t); }
            }
        } catch (Throwable ignored) {}

        releaseMediaRecorder();

        if (captureSession != null) {
            try { captureSession.close(); } catch (Throwable ignored) {}
            captureSession = null;
        }
        if (cameraDevice != null) {
            try { cameraDevice.close(); } catch (Throwable ignored) {}
            cameraDevice = null;
        }
        if (pfd != null) {
            try { pfd.close(); } catch (Throwable ignored) {}
            pfd = null;
        }

        // Clear BT routing
        if (btRouter != null) btRouter.clearRouting();

        stopCameraThread();

        if (getApplication() instanceof MyApplication) {
            ((MyApplication) getApplication()).setVideoRecording(false);
        }
        super.onDestroy();
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try { mediaRecorder.reset(); } catch (Throwable ignored) {}
            try { mediaRecorder.release(); } catch (Throwable ignored) {}
            mediaRecorder = null;
        }
    }

    // --- Service binding ---
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
