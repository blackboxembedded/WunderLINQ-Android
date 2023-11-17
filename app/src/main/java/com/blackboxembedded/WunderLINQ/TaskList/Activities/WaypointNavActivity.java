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
package com.blackboxembedded.WunderLINQ.TaskList.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.blackboxembedded.WunderLINQ.Utils.AppUtils;
import com.blackboxembedded.WunderLINQ.NavAppHelper;
import com.blackboxembedded.WunderLINQ.OnSwipeTouchListener;
import com.blackboxembedded.WunderLINQ.OsmAndHelper;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.WaypointDatasource;
import com.blackboxembedded.WunderLINQ.WaypointListView;
import com.blackboxembedded.WunderLINQ.WaypointRecord;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class WaypointNavActivity extends AppCompatActivity implements OsmAndHelper.OnOsmandMissingListener {

    public final static String TAG = "WaypointNav";

    private ListView waypointList;
    List<WaypointRecord> listValues;
    ArrayAdapter<WaypointRecord> adapter;

    private int lastPosition = 0;

    private SharedPreferences sharedPrefs;

    @Override
    public void osmandMissing() {
        //OsmAndMissingDialogFragment().show(supportFragmentManager, null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_waypoint_nav);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String orientation = sharedPrefs.getString("prefOrientation", "0");
        if (!orientation.equals("0")){
            if(orientation.equals("1")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if (orientation.equals("2")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }

        showActionBar();

        waypointList = findViewById(R.id.lv_waypoints);
        waypointList.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                goBack();
            }
        });

        // Open database
        WaypointDatasource datasource = new WaypointDatasource(this);
        datasource.open();

        listValues = datasource.getAllRecords();
        adapter = new
                WaypointListView(this, listValues);

        waypointList.setAdapter(adapter);

        waypointList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView < ? > adapter, View view, int position, long arg){
                lastPosition = position;
                WaypointRecord record = (WaypointRecord) waypointList.getItemAtPosition(position);

                // Check Location permissions
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(WaypointNavActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                } else {
                    // Get the location manager
                    LocationManager locationManager = (LocationManager)
                            WaypointNavActivity.this.getSystemService(LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    try {
                        // Get location
                        String bestProvider = locationManager.getBestProvider(criteria, false);
                        Location currentLocation = locationManager.getLastKnownLocation(bestProvider);

                        String[] latlon = record.getData().split(",");
                        LatLng location = new LatLng(Double.parseDouble(latlon[0]),Double.parseDouble(latlon[1]));
                                Location destination = new Location(LocationManager.GPS_PROVIDER);
                        destination.setLatitude(location.latitude);
                        destination.setLongitude(location.longitude);

                        if (!NavAppHelper.navigateTo(WaypointNavActivity.this, currentLocation, destination)) {
                            Toast.makeText(WaypointNavActivity.this, R.string.nav_app_feature_not_supported, Toast.LENGTH_LONG).show();
                        }
                    } catch (SecurityException|NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        int highlightColor = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getInt("prefHighlightColor", R.color.colorAccent);
        waypointList.setSelector(new ColorDrawable(highlightColor));
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.waypoints_nav_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private void goBack(){
        Intent backIntent = new Intent(WaypointNavActivity.this, com.blackboxembedded.WunderLINQ.TaskList.TaskActivity.class);
        startActivity(backIntent);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    goBack();
                    break;
            }
        }
    };

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                goBack();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_NUMPAD_SUBTRACT:
                if ((waypointList.getSelectedItemPosition() == (listValues.size() - 1)) && lastPosition == (listValues.size() - 1) ){
                    waypointList.setSelection(0);
                }
                lastPosition = waypointList.getSelectedItemPosition();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_NUMPAD_ADD:
                if (waypointList.getSelectedItemPosition() == 0 && lastPosition == 0){
                    waypointList.setSelection(listValues.size() - 1);
                }
                lastPosition = waypointList.getSelectedItemPosition();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
