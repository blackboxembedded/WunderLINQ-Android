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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.blackboxembedded.WunderLINQ.TaskList.TaskActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LoggingService extends Service {

    private static final String TAG = "LoggingSvc";

    private SharedPreferences sharedPrefs;

    Handler handler;
    Runnable runnable;

    private Location lastLocation;
    private PrintWriter outFile = null;

    private int loggingInterval = 1000;
    private String CHANNEL_ID = "WunderLINQ";

    String pressureFormat = "";
    String temperatureFormat = "";
    String distanceFormat = "";
    String consumptionFormat = "";

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
        Data.setNumberOfShifts(0);
        Data.setFrontBrake(0);
        Data.setRearBrake(0);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.deleteNotificationChannel(CHANNEL_ID);
        } else {
            mNotificationManager.cancel(1234);
        }
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

        Intent showTaskIntent = new Intent(getApplicationContext(), TaskActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                showTaskIntent,
                PendingIntent.FLAG_MUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);

        // Start foreground service to avoid unexpected kill
        /*
        Intent resumeReceive = new Intent(this, LoggingNotificationReceiver.class).setAction(LoggingNotificationReceiver.RESUME_ACTION);
        PendingIntent pendingIntentResume = PendingIntent.getBroadcast(this, LoggingNotificationReceiver.REQUEST_CODE_NOTIFICATION, resumeReceive, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action actionResume = new NotificationCompat.Action.Builder(R.drawable.ic_pause, "resume", pendingIntentResume).build();
        */

        Intent stopReceive = new Intent(this, LoggingNotificationReceiver.class).setAction(LoggingNotificationReceiver.STOP_ACTION);
        PendingIntent pendingIntentStop = PendingIntent.getBroadcast(this, LoggingNotificationReceiver.REQUEST_CODE_NOTIFICATION, stopReceive, PendingIntent.FLAG_MUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action actionStop = new NotificationCompat.Action.Builder(R.drawable.ic_stop, getResources().getString(R.string.btn_logging_notification_stop), pendingIntentStop).build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.title_logging_notification))
                .setContentText("")
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_road)
                //.addAction(actionResume)
                .addAction(actionStop);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, this.getString(R.string.title_logging_notification),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);
        }

        Notification notification = builder.build();
        startForeground(1234, notification);

        ((MyApplication) this.getApplication()).setTripRecording(true);
    }

    public LoggingService() {
        Log.d(TAG, "In LoggingService()");

        try {
            File root = new File(MyApplication.getContext().getExternalFilesDir(null), "/logs/");
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
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HH-mm-ss");
                String curdatetime = formatter.format(date);
                String filename = "WunderLINQ-TripLog-";

                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
                String pressureUnit = "bar";
                pressureFormat = sharedPrefs.getString("prefPressureF", "0");
                if (pressureFormat.contains("1")) {
                    // KPa
                    pressureUnit = "KPa";
                } else if (pressureFormat.contains("2")) {
                    // Kg-f
                    pressureUnit = "Kg-f";
                } else if (pressureFormat.contains("3")) {
                    // Psi
                    pressureUnit = "psi";
                }
                String temperatureUnit = "C";
                temperatureFormat = sharedPrefs.getString("prefTempF", "0");
                if (temperatureFormat.contains("1")) {
                    // F
                    temperatureUnit = "F";
                }
                String distanceUnit = "km";
                String heightUnit = "m";
                String distanceTimeUnit = "kmh";
                distanceFormat = sharedPrefs.getString("prefDistance", "0");
                if (distanceFormat.contains("1")) {
                    distanceUnit = "mi";
                    heightUnit = "ft";
                    distanceTimeUnit = "mph";
                }
                String consumptionUnit = "L/100";
                consumptionFormat = sharedPrefs.getString("prefConsumption", "0");
                if (consumptionFormat.contains("1")) {
                    consumptionUnit = "mpg";
                } else if (consumptionFormat.contains("2")) {
                    consumptionUnit = "mpg";
                } else if (consumptionFormat.contains("3")) {
                    consumptionUnit = "km/L";
                }
                String voltageUnit = "V";
                String throttleUnit = "%";

                String header = MyApplication.getContext().getResources().getString(R.string.time_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.latitude_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.longitude_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.altitude_header) + "(" + heightUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.gpsspeed_header) + "(" + distanceTimeUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.gear_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.enginetemp_header) + "(" + temperatureUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.ambienttemp_header) + "(" + temperatureUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.frontpressure_header) + "(" + pressureUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.rearpressure_header) + "(" + pressureUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.odometer_header) + "(" + distanceUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.voltage_header) + "(" + voltageUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.throttle_header) + "(" + throttleUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.frontbrakes_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.rearbrakes_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.shifts_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.vin_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.ambientlight_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.tripone_header) + "(" + distanceUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.triptwo_header) + "(" + distanceUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.tripauto_header) + "(" + distanceUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.speed_header) + "(" + distanceTimeUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.avgspeed_header) + "(" + distanceTimeUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.cconsumption_header) + "(" + consumptionUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.fueleconomyone_header) + "(" + consumptionUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.fueleconomytwo_header) + "(" + consumptionUnit + ")," +
                        MyApplication.getContext().getResources().getString(R.string.fuelrange_header) + "(" + distanceUnit + ")" + "," +
                        MyApplication.getContext().getResources().getString(R.string.leanangle_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.gforce_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.bearing_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.barometricpressure_header) + "(mBar)" + "," +
                        MyApplication.getContext().getResources().getString(R.string.rpm_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.leanangle_bike_header) +
                        MyApplication.getContext().getResources().getString(R.string.rearwheel_speed_header) +
                        "\n";

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
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
                    String curdatetime = formatter.format(date);
                    String lat = "No Fix";
                    String lon = "No Fix";
                    String alt = "No Fix";
                    String gpsSpeed = "No Fix";
                    if (lastLocation != null){
                        lat = Double.toString(lastLocation.getLatitude());
                        lon = Double.toString(lastLocation.getLongitude());
                        alt = Double.toString(lastLocation.getAltitude());
                        gpsSpeed = Double.toString(lastLocation.getSpeed() * 3.6);
                        if (distanceFormat.contains("1")) {
                            alt = Double.toString(Utils.mToFeet(lastLocation.getAltitude()));
                            gpsSpeed = Double.toString(Utils.kmToMiles(lastLocation.getSpeed() * 3.6));
                        }
                    }
                    Double rdcFront = Data.getFrontTirePressure();
                    if(Data.getFrontTirePressure() != null){
                        if (pressureFormat.contains("1")) {
                            // KPa
                            rdcFront = Utils.barTokPa(rdcFront);
                        } else if (pressureFormat.contains("2")) {
                            // Kg-f
                            rdcFront = Utils.barTokgf(rdcFront);
                        } else if (pressureFormat.contains("3")) {
                            // Psi
                            rdcFront = Double.valueOf(Utils.oneDigit.format(Utils.barToPsi(rdcFront)));
                        }
                    }
                    Double rdcRear = Data.getRearTirePressure();
                    if(Data.getRearTirePressure() != null){
                        if (pressureFormat.contains("1")) {
                            // KPa
                            rdcRear = Utils.barTokPa(rdcRear);
                        } else if (pressureFormat.contains("2")) {
                            // Kg-f
                            rdcRear = Utils.barTokgf(rdcRear);
                        } else if (pressureFormat.contains("3")) {
                            // Psi
                            rdcRear = Double.valueOf(Utils.oneDigit.format(Utils.barToPsi(rdcRear)));
                        }
                    }
                    Double engineTemp = Data.getEngineTemperature();
                    if(Data.getEngineTemperature() != null ){
                        if (temperatureFormat.contains("1")) {
                            // F
                            engineTemp = Utils.celsiusToFahrenheit(engineTemp);
                        }
                    }
                    Double ambientTemp = Data.getAmbientTemperature();
                    if(Data.getAmbientTemperature() != null ){
                        if (temperatureFormat.contains("1")) {
                            // F
                            ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                        }
                    }
                    Double odometer = Data.getOdometer();
                    if(Data.getOdometer() != null){
                        if (distanceFormat.contains("1")) {
                            odometer = Utils.kmToMiles(odometer);
                        }
                    }
                    Double trip1 = Data.getTripOne();
                    if(Data.getTripOne() != null) {
                        if (distanceFormat.contains("1")) {
                            trip1 = Utils.kmToMiles(trip1);
                        }
                    }
                    Double trip2 = Data.getTripTwo();
                    if (Data.getTripTwo() != null){
                        if (distanceFormat.contains("1")) {
                            trip2 = Utils.kmToMiles(trip2);
                        }
                    }
                    Double tripAuto = Data.getTripAuto();
                    if (Data.getTripAuto() != null){
                        if (distanceFormat.contains("1")) {
                            tripAuto = Utils.kmToMiles(tripAuto);
                        }
                    }
                    Double speed = Data.getSpeed();
                    if (Data.getSpeed() != null){
                        if (distanceFormat.contains("1")) {
                            speed = Utils.kmToMiles(speed);
                        }
                    }
                    Double rearWheelSpeed = Data.getRearSpeed();
                    if (Data.getRearSpeed() != null){
                        if (distanceFormat.contains("1")) {
                            rearWheelSpeed = Utils.kmToMiles(rearWheelSpeed);
                        }
                    }
                    Double avgSpeed = Data.getAvgSpeed();
                    if (Data.getAvgSpeed() != null){
                        if (distanceFormat.contains("1")) {
                            avgSpeed = Utils.kmToMiles(avgSpeed);
                        }
                    }
                    Double currentConsumption = Data.getCurrentConsumption();
                    if (Data.getCurrentConsumption() != null){
                        if (consumptionFormat.contains("1")) {
                            currentConsumption = Utils.l100Tompg(currentConsumption);
                        } else if (consumptionFormat.contains("2")) {
                            currentConsumption = Utils.l100Tompgi(currentConsumption);
                        } else if (consumptionFormat.contains("3")) {
                            currentConsumption = Utils.l100Tokml(currentConsumption);
                        }
                    }
                    Double fuelEconomyOne = Data.getFuelEconomyOne();
                    if (Data.getFuelEconomyOne() != null){
                        if (consumptionFormat.contains("1")) {
                            fuelEconomyOne = Utils.l100Tompg(fuelEconomyOne);
                        } else if (consumptionFormat.contains("2")) {
                            fuelEconomyOne = Utils.l100Tompgi(fuelEconomyOne);
                        } else if (consumptionFormat.contains("3")) {
                            fuelEconomyOne = Utils.l100Tokml(fuelEconomyOne);
                        }
                    }
                    Double fuelEconomyTwo = Data.getFuelEconomyTwo();
                    if (Data.getFuelEconomyTwo() != null){
                        if (consumptionFormat.contains("1")) {
                            fuelEconomyTwo = Utils.l100Tompg(fuelEconomyTwo);
                        } else if (consumptionFormat.contains("2")) {
                            fuelEconomyTwo  = Utils.l100Tompgi(fuelEconomyTwo);
                        } else if (consumptionFormat.contains("3")) {
                            fuelEconomyTwo  = Utils.l100Tokml(fuelEconomyTwo);
                        }
                    }
                    Double fuelRange = Data.getFuelRange();
                    if (Data.getFuelRange() != null){
                        if (distanceFormat.contains("1")) {
                            fuelRange = Utils.kmToMiles(fuelRange);
                        }
                    }
                    String bearing = "";
                    if (Data.getBearing() != null) {
                        Integer bearingValue = Data.getBearing();
                        bearing = bearingValue.toString();
                        if (!sharedPrefs.getString("prefBearing", "0").contains("0")) {
                            String cardinal = "";
                            if (bearingValue > 331 || bearingValue <= 28) {
                                cardinal = getString(R.string.north);
                            } else if (bearingValue > 28 && bearingValue <= 73) {
                                cardinal = getString(R.string.north_east);
                            } else if (bearingValue > 73 && bearingValue <= 118) {
                                cardinal = getString(R.string.east);
                            } else if (bearingValue > 118 && bearingValue <= 163) {
                                cardinal = getString(R.string.south_east);
                            } else if (bearingValue > 163 && bearingValue <= 208) {
                                cardinal = getString(R.string.south);
                            } else if (bearingValue > 208 && bearingValue <= 253) {
                                cardinal = getString(R.string.south_west);
                            } else if (bearingValue > 253 && bearingValue <= 298) {
                                cardinal = getString(R.string.west);
                            } else if (bearingValue > 298 && bearingValue <= 331) {
                                cardinal = getString(R.string.north_west);
                            }
                            bearing = cardinal;
                        }
                    }

                    outFile.write(curdatetime + "," + lat + "," + lon + "," + alt + "," + gpsSpeed + ","
                            + Data.getGear() + "," + engineTemp + "," + ambientTemp
                            + "," + rdcFront + "," + rdcRear + ","
                            + odometer + "," + Data.getvoltage() + "," + Data.getThrottlePosition() + ","
                            + Data.getFrontBrake() + "," + Data.getRearBrake() + "," + Data.getNumberOfShifts()
                            + "," + Data.getVin()  + "," + Data.getAmbientLight() + "," + trip1 + ","
                            + trip2 + "," + tripAuto + "," + speed + "," + avgSpeed + ","
                            + currentConsumption + "," + fuelEconomyOne + "," + fuelEconomyTwo + ","
                            + fuelRange + "," + Data.getLeanAngle() + "," + Data.getGForce() + ","
                            + bearing + "," + Data.getBarometricPressure() + "," + Data.getRPM() + ","
                            + Data.getLeanAngleBike() + "," + rearWheelSpeed + "\n");
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

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.deleteNotificationChannel(CHANNEL_ID);
        } else {
            mNotificationManager.cancel(1234);
        }

        ((MyApplication) this.getApplication()).setTripRecording(false);
    }
}
