package com.blackboxembedded.WunderLINQ;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

public class HWSettingsUSBActivity extends AppCompatActivity {

    private Spinner usbControlSpinner;
    private Button saveBT;
    private Button cancelBT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hwsettings_usb);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        saveBT = findViewById(R.id.btSave);
        cancelBT = findViewById(R.id.btCancel);

        saveBT.setOnClickListener(mClickListener);
        cancelBT.setOnClickListener(mClickListener);

        usbControlSpinner = findViewById(R.id.spUSBMode);

        showActionBar();

        updateDisplay();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.action_back) {
                // Go back
                Intent backIntent = new Intent(HWSettingsUSBActivity.this, HWSettingsCustomizeActivity.class);
                startActivity(backIntent);
            } else if (v.getId() == R.id.btSave) {
                // Save
                Intent backIntent = new Intent(HWSettingsUSBActivity.this, HWSettingsCustomizeActivity.class);
                byte[] changes = {0x00,0x00};
                int selection = usbControlSpinner.getSelectedItemPosition();
                switch (usbControlSpinner.getSelectedItemPosition()) {
                    case 1:
                        //Engine
                        changes[0] = (byte)0x02;
                        changes[1] = (byte)0xBC;
                        break;
                    case 2:
                        //OFF
                        changes[0] = (byte)0xFF;
                        changes[1] = (byte)0xFF;
                        break;
                }
                backIntent.putExtra("ACTION", FWConfig.USB);
                backIntent.putExtra("UPDATES", changes);
                startActivity(backIntent);
            } else if (v.getId() == R.id.btCancel) {
                // Cancel
                Intent backIntent = new Intent(HWSettingsUSBActivity.this, HWSettingsCustomizeActivity.class);
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
        navbarTitle.setText(getString(R.string.usb_threshold_label));

        ImageButton backButton = findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton forwardButton = findViewById(R.id.action_forward);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private void updateDisplay(){
        if(FWConfig.USBVinThreshold == 0){
            usbControlSpinner.setSelection(0);
        } else if(FWConfig.USBVinThreshold == 65535){
            usbControlSpinner.setSelection(2);
        } else {
            usbControlSpinner.setSelection(1);
        }
    }

}