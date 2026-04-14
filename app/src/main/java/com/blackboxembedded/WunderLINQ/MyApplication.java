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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static Context mContext;
    private static final Set<String> visibleActivities = new HashSet<>();

    private static boolean videoRecording;
    private static boolean tripRecording;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        registerActivityLifecycleCallbacks(this);
    }

    public static Context getContext(){
        return mContext;
    }

    public static boolean isActivityVisible(Class<?> activityClass) {
        return visibleActivities.contains(activityClass.getName());
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {}

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        visibleActivities.add(activity.getClass().getName());
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        visibleActivities.remove(activity.getClass().getName());
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}

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
