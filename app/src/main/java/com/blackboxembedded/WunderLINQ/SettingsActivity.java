package com.blackboxembedded.WunderLINQ;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class SettingsActivity extends PreferenceActivity {

    private final static String TAG = "SettingsActivity";
    static AlertDialog alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new UserSettingActivityFragment()).commit();

    }

    public static class UserSettingActivityFragment extends PreferenceFragment
    {

        @Override
        public void onStop() {
            super.onStop();
            if (alertDialog != null) {
                alertDialog.dismiss();
                alertDialog = null;
            }
        }
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

            Preference button = findPreference("prefSendLog");
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // save logcat in file
                    File root = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/debug/");
                    if(!root.exists()){
                        if(!root.mkdirs()){
                            Log.d(TAG,"Unable to create directory: " + root);
                        }
                    }
                    File outputFile = new File(Environment.getExternalStorageDirectory(),
                            "/WunderLINQ/debug/logcat.txt");
                    try {
                        Runtime.getRuntime().exec(
                                "logcat -f " + outputFile.getAbsolutePath());
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    //send file using email
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    // set the type to 'email'
                    emailIntent.setType("text/plain");
                    String to[] = {getString(R.string.pref_sendlogs_email)};
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
                    // the attachment
                    Uri uri = FileProvider.getUriForFile(getActivity(), "com.blackboxembedded.wunderlinq.fileprovider", outputFile);
                    emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    // the mail subject
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.pref_sendlogs_subject));
                    emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.pref_sendlogs_body));
                    emailIntent.setType("message/rfc822");
                    emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.pref_sendlogs_intent_title)));
                    return true;
                }
            });
            Preference dfuButton = findPreference("prefDfuMode");
            if (MainActivity.gattDFUCharacteristic != null) {
                dfuButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        //TODO : Restart application or go back to MainActivity
                        byte[] valueByte = {01};
                        BluetoothGattCharacteristic characteristic = MainActivity.gattDFUCharacteristic;
                        characteristic.setValue(valueByte);
                        if (BluetoothLeService.writeCharacteristic(characteristic)){

                            //Intent i = new Intent(getActivity(), MainActivity.class);
                            //startActivity(i);

                            try {
                                // Display dialog text here......
                                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle("You have placed the WunderLINQ into DFU mode");
                                builder.setMessage("You have placed the WunderLINQ into DFU mode, the LED should be green.  " +
                                        "You will be be returned to the main screen and when the app reconnects it should prompt to update,");
                                builder.setPositiveButton(R.string.alert_message_exit_ok,
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (alertDialog != null && alertDialog.isShowing()) {
                                                    alertDialog.dismiss();
                                                }
                                                Intent i = new Intent(getActivity(), MainActivity.class);
                                                startActivity(i);
                                                getActivity().finish();

                                            }
                                        });
                                alertDialog = builder.create();
                                alertDialog.show();


                            } catch (NullPointerException e){
                                return false;
                            }
                            return true;
                        }
                        return false;
                    }
                });
            } else {
                dfuButton.setEnabled(false);
            }
        }
    }

}
