package com.blackboxembedded.WunderLINQ;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SettingsActivity extends PreferenceActivity{

    private final static String TAG = "SettingsActivity";
    private static SharedPreferences sharedPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new UserSettingActivityFragment()).commit();
    }

    public static class UserSettingActivityFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
    {
        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            EditTextPreference addressPref = (EditTextPreference) findPreference("prefHomeAddress");
            addressPref.setSummary(sharedPrefs.getString("prefHomeAddress",getString(R.string.pref_homeAddress_summary)));

            EditTextPreference favNumberPref = (EditTextPreference) findPreference("prefHomePhone");
            favNumberPref.setSummary(sharedPrefs.getString("prefHomePhone",getString(R.string.pref_homePhone_summary)));

            Preference button = findPreference("prefSendLog");
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
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
                    String to[] = {getString(R.string.pref_sendlogs_email)};
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
                    // the attachment
                    //has to be an ArrayList
                    ArrayList<Uri> uris = new ArrayList<Uri>();
                    if(debugFile.exists()){
                        uris.add(FileProvider.getUriForFile(getActivity(), "com.blackboxembedded.wunderlinq.fileprovider", debugFile));
                    }
                    //convert from paths to Android friendly Parcelable Uri's
                    uris.add(FileProvider.getUriForFile(getActivity(), "com.blackboxembedded.wunderlinq.fileprovider", outputFile));
                    emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
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

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            EditTextPreference addressPref = (EditTextPreference) findPreference("prefHomeAddress");
            addressPref.setSummary(sharedPreferences.getString("prefHomeAddress",getString(R.string.pref_homeAddress_summary)));

            EditTextPreference favNumberPref = (EditTextPreference) findPreference("prefHomePhone");
            favNumberPref.setSummary(sharedPreferences.getString("prefHomePhone",getString(R.string.pref_homePhone_summary)));
        }
    }

}
