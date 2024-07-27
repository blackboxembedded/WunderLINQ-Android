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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blackboxembedded.WunderLINQ.Utils.AppUtils;

import java.util.List;

public class WaypointActivity extends AppCompatActivity {

    public final static String TAG = "WaypointActivity";

    private RecyclerView waypointList;
    private List<WaypointRecord> listValues;
    private WaypointsAdapter adapter;
    private WaypointDatasource datasource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_waypoint);
        waypointList = findViewById(R.id.rv_waypoints);
        waypointList.setLayoutManager(new LinearLayoutManager(this));

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Display dialog text here......
                final AlertDialog.Builder builder = new AlertDialog.Builder(WaypointActivity.this);
                builder.setTitle(getString(R.string.delete_waypoint_alert_title));
                builder.setMessage(getString(R.string.delete_waypoint_alert_body));
                builder.setPositiveButton(R.string.delete_bt,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int position = viewHolder.getAdapterPosition();
                                WaypointRecord record = (WaypointRecord) listValues.get(position);
                                datasource.removeRecord(record);
                                listValues.remove(position);
                                adapter.notifyItemRemoved(position);
                            }
                        });
                builder.setNegativeButton(R.string.cancel_bt,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                updateListing();
                            }
                        });
                builder.show();
            }

        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(waypointList);

        showActionBar();
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateListing();
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
        datasource = new WaypointDatasource(this);
        datasource.open();
        listValues = datasource.getAllRecords();
        Log.d(TAG, listValues.toString());
        datasource.close();
        adapter = new WaypointsAdapter(this, listValues);
        waypointList.setAdapter(adapter);
        adapter.setClickListener(new WaypointsAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent waypointViewIntent = new Intent(WaypointActivity.this, WaypointViewActivity.class);
                WaypointRecord record = (WaypointRecord) listValues.get(position);
                String recordID = Long.toString(record.getID());
                waypointViewIntent.putExtra("RECORD_ID", recordID);
                startActivity(waypointViewIntent);
            }
        });
    }
}
