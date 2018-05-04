package com.badasscompany.NavLINq;

import android.app.Application;

public class MyApplication extends Application {

    private boolean videoRecording;
    private boolean tripRecording;

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