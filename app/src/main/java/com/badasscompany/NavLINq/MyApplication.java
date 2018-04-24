package com.badasscompany.NavLINq;

import android.app.Application;

public class MyApplication extends Application {

    private boolean videoRecording;

    public boolean getVideoRecording() {
        return videoRecording;
    }

    public void setVideoRecording(boolean videoRecording) {
        this.videoRecording = videoRecording;
    }
}