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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class TripsActivity extends AppCompatActivity {

    private static final String TAG = "TripsActivity";

    private RecyclerView tripList;

    private ArrayList<String> trips;
    private TripsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_trips);

        tripList = findViewById(R.id.rv_trips);
        tripList.setLayoutManager(new LinearLayoutManager(this));
        trips = new ArrayList<String>();
        adapter = new TripsAdapter(this, trips);
        tripList.setAdapter(adapter);
        adapter.setClickListener(new TripsAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent tripViewIntent = new Intent(TripsActivity.this, TripViewActivity.class);
                tripViewIntent.putExtra("FILE", trips.get(position).toString());
                startActivity(tripViewIntent);
            }
        });
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Display dialog text here......
                final AlertDialog.Builder builder = new AlertDialog.Builder(TripsActivity.this);
                builder.setTitle(getString(R.string.delete_trip_alert_title));
                builder.setMessage(getString(R.string.delete_trip_alert_body));
                builder.setPositiveButton(R.string.delete_bt,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int position = viewHolder.getAdapterPosition();
                                String fileName = trips.get(position).toString();
                                File file = new File(MyApplication.getContext().getExternalFilesDir(null), "/logs/" + fileName);
                                file.delete();
                                trips.remove(position);  // mData is your data list
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
        itemTouchHelper.attachToRecyclerView(tripList);

        showActionBar();

        updateListing();
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
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.trips_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(TripsActivity.this, GeoDataActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };

    private void updateListing(){
        File root = new File(MyApplication.getContext().getExternalFilesDir(null), "/logs/");
        if(!root.exists()){
            if(!root.mkdirs()){
                Log.d(TAG,"Unable to create directory: " + root);
            }
        }
        File[] list = root.listFiles();
        trips.clear();
        if (list != null ) {
            Arrays.sort(list, Collections.reverseOrder());
            for (File file : list) {
                trips.add(file.getName());
            }
        }
        if (trips.size() > 0 ) {
            adapter.notifyDataSetChanged();
        }
    }
}
