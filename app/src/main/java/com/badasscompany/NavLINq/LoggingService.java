package com.badasscompany.NavLINq;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

/**
 * Created by keithconger on 9/24/17.
 *
 * // Use this to start and trigger a service
 * Intent i= new Intent(this, LoggingService.class);
 * this.startService(i);
 *
 */

public class LoggingService extends Service {

    private static final String TAG = "NavLINq";

    Handler handler;
    Runnable runnable;

    private Location lastLocation;
    private Logger tripLogger = null;

    private int loggingInterval = 60000;

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

        handler.removeCallbacks(runnable);
        tripLogger.shutdown();
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

        if (tripLogger == null) {
            tripLogger = new Logger();
        }
    }

    public LoggingService() {
        Log.d(TAG, "In LoggingService()");
        if (tripLogger == null) {
            tripLogger = new Logger();
        }
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                getLastLocation();
                Data.setLastLocation(lastLocation);
                // Log data
                String locationString ="No Fix";
                if (lastLocation != null){
                    locationString = lastLocation.toString();
                }

                tripLogger.write("trip", locationString + "," + Data.getGear() + "," + Data.getEngineTemperature() + "," + Data.getAmbientTemperature() + "," + Data.getFrontTirePressure()
                        + "," + Data.getRearTirePressure() + "," + Data.getOdometer());
                handler.postDelayed(runnable, loggingInterval);
            }
        };
        handler.postDelayed(runnable, 0);
    }
    public void stopLogging() {
        handler.removeCallbacks(runnable);
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