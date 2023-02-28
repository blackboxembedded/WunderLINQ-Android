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
import static java.lang.Math.abs;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class PhotoService extends Service implements LifecycleOwner {
    private static final String TAG = "PhotoService";
    private LifecycleRegistry mLifecycleRegistry;
    private int cameraArg = 0;
    private ImageCapture imageCapture;
    private File file;
    private Location location;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

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

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();
                if (cameraArg == 0){
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();
                }
                WindowManager windowService = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                int rotation = windowService.getDefaultDisplay().getRotation();
                Log.d(TAG,"rotation: " + rotation);
                imageCapture = new ImageCapture.Builder()
                        //.setTargetRotation(Surface.ROTATION_270)
                        .build();
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
                // Take a picture
                takePicture();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

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

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    private void takePicture() {
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                // Get the bitmap from the image
                Bitmap bitmap = imageProxyToBitmap(image);

                // Save the bitmap to file
                file = createFile();
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    Log.d(TAG, "File saved: " + file.getAbsolutePath());

                    if (location != null) {
                        Log.d(TAG,"Location: " + location.toString());
                        storeGeoCoordsToImage(file, location);
                    }

                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
                    if (sharedPrefs.getBoolean("prefPhotoPreview",false)) {
                        Intent alertIntent = new Intent(MyApplication.getContext(), AlertActivity.class);
                        alertIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                        alertIntent.putExtra("TYPE", 2);
                        alertIntent.putExtra("TITLE", MyApplication.getContext().getResources().getString(R.string.alert_title_photopreview));
                        alertIntent.putExtra("BODY", "");
                        alertIntent.putExtra("BACKGROUND", file.getAbsolutePath());
                        MyApplication.getContext().startActivity(alertIntent);
                    }

                    MediaScannerConnection.scanFile(PhotoService.this,
                            new String[] { file.toString() }, null,
                            (path, uri) -> {
                                Log.i(TAG, "Scanned file: " + path);
                                stopSelf();
                            });
                    // Send a broadcast to notify the picture has been taken
                    Intent pictureTakenIntent = new Intent("PICTURE_TAKEN");
                    pictureTakenIntent.putExtra("file_path", file.getAbsolutePath());
                    LocalBroadcastManager.getInstance(PhotoService.this).sendBroadcast(pictureTakenIntent);
                } catch (IOException e) {
                    Log.d(TAG, "Error Saving: ");
                    e.printStackTrace();
                    stopSelf();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
                Log.e(TAG, "Error taking picture", exception);
                stopSelf();
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        // Rotate bitmap if necessary
        Matrix matrix = new Matrix();
        matrix.postRotate((float)image.getImageInfo().getRotationDegrees());
        Bitmap bitmap2 =  Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap2;
    }

    private File createFile() {
        File root = new File( Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "/WunderLINQ/");
        if(!root.exists()){
            if(!root.mkdirs()){
                Log.d(TAG,"Unable to create directory: " + root);
            }
        }
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/WunderLINQ/"
                + "IMG_" + DateFormat.format("yyyyMMdd_kkmmss", new Date().getTime()) + ".jpg");
    }

    public static String getLatGeoCoordinates(Location location) {

        if (location == null) return "0/1,0/1,0/1000";
        String[] degMinSec = Location.convert(abs(location.getLatitude()), Location.FORMAT_SECONDS).split(":");
        return degMinSec[0] + "/1," + degMinSec[1] + "/1," + degMinSec[2] + "/1000";
    }

    public static String getLonGeoCoordinates(Location location) {

        if (location == null) return "0/1,0/1,0/1000";
        String[] degMinSec = Location.convert(abs(location.getLongitude()), Location.FORMAT_SECONDS).split(":");
        return degMinSec[0] + "/1," + degMinSec[1] + "/1," + degMinSec[2] + "/1000";
    }

    public static boolean storeGeoCoordsToImage(File imagePath, Location location) {
        // Avoid NullPointer
        if (imagePath == null || location == null) return false;
        try {
            ExifInterface exif = new ExifInterface(imagePath.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,  getLatGeoCoordinates(location));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, location.getLatitude() < 0 ? "S" : "N");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, getLonGeoCoordinates(location));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, location.getLongitude() < 0 ? "W" : "E");
            Log.d(TAG,exif.toString());
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
