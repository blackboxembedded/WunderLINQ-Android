package com.blackboxembedded.WunderLINQ;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    private ImageView ivAppLogo;
    private TextView tvAppName;
    private TextView tvVersion;
    private TextView tvCompany;
    private TextView tvCredits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ivAppLogo = findViewById(R.id.ivLogo);
        ivAppLogo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String url = "http://www.wunderlinq.com";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }

        });
        tvVersion = (TextView) findViewById(R.id.tvVersion);
        tvVersion.setText(getString(R.string.version_label) + " " + BuildConfig.VERSION_NAME);
        tvCompany = (TextView) findViewById(R.id.tvCompany);
        tvCompany.setMovementMethod(LinkMovementMethod.getInstance());
        tvCredits = (TextView) findViewById(R.id.tvCredits);
        tvCredits.setMovementMethod(new ScrollingMovementMethod());

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

        TextView navbarTitle;
        navbarTitle = (TextView) findViewById(R.id.action_title);
        navbarTitle.setText(R.string.about_title);

        ImageButton backButton = (ImageButton) findViewById(R.id.action_back);
        ImageButton forwardButton = (ImageButton) findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(AboutActivity.this, MainActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };

}
