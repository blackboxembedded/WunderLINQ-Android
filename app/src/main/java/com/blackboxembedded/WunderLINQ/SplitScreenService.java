package com.blackboxembedded.WunderLINQ;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class SplitScreenService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onCreate() {
        Log.d("SplitScreenService", "In onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SplitScreenService", "onStartCOmmand");
        performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);

        return super.onStartCommand(intent, flags, startId);
    }
}