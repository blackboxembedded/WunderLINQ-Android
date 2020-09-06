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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import com.rarepebble.colorpicker.ColorPreference;

public class SettingsActivity extends AppCompatActivity {

    private final static String TAG = "SettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new UserSettingActivityFragment()).commit();
    }

    public static class UserSettingActivityFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (preference instanceof ColorPreference) {
                ((ColorPreference) preference).showDialog(this, 0);
            } else super.onDisplayPreferenceDialog(preference);
        }

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            EditTextPreference addressPref = findPreference("prefHomeAddress");
            addressPref.setSummary(sharedPrefs.getString("prefHomeAddress",getString(R.string.pref_homeAddress_summary)));

            EditTextPreference favNumberPref = findPreference("prefHomePhone");
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
