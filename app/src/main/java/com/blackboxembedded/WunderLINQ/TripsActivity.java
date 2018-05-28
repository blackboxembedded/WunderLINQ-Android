package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.util.List;

public class TripsActivity extends AppCompatActivity {

    private static final String TAG = "WunderLINQ";

    private ImageButton backButton;
    private ImageButton forwardButton;

    private ListView tripList;

    private ArrayList myList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trips);

        tripList = findViewById(R.id.lv_trips);

        showActionBar();

        myList = new ArrayList<String>();

        File root = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/logs/");
        if(!root.exists()){
            if(!root.mkdirs()){
                Log.d(TAG,"Unable to create directory: " + root);
            }
        }
        File list[] = root.listFiles();

        for( int i=0; i< list.length; i++)
        {
            myList.add( list[i].getName() );
        }

        ArrayAdapter<WaypointRecord> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, myList);
        tripList.setAdapter(adapter);

        tripList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView < ? > adapter, View view, int position, long arg){
                Intent tripViewIntent = new Intent(TripsActivity.this, TripViewActivity.class);
                tripViewIntent.putExtra("FILE", myList.get(position).toString());
                startActivity(tripViewIntent);
            }
        });

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
                    Intent backIntent = new Intent(TripsActivity.this, DataActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };
}