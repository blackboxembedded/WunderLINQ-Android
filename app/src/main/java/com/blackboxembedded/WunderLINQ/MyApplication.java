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

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    private static Context mContext;

    private static boolean videoRecording;
    private static boolean tripRecording;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext(){
        return mContext;
    }

    public static boolean getVideoRecording() {
        return videoRecording;
    }
    public static void setVideoRecording(boolean videoRecording) {
        MyApplication.videoRecording = videoRecording;
    }

    public static boolean getTripRecording() {
        return tripRecording;
    }
    public static void setTripRecording(boolean tripRecording) {
       MyApplication.tripRecording = tripRecording;
    }
}
