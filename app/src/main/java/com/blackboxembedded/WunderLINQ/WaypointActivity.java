package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class WaypointActivity extends AppCompatActivity {

    private ImageButton backButton;
    private ImageButton forwardButton;

    private ListView waypointList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_waypoint);

        showActionBar();

        waypointList = (ListView) findViewById(R.id.lv_waypoints);

        // Open database
        WaypointDatasource datasource;
        datasource = new WaypointDatasource(this);
        datasource.open();

        List<WaypointRecord> listValues = datasource.getAllRecords();
        ArrayAdapter<WaypointRecord> adapter = new
                WaypointListView(this, listValues, false);

        waypointList.setAdapter(adapter);

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
        navbarTitle.setText(R.string.waypoint_title);

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
                    Intent backIntent = new Intent(WaypointActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };
}
