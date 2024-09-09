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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;

public class AlertActivity extends AppCompatActivity {

    public final static String TAG = "AlertActivity";

    int type = 0;
    public final static int ALERT_FUEL = 1;
    public final static  int ALERT_PHOTO = 2;
    public final static  int ALERT_IGNITION = 3;

    String title = "";
    String body = "";
    String backgroundPath = "";

    TextView tvAlertBody;
    Button btnOK;
    Button btnClose;
    TextView navbarTitle;
    ActionBar actionBar;
    ImageView backgroundImageView;

    private Handler handler;
    private Runnable runnable;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final int ACTIVITY_CLOSE_TIMER = 10000;  //MS to close activity

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        View view = findViewById(R.id.layout_alert);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                finish();
            }
        });

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String orientation = sharedPrefs.getString("prefOrientation", "0");
        if (!orientation.equals("0")){
            if(orientation.equals("1")){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        tvAlertBody = findViewById(R.id.tvAlertBody);
        btnClose = findViewById(R.id.btnClose);
        btnOK = findViewById(R.id.btnOK);
        btnClose.setOnClickListener(mClickListener);
        btnOK.setOnClickListener(mClickListener);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            type = extras.getInt("TYPE");
            title = extras.getString("TITLE");
            body = extras.getString("BODY");
            backgroundPath = extras.getString("BACKGROUND");
        }
        tvAlertBody.setText(body);

        switch (type){
            case ALERT_FUEL:
                break;
            case ALERT_PHOTO:
                btnOK.setVisibility(View.INVISIBLE);
                if(!backgroundPath.equals("")){
                    backgroundImageView = findViewById(R.id.imageViewBackground);
                    backgroundImageView.setImageDrawable(Drawable.createFromPath(backgroundPath));
                    backgroundImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
                break;
            default:
                break;
        }
        showActionBar();


        final CountDownTimer countDownTimer = new CountDownTimer(ACTIVITY_CLOSE_TIMER, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                final String closeBtnWithCountDown = (String) btnClose.getText() + "  ( " + millisUntilFinished / 1000 + " )";

                btnClose.setText(closeBtnWithCountDown);
            }

            @Override
            public void onFinish() {
                // Previously was being called rather than close.  Need to investigate the differences
                // finish();
                if (btnClose != null) {
                    btnClose.performClick();
                }
            }
        }.start();
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.btnClose:
                    finish();
                    break;
                case R.id.btnOK:
                    if (type == ALERT_FUEL) {
                        if (!NavAppHelper.navigateToFuel(AlertActivity.this, MotorcycleData.getLastLocation())) {
                            tvAlertBody.setText(getString(R.string.nav_app_feature_not_supported));
                        }
                    } else if (type == ALERT_IGNITION) {
                        //Stop Service
                        BluetoothLeService.close();
                        // End App
                        finishAffinity();
                    }
                    break;
            }
        }
    };

    private void showActionBar(){
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.actionbar_nav, null);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);
        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setVisibility(View.INVISIBLE);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                finish();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (type != ALERT_PHOTO) {
                    btnOK.performClick();
                }
                switch (type){
                    case ALERT_PHOTO:
                        finish();
                        break;
                    default:
                        btnOK.performClick();
                        break;
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
