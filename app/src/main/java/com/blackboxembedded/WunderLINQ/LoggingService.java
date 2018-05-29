package com.blackboxembedded.WunderLINQ;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class LoggingService extends Service {

    private static final String TAG = "WunderLINQ";

    Handler handler;
    Runnable runnable;

    private Location lastLocation;
    private PrintWriter outFile = null;

    private int loggingInterval = 1000;

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // TODO Auto-generated method stub
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
                String header = "Time,Latitude,Longitude,Altitude(meters),Speed(meters/second),Gear,Engine Temperature(celcius)," +
                        "Ambient Temperature(celcius),Front Tire Pressure(bar),Rear Tire Pressure(bar),Odometer(kilometers),Voltage(Volts)," +
                        "Throttle Position(%),Front Brakes,Rear Brakes,Shifts\n";
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
                    getLastLocation();
                    //getSpeed();
                    Data.setLastLocation(lastLocation);
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
                        spd = Float.toString(lastLocation.getSpeed());
                    }
                    outFile.write(curdatetime + "," + lat + "," + lon + "," + alt + "," + spd + ","
                            + Data.getGear() + "," + Data.getEngineTemperature() + "," + Data.getAmbientTemperature()
                            + "," + Data.getFrontTirePressure() + "," + Data.getRearTirePressure() + ","
                            + Data.getOdometer() + "," + Data.getvoltage() + "," + Data.getThrottlePosition() + ","
                            + Data.getFrontBrake() + "," + Data.getRearBrake() + "," + Data.getNumberOfShifts() + "\n");
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
        ((MyApplication) this.getApplication()).setTripRecording(false);
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // permission has been granted, continue as usual
            // Get last known recent location using new Google Play Services SDK (v11+)
            FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);

            locationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // GPS location can be null if GPS is switched off
                            if (location != null) {
                                lastLocation = location;
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Error trying to get last GPS location");
                            e.printStackTrace();
                        }
                    });
        } else {
            Log.d(TAG, "No permissions to obtain location");
        }
    }
}
