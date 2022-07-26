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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import com.rarepebble.colorpicker.ColorPreference;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private final static String TAG = "SettingsActivity";

    private static PreferenceScreen root;
    private static PreferenceScreen last;

    ImageButton backButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new UserSettingActivityFragment()).commit();
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
                    getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new UserSettingActivityFragment()).commit();
                    last = root;
                }
            }
        }
    };

    public static class UserSettingActivityFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener
    {
        EditTextPreference addressPref;
        EditTextPreference favNumberPref;
        ListPreference prefCellOne;
        ListPreference prefCellTwo;
        ListPreference prefCellThree;
        ListPreference prefCellFour;
        ListPreference prefCellFive;
        ListPreference prefCellSix;
        ListPreference prefCellSeven;
        ListPreference prefCellEight;
        ListPreference prefCellNine;
        ListPreference prefCellTen;
        ListPreference prefCellEleven;
        ListPreference prefCellTwelve;
        ListPreference prefCellThirteen;
        ListPreference prefCellFourteen;
        ListPreference prefCellFifteen;
        ListPreference prefQuickTaskOne;
        ListPreference prefQuickTaskTwo;
        ListPreference prefQuickTaskThree;
        ListPreference prefQuickTaskFour;
        ListPreference prefQuickTaskFive;
        ListPreference prefQuickTaskSix;
        ListPreference prefQuickTaskSeven;
        ListPreference prefQuickTaskEight;
        ListPreference prefQuickTaskNine;
        ListPreference prefQuickTaskTen;
        ListPreference prefQuickTaskEleven;
        ListPreference prefQuickTaskTwelve;
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
        ListPreference prefNavApp;
        ListPreference prefRoadBookApp;
        ListPreference prefActionCam;
        ListPreference prefPIPorientation;
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
            prefCellOne = findPreference("prefCellOne");
            prefCellOne.setSummary(prefCellOne.getEntry());
            prefCellTwo = findPreference("prefCellTwo");
            prefCellTwo.setSummary(prefCellTwo.getEntry());
            prefCellThree = findPreference("prefCellThree");
            prefCellThree.setSummary(prefCellThree.getEntry());
            prefCellFour = findPreference("prefCellFour");
            prefCellFour.setSummary(prefCellFour.getEntry());
            prefCellFive = findPreference("prefCellFive");
            prefCellFive.setSummary(prefCellFive.getEntry());
            prefCellSix = findPreference("prefCellSix");
            prefCellSix.setSummary(prefCellSix.getEntry());
            prefCellSeven = findPreference("prefCellSeven");
            prefCellSeven.setSummary(prefCellSeven.getEntry());
            prefCellEight = findPreference("prefCellEight");
            prefCellEight.setSummary(prefCellEight.getEntry());
            prefCellNine = findPreference("prefCellNine");
            prefCellNine.setSummary(prefCellNine.getEntry());
            prefCellTen = findPreference("prefCellTen");
            prefCellTen.setSummary(prefCellTen.getEntry());
            prefCellEleven = findPreference("prefCellEleven");
            prefCellEleven.setSummary(prefCellEleven.getEntry());
            prefCellTwelve = findPreference("prefCellTwelve");
            prefCellTwelve.setSummary(prefCellTwelve.getEntry());
            prefCellThirteen = findPreference("prefCellThirteen");
            prefCellThirteen.setSummary(prefCellThirteen.getEntry());
            prefCellFourteen = findPreference("prefCellFourteen");
            prefCellFourteen.setSummary(prefCellFourteen.getEntry());
            prefCellFifteen = findPreference("prefCellFifteen");
            prefCellFifteen.setSummary(prefCellFifteen.getEntry());
            prefQuickTaskOne = findPreference("prefQuickTaskOne");
            prefQuickTaskOne.setSummary(prefQuickTaskOne.getEntry());
            prefQuickTaskTwo = findPreference("prefQuickTaskTwo");
            prefQuickTaskTwo.setSummary(prefQuickTaskTwo.getEntry());
            prefQuickTaskThree = findPreference("prefQuickTaskThree");
            prefQuickTaskThree.setSummary(prefQuickTaskThree.getEntry());
            prefQuickTaskFour = findPreference("prefQuickTaskFour");
            prefQuickTaskFour.setSummary(prefQuickTaskFour.getEntry());
            prefQuickTaskFive = findPreference("prefQuickTaskFive");
            prefQuickTaskFive.setSummary(prefQuickTaskFive.getEntry());
            prefQuickTaskSix = findPreference("prefQuickTaskSix");
            prefQuickTaskSix.setSummary(prefQuickTaskSix.getEntry());
            prefQuickTaskSeven = findPreference("prefQuickTaskSeven");
            prefQuickTaskSeven.setSummary(prefQuickTaskSeven.getEntry());
            prefQuickTaskEight = findPreference("prefQuickTaskEight");
            prefQuickTaskEight.setSummary(prefQuickTaskEight.getEntry());
            prefQuickTaskNine = findPreference("prefQuickTaskNine");
            prefQuickTaskNine.setSummary(prefQuickTaskNine.getEntry());
            prefQuickTaskTen = findPreference("prefQuickTaskTen");
            prefQuickTaskTen.setSummary(prefQuickTaskTen.getEntry());
            prefQuickTaskEleven = findPreference("prefQuickTaskEleven");
            prefQuickTaskEleven.setSummary(prefQuickTaskEleven.getEntry());
            prefQuickTaskTwelve = findPreference("prefQuickTaskTwelve");
            prefQuickTaskTwelve.setSummary(prefQuickTaskTwelve.getEntry());
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
            prefNavApp = findPreference("prefNavApp");
            prefNavApp.setSummary(prefNavApp.getEntry());
            prefRoadBookApp = findPreference("prefRoadBookApp");
            prefRoadBookApp.setSummary(prefRoadBookApp.getEntry());
            prefActionCam = findPreference("prefActionCam");
            prefActionCam.setSummary(prefActionCam.getEntry());
            prefPIPorientation = findPreference("prefPIPorientation");
            prefPIPorientation.setSummary(prefPIPorientation.getEntry());
            prefPIPCellCount = findPreference("prefPIPCellCount");
            prefPIPCellCount.setSummary(prefPIPCellCount.getEntry());

            if (Build.VERSION.SDK_INT >= 24) {
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
            Log.d(TAG,"onSharedPreferenceChanged");
            if(key.equals("prefHomeAddress")) {
                addressPref.setSummary(sharedPreferences.getString(key, getString(R.string.pref_homeAddress_summary)));
            }
            if(key.equals("prefHomePhone")) {
                favNumberPref.setSummary(sharedPreferences.getString(key, getString(R.string.pref_homePhone_summary)));
            }
            if (key.equals("prefCellOne")){
                prefCellOne.setSummary(prefCellOne.getEntry());
            }
            if (key.equals("prefCellTwo")){
                prefCellTwo.setSummary(prefCellTwo.getEntry());
            }
            if (key.equals("prefCellThree")){
                prefCellThree.setSummary(prefCellThree.getEntry());
            }
            if (key.equals("prefCellFour")){
                prefCellFour.setSummary(prefCellFour.getEntry());
            }
            if (key.equals("prefCellFive")){
                prefCellFive.setSummary(prefCellFive.getEntry());
            }
            if (key.equals("prefCellSix")){
                prefCellSix.setSummary(prefCellSix.getEntry());
            }
            if (key.equals("prefCellSeven")){
                prefCellSeven.setSummary(prefCellSeven.getEntry());
            }
            if (key.equals("prefCellEight")){
                prefCellEight.setSummary(prefCellEight.getEntry());
            }
            if (key.equals("prefCellNine")){
                prefCellNine.setSummary(prefCellNine.getEntry());
            }
            if (key.equals("prefCellTen")){
                prefCellTen.setSummary(prefCellTen.getEntry());
            }
            if (key.equals("prefCellEleven")){
                prefCellEleven.setSummary(prefCellEleven.getEntry());
            }
            if (key.equals("prefCellTwelve")){
                prefCellTwelve.setSummary(prefCellTwelve.getEntry());
            }
            if (key.equals("prefCellThirteen")){
                prefCellThirteen.setSummary(prefCellThirteen.getEntry());
            }
            if (key.equals("prefCellFourteen")){
                prefCellFourteen.setSummary(prefCellFourteen.getEntry());
            }
            if (key.equals("prefCellFifteen")){
                prefCellFifteen.setSummary(prefCellFifteen.getEntry());
            }
            if (key.equals("prefQuickTaskOne")){
                prefQuickTaskOne.setSummary(prefQuickTaskOne.getEntry());
            }
            if (key.equals("prefQuickTaskTwo")){
                prefQuickTaskTwo.setSummary(prefQuickTaskTwo.getEntry());
            }
            if (key.equals("prefQuickTaskThree")){
                prefQuickTaskThree.setSummary(prefQuickTaskThree.getEntry());
            }
            if (key.equals("prefQuickTaskFour")){
                prefQuickTaskFour.setSummary(prefQuickTaskFour.getEntry());
            }
            if (key.equals("prefQuickTaskFive")){
                prefQuickTaskFive.setSummary(prefQuickTaskFive.getEntry());
            }
            if (key.equals("prefQuickTaskSix")){
                prefQuickTaskSix.setSummary(prefQuickTaskSix.getEntry());
            }
            if (key.equals("prefQuickTaskSeven")){
                prefQuickTaskSeven.setSummary(prefQuickTaskSeven.getEntry());
            }
            if (key.equals("prefQuickTaskEight")){
                prefQuickTaskEight.setSummary(prefQuickTaskEight.getEntry());
            }
            if (key.equals("prefQuickTaskNine")){
                prefQuickTaskNine.setSummary(prefQuickTaskNine.getEntry());
            }
            if (key.equals("prefQuickTaskTen")){
                prefQuickTaskTen.setSummary(prefQuickTaskTen.getEntry());
            }
            if (key.equals("prefQuickTaskEleven")){
                prefQuickTaskEleven.setSummary(prefQuickTaskEleven.getEntry());
            }
            if (key.equals("prefQuickTaskTwelve")){
                prefQuickTaskTwelve.setSummary(prefQuickTaskTwelve.getEntry());
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
            if (key.equals("prefNavApp")){
                prefNavApp.setSummary(prefNavApp.getEntry());
            }
            if (key.equals("prefRoadBookApp")){
                prefRoadBookApp.setSummary(prefRoadBookApp.getEntry());
            }
            if (key.equals("prefActionCam")){
                prefActionCam.setSummary(prefActionCam.getEntry());
            }
            if (key.equals("prefPIPorientation")){
                prefPIPorientation.setSummary(prefPIPorientation.getEntry());
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
