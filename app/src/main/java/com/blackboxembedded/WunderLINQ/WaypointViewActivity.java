package com.blackboxembedded.WunderLINQ;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;


public class WaypointViewActivity extends AppCompatActivity implements OnMapReadyCallback, OsmAndHelper.OnOsmandMissingListener  {

    public final static String TAG = "WptViewActivity";

    private EditText etLabel;

    private WaypointDatasource datasource;
    private List<WaypointRecord> allWaypoints;
    private WaypointRecord record;
    private int index;

    private Double lat;
    private Double lon;

    private String navApp;

    @Override
    public void osmandMissing() {
        //OsmAndMissingDialogFragment().show(supportFragmentManager, null);
    }

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
                    datasource.close();
                    return true;
                }
                return false;
            }
        });

        Bundle extras = getIntent().getExtras();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        navApp = sharedPrefs.getString("prefNavApp", "1");
        if (extras != null) {
            String recordID = extras.getString("RECORD_ID");

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
    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.setTrafficEnabled(false);
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(false);
        // Add a marker and move the camera
        LatLng location = new LatLng(lat, lon);
        map.addMarker(new MarkerOptions().position(location).title(getString(R.string.waypoint_view_waypoint_label)));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location,15));
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
        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.waypoint_view_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    // Delete button press
    public void onClickDelete(View view) {
        // Display dialog text here......
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

    // Open button press
    public void onClickOpen(View view) {
        //Open waypoint in map app
        String navUrl = "geo:0,0?q=" + record.getData() + "(" + getString(R.string.waypoint_view_waypoint_label) + " " + record.getDate() + ")";
        if (navApp.equals("1") || navApp.equals("2")){
            // Android Default or Google Maps
            // Nothing to do
        } else if (navApp.equals("3")){
            //Locus

        } else if (navApp.equals("4")){
            //Waze
            navUrl = "https://www.waze.com/ul?ll=" + record.getData() + "&zoom=10";
        } else if (navApp.equals("5")){
            //Maps.me
            navUrl = "mapsme://map?ll=" + record.getData() + "&n=" + record.getLabel() + "&id=WunderLINQ&backurl=wunderlinq://&appname=WunderLINQ";
        }
        try {
            Intent navIntent = new Intent(android.content.Intent.ACTION_VIEW);
            navIntent.setData(Uri.parse(navUrl));
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                navIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
            }
            startActivity(navIntent);
        } catch ( ActivityNotFoundException ex  ) {
            // Add Alert
        }
    }

    // Navigate
    public void onClickNav(View view) {
        //Navigation
        // Get location
        // Get the location manager
        LocationManager locationManager = (LocationManager)
                WaypointViewActivity.this.getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        try {
            String bestProvider = locationManager.getBestProvider(criteria, false);
            Log.d(TAG,"Trying Best Provider: " + bestProvider);
            Location currentLocation = locationManager.getLastKnownLocation(bestProvider);
            String navUrl = "google.navigation:q=" + record.getData() + "&navigate=yes";
            if (navApp.equals("1") || navApp.equals("2")){
                // Android Default or Google Maps
                // Nothing to do
            } else if (navApp.equals("3")){
                //Locus

            } else if (navApp.equals("4")){
                //Waze
                navUrl = "https://www.waze.com/ul?ll=" + record.getData() + "&navigate=yes&zoom=17";
            } else if (navApp.equals("5")){
                //Maps.me
                navUrl = "mapsme://route?sll=" + String.valueOf(currentLocation.getLatitude()) + ","
                        + String.valueOf(currentLocation.getLongitude()) + "&saddr="
                        + getString(R.string.trip_view_waypoint_start_label) + "&dll="
                        + record.getData() + "&daddr=" + record.getLabel() + "&type=vehicle";
            } else if (navApp.equals("6")){
                // OsmAnd
                String location[] = record.getData().split(",");
                Double latitude =  Double.parseDouble(location[0]);
                Double longitude =  Double.parseDouble(location[1]);
                //navUrl = "osmand.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                OsmAndHelper osmAndHelper = new OsmAndHelper(WaypointViewActivity.this, OsmAndHelper.REQUEST_OSMAND_API, WaypointViewActivity.this);
                osmAndHelper.navigate("Start",currentLocation.getLatitude(),currentLocation.getLongitude(),"Destination",latitude,longitude,"motorcycle", true);
            }
            if (!navApp.equals("6")) {
                try {
                    Intent navIntent = new Intent(android.content.Intent.ACTION_VIEW);
                    navIntent.setData(Uri.parse(navUrl));
                    if (android.os.Build.VERSION.SDK_INT >= 24) {
                        navIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                    }
                    startActivity(navIntent);
                } catch (ActivityNotFoundException ex) {
                    // Add Alert
                }
            }
        } catch (SecurityException|NullPointerException e) {
            e.printStackTrace();
        }
    }

    // Export button press
    public void onClickShare(View view) {
        String uri = "http://maps.google.com/maps?saddr=" +lat+","+lon;

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String ShareSub = getString(R.string.waypoint_view_waypoint_label);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ShareSub);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, uri);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.waypoint_view_share_label)));
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(WaypointViewActivity.this, WaypointActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };
}
