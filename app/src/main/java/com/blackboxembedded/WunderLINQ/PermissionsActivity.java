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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.os.Process.myUid;

public class PermissionsActivity extends AppCompatActivity {

    public final static String TAG = "PermissionsActivity";

    public static final int PERMISSION_LOCATION = 1;
    public static final int PERMISSION_CONTACTS = 2;
    public static final int PERMISSION_RECORD_AUDIO = 3;
    public static final int PERMISSION_CAMERA = 4;
    public static final int PERMISSION_WRITE_STORAGE = 5;
    public static final int PERMISSION_PHONE = 6;
    public static final int PERMISSION_NOTIFICATION = 7;
    public static final int PERMISSION_OVERLAY = 8;
    public static final int PERMISSION_USAGESTATS = 9;
    public static final int PERMISSION_ACCESSIBILITY = 10;

    private ListView permissionsList;
    List<PermissionRecord> listValues;
    ArrayAdapter<PermissionRecord> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppUtils.adjustDisplayScale(this, getResources().getConfiguration());
        setContentView(R.layout.activity_permissions);
        permissionsList = findViewById(R.id.lv_permissions);
        showActionBar();

        updateListing();

        adapter = new
                PermissionsListView(this, listValues);
        permissionsList.setAdapter(adapter);
        permissionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick (AdapterView < ? > adapter, View view, int position, long arg){
                switch (listValues.get(position).getID()){
                    case PERMISSION_LOCATION:
                        if (MyApplication.getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
                        }
                        break;
                    case PERMISSION_CONTACTS:
                        if (getApplication().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_CONTACTS);
                        }
                        break;
                    case PERMISSION_RECORD_AUDIO:
                        if (getApplication().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
                        }
                        break;
                    case PERMISSION_CAMERA:
                        if (getApplication().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                        }
                        break;
                    case PERMISSION_WRITE_STORAGE:
                        if (getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_STORAGE);
                        }
                        break;
                    case PERMISSION_PHONE:
                        if (getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_PHONE);
                        }
                        break;
                    case PERMISSION_NOTIFICATION:
                        if (Settings.Secure.getString(getApplication().getContentResolver(),"enabled_notification_listeners") == null
                                || !Settings.Secure.getString(getApplication().getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
                            startActivity(new Intent(
                                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        }
                        break;
                    case PERMISSION_OVERLAY:
                        if (!Settings.canDrawOverlays(getApplication())) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        }
                        break;
                    case PERMISSION_USAGESTATS:
                        AppOpsManager appOps = (AppOpsManager) getApplication().getSystemService(Context.APP_OPS_SERVICE);
                        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, myUid(), getApplication().getPackageName());
                        if (mode != MODE_ALLOWED) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                        break;
                    case PERMISSION_ACCESSIBILITY:
                        if (!isAccessibilityServiceEnabled(getApplication(), MyAccessibilityService.class)) {
                            Intent accessibilityIntent = new Intent();
                            accessibilityIntent.setAction(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            startActivity(accessibilityIntent);
                        }
                        break;
                    default:

                        break;
                }
            }
        });
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateListing();
        adapter.clear();
        adapter.addAll(listValues);
        adapter.notifyDataSetChanged();
    }

    private void showActionBar(){
        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_nav, null);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setCustomView(v);

        TextView navbarTitle;
        navbarTitle = findViewById(R.id.action_title);
        navbarTitle.setText(R.string.permissions_title);

        ImageButton backButton = findViewById(R.id.action_back);
        backButton.setOnClickListener(mClickListener);

        ImageButton forwardButton = findViewById(R.id.action_forward);
        forwardButton.setVisibility(View.INVISIBLE);

    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.action_back:
                    Intent backIntent = new Intent(PermissionsActivity.this, SettingsActivity.class);
                    startActivity(backIntent);
                    break;
            }
        }
    };

    private void updateListing(){
        listValues = new ArrayList<>();
        // Location permission
        boolean locationPermission = false;
        if (getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_LOCATION, getString(R.string.permission_location_label), locationPermission));

        //Contacts
        boolean contactsPermission = false;
        if (getApplication().checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            contactsPermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_CONTACTS, getString(R.string.permission_contacts_label), contactsPermission));

        //Microphone
        boolean microphonePermission = false;
        if (getApplication().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            microphonePermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_RECORD_AUDIO, getString(R.string.permission_microphone_label), microphonePermission));

        //Camera
        boolean cameraPermission = false;
        if (getApplication().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_CAMERA, getString(R.string.permission_camera_label), cameraPermission));

        //Write Storage
        boolean writePermission = false;
        if (getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            writePermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_WRITE_STORAGE, getString(R.string.permission_storagewrite_label), writePermission));

        //Phone
        boolean phonePermission = false;
        if (getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            phonePermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_PHONE, getString(R.string.permission_phone_label), phonePermission));

        //Notification Access
        boolean notificationPermission = false;
        if (Settings.Secure.getString(getApplication().getContentResolver(),"enabled_notification_listeners") != null
                || Settings.Secure.getString(getApplication().getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
            notificationPermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_NOTIFICATION, getString(R.string.permission_notification_label), notificationPermission));

        //Overlay
        boolean overlayPermission = false;
        if (Settings.canDrawOverlays(getApplication())) {
            overlayPermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_OVERLAY, getString(R.string.permission_overlays_label), overlayPermission));

        //Usage Stats
        boolean usageStatsPermission = false;
        AppOpsManager appOps = (AppOpsManager) getApplication().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, myUid(), getApplication().getPackageName());
        if (mode == MODE_ALLOWED) {
            usageStatsPermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_USAGESTATS, getString(R.string.permission_usagestats_label), usageStatsPermission));

        //Accessibility Service
        boolean accessibilityPermission = false;
        if (isAccessibilityServiceEnabled(getApplication(), MyAccessibilityService.class)) {
            accessibilityPermission = true;
        }
        listValues.add(new PermissionRecord(PERMISSION_ACCESSIBILITY, getString(R.string.permission_accessbility_label), accessibilityPermission));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_camera_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                break;
            }
            case PERMISSION_PHONE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Call Phone permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_call_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                break;
            }
            case PERMISSION_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Call Phone permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_contacts_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                break;
            }
            case PERMISSION_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Record Audio permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_record_audio_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                break;
            }
            case PERMISSION_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Write to storage permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_write_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                break;
            }
            case PERMISSION_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_location_alert_body));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                break;
            }
            default:
                Log.d(TAG, "Unknown Permissions Request Code");
                break;
        }
        updateListing();
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(),  Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }

        return false;
    }
}