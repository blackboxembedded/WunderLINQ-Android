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
