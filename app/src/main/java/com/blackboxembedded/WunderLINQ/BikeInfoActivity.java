package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;

public class BikeInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_info);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        View view = findViewById(R.id.clBikeInfo);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(BikeInfoActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
        });

        showActionBar();

        TextView tvVIN = findViewById(R.id.tvVINValue);
        TextView tvNextServiceDate = findViewById(R.id.tvNextServiceDateValue);
        TextView tvNextService = findViewById(R.id.tvNextServiceValue);

        if (Data.getVin() != null){
            tvVIN.setText(Data.getVin());
        }

        if (Data.getNextServiceDate() != null){
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
            String dateString = format.format(Data.getNextServiceDate());
            tvNextServiceDate.setText(dateString);
        }

        if (Data.getNextService() != null){
            String distanceFormat = sharedPrefs.getString("prefDistance", "0");
            String nextService = Data.getNextService() + "(km)";
            if (distanceFormat.contains("1")) {
                nextService = Math.round(Utils.kmToMiles(Data.getNextService())) + "(mi)";
            }
            tvNextService.setText(nextService);
        }
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        navbarTitle.setText(R.string.bike_info_title);

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
                    Intent backIntent = new Intent(BikeInfoActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };
}
