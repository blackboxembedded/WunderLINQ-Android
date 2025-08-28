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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.PreferenceManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class PhotoService extends Service implements LifecycleOwner {
    private static final String TAG = "PhotoService";

    // Intent extras you already use:
    // "CAMERA" (int: 0 front, 1 back), "PREFIX" (String), etc.
    private int cameraArg = 1; // default back
    private String prefixArg = "IMG_";

    private LifecycleRegistry lifecycleRegistry;
    private ImageCapture imageCapture;
    @Nullable private Location location;

    @Override
    public void onCreate() {
        super.onCreate();
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        if (intent != null && intent.getExtras() != null) {
            cameraArg = intent.getIntExtra("CAMERA", 1);
            String p = intent.getStringExtra("PREFIX");
            if (p != null && !p.isEmpty()) prefixArg = p;
        }

        // Prepare last-known location if app has permission.
        boolean hasFine = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (hasFine || hasCoarse) {
            try {
                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (lm != null) {
                    Criteria c = new Criteria();
                    String provider = lm.getBestProvider(c, false);
                    if (provider != null) location = lm.getLastKnownLocation(provider);
                }
            } catch (Throwable t) {
                Log.w(TAG, "Unable to get last known location", t);
            }
        }

        // Bind camera and capture one image.
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                CameraSelector selector = (cameraArg == 0)
                        ? new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
                        : new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                imageCapture = new ImageCapture.Builder()
                        // In 1.4.0, target rotation on headless capture isn’t required;
                        // we’ll rotate via the image’s metadata when saving.
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.bindToLifecycle(this, selector, imageCapture);
                takePicture(); // single shot then stopSelf()
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to get camera provider", e);
                stopSelf();
            }
        }, ContextCompat.getMainExecutor(this));

        return START_NOT_STICKY;
    }

    private void takePicture() {
        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        try {
                            // 1) Decode JPEG plane
                            Bitmap bmp = imageProxyToBitmap(image);

                            // 2) Rotate according to metadata
                            int rotationDegrees = image.getImageInfo().getRotationDegrees();
                            if (rotationDegrees != 0) {
                                Matrix m = new Matrix();
                                m.postRotate(rotationDegrees);
                                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                            }

                            // 3) Save to file
                            File file = saveBitmapToPictures(bmp);

                            // 4) Write EXIF GPS (if available)
                            if (file != null && location != null) {
                                try {
                                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
                                            getLatGeoCoordinates(location));
                                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,
                                            location.getLatitude() < 0 ? "S" : "N");
                                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
                                            getLonGeoCoordinates(location));
                                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,
                                            location.getLongitude() < 0 ? "W" : "E");
                                    exif.saveAttributes();
                                } catch (IOException e) {
                                    Log.w(TAG, "EXIF write failed", e);
                                }
                            }

                            // 5) Media scan & optional preview
                            if (file != null) {
                                MediaScannerConnection.scanFile(PhotoService.this,
                                        new String[]{file.toString()}, null, null);

                                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                if (sp.getBoolean("prefPhotoPreview", false)) {
                                    Intent alert = new Intent(getApplicationContext(), AlertActivity.class);
                                    alert.setFlags(FLAG_ACTIVITY_NEW_TASK);
                                    alert.putExtra("TYPE", AlertActivity.ALERT_PHOTO);
                                    alert.putExtra("TITLE", getString(R.string.alert_title_photopreview));
                                    alert.putExtra("BODY", "");
                                    alert.putExtra("BACKGROUND", file.getAbsolutePath());
                                    startActivity(alert);
                                }
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "Error handling captured image", t);
                        } finally {
                            // Always close the image!
                            image.close();
                            stopSelf();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Error taking picture", exception);
                        stopSelf();
                    }
                });
    }

    private static Bitmap imageProxyToBitmap(@NonNull ImageProxy image) {
        // Assumes JPEG output (default for ImageCapture). Plane[0] contains the full JPEG bytes.
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Nullable
    private File saveBitmapToPictures(@NonNull Bitmap bmp) {
        String dirName = Environment.DIRECTORY_PICTURES;
        File pictures = Environment.getExternalStoragePublicDirectory(dirName);
        File appDir = new File(pictures, "WunderLINQ");
        if (!appDir.exists() && !appDir.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + appDir);
            return null;
        }

        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String name = prefixArg + DateFormat.format("yyyyMMdd_HHmmss", System.currentTimeMillis()) + "_" + ts + ".jpg";
        File out = new File(appDir, name);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            fos.flush();
            return out;
        } catch (IOException e) {
            Log.e(TAG, "Saving bitmap failed", e);
            return null;
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
        }
    }

    // --- EXIF helpers (unchanged logic) ---
    private static String getLatGeoCoordinates(Location loc) {
        double lat = Math.abs(loc.getLatitude());
        int deg = (int) lat;
        lat = (lat - deg) * 60;
        int min = (int) lat;
        double sec = (lat - min) * 60;
        return deg + "/1," + min + "/1," + (int) (sec * 1000) + "/1000";
    }

    private static String getLonGeoCoordinates(Location loc) {
        double lon = Math.abs(loc.getLongitude());
        int deg = (int) lon;
        lon = (lon - deg) * 60;
        int min = (int) lon;
        double sec = (lon - min) * 60;
        return deg + "/1," + min + "/1," + (int) (sec * 1000) + "/1000";
    }

    // --- LifecycleOwner for camera binding ---
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // started service
    }
}
