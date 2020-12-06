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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class HWSettingsCustomizeActivity extends AppCompatActivity {

    private final static String TAG = "HWSettingsCustomizeAct";

    private TextView wwModeLabelTV;
    private Spinner wwModeSpinner;
    private LinearLayout sensitivityLLayout;
    private TextView sensitivityLabelTV;
    private TextView sensitivityTV;
    private SeekBar sensitivitySeekBar;
    private ConstraintLayout actionOneCL;
    private ConstraintLayout actionTwoCL;
    private ConstraintLayout actionThreeCL;
    private ConstraintLayout actionFourCL;
    private ConstraintLayout actionFiveCL;
    private ConstraintLayout actionSixCL;
    private TextView actionOneLabelTV;
    private TextView actionTwoLabelTV;
    private TextView actionThreeLabelTV;
    private TextView actionFourLabelTV;
    private TextView actionFiveLabelTV;
    private TextView actionSixLabelTV;
    private ImageButton actionOneBtn;
    private ImageButton actionTwoBtn;
    private ImageButton actionThreeBtn;
    private ImageButton actionFourBtn;
    private ImageButton actionFiveBtn;
    private ImageButton actionSixBtn;
    private Button writeBtn;
    private PopupMenu mPopupMenu;

    private boolean firstStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hwcustomize);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int action = extras.getInt("ACTION");
            byte[] updates = extras.getByteArray("UPDATES");
            Log.d(TAG,"Saving Action: " + action);
            Log.d(TAG, "UPDATES: " + Utils.ByteArraytoHex(updates));
            if (action == FWConfig.USB) {
                FWConfig.tempConfig[FWConfig.USBVinThresholdHigh_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.USBVinThresholdLow_INDEX] = updates[1];
            } else if (action == FWConfig.RTKPage) {
                FWConfig.tempConfig[FWConfig.RTKPagePressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.RTKPagePressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.RTKPagePressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.RTKPageDoublePressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.RTKPageDoublePressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.RTKPageDoublePressKey_INDEX] = updates[5];
            } else if (action == FWConfig.RTKZoomPlus) {
                FWConfig.tempConfig[FWConfig.RTKZoomPPressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.RTKZoomPPressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.RTKZoomPPressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.RTKZoomPDoublePressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.RTKZoomPDoublePressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.RTKZoomPDoublePressKey_INDEX] = updates[5];
            } else if (action == FWConfig.RTKZoomMinus) {
                FWConfig.tempConfig[FWConfig.RTKZoomMPressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.RTKZoomMPressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.RTKZoomMPressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.RTKZoomMDoublePressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.RTKZoomMDoublePressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.RTKZoomMDoublePressKey_INDEX] = updates[5];
            } else if (action == FWConfig.RTKSpeak) {
                FWConfig.tempConfig[FWConfig.RTKSpeakPressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.RTKSpeakPressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.RTKSpeakPressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.RTKSpeakDoublePressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.RTKSpeakDoublePressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.RTKSpeakDoublePressKey_INDEX] = updates[5];
            } else if (action == FWConfig.RTKMute) {
                FWConfig.tempConfig[FWConfig.RTKMutePressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.RTKMutePressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.RTKMutePressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.RTKMuteDoublePressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.RTKMuteDoublePressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.RTKMuteDoublePressKey_INDEX] = updates[5];
            } else if (action == FWConfig.RTKDisplayOff) {
                FWConfig.tempConfig[FWConfig.RTKDisplayPressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.RTKDisplayPressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.RTKDisplayPressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.RTKDisplayDoublePressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.RTKDisplayDoublePressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.RTKDisplayDoublePressKey_INDEX] = updates[5];
            } else if (action == FWConfig.fullScrollUp) {
                FWConfig.tempConfig[FWConfig.fullScrollUpKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.fullScrollUpKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.fullScrollUpKey_INDEX] = updates[2];
            } else if (action == FWConfig.fullScrollDown) {
                FWConfig.tempConfig[FWConfig.fullScrollDownKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.fullScrollDownKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.fullScrollDownKey_INDEX] = updates[2];
            } else if (action == FWConfig.fullToggleRight) {
                FWConfig.tempConfig[FWConfig.fullRightPressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.fullRightPressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.fullRightPressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.fullRightLongPressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.fullRightLongPressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.fullRightLongPressKey_INDEX] = updates[5];
            } else if (action == FWConfig.fullToggleLeft) {
                FWConfig.tempConfig[FWConfig.fullLeftPressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.fullLeftPressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.fullLeftPressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.fullLeftLongPressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.fullLeftLongPressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.fullLeftLongPressKey_INDEX] = updates[5];
            } else if (action == FWConfig.fullSignalCancel) {
                FWConfig.tempConfig[FWConfig.fullSignalPressKeyType_INDEX] = updates[0];
                FWConfig.tempConfig[FWConfig.fullSignalPressKeyModifier_INDEX] = updates[1];
                FWConfig.tempConfig[FWConfig.fullSignalPressKey_INDEX] = updates[2];
                FWConfig.tempConfig[FWConfig.fullSignalLongPressKeyType_INDEX] = updates[3];
                FWConfig.tempConfig[FWConfig.fullSignalLongPressKeyModifier_INDEX] = updates[4];
                FWConfig.tempConfig[FWConfig.fullSignalLongPressKey_INDEX] = updates[5];
            }
        }

        wwModeLabelTV = findViewById(R.id.tvwwModeLabel);
        wwModeLabelTV.setVisibility(View.INVISIBLE);
        wwModeSpinner = findViewById(R.id.wwModeSpinner);
        wwModeSpinner.setVisibility(View.INVISIBLE);
        wwModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                Log.d(TAG, "Bike Mode Selected: " + adapterView.getItemAtPosition(pos).toString());
                updateDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        sensitivityLLayout = findViewById(R.id.llSensitivity);
        sensitivityLabelTV = findViewById(R.id.tvSensitivityLabel);
        sensitivityTV = findViewById(R.id.tvSensitivityValue);
        sensitivitySeekBar = findViewById(R.id.sbSensitivity);
        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivityTV.setText(String.valueOf(progress));
                if (FWConfig.firmwareVersion != null) {
                    if (Double.parseDouble(FWConfig.firmwareVersion) < 2.0) {
                        if (progress == FWConfig.sensitivity) {
                            writeBtn.setVisibility(View.INVISIBLE);
                        } else {
                            writeBtn.setVisibility(View.VISIBLE);
                        }
                    } else {
                        if (wwModeSpinner.getSelectedItemPosition() == 0) {
                            //Full
                            FWConfig.tempConfig[FWConfig.fullSensitivity_INDEX] = (byte)progress;
                        } else if (wwModeSpinner.getSelectedItemPosition() == 1) {
                            //RTK
                            FWConfig.tempConfig[FWConfig.RTKSensitivity_INDEX] = (byte)progress;
                        }
                        updateDisplay();
                    }
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        actionOneCL = findViewById(R.id.clActionOne);
        actionOneCL.setVisibility(View.INVISIBLE);
        actionTwoCL = findViewById(R.id.clActionTwo);
        actionTwoCL.setVisibility(View.INVISIBLE);
        actionThreeCL = findViewById(R.id.clActionThree);
        actionThreeCL.setVisibility(View.INVISIBLE);
        actionFourCL = findViewById(R.id.clActionFour);
        actionFourCL.setVisibility(View.INVISIBLE);
        actionFiveCL = findViewById(R.id.clActionFive);
        actionFiveCL.setVisibility(View.INVISIBLE);
        actionSixCL = findViewById(R.id.clActionSix);
        actionSixCL.setVisibility(View.INVISIBLE);
        actionOneLabelTV = findViewById(R.id.tvActionOneLabel);
        actionTwoLabelTV = findViewById(R.id.tvActionTwoLabel);
        actionThreeLabelTV = findViewById(R.id.tvActionThreeLabel);
        actionFourLabelTV = findViewById(R.id.tvActionFourLabel);
        actionFiveLabelTV = findViewById(R.id.tvActionFiveLabel);
        actionSixLabelTV = findViewById(R.id.tvActionSixLabel);
        actionOneBtn = findViewById(R.id.actionOneBtn);
        actionOneBtn.setOnClickListener(mClickListener);
        actionTwoBtn = findViewById(R.id.actionTwoBtn);
        actionTwoBtn.setOnClickListener(mClickListener);
        actionThreeBtn = findViewById(R.id.actionThreeBtn);
        actionThreeBtn.setOnClickListener(mClickListener);
        actionFourBtn = findViewById(R.id.actionFourBtn);
        actionFourBtn.setOnClickListener(mClickListener);
        actionFiveBtn = findViewById(R.id.actionFiveBtn);
        actionFiveBtn.setOnClickListener(mClickListener);
        actionSixBtn = findViewById(R.id.actionSixBtn);
        actionSixBtn.setOnClickListener(mClickListener);
        writeBtn = findViewById(R.id.writeBtn);
        writeBtn.setOnClickListener(mClickListener);
        writeBtn.setVisibility(View.INVISIBLE);

        showActionBar();
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "In onResume");
        super.onResume();
        updateDisplay();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"In onDestroy");
        super.onDestroy();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.resetBtn) {
                // Write FW Config
                // Display dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(HWSettingsCustomizeActivity.this);
                builder.setTitle(getString(R.string.hwsave_alert_title));
                builder.setMessage(getString(R.string.hwsave_alert_body));
                builder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (FWConfig.firmwareVersion != null) {
                                    if (Double.parseDouble(FWConfig.firmwareVersion) >= 2.0) {
                                        try {
                                            Log.d(TAG,"Sending write default 2.x config command");
                                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                            outputStream.write(FWConfig.WRITE_CONFIG_CMD);
                                            outputStream.write(FWConfig.defaultConfig2);
                                            outputStream.write(FWConfig.CMD_EOM);
                                            byte[] writeConfigCmd = outputStream.toByteArray();
                                            Log.d(TAG, "Command Sent: " + Utils.ByteArraytoHex(writeConfigCmd));
                                            MainActivity.gattCommandCharacteristic.setValue(writeConfigCmd);
                                            BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                            //finish();
                                        } catch (IOException e) {
                                            Log.d(TAG, e.toString());
                                        }
                                    } else {
                                        try {
                                            Log.d(TAG,"Sending write default 1.x config command");
                                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                            outputStream.write(FWConfig.WRITE_CONFIG_CMD);
                                            outputStream.write(FWConfig.defaultConfig1);
                                            outputStream.write(FWConfig.CMD_EOM);
                                            byte[] writeConfigCmd = outputStream.toByteArray();
                                            Log.d(TAG, "Command Sent: " + Utils.ByteArraytoHex(writeConfigCmd));
                                            MainActivity.gattCommandCharacteristic.setValue(writeConfigCmd);
                                            BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                            finish();
                                        } catch (IOException e) {
                                            Log.d(TAG, e.toString());
                                        }
                                    }
                                }
                            }
                        });
                builder.setNegativeButton(R.string.hwsave_alert_btn_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                builder.show();
            } else if (v.getId() == R.id.writeBtn) {
                // Write FW Config
                // Display dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(HWSettingsCustomizeActivity.this);
                builder.setTitle(getString(R.string.hwsave_alert_title));
                builder.setMessage(getString(R.string.hwsave_alert_body));
                builder.setPositiveButton(R.string.hwsave_alert_btn_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (FWConfig.firmwareVersion != null) {
                                    if (Double.parseDouble(FWConfig.firmwareVersion) >= 2.0) {
                                        if (!Arrays.equals(FWConfig.wunderLINQConfig, FWConfig.tempConfig)) {
                                            Log.d(TAG,"New Config found");
                                            Log.d(TAG, "tempConfig: " + Utils.ByteArraytoHex(FWConfig.tempConfig));
                                            try {
                                                Log.d(TAG,"Sending write config command");
                                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                                outputStream.write(FWConfig.WRITE_CONFIG_CMD);
                                                outputStream.write(FWConfig.tempConfig);
                                                outputStream.write(FWConfig.CMD_EOM);
                                                byte[] writeConfigCmd = outputStream.toByteArray();
                                                Log.d(TAG, "Command Sent: " + Utils.ByteArraytoHex(writeConfigCmd));
                                                MainActivity.gattCommandCharacteristic.setValue(writeConfigCmd);
                                                BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                               // finish();
                                            } catch (IOException e) {
                                                Log.d(TAG, e.toString());
                                            }
                                        } else {
                                            Log.d(TAG,"New Config not found");
                                        }
                                    } else {
                                        byte wwMode = FWConfig.wheelMode_full;
                                        // Get Selection
                                        if (wwModeSpinner.getSelectedItemPosition() > 0) {
                                            //RTK
                                            wwMode = FWConfig.wheelMode_rtk;
                                        }
                                        if (FWConfig.wheelMode == wwMode) {
                                            if (FWConfig.sensitivity != sensitivitySeekBar.getProgress()) {
                                                Log.d(TAG, "Setting Sensitivity");
                                                // Write sensitivity
                                                char[] sensitivityChar = String.valueOf(sensitivitySeekBar.getProgress()).toCharArray();
                                                char sensOne = sensitivityChar[0];
                                                try {
                                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                                    outputStream.write(FWConfig.WRITE_SENSITIVITY_CMD);
                                                    if (sensitivityChar.length == 1) {
                                                        outputStream.write(wwMode);
                                                        outputStream.write(0x45);
                                                        outputStream.write((byte) sensOne);
                                                        outputStream.write(FWConfig.CMD_EOM);
                                                        byte[] writeSensitivityCmd = outputStream.toByteArray();
                                                        MainActivity.gattCommandCharacteristic.setValue(writeSensitivityCmd);
                                                    } else {
                                                        char sensTwo = sensitivityChar[1];
                                                        outputStream.write(wwMode);
                                                        outputStream.write(0x45);
                                                        outputStream.write((byte) sensOne);
                                                        outputStream.write((byte) sensTwo);
                                                        outputStream.write(FWConfig.CMD_EOM);
                                                        byte[] writeSensitivityCmd = outputStream.toByteArray();
                                                        MainActivity.gattCommandCharacteristic.setValue(writeSensitivityCmd);
                                                    }
                                                    BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                                    finish();
                                                } catch (IOException e){
                                                    Log.d(TAG,e.toString());
                                                }
                                            }
                                        } else {
                                            Log.d(TAG, "Setting Mode");
                                            // Write mode
                                            try {
                                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                                outputStream.write(FWConfig.WRITE_MODE_CMD);
                                                outputStream.write(wwMode);
                                                outputStream.write(FWConfig.CMD_EOM);
                                                byte[] writeModeCmd = outputStream.toByteArray();
                                                MainActivity.gattCommandCharacteristic.setValue(writeModeCmd);
                                                BluetoothLeService.writeCharacteristic(MainActivity.gattCommandCharacteristic);
                                                finish();
                                            } catch (IOException e){
                                                Log.d(TAG,e.toString());
                                            }
                                        }
                                    }
                                }
                            }
                        });
                builder.setNegativeButton(R.string.hwsave_alert_btn_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                builder.show();
            } else if (v.getId() == R.id.action_back) {
                // Go back
                Intent backIntent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActivity.class);
                startActivity(backIntent);
            } else if (v.getId() == R.id.actionOneBtn) {
                if (wwModeSpinner.getSelectedItemPosition() == 0) {
                    //Full
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.fullScrollUp);
                    startActivity(intent);
                } else {
                    //RTK
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.RTKPage);
                    startActivity(intent);
                }
            } else if (v.getId() == R.id.actionTwoBtn) {
                if (wwModeSpinner.getSelectedItemPosition() == 0) {
                    //Full
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.fullScrollDown);
                    startActivity(intent);
                } else {
                    //RTK
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.RTKZoomPlus);
                    startActivity(intent);
                }
            } else if (v.getId() == R.id.actionThreeBtn) {
                if (wwModeSpinner.getSelectedItemPosition() == 0) {
                    //Full
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.fullToggleRight);
                    startActivity(intent);
                } else {
                    //RTK
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.RTKZoomMinus);
                    startActivity(intent);
                }
            } else if (v.getId() == R.id.actionFourBtn) {
                if (wwModeSpinner.getSelectedItemPosition() == 0) {
                    //Full
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.fullToggleLeft);
                    startActivity(intent);
                } else {
                    //RTK
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.RTKSpeak);
                    startActivity(intent);
                }
            } else if (v.getId() == R.id.actionFiveBtn) {
                if (wwModeSpinner.getSelectedItemPosition() == 0) {
                    //Full
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.fullSignalCancel);
                    startActivity(intent);
                } else {
                    //RTK
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.RTKMute);
                    startActivity(intent);
                }
            } else if (v.getId() == R.id.actionSixBtn) {
                if (wwModeSpinner.getSelectedItemPosition() == 0) {
                    //Full
                    //Nothing
                } else {
                    //RTK
                    //Display Off
                    Intent intent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsActionActivity.class);
                    intent.putExtra("ACTION", FWConfig.RTKDisplayOff);
                    startActivity(intent);
                }
            } else if (v.getId() == R.id.action_menu) {
                mPopupMenu.show();
            }
        }
    };

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav_menu, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.keymode_custom_title);

        ImageButton backButton = findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton menuButton = findViewById(R.id.action_menu);
        menuButton.setOnClickListener(mClickListener);

        mPopupMenu = new PopupMenu(this, menuButton);
        MenuInflater menuOtherInflater = mPopupMenu.getMenuInflater();
        menuOtherInflater.inflate(R.menu.menu_settings_customize, mPopupMenu.getMenu());
        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.action_usb_control:
                        Intent backIntent = new Intent(HWSettingsCustomizeActivity.this, HWSettingsUSBActivity.class);
                        startActivity(backIntent);
                        break;
                }
                return true;
            }
        });
    }

    private void updateDisplay() {
        if (FWConfig.firmwareVersion != null) {
            if (Double.parseDouble(FWConfig.firmwareVersion) >= 2.0) {
                //New View
                if (wwModeSpinner.getSelectedItemPosition() == 0) {
                    //Full
                    sensitivityLabelTV.setText(getString(R.string.long_press_label));
                    wwModeLabelTV.setVisibility(View.VISIBLE);
                    wwModeSpinner.setVisibility(View.VISIBLE);
                    sensitivitySeekBar.setMax(30);
                    sensitivityLLayout.setVisibility(View.VISIBLE);
                    sensitivitySeekBar.setVisibility(View.VISIBLE);
                    sensitivitySeekBar.setProgress(FWConfig.fullSensitivity);
                    actionOneLabelTV.setText(getString(R.string.full_scroll_up_label));
                    actionTwoLabelTV.setText(getString(R.string.full_scroll_down_label));
                    actionThreeLabelTV.setText(getString(R.string.full_toggle_right_label));
                    actionFourLabelTV.setText(getString(R.string.full_toggle_left_label));
                    actionFiveLabelTV.setText(getString(R.string.full_signal_cancel_label));
                    actionSixLabelTV.setText("");
                    actionSixCL.setVisibility(View.INVISIBLE);
                } else if (wwModeSpinner.getSelectedItemPosition() == 1) {
                    //RTK
                    sensitivityLabelTV.setText(getString(R.string.double_press_label));
                    wwModeLabelTV.setVisibility(View.VISIBLE);
                    wwModeSpinner.setVisibility(View.VISIBLE);
                    sensitivitySeekBar.setMax(20);
                    sensitivityLLayout.setVisibility(View.VISIBLE);
                    sensitivitySeekBar.setVisibility(View.VISIBLE);
                    sensitivitySeekBar.setProgress(FWConfig.RTKSensitivity);
                    actionOneLabelTV.setText(getString(R.string.rtk_page_label));
                    actionTwoLabelTV.setText(getString(R.string.rtk_zoomp_label));
                    actionThreeLabelTV.setText(getString(R.string.rtk_zoomm_label));
                    actionFourLabelTV.setText(getString(R.string.rtk_speak_label));
                    actionFiveLabelTV.setText(getString(R.string.rtk_mute_label));
                    actionSixLabelTV.setText(getString(R.string.rtk_display_label));
                    actionSixCL.setVisibility(View.VISIBLE);
                }

                actionOneCL.setVisibility(View.VISIBLE);
                actionTwoCL.setVisibility(View.VISIBLE);
                actionThreeCL.setVisibility(View.VISIBLE);
                actionFourCL.setVisibility(View.VISIBLE);
                actionFiveCL.setVisibility(View.VISIBLE);

                if (!Arrays.equals(FWConfig.wunderLINQConfig, FWConfig.tempConfig)){
                    writeBtn.setVisibility(View.VISIBLE);
                } else {
                    writeBtn.setVisibility(View.INVISIBLE);
                }

            } else {
                //Old View
                if (FWConfig.wheelMode == FWConfig.wheelMode_full || FWConfig.wheelMode == FWConfig.wheelMode_rtk) {
                    wwModeLabelTV.setVisibility(View.VISIBLE);
                    wwModeSpinner.setVisibility(View.VISIBLE);
                    if(firstStart){
                        firstStart = false;
                        Log.d(TAG, "First start");
                        if (FWConfig.wheelMode == FWConfig.wheelMode_full) {
                            wwModeSpinner.setSelection(0);
                        } else if (FWConfig.wheelMode == FWConfig.wheelMode_rtk) {
                            wwModeSpinner.setSelection(1);
                        }
                    }
                    if (wwModeSpinner.getSelectedItemPosition() == 0) {
                        //Full
                        sensitivityLabelTV.setText(getString(R.string.long_press_label));
                        if (FWConfig.wheelMode == FWConfig.wheelMode_full) {
                            sensitivitySeekBar.setMax(30);
                            sensitivitySeekBar.setProgress(FWConfig.sensitivity);
                            sensitivityLLayout.setVisibility(View.VISIBLE);
                            sensitivitySeekBar.setVisibility(View.VISIBLE);
                            writeBtn.setVisibility(View.INVISIBLE);
                        } else {
                            sensitivityLLayout.setVisibility(View.INVISIBLE);
                            sensitivitySeekBar.setVisibility(View.INVISIBLE);
                            writeBtn.setVisibility(View.VISIBLE);
                        }
                    } else if (wwModeSpinner.getSelectedItemPosition() == 1) {
                        //RTK
                        sensitivityLabelTV.setText(getString(R.string.double_press_label));
                        if (FWConfig.wheelMode == FWConfig.wheelMode_rtk) {
                            sensitivitySeekBar.setMax(20);
                            sensitivitySeekBar.setProgress(FWConfig.sensitivity);
                            sensitivityLLayout.setVisibility(View.VISIBLE);
                            sensitivitySeekBar.setVisibility(View.VISIBLE);
                            writeBtn.setVisibility(View.INVISIBLE);
                        } else {
                            sensitivityLLayout.setVisibility(View.INVISIBLE);
                            sensitivitySeekBar.setVisibility(View.INVISIBLE);
                            writeBtn.setVisibility(View.VISIBLE);
                        }
                    }
                }
                actionOneCL.setVisibility(View.INVISIBLE);
                actionTwoCL.setVisibility(View.INVISIBLE);
                actionThreeCL.setVisibility(View.INVISIBLE);
                actionFourCL.setVisibility(View.INVISIBLE);
                actionFiveCL.setVisibility(View.INVISIBLE);
                actionSixCL.setVisibility(View.INVISIBLE);
            }
        }
    }
}
