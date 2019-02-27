package com.blackboxembedded.WunderLINQ;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SettingsActivity extends PreferenceActivity{

    private final static String TAG = "SettingsActivity";

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

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            EditTextPreference addressPref = (EditTextPreference) findPreference("prefHomeAddress");
            addressPref.setSummary(sharedPrefs.getString("prefHomeAddress",getString(R.string.pref_homeAddress_summary)));

            EditTextPreference favNumberPref = (EditTextPreference) findPreference("prefHomePhone");
            favNumberPref.setSummary(sharedPrefs.getString("prefHomePhone",getString(R.string.pref_homePhone_summary)));

            if (Build.VERSION.SDK_INT < 26) {
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                Preference pipPreference = findPreference("prefPIP");
                Preference pipOrientationPreference = findPreference("prefPIPorientation");
                Preference pipCellCountPreference = findPreference("prefPIPCellCount") ;
                preferenceScreen.removePreference(pipPreference);
                preferenceScreen.removePreference(pipOrientationPreference);
                preferenceScreen.removePreference(pipCellCountPreference);
            }
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
