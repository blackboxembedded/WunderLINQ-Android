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

import android.Manifest;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.os.Process.myUid;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

public class FirstRunActivity extends AppCompatActivity {

    private final static String TAG = "FirstRunActivity";

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 100;
    private static final int PERMISSION_REQUEST_CAMERA = 101;
    private static final int PERMISSION_REQUEST_CALL_PHONE = 102;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 103;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 105;
    private static final int PERMISSION_REQUEST_BLUETOOTH_CONNECT = 106;

    private int step = 0;
    private TextView tvMessage;
    private Button buttonOk;
    private Button buttonSkip;
    private SharedPreferences sharedPrefs;

    private Intent mainIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mainIntent = new Intent(FirstRunActivity.this, MainActivity.class);

        if (sharedPrefs.getBoolean("FIRST_LAUNCH1",true)){
            setContentView(R.layout.activity_first_run);
            tvMessage = findViewById(R.id.tvMessage);
            buttonOk = findViewById(R.id.buttonOK);
            buttonOk.setOnClickListener(mClickListener);
            buttonSkip = findViewById(R.id.buttonSkip);
            buttonSkip.setOnClickListener(mClickListener);
        } else {
            startActivity(mainIntent);
            finish();
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (step) {
                case 0:
                    buttonSkip.setVisibility(View.VISIBLE);
                    buttonOk.setText(R.string.firstrun_button_ok);
                    tvMessage.setText(getString(R.string.contacts_alert_body));
                    step = step + 1;
                    break;
                case 1:
                    // Read Contacts permission
                    tvMessage.setText(getString(R.string.camera_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(FirstRunActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 2:
                    // Camera permission
                    tvMessage.setText(getString(R.string.call_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(FirstRunActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 3:
                    // Call phone permission
                    tvMessage.setText(getString(R.string.record_audio_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(FirstRunActivity.this, new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CALL_PHONE);
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 4:
                    // Read Audio permission
                    tvMessage.setText(getString(R.string.location_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(FirstRunActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 5:
                    // Location permission
                    tvMessage.setText(getString(R.string.overlay_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(FirstRunActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 6:
                    // Overlay permission
                    tvMessage.setText(getString(R.string.notification_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!Settings.canDrawOverlays(getApplication())) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 7:
                    // Read notification permission
                    tvMessage.setText(getString(R.string.usagestats_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (Settings.Secure.getString(getApplication().getContentResolver(), "enabled_notification_listeners") == null
                                || !Settings.Secure.getString(getApplication().getContentResolver(), "enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
                            startActivity(new Intent(
                                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 8:
                    //Usage stats permission
                    tvMessage.setText(getString(R.string.accessibilityservice_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        AppOpsManager appOps = (AppOpsManager) getApplication().getSystemService(Context.APP_OPS_SERVICE);
                        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, myUid(), getApplication().getPackageName());
                        if (mode != MODE_ALLOWED) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 9:
                    //Accessibility service
                    tvMessage.setText(getString(R.string.btconnect_alert_body));
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (!isAccessibilityServiceEnabled(getApplication(), MyAccessibilityService.class)) {
                            Intent accessibilityIntent = new Intent();
                            accessibilityIntent.setAction(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            startActivity(accessibilityIntent);
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 10:
                    // Bluetooth Connect permission
                    tvMessage.setText(getString(R.string.firstrun_end));
                    buttonSkip.setVisibility(View.GONE);
                    buttonOk.setText(R.string.alert_btn_ok);
                    step = step + 1;
                    if (v.getId() == R.id.buttonOK) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                Log.d(TAG, "Requesting BT_CONNECT permission");
                                ActivityCompat.requestPermissions(FirstRunActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_BLUETOOTH_CONNECT);
                            }
                        } else {
                            buttonOk.performClick();
                        }
                    }
                    break;
                case 11:
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putBoolean("FIRST_LAUNCH1", false);
                    editor.apply();
                    startActivity(mainIntent);
                    finish();
                    break;
                default:
                    startActivity(mainIntent);
                    finish();
                    break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA: {
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
            case PERMISSION_REQUEST_CALL_PHONE: {
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
            case PERMISSION_REQUEST_READ_CONTACTS: {
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
            case PERMISSION_REQUEST_RECORD_AUDIO: {
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
            case PERMISSION_REQUEST_FINE_LOCATION: {
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
            case PERMISSION_REQUEST_BLUETOOTH_CONNECT: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "BLUETOOTH_CONNECT permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.negative_alert_title));
                    builder.setMessage(getString(R.string.negative_btconnect_alert_body));
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
    }

    public void moveDirectory(File source, File destination) throws IOException {
        FileUtils.copyDirectoryToDirectory(source, destination);
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
