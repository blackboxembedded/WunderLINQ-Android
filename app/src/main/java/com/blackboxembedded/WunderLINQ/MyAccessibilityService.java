package com.blackboxembedded.WunderLINQ;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {

    public final static String TAG = "MyAccessibilityService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            if (intent.getExtras() != null) {
                int command = intent.getIntExtra("command", 1);
                switch (command) {
                    case 1:
                        performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                        break;
                    case 2:
                        performGlobalAction(GLOBAL_ACTION_RECENTS);
                        break;
                    default:
                        Log.d(TAG, "Unknown command: " + command);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}