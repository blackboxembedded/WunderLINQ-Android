package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.IOException;
import java.util.ArrayList;

public class AboutActivity extends AppCompatActivity {

    private final static String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

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
        tvVersion.setText(String.format("%s %s", getString(R.string.version_label), BuildConfig.VERSION_NAME));
        TextView tvCompany = findViewById(R.id.tvCompany);
        tvCompany.setMovementMethod(LinkMovementMethod.getInstance());
        Button btSendLogs = findViewById(R.id.btSendLogs);
        btSendLogs.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // save logcat in file
                File root = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/debug/");
                if(!root.exists()){
                    if(!root.mkdirs()){
                        Log.d(TAG,"Unable to create directory: " + root);
                    }
                }
                File outputFile = new File(Environment.getExternalStorageDirectory(),
                        "/WunderLINQ/debug/logcat.txt");
                try {
                    Runtime.getRuntime().exec(
                            "logcat -f " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                File debugFile = new File(MyApplication.getContext().getCacheDir(), "/tmp/dbg");
                //send file using email
                Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                // set the type to 'email'
                emailIntent.setType("text/plain");
                String[] to;
                to = new String[]{getString(R.string.sendlogs_email)};
                emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
                // the attachment
                //has to be an ArrayList
                ArrayList<Uri> uris = new ArrayList<>();
                if(debugFile.exists()){
                    uris.add(FileProvider.getUriForFile(AboutActivity.this, "com.blackboxembedded.wunderlinq.fileprovider", debugFile));
                }
                //convert from paths to Android friendly Parcelable Uri's
                uris.add(FileProvider.getUriForFile(AboutActivity.this, "com.blackboxembedded.wunderlinq.fileprovider", outputFile));
                emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                // the mail subject
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sendlogs_subject));
                emailIntent.putExtra(Intent.EXTRA_TEXT, "App Version: " + BuildConfig.VERSION_NAME + "\n"
                        + "Android Version: " + Build.VERSION.RELEASE + "\n"
                        + "Manufacturer, Model: " + Build.MANUFACTURER + ", " + Build.MODEL + "\n"
                        + getString(R.string.sendlogs_body));
                emailIntent.setType("message/rfc822");
                emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.sendlogs_intent_title)));
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(AboutActivity.this);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putBoolean("prefDebugLogging", false);
                editor.apply();
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

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.action_back) {
                    Intent backIntent = new Intent(AboutActivity.this, MainActivity.class);
                    startActivity(backIntent);
            }
        }
    };
}
