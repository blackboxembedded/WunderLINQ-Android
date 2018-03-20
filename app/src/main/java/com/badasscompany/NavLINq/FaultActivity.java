package com.badasscompany.NavLINq;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class FaultActivity extends AppCompatActivity {

    private ImageButton backButton;
    private ImageButton forwardButton;

    private ListView faultList;
    private FaultStatus faults;
    ArrayList<String> faultListData = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fault);

        showActionBar();

        faultList = (ListView) findViewById(R.id.lv_faults);

        faults = (new FaultStatus(this));
        faultListData = faults.getallActiveDesc();

        faultList.setAdapter(new ArrayAdapter<String>(this, R.layout.list_fault,faultListData));

        faultList.setTextFilterEnabled(true);
        faultList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                Toast.makeText(getApplicationContext(),
                        ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
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
        navbarTitle.setText(R.string.fault_title);

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
                    Intent backIntent = new Intent(FaultActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };
}