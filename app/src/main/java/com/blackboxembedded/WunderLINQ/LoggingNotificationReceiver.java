package com.blackboxembedded.WunderLINQ;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LoggingNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "LogNotfiRec";
    public static int REQUEST_CODE_NOTIFICATION = 1234;
    public static final String RESUME_ACTION = "RESUME_ACTION";
    public static final String STOP_ACTION = "STOP_ACTION";
    public static final String CANCEL_ACTION = "CANCEL_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "Action: " + action);

        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case RESUME_ACTION :
                    Log.d(TAG,"Resume Action Pressed");
                    break;
                case STOP_ACTION :
                    Log.d(TAG,"Stop Action Pressed");
                    MyApplication.getContext().stopService(new Intent(MyApplication.getContext(), LoggingService.class));
                    break;
                case CANCEL_ACTION:
                    Log.d(TAG,"Start Action Pressed");
                    break;
            }
        }
    }
}
