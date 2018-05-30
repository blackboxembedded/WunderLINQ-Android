package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.siegmar.fastcsv.reader.CsvContainer;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;

public class TripViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "WunderLINQ";

    private ImageButton backButton;
    private ImageButton forwardButton;

    private TextView tvDate;
    private TextView tvDistance;
    private TextView tvDuration;
    private TextView tvSpeed;
    private TextView tvGearShifts;
    private TextView tvBrakes;
    private TextView tvAmbient;
    private TextView tvEngine;

    private SharedPreferences sharedPrefs;

    List<LatLng> routePoints;

    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_view);

        tvDate = findViewById(R.id.tvDate);
        tvDistance = findViewById(R.id.tvDistance);
        tvDuration = findViewById(R.id.tvDuration);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvGearShifts = findViewById(R.id.tvGearShifts);
        tvBrakes = findViewById(R.id.tvBrakes);
        tvAmbient = findViewById(R.id.tvAmbient);
        tvEngine = findViewById(R.id.tvEngine);

        showActionBar();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String fileName = extras.getString("FILE");
            Log.d(TAG,fileName);

            file = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/logs/" + fileName);
            CsvReader csvReader = new CsvReader();

            routePoints = new ArrayList<LatLng>();
            List<Double> speeds = new ArrayList<>();
            Double minSpeed = null;
            Double maxSpeed = null;
            List<Double> ambientTemps = new ArrayList<>();
            Double minAmbientTemp = null;
            Double maxAmbientTemp = null;
            List<Double> engineTemps = new ArrayList<>();
            Double minEngineTemp = null;
            Double maxEngineTemp = null;
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date startTime = null;
            Date endTime = null;
            Double startOdometer = null;
            Double endOdometer = null;
            Double endShiftCnt = null;
            Double endFrontBrakeCnt = null;
            Double endRearBrakeCnt = null;

            try {
                CsvContainer csv = csvReader.read(file, StandardCharsets.UTF_8);
                for (CsvRow row : csv.getRows()) {
                    //Log.d(TAG,"Read line: " + row);
                    //Log.d(TAG,"First column of line: " + row.getField(0));
                    try {
                        if (row.getOriginalLineNumber() == 2) {
                            startTime = df.parse(row.getField(0));
                        } else {
                            endTime = df.parse(row.getField(0));
                        }
                    } catch (ParseException e){

                    }
                    if((row.getOriginalLineNumber() > 1) && (!row.getField(1).equals("No Fix") && (!row.getField(2).equals("No Fix")))) {
                        LatLng location = new LatLng(Double.parseDouble(row.getField(1)), Double.parseDouble(row.getField(2)));
                        routePoints.add(location);
                        speeds.add(Double.parseDouble(row.getField(4)));
                        if (maxSpeed == null || maxSpeed < Double.parseDouble(row.getField(4))){
                            maxSpeed = Double.parseDouble(row.getField(4));
                        }
                        if (minSpeed == null || minSpeed > Double.parseDouble(row.getField(4))){
                            minSpeed = Double.parseDouble(row.getField(4));
                        }
                    }
                    if (row.getOriginalLineNumber() > 1) {
                        if (!row.getField(6).equals("null")){
                            engineTemps.add(Double.parseDouble(row.getField(6)));
                            if (maxEngineTemp == null || maxEngineTemp < Double.parseDouble(row.getField(6))){
                                maxEngineTemp = Double.parseDouble(row.getField(6));
                            }
                            if (minEngineTemp == null || minEngineTemp > Double.parseDouble(row.getField(6))){
                                minEngineTemp = Double.parseDouble(row.getField(6));
                            }
                        }
                        if (!row.getField(7).equals("null")){
                            ambientTemps.add(Double.parseDouble(row.getField(7)));
                            if (maxAmbientTemp == null || maxAmbientTemp < Double.parseDouble(row.getField(7))){
                                maxAmbientTemp = Double.parseDouble(row.getField(7));
                            }
                            if (minAmbientTemp == null || minAmbientTemp > Double.parseDouble(row.getField(7))){
                                minAmbientTemp = Double.parseDouble(row.getField(7));
                            }
                        }
                        if (!row.getField(10).equals("null")){
                            if (endOdometer == null || endOdometer < Double.parseDouble(row.getField(10))){
                                endOdometer = Double.parseDouble(row.getField(10));
                            }
                            if (startOdometer == null || startOdometer > Double.parseDouble(row.getField(10))){
                                startOdometer = Double.parseDouble(row.getField(10));
                            }
                        }
                        if (!row.getField(13).equals("null")){
                            if (endFrontBrakeCnt == null || endFrontBrakeCnt < Double.parseDouble(row.getField(13))){
                                endFrontBrakeCnt = Double.parseDouble(row.getField(13));
                            }
                        }
                        if (!row.getField(14).equals("null")){
                            if (endRearBrakeCnt == null || endRearBrakeCnt < Double.parseDouble(row.getField(14))){
                                endRearBrakeCnt = Double.parseDouble(row.getField(14));
                            }
                        }
                        if (!row.getField(15).equals("null")){
                            if (endShiftCnt == null || endShiftCnt < Double.parseDouble(row.getField(15))){
                                endShiftCnt = Double.parseDouble(row.getField(15));
                            }
                        }
                    }
                    if(row.getOriginalLineNumber() == 2){
                        tvDate.setText(row.getField(0));
                    }
                }

                String distanceUnit = "km";
                String speedUnit = "km/h";
                String distanceFormat = sharedPrefs.getString("prefDistance", "0");
                if (distanceFormat.contains("1")) {
                    distanceUnit = "mi";
                    speedUnit = "mi/h";
                }
                String temperatureUnit = "C";
                String temperatureFormat = sharedPrefs.getString("prefTempF", "0");
                if (temperatureFormat.contains("1")) {
                    // F
                    temperatureUnit = "F";
                }

                if (speeds.size() > 0){
                    Double avgSpeed = 0.0;
                    for (Double speed : speeds) {
                        avgSpeed = avgSpeed + speed;
                    }
                    avgSpeed = avgSpeed / speeds.size();
                    if (distanceFormat.contains("1")) {
                        minSpeed = kmToMiles(minSpeed);
                        avgSpeed = kmToMiles(avgSpeed);
                        maxSpeed = kmToMiles(maxSpeed);
                    }
                    tvSpeed.setText("(" + oneDigit.format(minSpeed) + "/" + oneDigit.format(avgSpeed) + "/" + oneDigit.format(maxSpeed) + ")" + speedUnit);
                }

                if(endShiftCnt != null){
                    tvGearShifts.setText(Double.toString(endShiftCnt));
                }

                String frontBrakeText = "0";
                String rearBrakeText = "0";
                if(endFrontBrakeCnt != null){
                    frontBrakeText = Double.toString(endFrontBrakeCnt);
                }
                if(endRearBrakeCnt != null){
                    rearBrakeText = Double.toString(endRearBrakeCnt);
                }
                tvBrakes.setText("(" + frontBrakeText + "/" + rearBrakeText + ")");

                Double avgEngineTemp = 0.0;
                if (engineTemps.size() > 0) {
                    for (Double engineTemp : engineTemps) {
                        avgEngineTemp = avgEngineTemp + engineTemp;
                    }
                    avgEngineTemp = avgEngineTemp / ambientTemps.size();
                    if (temperatureFormat.contains("1")) {
                        // F
                        minEngineTemp = celsiusToFahrenheit(minEngineTemp);
                        avgEngineTemp = celsiusToFahrenheit(avgEngineTemp);
                        maxEngineTemp = celsiusToFahrenheit(maxEngineTemp);
                    }
                }
                if(minEngineTemp == null || maxEngineTemp == null){
                    minEngineTemp = 0.0;
                    maxEngineTemp = 0.0;
                }
                tvEngine.setText("(" + oneDigit.format(minEngineTemp) + "/" + oneDigit.format(avgEngineTemp) + "/" + oneDigit.format(maxEngineTemp) + ")" + temperatureUnit);

                Double avgAmbientTemp = 0.0;
                if (ambientTemps.size() > 0) {
                    for (Double ambientTemp : ambientTemps) {
                        avgAmbientTemp = avgAmbientTemp + ambientTemp;
                    }
                    avgAmbientTemp = avgAmbientTemp / ambientTemps.size();
                    if (temperatureFormat.contains("1")) {
                        // F
                        minAmbientTemp = celsiusToFahrenheit(minAmbientTemp);
                        avgAmbientTemp = celsiusToFahrenheit(avgAmbientTemp);
                        maxAmbientTemp = celsiusToFahrenheit(maxAmbientTemp);
                    }
                }
                if(minAmbientTemp == null || maxAmbientTemp == null){
                    minAmbientTemp = 0.0;
                    maxAmbientTemp = 0.0;
                }
                tvAmbient.setText("(" + oneDigit.format(minAmbientTemp) + "/" + oneDigit.format(avgAmbientTemp) + "/" + oneDigit.format(maxAmbientTemp) + ")" + temperatureUnit);

                // Calculate Distance
                double distance = 0;
                if (endOdometer != null && startOdometer != null) {
                    distance = endOdometer - startOdometer;
                    if (distanceFormat.contains("1")) {
                        distance = kmToMiles(distance);
                    }
                }
                tvDistance.setText(oneDigit.format(distance) + distanceUnit);

                // Calculate Duration
                printDifference(startTime,endTime);

            } catch (IOException e){

            }

            FragmentManager myFragmentManager = getSupportFragmentManager();
            SupportMapFragment mapFragment = (SupportMapFragment) myFragmentManager.findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

        }

    }

    @Override
    public void onMapReady(final GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.setTrafficEnabled(false);
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);
        LatLng startLocation = routePoints.get(0);
        LatLng endLocation = routePoints.get(routePoints.size() - 1);
        map.addMarker(new MarkerOptions().position(startLocation)
                .title(getString(R.string.trip_view_waypoint_start_label))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        map.addMarker(new MarkerOptions().position(endLocation)
                .title(getString(R.string.trip_view_waypoint_end_label))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        map.addPolyline(new PolylineOptions()
                .width(10)
                .color(Color.RED)
                .geodesic(true)
                .zIndex(1)
                .addAll(routePoints));

        // Move Camera
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : routePoints) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int padding = 100; // offset from edges of the map in pixels
        final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        //map.moveCamera(cu);
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                map.animateCamera(cu);
            }
        });
    }

    // Delete button press
    public void onClickDelete(View view) {
        file.delete();
        Intent backIntent = new Intent(TripViewActivity.this, TripsActivity.class);
        startActivity(backIntent);
    }

    // Export button press
    public void onClickShare(View view) {
        Uri uri = FileProvider.getUriForFile(this, "com.blackboxembedded.wunderlinq.fileprovider", file);
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/csv");
        String ShareSub = getString(R.string.trip_view_trip_label);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ShareSub);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.trip_view_share_label)));
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle;
        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.trip_view_title);

        backButton = (ImageButton) findViewById(R.id.action_back);
        forwardButton = (ImageButton) findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(TripViewActivity.this, TripsActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };
    public void printDifference(Date startDate, Date endDate){

        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        //long elapsedDays = different / daysInMilli;
        //different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;
        tvDuration.setText(elapsedHours + " hours, " + elapsedMinutes + " minutes, " + elapsedSeconds + " seconds");
    }
    // kilometers to miles
    public double kmToMiles(double kilometers){
        return kilometers * 0.6214;
    }
    // Celsius to Fahrenheit
    public double celsiusToFahrenheit(double celsius){
        return (celsius * 1.8) + 32.0;
    }
    //format to 1 decimal place
    DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
}
