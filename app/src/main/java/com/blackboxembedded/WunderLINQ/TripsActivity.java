package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


public class TripsActivity extends AppCompatActivity {

    private static final String TAG = "WunderLINQ";

    private ImageButton backButton;
    private ImageButton forwardButton;

    private ListView tripList;

    private ArrayList myList;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_trips);

        tripList = findViewById(R.id.lv_trips);
        tripList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                Intent tripViewIntent = new Intent(TripsActivity.this, TripViewActivity.class);
                tripViewIntent.putExtra("FILE", myList.get(position).toString());
                startActivity(tripViewIntent);
            }
        });

        showActionBar();

        updateListing();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateListing();
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
        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.trips_title);

        backButton = (ImageButton) findViewById(R.id.action_back);
        forwardButton = (ImageButton) findViewById(R.id.action_forward);
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
        File root = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/logs/");
        if(!root.exists()){
            if(!root.mkdirs()){
                Log.d(TAG,"Unable to create directory: " + root);
            }
        }
        File list[] = root.listFiles();
        myList = new ArrayList<String>();
        if (list != null ) {
            Arrays.sort(list, Collections.reverseOrder());

            for (int i = 0; i < list.length; i++) {
                myList.add(list[i].getName());
            }
        }
        if (myList.size() > 0 ) {
            adapter = new
                    TripListView(this, myList);
            tripList.setAdapter(adapter);
        }
    }
}
