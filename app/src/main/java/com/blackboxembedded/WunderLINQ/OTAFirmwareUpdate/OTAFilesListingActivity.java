/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign),
 * United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate
 * license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 *
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 *
 *
 */

package com.blackboxembedded.WunderLINQ.OTAFirmwareUpdate;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.xmlpull.v1.XmlPullParserException;

import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.OTAFirmwareUpdate.FirmwareXMLParser.Entry;


/**
 * Fragment that display the firmware files.User can select the firmware file for upgrade
 */
public class OTAFilesListingActivity extends Activity {

    public final static String TAG = "OTAFlsLstActivity";
    //Constants
    private static int mFilesCount;
    private final ArrayList<OTAFileModel> mArrayListFiles = new ArrayList<OTAFileModel>();
    private final ArrayList<String> mArrayListPaths = new ArrayList<String>();
    private final ArrayList<String> mArrayListFileNames = new ArrayList<String>();

    List<Entry> entries = null;

    private OTAFileListAdapter mFirmwareAdapter;
    private ListView mFileListView;
    private Button mUpgrade;
    private Button mNext;
    private TextView mHeading;

    public static Boolean mApplicationInBackground = false;

    // Progress Dialog
    private ProgressDialog pDialog;
    public static final int progress_bar_type = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ota_firmware_files_list);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Utils.isTablet(this)) {
            Log.d("OTAFilesListingActivity","tablet");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            Log.d("OTAFilesListingActivity","Phone");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mFilesCount = extras.getInt(Constants.REQ_FILE_COUNT);
        }

        mFileListView = (ListView) findViewById(R.id.listView);
        mUpgrade = (Button) findViewById(R.id.upgrade_button);
        mNext = (Button) findViewById(R.id.next_button);
        mHeading = (TextView) findViewById(R.id.heading_2);

        mFirmwareAdapter = new OTAFileListAdapter(this,
                mArrayListFiles, mFilesCount);
        mFileListView.setAdapter(mFirmwareAdapter);

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    List<Entry> entries = null;
                    FirmwareXMLParser firmwareXMLParser = new FirmwareXMLParser();


                    URL url = new URL(getResources().getString((R.string.ota_url)));
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.connect();

                    if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Log.e(TAG, "run: http request error");
                        return;
                    }

                    String firmwareRoot = Environment.getExternalStorageDirectory() + "/WunderLINQ/firmware/";
                    entries = firmwareXMLParser.parse(urlConnection.getInputStream());
                    for (Entry entry : entries) {
                        OTAFileModel fileModel = new OTAFileModel(entry.name,
                                firmwareRoot + entry.name + ".navfw", false, firmwareRoot, entry.file, entry.description );
                        mArrayListFiles.add(fileModel);
                        mFirmwareAdapter.addFiles(mArrayListFiles);
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                // Stuff that updates the UI
                                mFirmwareAdapter.notifyDataSetChanged();

                            }
                        });

                    }
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

        if (mFilesCount == OTAFirmwareUpgradeActivity.mApplicationAndStackSeparate) {
            mHeading.setText(getResources().getString((R.string.ota_stack_file)));
            mUpgrade.setVisibility(View.GONE);
            mNext.setVisibility(View.VISIBLE);
        } else {
            mUpgrade.setVisibility(View.VISIBLE);
            mNext.setVisibility(View.GONE);
        }

        /**
         * File Selection click event
         */
        mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                String remoteFile = mArrayListFiles.get(position).getFileRemote();

                File root = new File(mArrayListFiles.get(position).getFilePath());
                if(!root.exists()){
                    Log.d(TAG, "Not downloaded yet: " + mArrayListFiles.get(position).getFilePath());
                    new DownloadFileFromURL().execute(remoteFile);
                } else {
                    Log.d(TAG, "Already downloaded: " + mArrayListFiles.get(position).getFilePath());
                    mFirmwareAdapter.notifyDataSetChanged();
                }

                OTAFileModel model = mArrayListFiles.get(position);
                model.setSelected(!model.isSelected());
                for (int i = 0; i < mArrayListFiles.size(); i++) {
                    if (position != i) {
                        mArrayListFiles.get(i).setSelected(false);
                    }
                }
            }
        });

        /**
         * returns to the type selection fragment by selecting the required files
         */
        mUpgrade.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (mFilesCount == OTAFirmwareUpgradeActivity.mApplicationAndStackSeparate) {
                    Log.d(TAG,"if1");
                    for (int count = 0; count < mArrayListFiles.size(); count++) {
                        if (mArrayListFiles.get(count).isSelected()) {
                            mArrayListPaths.add(1, mArrayListFiles.get(count).getFilePath());
                            mArrayListFileNames.add(1, mArrayListFiles.get(count).getFileName());
                        }
                    }
                } else { // Fixed bootloader
                    Log.d(TAG,"else1");
                    for (int count = 0; count < mArrayListFiles.size(); count++) {
                        if (mArrayListFiles.get(count).isSelected()) {
                            mArrayListPaths.add(0, mArrayListFiles.get(count).getFilePath());
                            mArrayListFileNames.add(0, mArrayListFiles.get(count).getFileName());
                        }
                    }
                }

                if (mFilesCount == OTAFirmwareUpgradeActivity.mApplicationAndStackSeparate) {
                    if (mArrayListPaths.size() == 2) {
                        Log.d(TAG,"if2");
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra(Constants.SELECTION_FLAG, true);
                        returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_PATHS, mArrayListPaths);
                        returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_NAMES, mArrayListFileNames);
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    } else {
                        alertFileSelection(getResources().getString(R.string.ota_alert_file_applicationstacksep_app_sel));
                    }
                } else if (mFilesCount != OTAFirmwareUpgradeActivity.mApplicationAndStackSeparate
                        && mArrayListPaths.size() == 1) {
                    Log.d(TAG,"elif2");
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(Constants.SELECTION_FLAG, true);
                    returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_PATHS, mArrayListPaths);
                    returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_NAMES, mArrayListFileNames);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                } else { // Fixed bootloader
                    Log.d(TAG,"else2");
                    if (mFilesCount != OTAFirmwareUpgradeActivity.mApplicationAndStackCombined) {
                        alertFileSelection(getResources().getString(R.string.ota_alert_file_application));
                    } else {
                        alertFileSelection(getResources().getString(R.string.ota_alert_file_applicationstackcomb));
                    }
            }
        }
    }

    );

    /**
     * returns to the type selection fragment by selecting the required files
     */
    mNext.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick (View view){
        for (int count = 0; count < mArrayListFiles.size(); count++) {
            if (mArrayListFiles.get(count).isSelected()) {
                mArrayListPaths.add(0, mArrayListFiles.get(count).getFilePath());
                mArrayListFileNames.add(0, mArrayListFiles.get(count).getFileName());
                mHeading.setText(getResources().getString((R.string.ota_app_file)));
                mArrayListFiles.remove(count);
                mFirmwareAdapter.addFiles(mArrayListFiles);
                mFirmwareAdapter.notifyDataSetChanged();
                mUpgrade.setVisibility(View.VISIBLE);
                mNext.setVisibility(View.GONE);
            }
        }

            if(mArrayListPaths.size() == 0){
                alertFileSelection(getResources().getString(R.string.ota_alert_file_applicationstacksep_stack_sel));
            }
      }
    }

    );
}

    void alertFileSelection(String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(R.string.app_name)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume() {
        mApplicationInBackground = false;
        super.onResume();
    }

    @Override
    protected void onPause() {
        mApplicationInBackground = true;
        super.onPause();
    }

    /**
     * Showing Dialog
     * */

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    /**
     * Background Async Task to download file
     * */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(progress_bar_type);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                String path = url.getPath();
                String file = path.substring(path.lastIndexOf('/') + 1);
                URLConnection conection = url.openConnection();
                conection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                File root = new File(Environment.getExternalStorageDirectory(), "/WunderLINQ/firmware/");
                if(!root.exists()){
                    if(!root.mkdirs()){
                        Log.d(TAG,"Unable to create directory: " + root);
                    }
                }

                // Output stream
                OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory().toString()
                        + "/WunderLINQ/firmware/" + file);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        // Stuff that updates the UI
                        mFirmwareAdapter.notifyDataSetChanged();

                    }
                });

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(progress_bar_type);

        }

    }
}


