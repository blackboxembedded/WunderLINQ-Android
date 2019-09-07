package com.blackboxembedded.WunderLINQ;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static android.os.Process.myUid;

public class BTConnectReceiver extends BroadcastReceiver {

    public final static String TAG = "BTConnectReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPrefs;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        if (sharedPrefs.getBoolean("prefAutoLaunch",true)) {

            if (intent.getAction().equals("android.bluetooth.device.action.ACL_CONNECTED")) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null) {
                    if (device.getName().contains("WunderLINQ")) {
                        String topPackageName;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
