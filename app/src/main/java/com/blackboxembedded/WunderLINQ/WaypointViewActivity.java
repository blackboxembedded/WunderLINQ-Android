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

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.WayPoint;

public class WaypointViewActivity extends AppCompatActivity implements OnMapReadyCallback  {

    public final static String TAG = "WptViewActivity";

    private PopupMenu mPopupMenu;

    private EditText etLabel;

    private String recordID;
    private WaypointDatasource datasource;
    private List<WaypointRecord> allWaypoints;
    private WaypointRecord record;
    private int index;

    private Double lat;
    private Double lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_waypoint_view);

        showActionBar();
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvLatitude = findViewById(R.id.tvLatitude);
        TextView tvLongitude = findViewById(R.id.tvLongitude);
        etLabel = findViewById(R.id.tvLabel);
        etLabel.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    // Open database
                    WaypointDatasource datasource = new WaypointDatasource(WaypointViewActivity.this);
                    datasource.open();
                    datasource.addLabel(record.getID(), etLabel.getText().toString());
                    record = datasource.returnRecord(recordID);
                    datasource.close();
                    return true;
                }
                return false;
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            recordID = extras.getString("RECORD_ID");

            // Open database
            datasource = new WaypointDatasource(this);
            datasource.open();
            record = datasource.returnRecord(recordID);
            allWaypoints = datasource.getAllRecords();
            datasource.close();
            index = allWaypoints.indexOf(record);

            View view = findViewById(R.id.layout_waypoint_view);
            view.setOnTouchListener(new OnSwipeTouchListener(this) {
                @Override
                public void onSwipeLeft() {
                    if (index != (allWaypoints.size() - 1)) {
                        Intent waypointViewIntent = new Intent(MyApplication.getContext(), WaypointViewActivity.class);
                        WaypointRecord previousRecord = allWaypoints.get(index + 1);
                        String recordID = Long.toString(previousRecord.getID());
                        waypointViewIntent.putExtra("RECORD_ID", recordID);
                        startActivity(waypointViewIntent);
                    }
                }
                @Override
                public void onSwipeRight() {
                    if (index > 0) {
                        Intent waypointViewIntent = new Intent(MyApplication.getContext(), WaypointViewActivity.class);
                        WaypointRecord previousRecord = allWaypoints.get(index - 1);
                        String recordID = Long.toString(previousRecord.getID());
                        waypointViewIntent.putExtra("RECORD_ID", recordID);
                        startActivity(waypointViewIntent);
                    }
                }
            });

            tvDate.setText(record.getDate());
            String[] latlong = record.getData().split(",");
            lat = Double.parseDouble(latlong[0]);
            lon = Double.parseDouble(latlong[1]);
            tvLatitude.setText(latlong[0]);
            tvLongitude.setText(latlong[1]);
            etLabel.setText(record.getLabel());

            FragmentManager myFragmentManager = getSupportFragmentManager();
            SupportMapFragment mapFragment = (SupportMapFragment) myFragmentManager.findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
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
    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.setTrafficEnabled(false);
        map.setIndoorEnabled(false);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);
        // Add a marker and move the camera
        LatLng location = new LatLng(lat, lon);
        map.addMarker(new MarkerOptions().position(location).title(getString(R.string.waypoint_view_waypoint_label)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location,15));
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
        navbarTitle.setText(R.string.waypoint_view_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton menuButton = findViewById(R.id.action_menu);
        backButton.setOnClickListener(mClickListener);
        menuButton.setOnClickListener(mClickListener);

        mPopupMenu = new PopupMenu(this, menuButton);
        MenuInflater menuOtherInflater = mPopupMenu.getMenuInflater();
        menuOtherInflater.inflate(R.menu.menu_waypointviewer, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.action_open:
                        open();
                        break;
                    case R.id.action_navigate:
                        navigate();
                        break;
                    case R.id.action_share_original:
                        share("\"text/plain\"", Uri.parse("http://maps.google.com/maps?saddr=" + lat + "," + lon), false);
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

    // Delete button press
    public void delete() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_waypoint_alert_title));
        builder.setMessage(getString(R.string.delete_waypoint_alert_body));
        builder.setPositiveButton(R.string.delete_bt,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        datasource.removeRecord(record);
                        Intent intent = new Intent(WaypointViewActivity.this, WaypointActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                });
        builder.setNegativeButton(R.string.cancel_bt,null);
        builder.show();
    }

    // Open In Map app
    public void open() {
        //Open waypoint in map app
        String[] latlon = record.getData().split(",");
        LatLng location = new LatLng(Double.parseDouble(latlon[0]), Double.parseDouble(latlon[1]));
        Location destination = new Location(LocationManager.GPS_PROVIDER);
        destination.setLatitude(location.latitude);
        destination.setLongitude(location.longitude);

        NavAppHelper.viewWaypoint(WaypointViewActivity.this, destination, record.getLabel());
    }

    // Navigate
    public void navigate() {
        //Navigation
        // Check Location permissions
        if (getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(WaypointViewActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
        } else {
            // Get the location manager
            LocationManager locationManager = (LocationManager)
                    WaypointViewActivity.this.getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            try {
                // Get current location
                String bestProvider = locationManager.getBestProvider(criteria, false);
                Location currentLocation = locationManager.getLastKnownLocation(bestProvider);

                String[] latlon = record.getData().split(",");
                LatLng location = new LatLng(Double.parseDouble(latlon[0]), Double.parseDouble(latlon[1]));
                Location destination = new Location(LocationManager.GPS_PROVIDER);
                destination.setLatitude(location.latitude);
                destination.setLongitude(location.longitude);

                NavAppHelper.navigateTo(WaypointViewActivity.this, currentLocation, destination);
            } catch (SecurityException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    // Export button press
    public void share(String type, Uri uri, boolean stream) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType(type);
        String ShareSub = getString(R.string.waypoint_view_waypoint_label);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ShareSub);
        if (stream){
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        } else {
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, uri.toString());
        }

        startActivity(Intent.createChooser(sharingIntent, getString(R.string.waypoint_view_share_label)));
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
                String label = getString(R.string.waypoint_view_waypoint_label);
                if (!record.getLabel().equals("")){
                    label = record.getLabel();
                }
                File gpxFile = new File( root, label + ".gpx" );
                if (gpxFile.exists())
                    gpxFile.delete();
                while(!gpxFile.exists())
                    gpxFile.createNewFile();
                FileOutputStream gpxOutStream = new FileOutputStream(gpxFile);
                Calendar cal = Calendar.getInstance();
                Date date = cal.getTime();
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ").parse(record.getDate());
                } catch (ParseException e){
                    Log.d(TAG, "Exception parsing a date: " + e.toString());
                }

                final GPX gpx = GPX.builder()
                        .creator(getString(R.string.app_name)).addWayPoint(WayPoint.builder().lat(lat).lon(lon).time(date.getTime()).cmt(label).build())
                        .build();
                GPX.write(gpx, gpxOutStream);
                Uri uri = FileProvider.getUriForFile(this, "com.blackboxembedded.wunderlinq.fileprovider", gpxFile);
                share("application/gpx+xml", uri, true);
            }

        } catch (IOException e){
            Log.d(TAG,"Exception creating GPX: " + e.toString());
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(WaypointViewActivity.this, WaypointActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_menu:
                    mPopupMenu.show();
                    break;
            }
        }
    };
}
