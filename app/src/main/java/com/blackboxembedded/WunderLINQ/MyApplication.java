package com.blackboxembedded.WunderLINQ;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    private static Context mContext;

    private boolean videoRecording;
    private boolean tripRecording;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext(){
        return mContext;
    }

    public boolean getVideoRecording() {
        return videoRecording;
    }
    public void setVideoRecording(boolean videoRecording) {
        this.videoRecording = videoRecording;
    }

    public boolean getTripRecording() {
        return tripRecording;
    }
    public void setTripRecording(boolean tripRecording) {
        this.tripRecording = tripRecording;
    }
}
