package com.badasscompany.NavLINq;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new UserSettingActivityFragment()).commit();

    }

    public static class UserSettingActivityFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            // Disable auto night mode option when no sensor is found
            if (!MainActivity.hasSensor){
                getPreferenceScreen().findPreference("prefAutoNightMode").setEnabled(false);
                getPreferenceScreen().findPreference("prefAutoNightModeDelay").setEnabled(false);
            }

            Preference button = findPreference("prefSendLog");
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // save logcat in file
                    File outputFile = new File(Environment.getExternalStorageDirectory(),
                            "/NavLINq/logcat.txt");
                    try {
                        Runtime.getRuntime().exec(
                                "logcat -f " + outputFile.getAbsolutePath());
                        Log.d("NavLINqSettings","running logcat");
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    //send file using email
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    // set the type to 'email'
                    emailIntent.setType("text/plain");
                    String to[] = {getString(R.string.pref_sendlogs_email)};
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
                    // the attachment
                    Uri uri = FileProvider.getUriForFile(getActivity(), "com.badasscompany.navlinq.fileprovider", outputFile);
                    emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    // the mail subject
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pref_sendlogs_subject));
                    emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.pref_sendlogs_body));
                    emailIntent.setType("message/rfc822");
                    emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.pref_sendlogs_intent_title)));
                    return true;
                }
            });
        }
    }
}
