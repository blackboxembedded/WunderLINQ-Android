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
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.os.Process.myUid;

public class FirstRunActivity extends AppCompatActivity {

    private final static String TAG = "FirstRunActivity";

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int PERMISSION_REQUEST_CALL_PHONE = 101;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 102;
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 112;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 122;

    private int step = 0;
    private TextView tvMessage;

    private SharedPreferences sharedPrefs;

    private Intent mainIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mainIntent = new Intent(FirstRunActivity.this, MainActivity.class);

        if (!sharedPrefs.getBoolean("FIRST_LAUNCH",true)){
            startActivity(mainIntent);
        } else {
            setContentView(R.layout.activity_first_run);
            tvMessage = findViewById(R.id.tvMessage);
            Button button = findViewById(R.id.button);
            button.setOnClickListener(mClickListener);
        }
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.button:
                    switch (step) {
                        case 0:
                            tvMessage.setText(getString(R.string.contacts_alert_body));

                            step = step + 1;
                            break;
                        case 1:
                            // Read Contacts permission
                            tvMessage.setText(getString(R.string.camera_alert_body));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (getApplication().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
                                }
                            }

                            step = step + 1;
                            break;
                        case 2:
                            // Camera permission
                            tvMessage.setText(getString(R.string.call_alert_body));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (getApplication().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                                }
                            }
                            step = step + 1;
                            break;
                        case 3:
                            // Call phone permission
                            tvMessage.setText(getString(R.string.record_audio_alert_body));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CALL_PHONE);
                                }
                            }
                            step = step + 1;
                            break;
                        case 4:
                            // Read Audio permission
                            tvMessage.setText(getString(R.string.write_alert_body));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (getApplication().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
                                }
                            }
                            step = step + 1;
                            break;
                        case 5:
                            // Write storage permission
                            tvMessage.setText(getString(R.string.location_alert_body));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (getApplication().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_STORAGE);
                                }
                            }
                            step = step + 1;
                            break;
                        case 6:
                            // Location permission
                            tvMessage.setText(getString(R.string.overlay_alert_body));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                                }
                            }
                            step = step + 1;
                            break;
                        case 7:
                            // Overlay permission
                            tvMessage.setText(getString(R.string.notification_alert_body));

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (!Settings.canDrawOverlays(getApplication())) {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                }
                            }
                            step = step + 1;
                            break;
                        case 8:
                            // Read notification permission
                            tvMessage.setText(getString(R.string.usagestats_alert_body));

                            if (Settings.Secure.getString(getApplication().getContentResolver(),"enabled_notification_listeners") == null
                                    || !Settings.Secure.getString(getApplication().getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
                                startActivity(new Intent(
                                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                            }
                            step = step + 1;
                            break;
                        case 9:
                            //Usage stats permission
                            tvMessage.setText(getString(R.string.accessibilityservice_alert_body));

                            AppOpsManager appOps = (AppOpsManager) getApplication().getSystemService(Context.APP_OPS_SERVICE);
                            int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, myUid(), getApplication().getPackageName());
                            if (mode != MODE_ALLOWED) {
                                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                            }
                            step = step + 1;
                            break;
                        case 10:
                            //Accessibility service
                            tvMessage.setText(getString(R.string.firstrun_end));

                            if (!isAccessibilityServiceEnabled(getApplication(), MyAccessibilityService.class)) {
                                Intent accessibilityIntent = new Intent();
                                accessibilityIntent.setAction(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                startActivity(accessibilityIntent);
                            }
                            step = step + 1;
                            break;
                        case 11:
                            SharedPreferences.Editor editor = sharedPrefs.edit();
                            editor.putBoolean("FIRST_LAUNCH", false);
                            editor.apply();
                            startActivity(mainIntent);
                            break;
                        default:
                            startActivity(mainIntent);
                            break;
                    }
                    break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults != null) {
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
                }
                case PERMISSION_REQUEST_WRITE_STORAGE: {
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
                }
                default:
                    Log.d(TAG, "Unknown Permissions Request Code");
            }
        }
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
