package com.blackboxembedded.WunderLINQ;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BTConnectReceiver extends BroadcastReceiver {

    public final static String TAG = "BTConnectReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.bluetooth.device.action.ACL_CONNECTED")){
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device.getName().contains("WunderLINQ")){
                Log.d(TAG,"WunderLINQ Connected, launching App");
                // Start activity
                Intent i = new Intent();
                i.setClassName("com.blackboxembedded.WunderLINQ", "com.blackboxembedded.WunderLINQ.MainActivity");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }
    }
}
