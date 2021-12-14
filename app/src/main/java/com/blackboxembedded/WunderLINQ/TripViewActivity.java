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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;

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
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.MapsInitializer.Renderer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;

public class TripViewActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapsSdkInitializedCallback {

    private static final String TAG = "TripViewActivity";

    private PopupMenu mPopupMenu;

    private List<LatLng> routePoints;
    private ArrayList tripFileList = new ArrayList<String>();
    private String fileName;
    private File file;
    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_trip_view);

        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvDistance = findViewById(R.id.tvDistance);
        TextView tvDuration = findViewById(R.id.tvDuration);
        TextView tvSpeed = findViewById(R.id.tvSpeed);
        TextView tvGearShifts = findViewById(R.id.tvGearShifts);
        TextView tvBrakes = findViewById(R.id.tvBrakes);
        TextView tvAmbient = findViewById(R.id.tvAmbient);
        TextView tvEngine = findViewById(R.id.tvEngine);

        showActionBar();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            updateListing();
            fileName = extras.getString("FILE");
            index = tripFileList.indexOf(fileName);
            file = new File(this.getExternalFilesDir(null), "/logs/" + fileName);

            View view = findViewById(R.id.layout_trip_view);
            view.setOnTouchListener(new OnSwipeTouchListener(this) {
                @Override
                public void onSwipeLeft() {
                    if (index != (tripFileList.size() - 1)) {
                        Intent tripViewIntent = new Intent(TripViewActivity.this, TripViewActivity.class);
                        tripViewIntent.putExtra("FILE", tripFileList.get(index + 1).toString());
                        startActivity(tripViewIntent);
                    }
                }
                @Override
                public void onSwipeRight() {
                    if (index > 0) {
                        Intent tripViewIntent = new Intent(TripViewActivity.this, TripViewActivity.class);
                        tripViewIntent.putExtra("FILE", tripFileList.get(index - 1).toString());
                        startActivity(tripViewIntent);
                    }
                }
            });

            routePoints = new ArrayList<>();
            List<Double> speeds = new ArrayList<>();
            Double maxSpeed = null;
            List<Double> ambientTemps = new ArrayList<>();
            Double minAmbientTemp = null;
            Double maxAmbientTemp = null;
            List<Double> engineTemps = new ArrayList<>();
            Double minEngineTemp = null;
            Double maxEngineTemp = null;
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
            Date startTime = null;
            Date endTime = null;
            Double startOdometer = null;
            Double endOdometer = null;
            Integer endShiftCnt = null;
            Integer endFrontBrakeCnt = null;
            Integer endRearBrakeCnt = null;

            String distanceUnit = "km";
            String temperatureUnit = "C";
            String speedUnit = "kmh";

            try {
                CSVReader reader = new CSVReader(new FileReader(file));
                List<String[]> myEntries = reader.readAll();
                int lineNumber = 0;
                for(String[] nextLine : myEntries) {
                    if (lineNumber == 0){
                        distanceUnit = nextLine[10].substring(nextLine[10].indexOf("(") + 1, nextLine[10].indexOf(")"));
                        temperatureUnit = nextLine[6].substring(nextLine[6].indexOf("(") + 1, nextLine[6].indexOf(")"));
                        speedUnit = nextLine[4].substring(nextLine[4].indexOf("(") + 1, nextLine[4].indexOf(")"));
                    } else {
                        try {
                            if (lineNumber == 1) {
                                startTime = df.parse(nextLine[0]);
                            } else {
                                endTime = df.parse(nextLine[0]);
                            }
                        } catch (ParseException e){
                            e.printStackTrace();
                        }
                        if((lineNumber > 1) && (!nextLine[1].equals("No Fix") && (!nextLine[2].equals("No Fix")))) {
                            LatLng location = new LatLng(Double.parseDouble(nextLine[1]), Double.parseDouble(nextLine[2]));
                            routePoints.add(location);
                            speeds.add(Double.parseDouble(nextLine[4]));
                            if (maxSpeed == null || maxSpeed < Double.parseDouble(nextLine[4])){
                                maxSpeed = Double.parseDouble(nextLine[4]);
                            }
                        }
                        if (lineNumber > 1) {
                            if (!nextLine[6].equals("null")){
                                engineTemps.add(Double.parseDouble(nextLine[6]));
                                if (maxEngineTemp == null || maxEngineTemp < Double.parseDouble(nextLine[6])){
                                    maxEngineTemp = Double.parseDouble(nextLine[6]);
                                }
                                if (minEngineTemp == null || minEngineTemp > Double.parseDouble(nextLine[6])){
                                    minEngineTemp = Double.parseDouble(nextLine[6]);
                                }
                            }
                            if (!nextLine[7].equals("null")){
                                ambientTemps.add(Double.parseDouble(nextLine[7]));
                                if (maxAmbientTemp == null || maxAmbientTemp < Double.parseDouble(nextLine[7])){
                                    maxAmbientTemp = Double.parseDouble(nextLine[7]);
                                }
                                if (minAmbientTemp == null || minAmbientTemp > Double.parseDouble(nextLine[7])){
                                    minAmbientTemp = Double.parseDouble(nextLine[7]);
                                }
                            }
                            if (!nextLine[10].equals("null")){
                                if (endOdometer == null || endOdometer < Double.parseDouble(nextLine[10])){
                                    endOdometer = Double.parseDouble(nextLine[10]);
                                }
                                if (startOdometer == null || startOdometer > Double.parseDouble(nextLine[10])){
                                    startOdometer = Double.parseDouble(nextLine[10]);
                                }
                            }
                            if (!nextLine[13].equals("null")){
                                if (endFrontBrakeCnt == null || endFrontBrakeCnt < Double.parseDouble(nextLine[13])){
                                    endFrontBrakeCnt = Integer.parseInt(nextLine[13]);
                                }
                            }
                            if (!nextLine[14].equals("null")){
                                if (endRearBrakeCnt == null || endRearBrakeCnt < Double.parseDouble(nextLine[14])){
                                    endRearBrakeCnt = Integer.parseInt(nextLine[14]);
                                }
                            }
                            if (!nextLine[15].equals("null")){
                                if (endShiftCnt == null || endShiftCnt < Double.parseDouble(nextLine[15])){
                                    endShiftCnt = Integer.parseInt(nextLine[15]);
                                }
                            }
                        }
                        tvDate.setText(nextLine[0]);
                    }
                    lineNumber = lineNumber + 1;
                }

                if (speeds.size() > 0){
                    double avgSpeed = 0.0;
                    for (double speed : speeds) {
                        avgSpeed = avgSpeed + speed;
                    }
                    avgSpeed = avgSpeed / speeds.size();
                    tvSpeed.setText(Utils.oneDigit.format(avgSpeed) + "/" + Utils.oneDigit.format(maxSpeed) + " (" + speedUnit + ")");
                }

                if(endShiftCnt != null){
                    tvGearShifts.setText(Integer.toString(endShiftCnt));
                }

                String frontBrakeText = "0";
                String rearBrakeText = "0";
                if(endFrontBrakeCnt != null){
                    frontBrakeText = Integer.toString(endFrontBrakeCnt);
                }
                if(endRearBrakeCnt != null){
                    rearBrakeText = Integer.toString(endRearBrakeCnt);
                }
                tvBrakes.setText(frontBrakeText + "/" + rearBrakeText);

                double avgEngineTemp = 0.0;
                if (engineTemps.size() > 0) {
                    for (double engineTemp : engineTemps) {
                        avgEngineTemp = avgEngineTemp + engineTemp;
                    }
                    avgEngineTemp = avgEngineTemp / ambientTemps.size();
                }
                if(minEngineTemp == null || maxEngineTemp == null){
                    minEngineTemp = 0.0;
                    maxEngineTemp = 0.0;
                }
                tvEngine.setText(Utils.oneDigit.format(minEngineTemp) + "/" + Utils.oneDigit.format(avgEngineTemp) + "/" + Utils.oneDigit.format(maxEngineTemp) + " (" + temperatureUnit + ")");

                double avgAmbientTemp = 0.0;
                if (ambientTemps.size() > 0) {
                    for (double ambientTemp : ambientTemps) {
                        avgAmbientTemp = avgAmbientTemp + ambientTemp;
                    }
                    avgAmbientTemp = avgAmbientTemp / ambientTemps.size();
                }
                if(minAmbientTemp == null || maxAmbientTemp == null){
                    minAmbientTemp = 0.0;
                    maxAmbientTemp = 0.0;
                }
                tvAmbient.setText(Utils.oneDigit.format(minAmbientTemp) + "/" + Utils.oneDigit.format(avgAmbientTemp) + "/" + Utils.oneDigit.format(maxAmbientTemp) + " (" + temperatureUnit + ")");

                // Calculate Distance
                double distance = 0;
                if (endOdometer != null && startOdometer != null) {
                    distance = endOdometer - startOdometer;
                }
                tvDistance.setText(Utils.oneDigit.format(distance) + " " + distanceUnit);

                // Calculate Duration
                if (startTime != null && endTime != null) {
                    long[] duration = Utils.calculateDuration(startTime, endTime);
                    tvDuration.setText( String.valueOf(duration[2]) + " " + getString(R.string.hours) + ", " + String.valueOf(duration[1]) + " " + getString(R.string.minutes) + ", " + String.valueOf(duration[0]) + " " + getString(R.string.seconds));
                }

            } catch (IOException e){
                Log.d(TAG,"Exception reading CSV: " + e.toString());
            }

            if (routePoints.size() > 0) {
                MapsInitializer.initialize(getApplicationContext(), Renderer.LATEST, this);
                FragmentManager myFragmentManager = getSupportFragmentManager();
                SupportMapFragment mapFragment = (SupportMapFragment) myFragmentManager.findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
            }
        }

    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onMapsSdkInitialized(MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d(TAG, "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d(TAG, "The legacy version of the renderer is used.");
                break;
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
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                map.animateCamera(cu);
            }
        });
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav_menu, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle;
        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.trip_view_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton menuButton = findViewById(R.id.action_menu);
        backButton.setOnClickListener(mClickListener);
        menuButton.setOnClickListener(mClickListener);

        mPopupMenu = new PopupMenu(this, menuButton);
        MenuInflater menuOtherInflater = mPopupMenu.getMenuInflater();
        menuOtherInflater.inflate(R.menu.menu_tripviewer, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.action_share_original:
                        Uri uri = FileProvider.getUriForFile(TripViewActivity.this, "com.blackboxembedded.wunderlinq.fileprovider", file);
                        share("text/csv", uri);
                        break;
                    case R.id.action_share_gpx:
                        exportGPX();
                        break;
                    case R.id.action_delete:
                        delete();
                        break;
                }
                return true;
            }
        });
    }

    // Delete file
    public void delete() {
        // Display dialog text here......
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_trip_alert_title));
        builder.setMessage(getString(R.string.delete_trip_alert_body));
        builder.setPositiveButton(R.string.delete_bt,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        file.delete();
                        Intent backIntent = new Intent(TripViewActivity.this, TripsActivity.class);
                        startActivity(backIntent);
                    }
                });
        builder.setNegativeButton(R.string.cancel_bt,null);
        builder.show();
    }

    // Share file
    public void share(String type, Uri uri) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType(type);
        String ShareSub = getString(R.string.trip_view_trip_label);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ShareSub);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.trip_view_share_label)));
    }

    // Export GPX button press
    public void exportGPX() {
        try {
            File root = new File(MyApplication.getContext().getCacheDir(), "/tmp/");
            if(!root.exists()){
                if(!root.mkdirs()){
                    Log.d(TAG,"Unable to create directory: " + root);
                }
            }
            if(root.canWrite()){
                File gpxFile = new File( root, fileName + ".gpx" );
                if (gpxFile.exists())
                    gpxFile.delete();
                while(!gpxFile.exists())
                    gpxFile.createNewFile();
                FileOutputStream gpxOutStream = new FileOutputStream(gpxFile);

                CSVReader reader = new CSVReader(new FileReader(file));
                TrackSegment.Builder segment = TrackSegment.builder();

                List<String[]> myEntries = reader.readAll();
                int lineNumber = 0;
                for (String[] nextLine : myEntries) {
                    if ((lineNumber > 1) && (!nextLine[1].equals("No Fix") && (!nextLine[2].equals("No Fix")))) {
                        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ").parse(nextLine[0]);
                        double lat = Double.parseDouble(nextLine[1]);
                        double lon = Double.parseDouble(nextLine[1]);
                        double elevation = Double.parseDouble(nextLine[3]);
                        double speed = Double.parseDouble(nextLine[4]);
                        final WayPoint point = WayPoint.builder()
                                .lat(lat).lon(lon).time(date.getTime()).ele(elevation).speed(speed)
                                .build();
                        segment.addPoint(point);
                    }
                    lineNumber = lineNumber +1;
                }
                final GPX gpx = GPX.builder().creator(getString(R.string.app_name)).addTrack(track -> track.addSegment(segment.build())).build();
                GPX.write(gpx, gpxOutStream);
                Uri uri = FileProvider.getUriForFile(this, "com.blackboxembedded.wunderlinq.fileprovider", gpxFile);

                share("application/gpx+xml", uri);
            }

        } catch (IOException | ParseException e){
            Log.d(TAG,"Exception creating GPX: " + e.toString());
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(TripViewActivity.this, TripsActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_menu:
                    mPopupMenu.show();
                    break;
            }
        }
    };

    private void updateListing(){
        File root = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/logs/");
        if(!root.exists()){
            if(!root.mkdirs()){
                Log.d(TAG,"Unable to create directory: " + root);
            }
        }
        File[] list = root.listFiles();
        if (list != null ) {
            Arrays.sort(list, Collections.reverseOrder());
            for (File value : list) {
                tripFileList.add(value.getName());
            }
        }
    }
}
