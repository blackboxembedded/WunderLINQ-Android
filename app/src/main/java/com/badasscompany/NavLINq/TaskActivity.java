package com.badasscompany.NavLINq;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TaskActivity extends AppCompatActivity {

    public final static String TAG = "NavLINq";

    private ImageButton backButton;
    private ImageButton forwardButton;

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

        String videoTaskText = getResources().getString(R.string.task_title_start_record);
        if (((MyApplication) this.getApplication()).getVideoRecording()){
            videoTaskText = getResources().getString(R.string.task_title_stop_record);
        }

        String tripTaskText = getResources().getString(R.string.task_title_start_trip);
        if (((MyApplication) this.getApplication()).getTripRecording()){
            tripTaskText = getResources().getString(R.string.task_title_stop_trip);
        }

        final String[] taskTitles = new String[] {
                getResources().getString(R.string.task_title_gohome),
                getResources().getString(R.string.task_title_callhome),
                getResources().getString(R.string.task_title_callcontact),
                videoTaskText,
                tripTaskText,
                getResources().getString(R.string.task_title_voicecontrol)
        };
        Drawable[] iconId = {
                getResources().getDrawable(R.drawable.ic_home,getTheme()),
                getResources().getDrawable(R.drawable.ic_phone,getTheme()),
                getResources().getDrawable(R.drawable.ic_address_book,getTheme()),
                getResources().getDrawable(R.drawable.ic_video_camera,getTheme()),
                getResources().getDrawable(R.drawable.ic_road,getTheme()),
                getResources().getDrawable(R.drawable.ic_microphone,getTheme())
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
                switch (position){
                    case 0:
                        //Navigate Home
                        String address = sharedPrefs.getString("prefHomeAddress","");
                        if ( address != "" ) {
                            Intent goHomeIntent = new Intent(android.content.Intent.ACTION_VIEW);
                            goHomeIntent.setData(Uri.parse("google.navigation:q=" + address));
                            startActivity(goHomeIntent);
                        } else {
                            Toast.makeText(TaskActivity.this, R.string.toast_address_not_set, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1:
                        //Call Home
                        String phonenumber = sharedPrefs.getString("prefHomePhone","");
                        if (phonenumber != "") {
                            Intent callHomeIntent = new Intent(Intent.ACTION_DIAL);
                            callHomeIntent.setData(Uri.parse("tel:" + phonenumber));
                            startActivity(callHomeIntent);
                        } else {
                            Toast.makeText(TaskActivity.this, R.string.toast_phone_not_set, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        //Call Contact
                        Intent forwardIntent = new Intent(TaskActivity.this, ContactListActivity.class);
                        startActivity(forwardIntent);
                        break;
                    case 3:
                        //Record Video
                        TextView taskText=(TextView)view.findViewById(R.id.tv_label);
                        if (taskText.getText().equals(getResources().getString(R.string.task_title_start_record))) {
                            if (Build.VERSION.SDK_INT >= 23) {
                                if (!Settings.canDrawOverlays(TaskActivity.this)) {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:" + getPackageName()));
                                    startActivityForResult(intent, 1234);
                                } else {
                                    startService(new Intent(TaskActivity.this, VideoRecService.class));
                                }
                            } else {
                                startService(new Intent(TaskActivity.this, VideoRecService.class));
                            }
                            taskText.setText(getResources().getString(R.string.task_title_stop_record));
                        } else {
                            stopService(new Intent(TaskActivity.this, VideoRecService.class));
                            taskText.setText(getResources().getString(R.string.task_title_start_record));
                        }
                        break;
                    case 4:
                        //Trip Log
                        TextView tripTaskText=(TextView)view.findViewById(R.id.tv_label);
                        if (tripTaskText.getText().equals(getResources().getString(R.string.task_title_start_trip))) {
                            startService(new Intent(TaskActivity.this, LoggingService.class));
                            tripTaskText.setText(getResources().getString(R.string.task_title_stop_trip));
                        } else {
                            stopService(new Intent(TaskActivity.this, LoggingService.class));
                            tripTaskText.setText(getResources().getString(R.string.task_title_start_trip));
                        }
                        break;
                    case 5:
                        //Voice Assistant
                        startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        break;
                }
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234) {
            startService(new Intent(TaskActivity.this,
                    VideoRecService.class));

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

        TextView navbarTitle;
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_ESCAPE:
                Intent backIntent = new Intent(TaskActivity.this, CompassActivity.class);
                startActivity(backIntent);
                return true;
            case KeyEvent.KEYCODE_ENTER:
                Intent forwardIntent = new Intent(TaskActivity.this, MainActivity.class);
                startActivity(forwardIntent);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
