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
package com.blackboxembedded.WunderLINQ;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
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
                        if (Build.VERSION.SDK_INT >= 24) {
                            performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
                        }
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