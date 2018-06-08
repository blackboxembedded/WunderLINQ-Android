package com.blackboxembedded.WunderLINQ;

import android.app.Application;

public class MyApplication extends Application {

    private boolean itsDark;
    private boolean videoRecording;
    private boolean tripRecording;

    public boolean getitsDark() {
        return itsDark;
    }
    public void setitsDark(boolean itsDark) {
        this.itsDark = itsDark;
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
