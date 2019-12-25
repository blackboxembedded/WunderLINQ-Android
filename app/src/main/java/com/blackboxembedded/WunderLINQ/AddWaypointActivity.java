package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AddWaypointActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private EditText etSearch;
    private Button btSearch;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private EditText etLabel;
    private Button btSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_waypoint);
        showActionBar();

        etSearch = findViewById(R.id.etSearch);
        btSearch = findViewById(R.id.btSearch);
        btSearch.setOnClickListener(mClickListener);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        etLabel = findViewById(R.id.etLabel);
        btSave = findViewById(R.id.btSave);
        btSave.setOnClickListener(mClickListener);

        FragmentManager myFragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) myFragmentManager.findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        tvLatitude.setText(String.valueOf(Data.getLastLocation().getLatitude()));
        tvLongitude.setText(String.valueOf(Data.getLastLocation().getLongitude()));
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
                tvLatitude.setText(String.valueOf(point.latitude));
                tvLongitude.setText(String.valueOf(point.longitude));
            }
        });

        // Add a marker and move the camera
        LatLng location = new LatLng(Data.getLastLocation().getLatitude(), Data.getLastLocation().getLongitude());
        googleMap.addMarker(new MarkerOptions().position(location));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,15));
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
        if(!etSearch.getText().equals("")){
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
                    tvLatitude.setText(String.valueOf(location.getLatitude()));
                    tvLongitude.setText(String.valueOf(location.getLongitude()));
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
        if(!tvLatitude.getText().equals("") && !tvLongitude.getText().equals("")) {
            // Get current date/time
            Calendar cal = Calendar.getInstance();
            Date date = cal.getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
            String curdatetime = formatter.format(date);

            // Open database
            WaypointDatasource datasource = new WaypointDatasource(this);
            datasource.open();

            String waypoint = tvLatitude.getText().toString() + "," + tvLongitude.getText().toString();
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
