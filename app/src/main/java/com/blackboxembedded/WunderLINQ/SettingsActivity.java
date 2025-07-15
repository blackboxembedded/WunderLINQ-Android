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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import com.blackboxembedded.WunderLINQ.TaskList.TaskActivity;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.MotorcycleData;
import com.rarepebble.colorpicker.ColorPreference;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private final static String TAG = "SettingsActivity";

    private static PreferenceScreen root;
    private static PreferenceScreen last;

    ImageButton backButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings); // Use your new layout

        // Explicitly tell the system to avoid edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_fragment_container, new UserSettingActivityFragment())
                    .commit();
        }

        showActionBar();
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        caller.setPreferenceScreen(pref);
        last = pref;
        return true;
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
        navbarTitle.setText(R.string.appsettings_label);

        backButton = findViewById(R.id.action_back);
        ImageButton forwardButton = findViewById(R.id.action_forward);
        backButton.setOnClickListener(mClickListener);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.action_back) {
                Log.d(TAG, "last.getKey() == " + last.getKey());
                if (last.getKey().equals("prefScreenRoot")) {
                    Intent backIntent = new Intent(SettingsActivity.this, MainActivity.class);
                    startActivity(backIntent);
                } else {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.settings_fragment_container, new UserSettingActivityFragment())
                            .commit();
                    last = root;
                }
            }
        }
    };

    public static class UserSettingActivityFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener
    {
        EditTextPreference addressPref;
        EditTextPreference favNumberPref;
        ListPreference prefCell1;
        ListPreference prefCell2;
        ListPreference prefCell3;
        ListPreference prefCell4;
        ListPreference prefCell5;
        ListPreference prefCell6;
        ListPreference prefCell7;
        ListPreference prefCell8;
        ListPreference prefCell9;
        ListPreference prefCell10;
        ListPreference prefCell11;
        ListPreference prefCell12;
        ListPreference prefCell13;
        ListPreference prefCell14;
        ListPreference prefCell15;
        ListPreference prefQuickTask1;
        ListPreference prefQuickTask2;
        ListPreference prefQuickTask3;
        ListPreference prefQuickTask4;
        ListPreference prefQuickTask5;
        ListPreference prefQuickTask6;
        ListPreference prefQuickTask7;
        ListPreference prefQuickTask8;
        ListPreference prefQuickTask9;
        ListPreference prefQuickTask10;
        ListPreference prefQuickTask11;
        ListPreference prefQuickTask12;
        ListPreference prefQuickTask13;
        ListPreference prefQuickTask14;
        ListPreference prefQuickTask15;
        ListPreference prefQuickTask16;
        ListPreference prefQuickTask17;
        ListPreference prefQuickTask18;
        ListPreference prefRPMMax;
        ListPreference prefContactsFilter;
        ListPreference prefPressureF;
        ListPreference prefTempF;
        ListPreference prefDistance;
        ListPreference prefConsumption;
        ListPreference prefBearing;
        ListPreference prefTime;
        ListPreference prefOrientation;
        ListPreference prefNightModeCombo;
        ListPreference prefDashSpeedSource;
        ListPreference prefAppLaunchOptions;
        ListPreference prefNavApp;
        ListPreference prefRoadBookApp;
        ListPreference prefPIPOrientation;
        ListPreference prefPIPCellCount;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);
            root = this.getPreferenceScreen();
            last = root;
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
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            addressPref = findPreference("prefHomeAddress");
            addressPref.setSummary(sharedPreferences.getString("prefHomeAddress",getString(R.string.pref_homeAddress_summary)));
            favNumberPref = findPreference("prefHomePhone");
            favNumberPref.setSummary(sharedPreferences.getString("prefHomePhone",getString(R.string.pref_homePhone_summary)));
            prefCell1 = findPreference("prefCell1");
            prefCell1.setSummary(prefCell1.getEntry());
            prefCell1.setDefaultValue(MotorcycleData.defaultCellData[0]);
            prefCell2 = findPreference("prefCell2");
            prefCell2.setSummary(prefCell2.getEntry());
            prefCell2.setDefaultValue(MotorcycleData.defaultCellData[1]);
            prefCell3 = findPreference("prefCell3");
            prefCell3.setSummary(prefCell3.getEntry());
            prefCell3.setDefaultValue(MotorcycleData.defaultCellData[2]);
            prefCell4 = findPreference("prefCell4");
            prefCell4.setSummary(prefCell4.getEntry());
            prefCell4.setDefaultValue(MotorcycleData.defaultCellData[3]);
            prefCell5 = findPreference("prefCell5");
            prefCell5.setSummary(prefCell5.getEntry());
            prefCell5.setDefaultValue(MotorcycleData.defaultCellData[4]);
            prefCell6 = findPreference("prefCell6");
            prefCell6.setSummary(prefCell6.getEntry());
            prefCell6.setDefaultValue(MotorcycleData.defaultCellData[5]);
            prefCell7 = findPreference("prefCell7");
            prefCell7.setSummary(prefCell7.getEntry());
            prefCell7.setDefaultValue(MotorcycleData.defaultCellData[6]);
            prefCell8 = findPreference("prefCell8");
            prefCell8.setSummary(prefCell8.getEntry());
            prefCell8.setDefaultValue(MotorcycleData.defaultCellData[7]);
            prefCell9 = findPreference("prefCell9");
            prefCell9.setSummary(prefCell9.getEntry());
            prefCell9.setDefaultValue(MotorcycleData.defaultCellData[8]);
            prefCell10 = findPreference("prefCell10");
            prefCell10.setSummary(prefCell10.getEntry());
            prefCell10.setDefaultValue(MotorcycleData.defaultCellData[9]);
            prefCell11 = findPreference("prefCell11");
            prefCell11.setSummary(prefCell11.getEntry());
            prefCell11.setDefaultValue(MotorcycleData.defaultCellData[10]);
            prefCell12 = findPreference("prefCell12");
            prefCell12.setSummary(prefCell12.getEntry());
            prefCell12.setDefaultValue(MotorcycleData.defaultCellData[11]);
            prefCell13 = findPreference("prefCell13");
            prefCell13.setSummary(prefCell13.getEntry());
            prefCell13.setDefaultValue(MotorcycleData.defaultCellData[12]);
            prefCell14 = findPreference("prefCell14");
            prefCell14.setSummary(prefCell14.getEntry());
            prefCell14.setDefaultValue(MotorcycleData.defaultCellData[13]);
            prefCell15 = findPreference("prefCell15");
            prefCell15.setSummary(prefCell15.getEntry());
            prefCell15.setDefaultValue(MotorcycleData.defaultCellData[14]);
            prefQuickTask1 = findPreference("prefQuickTask1");
            prefQuickTask1.setSummary(prefQuickTask1.getEntry());
            prefQuickTask1.setDefaultValue(TaskActivity.defaultTaskData[0]);
            prefQuickTask2 = findPreference("prefQuickTask2");
            prefQuickTask2.setSummary(prefQuickTask2.getEntry());
            prefQuickTask2.setDefaultValue(TaskActivity.defaultTaskData[1]);
            prefQuickTask3 = findPreference("prefQuickTask3");
            prefQuickTask3.setSummary(prefQuickTask3.getEntry());
            prefQuickTask3.setDefaultValue(TaskActivity.defaultTaskData[2]);
            prefQuickTask4 = findPreference("prefQuickTask4");
            prefQuickTask4.setSummary(prefQuickTask4.getEntry());
            prefQuickTask4.setDefaultValue(TaskActivity.defaultTaskData[3]);
            prefQuickTask5 = findPreference("prefQuickTask5");
            prefQuickTask5.setSummary(prefQuickTask5.getEntry());
            prefQuickTask5.setDefaultValue(TaskActivity.defaultTaskData[4]);
            prefQuickTask6 = findPreference("prefQuickTask6");
            prefQuickTask6.setSummary(prefQuickTask6.getEntry());
            prefQuickTask6.setDefaultValue(TaskActivity.defaultTaskData[5]);
            prefQuickTask7 = findPreference("prefQuickTask7");
            prefQuickTask7.setSummary(prefQuickTask7.getEntry());
            prefQuickTask7.setDefaultValue(TaskActivity.defaultTaskData[6]);
            prefQuickTask8 = findPreference("prefQuickTask8");
            prefQuickTask8.setSummary(prefQuickTask8.getEntry());
            prefQuickTask8.setDefaultValue(TaskActivity.defaultTaskData[7]);
            prefQuickTask9 = findPreference("prefQuickTask9");
            prefQuickTask9.setSummary(prefQuickTask9.getEntry());
            prefQuickTask9.setDefaultValue(TaskActivity.defaultTaskData[8]);
            prefQuickTask10 = findPreference("prefQuickTask10");
            prefQuickTask10.setSummary(prefQuickTask10.getEntry());
            prefQuickTask10.setDefaultValue(TaskActivity.defaultTaskData[9]);
            prefQuickTask11 = findPreference("prefQuickTask11");
            prefQuickTask11.setSummary(prefQuickTask11.getEntry());
            prefQuickTask11.setDefaultValue(TaskActivity.defaultTaskData[10]);
            prefQuickTask12 = findPreference("prefQuickTask12");
            prefQuickTask12.setSummary(prefQuickTask12.getEntry());
            prefQuickTask12.setDefaultValue(TaskActivity.defaultTaskData[11]);
            prefQuickTask13 = findPreference("prefQuickTask13");
            prefQuickTask13.setSummary(prefQuickTask13.getEntry());
            prefQuickTask13.setDefaultValue(TaskActivity.defaultTaskData[12]);
            prefQuickTask14 = findPreference("prefQuickTask14");
            prefQuickTask14.setSummary(prefQuickTask14.getEntry());
            prefQuickTask14.setDefaultValue(TaskActivity.defaultTaskData[13]);
            prefQuickTask15 = findPreference("prefQuickTask15");
            prefQuickTask15.setSummary(prefQuickTask15.getEntry());
            prefQuickTask15.setDefaultValue(TaskActivity.defaultTaskData[14]);
            prefQuickTask16 = findPreference("prefQuickTask16");
            prefQuickTask16.setSummary(prefQuickTask16.getEntry());
            prefQuickTask16.setDefaultValue(TaskActivity.defaultTaskData[15]);
            prefQuickTask17 = findPreference("prefQuickTask17");
            prefQuickTask17.setSummary(prefQuickTask17.getEntry());
            prefQuickTask17.setDefaultValue(TaskActivity.defaultTaskData[16]);
            prefQuickTask18 = findPreference("prefQuickTask18");
            prefQuickTask18.setSummary(prefQuickTask18.getEntry());
            prefQuickTask18.setDefaultValue(TaskActivity.defaultTaskData[17]);
            prefRPMMax = findPreference("prefRPMMax");
            prefRPMMax.setSummary(prefRPMMax.getEntry());
            prefContactsFilter = findPreference("prefContactsFilter");
            prefContactsFilter.setSummary(prefContactsFilter.getEntry());
            prefPressureF = findPreference("prefPressureF");
            prefPressureF.setSummary(prefPressureF.getEntry());
            prefTempF = findPreference("prefTempF");
            prefTempF.setSummary(prefTempF.getEntry());
            prefDistance = findPreference("prefDistance");
            prefDistance.setSummary(prefDistance.getEntry());
            prefConsumption = findPreference("prefConsumption");
            prefConsumption.setSummary(prefConsumption.getEntry());
            prefBearing = findPreference("prefBearing");
            prefBearing.setSummary(prefBearing.getEntry());
            prefTime = findPreference("prefTime");
            prefTime.setSummary(prefTime.getEntry());
            prefOrientation = findPreference("prefOrientation");
            prefOrientation.setSummary(prefOrientation.getEntry());
            prefNightModeCombo = findPreference("prefNightModeCombo");
            prefNightModeCombo.setSummary(prefNightModeCombo.getEntry());
            prefDashSpeedSource = findPreference("prefDashSpeedSource");
            prefDashSpeedSource.setSummary(prefDashSpeedSource.getEntry());
            prefAppLaunchOptions = findPreference("prefAppLaunchOptions");
            prefAppLaunchOptions.setSummary(prefAppLaunchOptions.getEntry());
            prefNavApp = findPreference("prefNavApp");
            prefNavApp.setSummary(prefNavApp.getEntry());
            prefRoadBookApp = findPreference("prefRoadBookApp");
            prefRoadBookApp.setSummary(prefRoadBookApp.getEntry());
            prefPIPOrientation = findPreference("prefPIPOrientation");
            prefPIPOrientation.setSummary(prefPIPOrientation.getEntry());
            prefPIPCellCount = findPreference("prefPIPCellCount");
            prefPIPCellCount.setSummary(prefPIPCellCount.getEntry());

            PreferenceScreen preferenceScreen = getPreferenceScreen();
            Preference pipPreference = findPreference("prefPIP");
            Preference pipOrientationPreference = findPreference("prefPIPOrientation");
            Preference pipCellCountPreference = findPreference("prefPIPCellCount") ;
            preferenceScreen.removePreference(pipPreference);
            preferenceScreen.removePreference(pipOrientationPreference);
            preferenceScreen.removePreference(pipCellCountPreference);
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
            Log.d(TAG,"onSharedPreferenceChanged");
            if(key.equals("prefHomeAddress")) {
                addressPref.setSummary(sharedPreferences.getString(key, getString(R.string.pref_homeAddress_summary)));
            }
            if(key.equals("prefHomePhone")) {
                favNumberPref.setSummary(sharedPreferences.getString(key, getString(R.string.pref_homePhone_summary)));
            }
            if (key.equals("prefCell1")){
                prefCell1.setSummary(prefCell1.getEntry());
            }
            if (key.equals("prefCell2")){
                prefCell2.setSummary(prefCell2.getEntry());
            }
            if (key.equals("prefCell3")){
                prefCell3.setSummary(prefCell3.getEntry());
            }
            if (key.equals("prefCell4")){
                prefCell4.setSummary(prefCell4.getEntry());
            }
            if (key.equals("prefCell5")){
                prefCell5.setSummary(prefCell5.getEntry());
            }
            if (key.equals("prefCell6")){
                prefCell6.setSummary(prefCell6.getEntry());
            }
            if (key.equals("prefCell7")){
                prefCell7.setSummary(prefCell7.getEntry());
            }
            if (key.equals("prefCell8")){
                prefCell8.setSummary(prefCell8.getEntry());
            }
            if (key.equals("prefCell9")){
                prefCell9.setSummary(prefCell9.getEntry());
            }
            if (key.equals("prefCell10")){
                prefCell10.setSummary(prefCell10.getEntry());
            }
            if (key.equals("prefCell11")){
                prefCell11.setSummary(prefCell11.getEntry());
            }
            if (key.equals("prefCell12")){
                prefCell12.setSummary(prefCell12.getEntry());
            }
            if (key.equals("prefCell13")){
                prefCell13.setSummary(prefCell13.getEntry());
            }
            if (key.equals("prefCell14")){
                prefCell14.setSummary(prefCell14.getEntry());
            }
            if (key.equals("prefCell15")){
                prefCell15.setSummary(prefCell15.getEntry());
            }
            if (key.equals("prefQuickTask1")){
                prefQuickTask1.setSummary(prefQuickTask1.getEntry());
            }
            if (key.equals("prefQuickTask2")){
                prefQuickTask2.setSummary(prefQuickTask2.getEntry());
            }
            if (key.equals("prefQuickTask3")){
                prefQuickTask3.setSummary(prefQuickTask3.getEntry());
            }
            if (key.equals("prefQuickTask4")){
                prefQuickTask4.setSummary(prefQuickTask4.getEntry());
            }
            if (key.equals("prefQuickTask5")){
                prefQuickTask5.setSummary(prefQuickTask5.getEntry());
            }
            if (key.equals("prefQuickTask6")){
                prefQuickTask6.setSummary(prefQuickTask6.getEntry());
            }
            if (key.equals("prefQuickTask7")){
                prefQuickTask7.setSummary(prefQuickTask7.getEntry());
            }
            if (key.equals("prefQuickTask8")){
                prefQuickTask8.setSummary(prefQuickTask8.getEntry());
            }
            if (key.equals("prefQuickTask9")){
                prefQuickTask9.setSummary(prefQuickTask9.getEntry());
            }
            if (key.equals("prefQuickTask10")){
                prefQuickTask10.setSummary(prefQuickTask10.getEntry());
            }
            if (key.equals("prefQuickTask11")){
                prefQuickTask11.setSummary(prefQuickTask11.getEntry());
            }
            if (key.equals("prefQuickTask12")){
                prefQuickTask12.setSummary(prefQuickTask12.getEntry());
            }
            if (key.equals("prefQuickTask13")){
                prefQuickTask13.setSummary(prefQuickTask13.getEntry());
            }
            if (key.equals("prefQuickTask14")){
                prefQuickTask14.setSummary(prefQuickTask14.getEntry());
            }
            if (key.equals("prefQuickTask15")){
                prefQuickTask15.setSummary(prefQuickTask15.getEntry());
            }
            if (key.equals("prefQuickTask16")){
                prefQuickTask16.setSummary(prefQuickTask16.getEntry());
            }
            if (key.equals("prefQuickTask17")){
                prefQuickTask17.setSummary(prefQuickTask17.getEntry());
            }
            if (key.equals("prefQuickTask18")){
                prefQuickTask18.setSummary(prefQuickTask18.getEntry());
            }
            if (key.equals("prefRPMMax")){
                prefRPMMax.setSummary(prefRPMMax.getEntry());
            }
            if (key.equals("prefContactsFilter")){
                prefContactsFilter.setSummary(prefContactsFilter.getEntry());
            }
            if (key.equals("prefPressureF")){
                prefPressureF.setSummary(prefPressureF.getEntry());
            }
            if (key.equals("prefTempF")){
                prefTempF.setSummary(prefTempF.getEntry());
            }
            if (key.equals("prefDistance")){
                prefDistance.setSummary(prefDistance.getEntry());
            }
            if (key.equals("prefConsumption")){
                prefConsumption.setSummary(prefConsumption.getEntry());
            }
            if (key.equals("prefBearing")){
                prefBearing.setSummary(prefBearing.getEntry());
            }
            if (key.equals("prefTime")){
                prefTime.setSummary(prefTime.getEntry());
            }
            if (key.equals("prefOrientation")){
                prefOrientation.setSummary(prefOrientation.getEntry());
            }
            if (key.equals("prefNightModeCombo")){
                prefNightModeCombo.setSummary(prefNightModeCombo.getEntry());
            }
            if (key.equals("prefDashSpeedSource")){
                prefDashSpeedSource.setSummary(prefDashSpeedSource.getEntry());
            }
            if (key.equals("prefAppLaunchOptions")){
                prefAppLaunchOptions.setSummary(prefAppLaunchOptions.getEntry());
            }
            if (key.equals("prefNavApp")){
                prefNavApp.setSummary(prefNavApp.getEntry());
            }
            if (key.equals("prefRoadBookApp")){
                prefRoadBookApp.setSummary(prefRoadBookApp.getEntry());
            }
            if (key.equals("prefPIPOrientation")){
                prefPIPOrientation.setSummary(prefPIPOrientation.getEntry());
            }
            if (key.equals("prefPIPCellCount")){
                prefPIPCellCount.setSummary(prefPIPCellCount.getEntry());
            }
        }

        @Override
        public Fragment getCallbackFragment() {
            return this;
        }

        public void goToRoot(){
            setPreferenceScreen(root);
        }
    }
}
