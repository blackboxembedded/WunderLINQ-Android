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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

public class HWSettingsActionActivity extends AppCompatActivity {

    private final static String TAG = "HWSettingsActionAct";

    private int action = 0;

    private TextView action1LabelTV;
    private TextView action2LabelTV;
    private Spinner action1TypeSP;
    private Spinner action2TypeSP;
    private Spinner action1KeySP;
    private Spinner action2KeySP;
    private MultiSpinner action1ModifiersSP;
    private MultiSpinner action2ModifiersSP;
    private Button saveBT;
    private Button cancelBT;

    private ArrayAdapter<String>  keyboard;
    private ArrayAdapter<String>  consumer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hwsettings_action);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            action = extras.getInt("ACTION");
            Log.d(TAG,"Editing Action: " + action);
        }

        action1LabelTV = findViewById(R.id.tvAction1Label);
        action2LabelTV = findViewById(R.id.tvAction2Label);
        action1TypeSP = findViewById(R.id.spType1);
        action2TypeSP = findViewById(R.id.spType2);
        action1KeySP = findViewById(R.id.spKey1);
        action2KeySP = findViewById(R.id.spKey2);
        action1ModifiersSP = findViewById(R.id.msModifiers1);
        action2ModifiersSP = findViewById(R.id.msModifiers2);
        saveBT = findViewById(R.id.btSave);
        cancelBT = findViewById(R.id.btCancel);

        keyboard = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.hid_keyboard_usage_table_names_array));
        consumer = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.hid_consumer_usage_table_names_array));

        action1TypeSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                if (pos == 0){
                    action1KeySP.setVisibility(View.INVISIBLE);
                    action1ModifiersSP.setVisibility(View.INVISIBLE);
                } else if (pos == 1){
                    action1KeySP.setVisibility(View.VISIBLE);
                    action1ModifiersSP.setVisibility(View.VISIBLE);
                    int position = action1KeySP.getSelectedItemPosition();
                    action1KeySP.setAdapter(keyboard);
                    action1KeySP.setSelection(position);
                } else if (pos == 2){
                    action1KeySP.setVisibility(View.VISIBLE);
                    action1ModifiersSP.setVisibility(View.INVISIBLE);
                    int position = action1KeySP.getSelectedItemPosition();
                    action1KeySP.setAdapter(consumer);
                    action1KeySP.setSelection(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        action2TypeSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                if (pos == 0){
                    action2KeySP.setVisibility(View.INVISIBLE);
                    action2ModifiersSP.setVisibility(View.INVISIBLE);
                } else if (pos == 1){
                    action2KeySP.setVisibility(View.VISIBLE);
                    action2ModifiersSP.setVisibility(View.VISIBLE);
                    int position = action2KeySP.getSelectedItemPosition();
                    action2KeySP.setAdapter(keyboard);
                    action2KeySP.setSelection(position);
                } else if (pos == 2){
                    action2KeySP.setVisibility(View.VISIBLE);
                    action2ModifiersSP.setVisibility(View.INVISIBLE);
                    int position = action2KeySP.getSelectedItemPosition();
                    action2KeySP.setAdapter(consumer);
                    action2KeySP.setSelection(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        saveBT.setOnClickListener(mClickListener);
        cancelBT.setOnClickListener(mClickListener);

        showActionBar();

        updateDisplay();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.action_back) {
                // Go back
                Intent backIntent = new Intent(HWSettingsActionActivity.this, HWSettingsCustomizeActivity.class);
                startActivity(backIntent);
            } else if (v.getId() == R.id.btSave) {
                Intent backIntent = new Intent(HWSettingsActionActivity.this, HWSettingsCustomizeActivity.class);
                byte[] changes = {0x00,0x00,0x00,0x00,0x00,0x00};
                changes[0] = (byte)action1TypeSP.getSelectedItemPosition();
                if (changes[0] == FWConfig.KEYBOARD_HID) {
                    changes[2] = Integer.decode(getResources().getStringArray(R.array.hid_keyboard_usage_table_codes_array)[action1KeySP.getSelectedItemPosition()]).byteValue();
                    int i = -1;
                    for (boolean cc: action1ModifiersSP.selected) {
                        i++;
                        if (cc) {
                            changes[1] = (byte) (Integer.decode(getResources().getStringArray(R.array.hid_keyboard_modifier_usage_table_codes_array)[i]).byteValue() + changes[1]);
                        }
                    }
                } else if (changes[0] == FWConfig.CONSUMER_HID) {
                    changes[2] = Integer.decode(getResources().getStringArray(R.array.hid_consumer_usage_table_codes_array)[action1KeySP.getSelectedItemPosition()]).byteValue();
                }
                changes[3] = (byte)action2TypeSP.getSelectedItemPosition();
                if (changes[3] == FWConfig.KEYBOARD_HID) {
                    changes[5] = Integer.decode(getResources().getStringArray(R.array.hid_keyboard_usage_table_codes_array)[action2KeySP.getSelectedItemPosition()]).byteValue();
                    int i = -1;
                    for (boolean cc: action2ModifiersSP.selected) {
                        i++;
                        if (cc) {
                            changes[4] = (byte) (Integer.decode(getResources().getStringArray(R.array.hid_keyboard_modifier_usage_table_codes_array)[i]).byteValue() + changes[4]);
                        }
                    }
                } else if (changes[3] == FWConfig.CONSUMER_HID) {
                    changes[5] = Integer.decode(getResources().getStringArray(R.array.hid_consumer_usage_table_codes_array)[action2KeySP.getSelectedItemPosition()]).byteValue();
                }
                backIntent.putExtra("ACTION", action);
                backIntent.putExtra("UPDATES", changes);
                startActivity(backIntent);
            } else if (v.getId() == R.id.btCancel) {
                Intent backIntent = new Intent(HWSettingsActionActivity.this, HWSettingsCustomizeActivity.class);
                startActivity(backIntent);
            }
        }
    };

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(getString(R.string.keymode_custom_title));

        ImageButton backButton = findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton forwardButton = findViewById(R.id.action_forward);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private void updateDisplay(){
        if (action == FWConfig.RTKPage){
            action1LabelTV.setText(getString(R.string.rtk_page_label));
            action1TypeSP.setSelection(FWConfig.RTKPagePressKeyType);
            updateModifierSpinner1(FWConfig.RTKPagePressKeyModifier);
            if(FWConfig.RTKPagePressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKPagePressKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKPagePressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKPagePressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setText(getString(R.string.rtk_page_label) + " " + getString(R.string.rtk_double_press_label));
            action2TypeSP.setSelection(FWConfig.RTKPageDoublePressKeyType);
            updateModifierSpinner2(FWConfig.RTKPageDoublePressKeyModifier);
            if(FWConfig.RTKPageDoublePressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKPageDoublePressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKPageDoublePressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKPageDoublePressKey));
                action2ModifiersSP.setVisibility(View.INVISIBLE);
            }
        } else if (action == FWConfig.RTKZoomPlus){
            action1LabelTV.setText(getString(R.string.rtk_zoomp_label));
            action1TypeSP.setSelection(FWConfig.RTKZoomPPressKeyType);
            updateModifierSpinner1(FWConfig.RTKZoomPPressKeyModifier);
            if(FWConfig.RTKZoomPPressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKZoomPPressKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKZoomPPressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKZoomPPressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setText(getString(R.string.rtk_zoomp_label) + " " + getString(R.string.rtk_double_press_label));
            action2TypeSP.setSelection(FWConfig.RTKZoomPDoublePressKeyType);
            updateModifierSpinner2(FWConfig.RTKZoomPDoublePressKeyModifier);
            if(FWConfig.RTKZoomPDoublePressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKZoomPDoublePressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKZoomPDoublePressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKZoomPDoublePressKey));
                action2ModifiersSP.setVisibility(View.INVISIBLE);
            }
        } else if (action == FWConfig.RTKZoomMinus){
            action1LabelTV.setText(getString(R.string.rtk_zoomm_label));
            action1TypeSP.setSelection(FWConfig.RTKZoomMPressKeyType);
            updateModifierSpinner1(FWConfig.RTKZoomMPressKeyModifier);
            if(FWConfig.RTKZoomMPressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKZoomMPressKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKZoomMPressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKZoomMPressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setText(getString(R.string.rtk_zoomm_label) + " " + getString(R.string.rtk_double_press_label));
            action2TypeSP.setSelection(FWConfig.RTKZoomMDoublePressKeyType);
            updateModifierSpinner2(FWConfig.RTKZoomMDoublePressKeyModifier);
            if(FWConfig.RTKZoomMDoublePressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKZoomMDoublePressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKZoomMDoublePressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKZoomMDoublePressKey));
                action2ModifiersSP.setVisibility(View.INVISIBLE);
            }
        } else if (action == FWConfig.RTKSpeak){
            action1LabelTV.setText(getString(R.string.rtk_speak_label));
            action1TypeSP.setSelection(FWConfig.RTKSpeakPressKeyType);
            updateModifierSpinner1(FWConfig.RTKSpeakPressKeyModifier);
            if(FWConfig.RTKSpeakPressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKSpeakPressKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKSpeakPressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKSpeakPressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setText(getString(R.string.rtk_speak_label) + " " + getString(R.string.rtk_double_press_label));
            action2TypeSP.setSelection(FWConfig.RTKSpeakDoublePressKeyType);
            updateModifierSpinner2(FWConfig.RTKSpeakDoublePressKeyModifier);
            if(FWConfig.RTKSpeakDoublePressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKSpeakDoublePressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKSpeakDoublePressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKSpeakDoublePressKey));
                action2ModifiersSP.setVisibility(View.INVISIBLE);
            }
        } else if (action == FWConfig.RTKMute){
            action1LabelTV.setText(getString(R.string.rtk_mute_label));
            action1TypeSP.setSelection(FWConfig.RTKMutePressKeyType);
            updateModifierSpinner1(FWConfig.RTKMutePressKeyModifier);
            if(FWConfig.RTKMutePressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKMutePressKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKMutePressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKMutePressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setText(getString(R.string.rtk_mute_label) + " " + getString(R.string.rtk_double_press_label));
            action2TypeSP.setSelection(FWConfig.RTKMuteDoublePressKeyType);
            updateModifierSpinner2(FWConfig.RTKMuteDoublePressKeyModifier);
            if(FWConfig.RTKMuteDoublePressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKMuteDoublePressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKMuteDoublePressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKMuteDoublePressKey));
                action2ModifiersSP.setVisibility(View.INVISIBLE);
            }
        } else if (action == FWConfig.RTKDisplayOff){
            action1LabelTV.setText(getString(R.string.rtk_display_label));
            action1TypeSP.setSelection(FWConfig.RTKDisplayPressKeyType);
            updateModifierSpinner1(FWConfig.RTKDisplayPressKeyModifier);
            if(FWConfig.RTKDisplayPressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKDisplayPressKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKDisplayPressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKDisplayPressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setText(getString(R.string.rtk_display_label) + " " + getString(R.string.rtk_double_press_label));
            action2TypeSP.setSelection(FWConfig.RTKDisplayDoublePressKeyType);
            updateModifierSpinner2(FWConfig.RTKDisplayDoublePressKeyModifier);
            if(FWConfig.RTKDisplayDoublePressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.RTKDisplayDoublePressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.RTKDisplayDoublePressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.RTKDisplayDoublePressKey));
                action2ModifiersSP.setVisibility(View.INVISIBLE);
            }
        } else if (action == FWConfig.fullScrollUp){
            action1LabelTV.setText(getString(R.string.full_scroll_up_label));
            action1TypeSP.setSelection(FWConfig.fullScrollUpKeyType);
            updateModifierSpinner1(FWConfig.fullScrollUpKeyModifier);
            if(FWConfig.fullScrollUpKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.fullScrollUpKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.fullScrollUpKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.fullScrollUpKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setVisibility(View.INVISIBLE);
            action2TypeSP.setVisibility(View.INVISIBLE);
            action2KeySP.setVisibility(View.INVISIBLE);
            action2ModifiersSP.setVisibility(View.INVISIBLE);
        } else if (action == FWConfig.fullScrollDown){
            action1LabelTV.setText(getString(R.string.full_scroll_down_label));
            action1TypeSP.setSelection(FWConfig.fullScrollDownKeyType);
            updateModifierSpinner1(FWConfig.fullScrollDownKeyModifier);
            if(FWConfig.fullScrollDownKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.fullScrollDownKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.fullScrollDownKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.fullScrollDownKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setVisibility(View.INVISIBLE);
            action2TypeSP.setVisibility(View.INVISIBLE);
            action2KeySP.setVisibility(View.INVISIBLE);
            action2ModifiersSP.setVisibility(View.INVISIBLE);
        } else if (action == FWConfig.fullToggleRight){
            action1LabelTV.setText(getString(R.string.full_toggle_right_label));
            action1TypeSP.setSelection(FWConfig.fullRightPressKeyType);
            updateModifierSpinner1(FWConfig.fullRightPressKeyModifier);
            if(FWConfig.fullRightPressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.fullRightPressKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.fullRightPressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.fullRightPressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setText(getString(R.string.full_toggle_right_label) + " " + getString(R.string.full_long_press_label));
            action2TypeSP.setSelection(FWConfig.fullRightLongPressKeyType);
            updateModifierSpinner2(FWConfig.fullRightLongPressKeyModifier);
            if(FWConfig.fullRightLongPressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.fullRightLongPressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.fullRightLongPressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.fullRightLongPressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            }
        } else if (action == FWConfig.fullToggleLeft){
            action1LabelTV.setText(getString(R.string.full_toggle_left_label));
            action1TypeSP.setSelection(FWConfig.fullLeftPressKeyType);
            updateModifierSpinner1(FWConfig.fullLeftPressKeyModifier);
            if(FWConfig.fullLeftPressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setAdapter(keyboard);
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.fullLeftPressKey));
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.fullLeftPressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.fullLeftPressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }
            action2LabelTV.setText(getString(R.string.full_toggle_left_label) + " " + getString(R.string.full_long_press_label));
            action2TypeSP.setSelection(FWConfig.fullLeftLongPressKeyType);
            updateModifierSpinner2(FWConfig.fullLeftLongPressKeyModifier);
            if(FWConfig.fullLeftLongPressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.fullLeftLongPressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.fullLeftLongPressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.fullLeftLongPressKey));
                action2ModifiersSP.setVisibility(View.INVISIBLE);
            }
        } else if (action == FWConfig.fullSignalCancel){
            action1LabelTV.setText(getString(R.string.full_signal_cancel_label));
            action1TypeSP.setSelection(FWConfig.fullSignalPressKeyType);
            updateModifierSpinner1(FWConfig.fullSignalPressKeyModifier);
            if(FWConfig.fullSignalPressKeyType == FWConfig.KEYBOARD_HID){
                action1KeySP.setSelection(getKeyboardKeyByCode(FWConfig.fullSignalPressKey));
                action1KeySP.setAdapter(keyboard);
                action1ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.fullSignalPressKeyType == FWConfig.CONSUMER_HID){
                action1KeySP.setAdapter(consumer);
                action1KeySP.setSelection(getConsumerKeyByCode(FWConfig.fullSignalPressKey));
                action1ModifiersSP.setVisibility(View.INVISIBLE);
            }

            action2LabelTV.setText(getString(R.string.full_signal_cancel_label) + " " + getString(R.string.full_long_press_label));
            action2TypeSP.setSelection(FWConfig.fullSignalLongPressKeyType);
            updateModifierSpinner2(FWConfig.fullSignalLongPressKeyModifier);
            if(FWConfig.fullSignalLongPressKeyType == FWConfig.KEYBOARD_HID){
                action2KeySP.setAdapter(keyboard);
                action2KeySP.setSelection(getKeyboardKeyByCode(FWConfig.fullSignalLongPressKey));
                action2ModifiersSP.setVisibility(View.VISIBLE);
            } else if(FWConfig.fullSignalLongPressKeyType == FWConfig.CONSUMER_HID){
                action2KeySP.setAdapter(consumer);
                action2KeySP.setSelection(getConsumerKeyByCode(FWConfig.fullSignalLongPressKey));
                action2ModifiersSP.setVisibility(View.INVISIBLE);
            }
        }
    }

    private int getKeyboardKeyByCode(byte code) {
        int i = -1;
        for (String cc: getResources().getStringArray(R.array.hid_keyboard_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return i;
    }

    private int getConsumerKeyByCode(byte code) {
        int i = -1;
        for (String cc: getResources().getStringArray(R.array.hid_consumer_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return i;
    }

    private int getModifierKeyByCode(byte code) {
        int i = -1;
        for (String cc: getResources().getStringArray(R.array.hid_keyboard_modifier_usage_table_codes_array)) {
            i++;
            if (Integer.decode(cc) == (code & 0xFF))
                break;
        }
        return i;
    }

    private static boolean isSet(byte value, byte bit){
       return ( (value & bit) == bit );
    }

    private void updateModifierSpinner1(byte mask){
        if (mask != 0x00) {
            if (isSet(mask, (byte)0x01)) {
                action1ModifiersSP.selected[getModifierKeyByCode((byte) 0x01)] = true;
            }
            if (isSet(mask, (byte)0x02)) {
                action1ModifiersSP.selected[getModifierKeyByCode((byte) 0x02)] = true;
            }
            if (isSet(mask, (byte)0x04)) {
                action1ModifiersSP.selected[getModifierKeyByCode((byte) 0x04)] = true;
            }
            if (isSet(mask, (byte)0x08)) {
                action1ModifiersSP.selected[getModifierKeyByCode((byte) 0x08)] = true;
            }
            if (isSet(mask, (byte)0x10)) {
                action1ModifiersSP.selected[getModifierKeyByCode((byte) 0x10)] = true;
            }
            if (isSet(mask, (byte)0x20)) {
                action1ModifiersSP.selected[getModifierKeyByCode((byte) 0x20)] = true;
            }
            if (isSet(mask, (byte)0x40)) {
                action1ModifiersSP.selected[getModifierKeyByCode((byte) 0x40)] = true;
            }
            if (isSet(mask, (byte)0x80)) {
                action1ModifiersSP.selected[getModifierKeyByCode((byte) 0x80)] = true;
            }
            action1ModifiersSP.updateText();
        }
    }

    private void updateModifierSpinner2(byte mask){
        if (mask != 0x00) {
            if (isSet(mask, (byte)0x01)) {
                action2ModifiersSP.selected[getModifierKeyByCode((byte) 0x01)] = true;
            }
            if (isSet(mask, (byte)0x02)) {
                action2ModifiersSP.selected[getModifierKeyByCode((byte) 0x02)] = true;
            }
            if (isSet(mask, (byte)0x04)) {
                action2ModifiersSP.selected[getModifierKeyByCode((byte) 0x04)] = true;
            }
            if (isSet(mask, (byte)0x08)) {
                action2ModifiersSP.selected[getModifierKeyByCode((byte) 0x08)] = true;
            }
            if (isSet(mask, (byte)0x10)) {
                action2ModifiersSP.selected[getModifierKeyByCode((byte) 0x10)] = true;
            }
            if (isSet(mask, (byte)0x20)) {
                action2ModifiersSP.selected[getModifierKeyByCode((byte) 0x20)] = true;
            }
            if (isSet(mask, (byte)0x40)) {
                action2ModifiersSP.selected[getModifierKeyByCode((byte) 0x40)] = true;
            }
            if (isSet(mask, (byte)0x80)) {
                action2ModifiersSP.selected[getModifierKeyByCode((byte) 0x80)] = true;
            }
            action2ModifiersSP.updateText();
        }
    }
}