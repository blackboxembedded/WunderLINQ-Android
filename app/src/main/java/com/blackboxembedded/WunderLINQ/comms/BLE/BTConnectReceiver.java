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
package com.blackboxembedded.WunderLINQ.comms.BLE;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.os.Process.myUid;

import androidx.core.app.ActivityCompat;

import com.blackboxembedded.WunderLINQ.MyApplication;

public class BTConnectReceiver extends BroadcastReceiver {

    public final static String TAG = "BTConnectReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPrefs;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        if (sharedPrefs.getBoolean("prefAutoLaunch", false)) {

            if (intent.getAction().equals("android.bluetooth.device.action.ACL_CONNECTED")) {
                Log.d(TAG, "android.bluetooth.device.action.ACL_CONNECTED!");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (device.getName() != null) {
                    if (device.getName().contains("WunderLINQ")) {
                        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                            String topPackageName;
                            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                            int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, myUid(), context.getPackageName());
                            if (mode == MODE_ALLOWED) {
                                UsageStatsManager mUsageStatsManager = (UsageStatsManager) MyApplication.getContext().getSystemService(Context.USAGE_STATS_SERVICE);
                                long currentTime = System.currentTimeMillis();
                                // get usage stats for the last 10 seconds
                                List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 10, currentTime);
                                // search for app with most recent last used time
                                if (stats != null) {
                                    long lastUsedAppTime = 0;
                                    for (UsageStats usageStats : stats) {
                                        //Log.d(TAG, "Package: " + usageStats.getPackageName() + ", LastTime Used: " + usageStats.getLastTimeUsed());
                                        if (usageStats.getLastTimeUsed() > lastUsedAppTime) {
                                            topPackageName = usageStats.getPackageName();
                                            if (topPackageName.equals("com.blackboxembedded.wunderlinqdfu")) {
                                                Log.d(TAG, "WunderLINQ DFU Running, not launching App");
                                                return;
                                            }
                                        }
                                    }
                                    Log.d(TAG, "WunderLINQ Connected, launching App");
                                    // Start activity
                                    Intent i = new Intent();
                                    i.setClassName("com.blackboxembedded.WunderLINQ", "com.blackboxembedded.WunderLINQ.MainActivity");
                                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(i);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
