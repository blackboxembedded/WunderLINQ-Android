package com.badasscompany.NavLINq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class TaskActivity extends AppCompatActivity {

    public final static String TAG = "NavLINq";

    private ImageButton backButton;
    private ImageButton forwardButton;
    private TextView navbarTitle;

    private ListView taskList;

    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_task);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        taskList = (ListView) findViewById(R.id.lv_tasks);

        showActionBar();

        final String[] taskTitles = new String[] {
                getResources().getString(R.string.task_title_gohome),
                getResources().getString(R.string.task_title_callhome)
        };
        Integer[] iconId = {
                R.drawable.ic_home,
                R.drawable.ic_phone
        };

        TaskListView adapter = new
                TaskListView(this, taskTitles, iconId);
        taskList=(ListView)findViewById(R.id.lv_tasks);
        taskList.setAdapter(adapter);

        taskList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                Log.d(TAG,"Item "+ position + ":" + item );
                switch (position){
                    case 0:
                        //Navigate Home
                        String address = sharedPrefs.getString("prefHomeAddress","");
                        if ( address != "" ) {
                            Intent goHomeIntent = new Intent();
                            goHomeIntent.setData(Uri.parse("geo:0,0?q=" + address));
                            startActivity(goHomeIntent);
                        } else {
                            //TODO Toast
                        }
                    case 1:
                        //Call Home
                        String phonenumber = sharedPrefs.getString("prefHomePhone","");
                        if (phonenumber != "") {
                            Intent callHomeIntent = new Intent(Intent.ACTION_DIAL);
                            callHomeIntent.setData(Uri.parse("tel:" + phonenumber));
                            startActivity(callHomeIntent);
                        } else {
                            //TODO Toast
                        }
                }
            }

        });
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
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

        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.quicktask_title);

        backButton = (ImageButton) findViewById(R.id.action_back);
        forwardButton = (ImageButton) findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setOnClickListener(mClickListener);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(TaskActivity.this, CompassActivity.class);
                    startActivity(backIntent);
                    break;
                case R.id.action_forward:
                    Intent forwardIntent = new Intent(TaskActivity.this, MainActivity.class);
                    startActivity(forwardIntent);
                    break;
            }
        }
    };
}
