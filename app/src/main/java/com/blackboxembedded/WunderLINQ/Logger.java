package com.blackboxembedded.WunderLINQ;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

class Logger {
    private static PrintWriter outFile = null;
    private static final String TAG = "WunderLINQ";

    private static void initialize()
    {
        try {
            File root = new File(MyApplication.getContext().getCacheDir(), "/tmp/");
            if(!root.exists()){
                if(!root.mkdirs()){
                    Log.d(TAG,"Unable to create directory: " + root);
                }
            }
            if(root.canWrite()){
                Log.d(TAG,"Initialize Debug Message Logging");
                String filename = "dbg";
                File logFile = new File( root, filename );
                FileWriter logWriter = new FileWriter( logFile );
                outFile = new PrintWriter( logWriter );
            }
        } catch (IOException e) {
            Log.d(TAG, "Could not write to file: " + e.getMessage());
        }
    }

    public void write(String entry)
    {
        if(outFile == null)
            initialize();

        // Write message
        if(outFile != null) {
            // Get current time in UTC
            Calendar cal = Calendar.getInstance();
            Date date = cal.getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSSZ");
            String curdatetime = formatter.format(date);
            outFile.write(curdatetime + "," + entry + "\n");
            outFile.flush();
        }
    }

    public void shutdown()
    {
        if(outFile != null)
            outFile.close();
    }
}
