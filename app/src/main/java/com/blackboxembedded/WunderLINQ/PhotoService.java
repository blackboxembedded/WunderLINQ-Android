package com.blackboxembedded.WunderLINQ;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

/*
Reference: http://stackoverflow.com/questions/28003186/capture-picture-without-preview-using-camera2-api

Problem
   1.  BufferQueue has been abandoned  from ImageCapture
 */
public class PhotoService extends Service {
    protected static final String TAG = "WunderLINQ";
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_BACK;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "CameraCaptureSession.StateCallback onConfigured");
            PhotoService.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "onImageAvailable");
            Image img = reader.acquireLatestImage();
            if (img != null) {
                processImage(img);
                img.close();
            }
        }
    };

    public void readyCamera()
    {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(pickedCamera);
            Size[] jpegSizes = null;
            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamConfigurationMap != null) {
                jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            }
            final boolean jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
            int width = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 640;
            int height = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 480;
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.i(TAG, "imageReader created");
        } catch (CameraAccessException|SecurityException e){
            Log.e(TAG, e.getMessage());
        }
    }


    /**
     *  Return the Camera Id which matches the field CAMERACHOICE.
     */
    public String getCamera(CameraManager manager){
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException | IllegalArgumentException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand flags " + flags + " startId " + startId);

        readyCamera();

        return super.onStartCommand(intent, flags, startId);
    }

    public void actOnReadyCameraDevice()
    {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        try {
            session.abortCaptures();
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        } catch (IllegalStateException e){
            Log.e(TAG, e.getMessage());
        }
        session.close();
        cameraDevice.close();
    }

    /**
     *  Process image data as desired.
     */
    private void processImage(Image image){
        //Process image data
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        final byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        File root = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/photos/");
        if(!root.exists()){
            if(!root.mkdirs()){
                Log.d(TAG,"Unable to create directory: " + root);
            } else {
                saveImageToDisk(bytes);
            }
        } else {
            saveImageToDisk(bytes);
        }
        image.close();
        this.stopSelf();
    }

    private void saveImageToDisk(final byte[] bytes) {

        final File file = new File(Environment.getExternalStorageDirectory() + "/WunderLINQ/photos/"
                + "WunderLINQ-Photo-" + DateFormat.format("yyyy-MM-dd_kk-mm-ss", new Date().getTime()) + ".jpg");

        try (final OutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.flush();
            output.close();
            MediaScannerConnection.scanFile(this,
                    new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
            Toast.makeText(this, R.string.toast_photo_taken, Toast.LENGTH_LONG).show();
        } catch (final IOException e) {
            Log.e(TAG, "Exception occurred while saving picture to external storage ", e);
        }
    }

    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            builder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /***
     * @return  orientation
     */
    int getOrientation() {
        WindowManager windowService = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = 0;
        switch (windowService.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                Log.d(TAG,"Rotation 0");
                rotation = 90;
                break;
            case Surface.ROTATION_90:
                Log.d(TAG,"Rotation 90");
                rotation = 0;
                break;
            case Surface.ROTATION_180:
                Log.d(TAG,"Rotation 180");
                rotation = 270;
                break;
            case Surface.ROTATION_270:
                Log.d(TAG,"Rotation 270");
                rotation = 270;
                break;
        }
        return rotation;
    }
}
