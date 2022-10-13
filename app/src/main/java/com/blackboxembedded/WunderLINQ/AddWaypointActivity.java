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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.blackboxembedded.WunderLINQ.Utils.Utils;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.jenetics.jpx.GPX;

public class AddWaypointActivity extends AppCompatActivity implements OnMapReadyCallback {

    public final static String TAG = "AddWaypointActivity";

    private GoogleMap googleMap;
    private EditText etSearch;
    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_waypoint);
        showActionBar();

        etSearch = findViewById(R.id.etSearch);
        Button btSearch = findViewById(R.id.btSearch);
        btSearch.setOnClickListener(mClickListener);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        etLabel = findViewById(R.id.etLabel);
        Button btSave = findViewById(R.id.btSave);
        btSave.setOnClickListener(mClickListener);

        etLatitude.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    LatLng latLong = new LatLng(Double.parseDouble(etLatitude.getText().toString()), Double.parseDouble(etLongitude.getText().toString()));
                    if(etLatitude.getText().toString().matches(Utils.LATITUDE_PATTERN) && etLongitude.getText().toString().matches(Utils.LONGITUDE_PATTERN)){
                        googleMap.clear();
                        googleMap.addMarker(new MarkerOptions().position(latLong));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong,15));
                    }
                    return true;
                }
                return false;
            }
        });
        etLongitude.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    LatLng latLong = new LatLng(Double.parseDouble(etLatitude.getText().toString()), Double.parseDouble(etLongitude.getText().toString()));
                    if(etLatitude.getText().toString().matches(Utils.LATITUDE_PATTERN) && etLongitude.getText().toString().matches(Utils.LONGITUDE_PATTERN)){
                        googleMap.clear();
                        googleMap.addMarker(new MarkerOptions().position(latLong));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong,15));
                    }
                    return true;
                }
                return false;
            }
        });

        FragmentManager myFragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) myFragmentManager.findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (Data.getLastLocation() != null) {
            etLatitude.setText(String.valueOf(Data.getLastLocation().getLatitude()));
            etLongitude.setText(String.valueOf(Data.getLastLocation().getLongitude()));
        }

        Intent intent = getIntent();
        if (intent != null){
            final Uri uri = intent.getData();
            if (uri != null){
                String scheme = uri.getScheme();
                if (ContentResolver.SCHEME_FILE.equals(scheme)){
                    final File file = new File(uri.getPath());
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.gpx_import_alert_title))
                            .setMessage(getString(R.string.gpx_import_alert_body))
                            .setPositiveButton(getString(R.string.alert_btn_ok), new DialogInterface.OnClickListener()
                            {
                                @SuppressLint("NewApi")
                                @Override
                                public void onClick(DialogInterface dialog, int which){
                                    InputStream is;

                                    try {
                                        is = new FileInputStream(new File(file.getAbsolutePath()));
                                        // Open database
                                        WaypointDatasource datasource = new WaypointDatasource(AddWaypointActivity.this);
                                        datasource.open();
                                        GPX.read(is).getWayPoints().forEach(e -> {
                                            String waypoint = e.getLatitude().toString() + "," + e.getLongitude().toString();
                                            String label = "";
                                            if (e.getComment().isPresent()){
                                                label = e.getComment().get();
                                            }

                                            // Get current date/time
                                            Calendar cal = Calendar.getInstance();
                                            Date date = cal.getTime();
                                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
                                            String time = formatter.format(date);
                                            if (e.getTime().isPresent()){
                                                date = Date.from(e.getTime().get().toInstant());
                                                time = formatter.format(date);
                                            }
                                            WaypointRecord record = new WaypointRecord(time, waypoint, label);
                                            datasource.addRecord(record);

                                            LatLng latLong = new LatLng(e.getLatitude().doubleValue(), e.getLongitude().doubleValue());
                                            etLatitude.setText(String.valueOf(e.getLatitude().doubleValue()));
                                            etLongitude.setText(String.valueOf(e.getLongitude().doubleValue()));
                                            etLabel.setText(label);
                                            googleMap.clear();
                                            googleMap.addMarker(new MarkerOptions().position(latLong));
                                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong,15));
                                        });
                                        datasource.close();
                                    } catch (IOException e){
                                        Log.e(TAG, e.toString());
                                    }

                                }
                            }).setNegativeButton(getString(R.string.cancel), null).show();
                } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)){
                    final ContentResolver resolver = getContentResolver();

                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.gpx_import_alert_title))
                            .setMessage(getString(R.string.gpx_import_alert_body))
                            .setPositiveButton(getString(R.string.alert_btn_ok), new DialogInterface.OnClickListener()
                            {
                                @SuppressLint("NewApi")
                                @Override
                                public void onClick(DialogInterface dialog, int which){
                                    InputStream is;
                                    try {
                                        is = resolver.openInputStream(uri);
                                        // Open database
                                        WaypointDatasource datasource = new WaypointDatasource(AddWaypointActivity.this);
                                        datasource.open();
                                        GPX.read(is).getWayPoints().forEach(e -> {
                                            String waypoint = e.getLatitude().toString() + "," + e.getLongitude().toString();
                                            String label = "";
                                            if (e.getComment().isPresent()){
                                                label = e.getComment().get();
                                            }

                                            // Get current date/time
                                            Calendar cal = Calendar.getInstance();
                                            Date date = cal.getTime();
                                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
                                            String time = formatter.format(date);
                                            if (e.getTime().isPresent()){
                                                date = Date.from(e.getTime().get().toInstant());
                                                time = formatter.format(date);
                                            }
                                            WaypointRecord record = new WaypointRecord(time, waypoint, label);
                                            datasource.addRecord(record);

                                            LatLng latLong = new LatLng(e.getLatitude().doubleValue(), e.getLongitude().doubleValue());
                                            etLatitude.setText(String.valueOf(e.getLatitude().doubleValue()));
                                            etLongitude.setText(String.valueOf(e.getLongitude().doubleValue()));
                                            etLabel.setText(label);
                                            googleMap.clear();
                                            googleMap.addMarker(new MarkerOptions().position(latLong));
                                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong,15));
                                        });
                                        datasource.close();
                                    } catch (IOException e){
                                        Log.e(TAG, e.toString());
                                    }
                                }
                            }).setNegativeButton(getString(R.string.cancel), null).show();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.setTrafficEnabled(true);
        googleMap.setIndoorEnabled(false);
        googleMap.setBuildingsEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(point));
                etLatitude.setText(String.valueOf(point.latitude));
                etLongitude.setText(String.valueOf(point.longitude));
            }
        });

        // Add a marker and move the camera
        if(Data.getLastLocation() != null) {
            LatLng location = new LatLng(Data.getLastLocation().getLatitude(), Data.getLastLocation().getLongitude());
            googleMap.addMarker(new MarkerOptions().position(location));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(AddWaypointActivity.this, WaypointActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.btSearch:
                    lookupGeoCode();
                    break;
                case R.id.btSave:
                    saveWaypoint();
                    break;
            }
        }
    };

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
        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.addwaypoint_view_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private void lookupGeoCode(){
        if(!etSearch.getText().toString().equals("")){
            Geocoder coder = new Geocoder(this);
            List<Address> address;

            try {
                // May throw an IOException
                address = coder.getFromLocationName(etSearch.getText().toString(), 5);
                if (address == null) {
                    Toast.makeText(this, R.string.geocode_error, Toast.LENGTH_LONG).show();
                }
                if (address.size() > 0) {
                    Address location = address.get(0);
                    LatLng latLong = new LatLng(location.getLatitude(), location.getLongitude());
                    etLatitude.setText(String.valueOf(location.getLatitude()));
                    etLongitude.setText(String.valueOf(location.getLongitude()));
                    googleMap.clear();
                    googleMap.addMarker(new MarkerOptions().position(latLong));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong,15));
                } else {
                    Toast.makeText(this, R.string.geocode_error, Toast.LENGTH_LONG).show();
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(this, R.string.geocode_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveWaypoint(){
        if(!etLatitude.getText().toString().equals("") && !etLongitude.getText().toString().equals("")) {
            // Get current date/time
            Calendar cal = Calendar.getInstance();
            Date date = cal.getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
            String curdatetime = formatter.format(date);

            // Open database
            WaypointDatasource datasource = new WaypointDatasource(this);
            datasource.open();

            String waypoint = etLatitude.getText().toString() + "," + etLongitude.getText().toString();
            String label = etLabel.getText().toString();
            WaypointRecord record = new WaypointRecord(curdatetime, waypoint, label);
            datasource.addRecord(record);
            datasource.close();

            Toast.makeText(this, R.string.toast_waypoint_saved, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.toast_waypoint_error, Toast.LENGTH_LONG).show();
        }
    }
}
