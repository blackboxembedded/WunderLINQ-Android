package com.badasscompany.NavLINq;


import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

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

        }
    }
}
