package com.blackboxembedded.WunderLINQ;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import static android.content.Context.WINDOW_SERVICE;

public class AppUtils {

    public final static String TAG = "AppUtils";

    public static void adjustDisplayScale(Context context, Configuration configuration) {
        if (configuration != null) {
            Log.d("TAG", "adjustDisplayScale: " + configuration.densityDpi);
            if(configuration.densityDpi >= 485) //for 6 inch device OR for 538 ppi
                configuration.densityDpi = 500; //decrease "display size" by ~30
            else if(configuration.densityDpi >= 300) //for 5.5 inch device OR for 432 ppi
                configuration.densityDpi = 400; //decrease "display size" by ~30
            else if(configuration.densityDpi >= 100) //for 4 inch device OR for 233 ppi
                configuration.densityDpi = 200; //decrease "display size" by ~30
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);
            metrics.scaledDensity = configuration.densityDpi * metrics.density;
            context.getResources().updateConfiguration(configuration, metrics);
        }
    }

    public static void adjustFontScale(Context context, Configuration configuration) {
        configuration = context.getResources().getConfiguration();
        configuration.fontScale = (float) 1; //0.85 small size, 1 normal size, 1,15 big etc
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        configuration.densityDpi = (int) context.getResources().getDisplayMetrics().xdpi;
        context.getResources().updateConfiguration(configuration, metrics);
    }

}
