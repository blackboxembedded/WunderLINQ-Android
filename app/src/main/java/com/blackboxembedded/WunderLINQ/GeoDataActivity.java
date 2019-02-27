package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GeoDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_data);

        View view = findViewById(R.id.clGeoData);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(GeoDataActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
        });

        LinearLayout tripsLayout = findViewById(R.id.llTrips);
        LinearLayout waypointsLayout = findViewById(R.id.llWaypoints);
        tripsLayout.setOnClickListener(mClickListener);
        waypointsLayout.setOnClickListener(mClickListener);
        showActionBar();
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
        navbarTitle.setText(R.string.geodata_label);

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
                    Intent backIntent = new Intent(GeoDataActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.llTrips:
                    Intent tripsIntent = new Intent(GeoDataActivity.this, TripsActivity.class);
                    startActivity(tripsIntent);
                    break;
                case R.id.llWaypoints:
                    Intent waypointsIntent = new Intent(GeoDataActivity.this, WaypointActivity.class);
                    startActivity(waypointsIntent);
                    break;
            }
        }
    };
}
