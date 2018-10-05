package com.blackboxembedded.WunderLINQ;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{

    private final static String TAG = "SettingsActivity";
    private static SharedPreferences sharedPrefs;
    static AlertDialog alertDialog;

    private static int versionButtonTouches = 0;

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        EditTextPreference addressPref = (EditTextPreference) findPreference("prefHomeAddress");
        addressPref.setSummary(sharedPreferences.getString("prefHomeAddress",getString(R.string.pref_homeAddress_summary)));

        EditTextPreference favNumberPref = (EditTextPreference) findPreference("prefHomePhone");
        favNumberPref.setSummary(sharedPreferences.getString("prefHomePhone",getString(R.string.pref_homePhone_summary)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new UserSettingActivityFragment()).commit();

    }

    public static class UserSettingActivityFragment extends PreferenceFragment
    {

        @Override
        public void onStop() {
            super.onStop();
            if (alertDialog != null) {
                alertDialog.dismiss();
                Log.d(TAG,"In onStop alertdialog.dismiss");
                alertDialog = null;
            }
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
            favNumberPref.setSummary(sharedPrefs.getString("prefHomePhone",getString(R.string.pref_homeAddress_summary)));

            if (!(sharedPrefs.getBoolean("DEBUG_ENABLED",false))){
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                PreferenceCategory myCategory = (PreferenceCategory) findPreference("prefDebugCategory");
                preferenceScreen.removePreference(myCategory);
            }

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

                    //send file using email
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    // set the type to 'email'
                    emailIntent.setType("text/plain");
                    String to[] = {getString(R.string.pref_sendlogs_email)};
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
                    // the attachment
                    Uri uri = FileProvider.getUriForFile(getActivity(), "com.blackboxembedded.wunderlinq.fileprovider", outputFile);
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

            //Secret Debug Menu
            String versionName = BuildConfig.VERSION_NAME;
            final Preference versionButton = findPreference("prefVersion");
            versionButton.setSummary(versionName);
            versionButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    versionButtonTouches = versionButtonTouches + 1;
                    if (versionButtonTouches == 10) {
                        Toast.makeText(getActivity(), R.string.pref_btn_version_toast, Toast.LENGTH_LONG).show();
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putBoolean("DEBUG_ENABLED", true);
                        editor.commit();
                    }
                    return true;
                }
            });
        }
    }

}
