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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LoggingNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "LogNotificationRec";
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
