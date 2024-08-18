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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AboutActivity extends AppCompatActivity {

    private final static String TAG = "AboutActivity";
    SharedPreferences sharedPrefs;
    String fwVersion = "Unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AboutActivity.this);
        fwVersion = sharedPrefs.getString("firmwareVersion", "Unknown");
        View view = findViewById(R.id.clAbout);
        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeRight() {
                Intent backIntent = new Intent(AboutActivity.this, MainActivity.class);
                startActivity(backIntent);
            }
        });

        ImageView ivAppLogo = findViewById(R.id.ivLogo);
        ivAppLogo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String url = "http://www.wunderlinq.com";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }

        });
        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText(String.format("%s %s %s %s %s", getString(R.string.version_label), getString(R.string.app_ver_label), BuildConfig.VERSION_NAME, getString(R.string.fw_ver_label), fwVersion));
        TextView tvCompany = findViewById(R.id.tvCompany);
        tvCompany.setMovementMethod(LinkMovementMethod.getInstance());
        Button btDocumentation = findViewById(R.id.btDocumentation);
        btDocumentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://blackboxembedded.github.io/WunderLINQ-Documentation/";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        Button btSendLogs = findViewById(R.id.btSendLogs);
        btSendLogs.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // Get current date
                Calendar cal = Calendar.getInstance();
                Date date = cal.getTime();
                @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HH:mm");
                String curDateTime = formatter.format(date);
                //Send file(s) using email
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/plain");
                String[] to;
                to = new String[]{getString(R.string.sendlogs_email)};
                emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
                File outputFile = new File(MyApplication.getContext().getExternalFilesDir(null), "wunderlinq.log");
                if(outputFile.exists()) {
                    emailIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(AboutActivity.this, "com.blackboxembedded.wunderlinq.fileprovider", outputFile));
                }
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sendlogs_subject) + " " + curDateTime);
                emailIntent.putExtra(Intent.EXTRA_TEXT, "App Version: " + BuildConfig.VERSION_NAME + "\n"
                        + "Firmware Version: " + fwVersion + "\n"
                        + "Android Version: " + Build.VERSION.RELEASE + "\n"
                        + "Manufacturer, Model: " + Build.MANUFACTURER + ", " + Build.MODEL + "\n"
                        + getString(R.string.sendlogs_body));
                emailIntent.setType("message/rfc822");
                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant read access to the URI
                startActivity(Intent.createChooser(emailIntent, getString(R.string.sendlogs_intent_title)));
            }

        });
        TextView tvCredits = findViewById(R.id.tvCredits);
        tvCredits.setMovementMethod(new ScrollingMovementMethod());

        showActionBar();
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
    protected void onDestroy() {
        Log.d(TAG,"In onDestroy");
        super.onDestroy();
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

        TextView navbarTitle;
        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.about_title);

        ImageButton backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private final View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.action_back) {
                    Intent backIntent = new Intent(AboutActivity.this, MainActivity.class);
                    startActivity(backIntent);
            }
        }
    };
}
