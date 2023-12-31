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

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.blackboxembedded.WunderLINQ.TaskList.TaskActivity;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LoggingService extends Service {

    private static final String TAG = "LoggingSvc";

    Handler handler;
    Runnable runnable;

    private Location lastLocation;
    private PrintWriter outFile = null;
    private Date logStartDate;

    private int loggingInterval = 250;
    private String CHANNEL_ID = "WunderLINQ";

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
        stopService(new Intent(this, LoggingService.class));
        if(outFile != null) {
            outFile.flush();
            outFile.close();
        }
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
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

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_road)
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1234, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1234, notification);
        }

        ((MyApplication) this.getApplication()).setTripRecording(true);
    }

    public LoggingService() {
        Log.d(TAG, "In LoggingService()");

        initializeFile();
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    lastLocation = Data.getLastLocation();
                    // Log data
                    Calendar cal = Calendar.getInstance();
                    Date date = cal.getTime();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                    String curdatetime = formatter.format(date);
                    String lat = getString(R.string.gps_nofix);
                    String lon = getString(R.string.gps_nofix);
                    if (lastLocation != null){
                        lat = Double.toString(lastLocation.getLatitude());
                        lon = Double.toString(lastLocation.getLongitude());
                    }
                    if (outFile != null) {
                        Calendar startDate = Calendar.getInstance();
                        startDate.setTime(logStartDate);
                        if (startDate.get(Calendar.YEAR) != cal.get(Calendar.YEAR)
                                || startDate.get(Calendar.DAY_OF_YEAR) != cal.get(Calendar.DAY_OF_YEAR)){
                            initializeFile();
                        } else {
                            outFile.write(curdatetime + "," +
                                    lat + "," + lon + "," +
                                    Data.getValue(Data.DATA_ALTITUDE_DEVICE) + "," +
                                    Data.getValue(Data.DATA_SPEED_DEVICE) + "," +
                                    Data.getValue(Data.DATA_GEAR) + "," +
                                    Data.getValue(Data.DATA_ENGINE_TEMP) + "," +
                                    Data.getValue(Data.DATA_AIR_TEMP) + "," +
                                    Data.getValue(Data.DATA_FRONT_RDC) + "," +
                                    Data.getValue(Data.DATA_REAR_RDC) + "," +
                                    Data.getValue(Data.DATA_ODOMETER) + "," +
                                    Data.getValue(Data.DATA_VOLTAGE) + "," +
                                    Data.getValue(Data.DATA_THROTTLE) + "," +
                                    Data.getValue(Data.DATA_FRONT_BRAKE) + "," +
                                    Data.getValue(Data.DATA_REAR_BRAKE) + "," +
                                    Data.getValue(Data.DATA_SHIFTS) + "," +
                                    Data.getVin() + "," +
                                    Data.getValue(Data.DATA_AMBIENT_LIGHT) + "," +
                                    Data.getValue(Data.DATA_TRIP_ONE) + "," +
                                    Data.getValue(Data.DATA_TRIP_TWO) + "," +
                                    Data.getValue(Data.DATA_TRIP_AUTO) + "," +
                                    Data.getValue(Data.DATA_SPEED) + "," +
                                    Data.getValue(Data.DATA_AVG_SPEED) + "," +
                                    Data.getValue(Data.DATA_CURRENT_CONSUMPTION) + "," +
                                    Data.getValue(Data.DATA_ECONOMY_ONE) + "," +
                                    Data.getValue(Data.DATA_ECONOMY_TWO) + "," +
                                    Data.getValue(Data.DATA_RANGE) + "," +
                                    Data.getValue(Data.DATA_LEAN_DEVICE) + "," +
                                    Data.getValue(Data.DATA_GFORCE_DEVICE) + "," +
                                    Data.getValue(Data.DATA_BEARING_DEVICE) + "," +
                                    Data.getValue(Data.DATA_BAROMETRIC_DEVICE) + "," +
                                    Data.getValue(Data.DATA_RPM) + "," +
                                    Data.getValue(Data.DATA_LEAN) + "," +
                                    Data.getValue(Data.DATA_REAR_SPEED) + "," +
                                    Data.getValue(Data.DATA_CELL_SIGNAL) + "," +
                                    Data.getValue(Data.DATA_BATTERY_DEVICE) +
                                    "\n");
                            outFile.flush();
                        }
                    } else {
                        initializeFile();
                    }
                    handler.postDelayed(runnable, loggingInterval);
                }
            };
            handler.postDelayed(runnable, 0);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"In onDestroy()");
        if(outFile != null) {
            outFile.flush();
            outFile.close();
        }
        if(handler != null) {
            handler.removeCallbacks(runnable);
        }

        ((MyApplication) this.getApplication()).setTripRecording(false);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = CHANNEL_ID;
        String channelName = this.getString(R.string.title_logging_notification);
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    private void initializeFile(){
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
                logStartDate = cal.getTime();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HH-mm-ss");
                String curdatetime = formatter.format(logStartDate);
                String filename = "WunderLINQ-TripLog-";

                String header = MyApplication.getContext().getResources().getString(R.string.time_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.latitude_header) + "," +
                        MyApplication.getContext().getResources().getString(R.string.longitude_header) + "," +
                        Data.getLabel(Data.DATA_ALTITUDE_DEVICE) + "," +
                        Data.getLabel(Data.DATA_SPEED_DEVICE) + "," +
                        Data.getLabel(Data.DATA_GEAR) + "," +
                        Data.getLabel(Data.DATA_ENGINE_TEMP) + "," +
                        Data.getLabel(Data.DATA_AIR_TEMP) + "," +
                        Data.getLabel(Data.DATA_FRONT_RDC) + "," +
                        Data.getLabel(Data.DATA_REAR_RDC) + "," +
                        Data.getLabel(Data.DATA_ODOMETER) + "," +
                        Data.getLabel(Data.DATA_VOLTAGE) + "," +
                        Data.getLabel(Data.DATA_THROTTLE) + "," +
                        Data.getLabel(Data.DATA_FRONT_BRAKE) + "," +
                        Data.getLabel(Data.DATA_REAR_BRAKE) + "," +
                        Data.getLabel(Data.DATA_SHIFTS) + "," +
                        MyApplication.getContext().getResources().getString(R.string.vin_header) + "," +
                        Data.getLabel(Data.DATA_AMBIENT_LIGHT) + "," +
                        Data.getLabel(Data.DATA_TRIP_ONE) + "," +
                        Data.getLabel(Data.DATA_TRIP_TWO) + "," +
                        Data.getLabel(Data.DATA_TRIP_AUTO) + "," +
                        Data.getLabel(Data.DATA_SPEED) + "," +
                        Data.getLabel(Data.DATA_AVG_SPEED) + "," +
                        Data.getLabel(Data.DATA_CURRENT_CONSUMPTION) + "," +
                        Data.getLabel(Data.DATA_ECONOMY_ONE) + "," +
                        Data.getLabel(Data.DATA_ECONOMY_TWO) + "," +
                        Data.getLabel(Data.DATA_RANGE) + "," +
                        Data.getLabel(Data.DATA_LEAN_DEVICE) + "," +
                        Data.getLabel(Data.DATA_GFORCE_DEVICE) + "," +
                        Data.getLabel(Data.DATA_BEARING_DEVICE) + "," +
                        Data.getLabel(Data.DATA_BAROMETRIC_DEVICE) + "," +
                        Data.getLabel(Data.DATA_RPM) + "," +
                        Data.getLabel(Data.DATA_LEAN) + "," +
                        Data.getLabel(Data.DATA_REAR_SPEED) + "," +
                        Data.getLabel(Data.DATA_CELL_SIGNAL) + "," +
                        Data.getLabel(Data.DATA_BATTERY_DEVICE) +
                        "\n";

                File logFile = new File( root, filename + curdatetime + ".csv" );
                FileWriter logWriter = new FileWriter( logFile );
                outFile = new PrintWriter( logWriter );
                outFile.write(header);
                outFile.flush();
            }
        } catch (IOException e) {
            Log.d(TAG, "Could not write to file: " + e.getMessage());
            ((MyApplication) this.getApplication()).setTripRecording(false);
        }
    }
}
