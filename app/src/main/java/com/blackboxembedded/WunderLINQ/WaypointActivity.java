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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.blackboxembedded.WunderLINQ.TaskList.Activities.AddWaypointActivity;

import java.util.List;

public class WaypointActivity extends AppCompatActivity {

    public final static String TAG = "WaypointActivity";

    private ListView waypointList;
    List<WaypointRecord> listValues;
    ArrayAdapter<WaypointRecord> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_waypoint);
        waypointList = findViewById(R.id.lv_waypoints);
        showActionBar();

        updateListing();

        adapter = new
                WaypointListView(this, listValues);
        waypointList.setAdapter(adapter);
        waypointList.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(WaypointActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
        });
        waypointList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView < ? > adapter, View view, int position, long arg){
                Intent waypointViewIntent = new Intent(WaypointActivity.this, WaypointViewActivity.class);
                WaypointRecord record = (WaypointRecord) waypointList.getItemAtPosition(position);
                String recordID = Long.toString(record.getID());
                waypointViewIntent.putExtra("RECORD_ID", recordID);
                startActivity(waypointViewIntent);
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

        updateListing();
        adapter.clear();
        adapter.addAll(listValues);
        adapter.notifyDataSetChanged();
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav_addwpt, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle;
        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.waypoint_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(WaypointActivity.this, GeoDataActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_forward:
                    Intent addIntent = new Intent(WaypointActivity.this, AddWaypointActivity.class);
                    startActivity(addIntent);
                    break;
            }
        }
    };

    private void updateListing(){
        WaypointDatasource datasource;
        datasource = new WaypointDatasource(this);
        datasource.open();
        listValues = null;
        listValues = datasource.getAllRecords();
        datasource.close();
    }
}
