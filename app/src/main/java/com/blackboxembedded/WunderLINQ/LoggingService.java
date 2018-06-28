package com.blackboxembedded.WunderLINQ;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LoggingService extends Service implements LocationListener, GoogleApiClient
        .ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "WunderLINQ";

    LocationRequest locationRequest;
    GoogleApiClient googleApiClient;

    Handler handler;
    Runnable runnable;

    private Location lastLocation;
    private PrintWriter outFile = null;

    private int loggingInterval = 1000;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "In onTaskRemoved");
        if(outFile != null) {
            outFile.flush();
            outFile.close();
        }
        handler.removeCallbacks(runnable);
        ((MyApplication) this.getApplication()).setTripRecording(false);
        /*
        Intent restartService = new Intent(getApplicationContext(),
                this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);

        //Restart the service once it has been killed android
        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +100, restartServicePI);
        */
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "In onCreate");
        super.onCreate();

        createLocationRequest();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();

        ((MyApplication) this.getApplication()).setTripRecording(true);
    }

    public LoggingService() {
        Log.d(TAG, "In LoggingService()");

        try {
            File root = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/logs/");
            if(!root.exists()){
                if(!root.mkdirs()){
                    Log.d(TAG,"Unable to create directory: " + root);
                }
            }

            if(root.canWrite()){
                Log.d(TAG,"Initialize Logging");
                // Get current time in UTC
                Calendar cal = Calendar.getInstance();
                Date date = cal.getTime();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
                String curdatetime = formatter.format(date);
                String filename = "WunderLINQ-TripLog-";
                String header = "Time,Latitude,Longitude,Altitude(meters),Speed(km/h),Gear,Engine Temperature(celcius)," +
                        "Ambient Temperature(celcius),Front Tire Pressure(bar),Rear Tire Pressure(bar),Odometer(kilometers),Voltage(Volts)," +
                        "Throttle Position(%),Front Brakes,Rear Brakes,Shifts,VIN\n";
                File logFile = new File( root, filename + curdatetime + ".csv" );
                FileWriter logWriter = new FileWriter( logFile );
                outFile = new PrintWriter( logWriter );
                outFile.write(header);
                outFile.flush();
            }

            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    lastLocation = Data.getLastLocation();
                    // Log data
                    Calendar cal = Calendar.getInstance();
                    Date date = cal.getTime();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String curdatetime = formatter.format(date);
                    String lat = "No Fix";
                    String lon = "No Fix";
                    String alt = "No Fix";
                    String spd = "No Fix";
                    if (lastLocation != null){
                        lat = Double.toString(lastLocation.getLatitude());
                        lon = Double.toString(lastLocation.getLongitude());
                        alt = Double.toString(lastLocation.getAltitude());
                        spd = Double.toString(lastLocation.getSpeed() * 3.6);
                    }
                    outFile.write(curdatetime + "," + lat + "," + lon + "," + alt + "," + spd + ","
                            + Data.getGear() + "," + Data.getEngineTemperature() + "," + Data.getAmbientTemperature()
                            + "," + Data.getFrontTirePressure() + "," + Data.getRearTirePressure() + ","
                            + Data.getOdometer() + "," + Data.getvoltage() + "," + Data.getThrottlePosition() + ","
                            + Data.getFrontBrake() + "," + Data.getRearBrake() + "," + Data.getNumberOfShifts() + "," + Data.getVin() +"\n");
                    outFile.flush();
                    handler.postDelayed(runnable, loggingInterval);
                }
            };
            handler.postDelayed(runnable, 0);

        } catch (IOException e) {
            Log.d(TAG, "Could not write to file: " + e.getMessage());
            ((MyApplication) this.getApplication()).setTripRecording(false);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"In onDestroy()");
        if(outFile != null) {
            outFile.flush();
            outFile.close();
        }
        handler.removeCallbacks(runnable);

        stopLocationUpdates();
        googleApiClient.disconnect();

        ((MyApplication) this.getApplication()).setTripRecording(false);
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi
                .requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Data.setLastLocation(location);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
