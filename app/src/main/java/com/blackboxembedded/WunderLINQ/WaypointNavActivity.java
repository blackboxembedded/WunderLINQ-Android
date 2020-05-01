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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT;

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
                String navApp = sharedPrefs.getString("prefNavApp", "1");
                Intent navIntent = new Intent(android.content.Intent.ACTION_VIEW);
                String navUrl = "google.navigation:q=" + record.getData() + "&navigate=yes";
                // Get location
                // Get the location manager
                LocationManager locationManager = (LocationManager)
                        WaypointNavActivity.this.getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                try {
                    String bestProvider = locationManager.getBestProvider(criteria, false);
                    Log.d(TAG,"Trying Best Provider: " + bestProvider);
                    Location currentLocation = locationManager.getLastKnownLocation(bestProvider);
                    if (navApp.equals("1")) {
                        // Android Default
                        // Nothing to do
                    } else if (navApp.equals("2")){
                        //Google Maps
                        navIntent.setPackage("com.google.android.apps.maps");
                    } else if (navApp.equals("3")){
                        //Locus Maps
                        navIntent.setPackage("menion.android.locus.pro");
                        navIntent.setData(Uri.parse(navUrl));
                        if(!isCallable(navIntent)){
                            Log.d(TAG,"Locus Maps Pro Not Installed");
                            navIntent.setPackage("menion.android.locus");
                        }
                    } else if (navApp.equals("4")){
                        //Waze
                        navUrl = "https://www.waze.com/ul?ll=" + record.getData() + "&navigate=yes&zoom=17";
                    } else if (navApp.equals("5")){
                        //Maps.me
                        navUrl = "https://dlink.maps.me/route?sll=" + String.valueOf(currentLocation.getLatitude()) + ","
                                + String.valueOf(currentLocation.getLongitude()) + "&saddr="
                                + getString(R.string.trip_view_waypoint_start_label) + "&dll="
                                + record.getData() + "&daddr=" + record.getLabel() + "&type=vehicle&back_url=wunderlinq://datagrid";
                    } else if (navApp.equals("6")){
                        // OsmAnd
                        String location[] = record.getData().split(",");
                        Double latitude =  Double.parseDouble(location[0]);
                        Double longitude =  Double.parseDouble(location[1]);
                        //navUrl = "osmand.navigation:q=" + String.valueOf(location.latitude) + "," + String.valueOf(location.longitude) + "&navigate=yes";
                        OsmAndHelper osmAndHelper = new OsmAndHelper(WaypointNavActivity.this, OsmAndHelper.REQUEST_OSMAND_API, WaypointNavActivity.this);
                        osmAndHelper.navigate("Start",currentLocation.getLatitude(),currentLocation.getLongitude(),"Destination",latitude,longitude,"motorcycle", true);
                    } else if (navApp.equals("7")){
                        //Mapfactor Navigator
                        navIntent.setPackage("com.mapfactor.navigator");
                        navUrl = "http://maps.google.com/maps?f=d&daddr=@"  + record.getData() + "&navigate=yes";
                    } else if (navApp.equals("8")) {
                        //Sygic
                        //https://www.sygic.com/developers/professional-navigation-sdk/android/api-examples/custom-url
                        String latlon[] = record.getData().split(",");
                        navUrl = "com.sygic.aura://coordinate|"  + latlon[1] + "|" + latlon[0] + "|drive";
                        //navUrl = "com.sygic.aura://coordinate|"  + latlon[1] + "|" + latlon[0] + "|drive&&&back_button|com.blackboxembedded.wunderlinq";
                    }  else if (navApp.equals("9")) {
                        //Kurviger
                        navUrl = "https://kurviger.de/en?point="  + record.getData() + "&locale=en" +"&vehicle=motorycycle"
                                + "weighting=fastest" + "use_miles=true";
                    }
                    if (!navApp.equals("6")) {
                        try {
                            navIntent.setData(Uri.parse(navUrl));
                            if (android.os.Build.VERSION.SDK_INT >= 24) {
                                if (isInMultiWindowMode()) {
                                    navIntent.setFlags(FLAG_ACTIVITY_LAUNCH_ADJACENT);
                                }
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
        });
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    public void onResume() {
        super.onResume();
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
                if ((waypointList.getSelectedItemPosition() == (listValues.size() - 1)) && lastPosition == (listValues.size() - 1) ){
                    waypointList.setSelection(0);
                }
                lastPosition = waypointList.getSelectedItemPosition();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (waypointList.getSelectedItemPosition() == 0 && lastPosition == 0){
                    waypointList.setSelection(listValues.size() - 1);
                }
                lastPosition = waypointList.getSelectedItemPosition();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,

                PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;
    }
}
