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

package com.badasscompany.NavLINq.OTAFirmwareUpdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.badasscompany.NavLINq.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Fragment that display the firmware files.User can select the firmware file for upgrade
 */
public class OTAFilesListingActivity extends Activity {

    //Constants
    private static int mFilesCount;
    private final ArrayList<OTAFileModel> mArrayListFiles = new ArrayList<OTAFileModel>();
    private final ArrayList<String> mArrayListPaths = new ArrayList<String>();
    private final ArrayList<String> mArrayListFileNames = new ArrayList<String>();


    private OTAFileListAdapter mFirmwareAdapter;
    private ListView mFileListView;
    private Button mUpgrade;
    private Button mNext;
    private TextView mHeading;

    public static Boolean mApplicationInBackground = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ota_firmware_files_list);

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

        /**
         * Shows the cyacd file in the device
         */
        File filedir = new File(Environment.getExternalStorageDirectory()
                + File.separator + "NavLINq");
        mFirmwareAdapter = new OTAFileListAdapter(this,
                mArrayListFiles, mFilesCount);
        mFileListView.setAdapter(mFirmwareAdapter);
        searchRequiredFile(filedir);

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

                OTAFileModel model = mArrayListFiles.get(position);
                model.setSelected(!model.isSelected());
                for (int i = 0; i < mArrayListFiles.size(); i++) {
                    if (position != i) {
                        mArrayListFiles.get(i).setSelected(false);
                    }
                }
                mFirmwareAdapter.notifyDataSetChanged();
            }
        });

        /**
         * returns to the type selection fragment by selecting the required files
         */
        mUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mFilesCount == OTAFirmwareUpgradeActivity.mApplicationAndStackSeparate) {
                    for (int count = 0; count < mArrayListFiles.size(); count++) {
                        if (mArrayListFiles.get(count).isSelected()) {
                            mArrayListPaths.add(1, mArrayListFiles.get(count).getFilePath());
                            mArrayListFileNames.add(1, mArrayListFiles.get(count).getFileName());
                        }
                    }
                } else {
                    for (int count = 0; count < mArrayListFiles.size(); count++) {
                        if (mArrayListFiles.get(count).isSelected()) {
                            mArrayListPaths.add(0, mArrayListFiles.get(count).getFilePath());
                            mArrayListFileNames.add(0, mArrayListFiles.get(count).getFileName());
                        }
                    }
                }

                if (mFilesCount == OTAFirmwareUpgradeActivity.mApplicationAndStackSeparate) {
                    if (mArrayListPaths.size() == 2) {
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
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(Constants.SELECTION_FLAG, true);
                    returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_PATHS, mArrayListPaths);
                    returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_NAMES, mArrayListFileNames);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                } else {
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


    /**
     * Method to search phone/directory for the .cyacd files
     *
     * @param dir
     */
    void searchRequiredFile(File dir) {
        if (dir.exists()) {
            String filePattern = "cyacd";
            File[] allFilesList = dir.listFiles();
            for (int pos = 0; pos < allFilesList.length; pos++) {
                File analyseFile = allFilesList[pos];
                if (analyseFile != null) {
                    if (analyseFile.isDirectory()) {
                        searchRequiredFile(analyseFile);
                    } else {
                        Uri selectedUri = Uri.fromFile(analyseFile);
                        String fileExtension
                                = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
                        if (fileExtension.equalsIgnoreCase(filePattern)) {
                            OTAFileModel fileModel = new OTAFileModel(analyseFile.getName(),
                                    analyseFile.getAbsolutePath(), false, analyseFile.getParent());
                            mArrayListFiles.add(fileModel);
                            mFirmwareAdapter.addFiles(mArrayListFiles);
                            mFirmwareAdapter.notifyDataSetChanged();
                        }
                    }

                }
            }
        } else {
            Toast.makeText(this, "Directory does not exist", Toast.LENGTH_SHORT).show();
        }
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
}


