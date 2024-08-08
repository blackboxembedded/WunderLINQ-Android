package com.blackboxembedded.WunderLINQ.SVGDashboards;

import android.content.SharedPreferences;


public class SVGSettings {
    public SharedPreferences sharedPrefs;
    public String pressureFormat = "0";
    public String temperatureFormat = "0";
    public String distanceFormat = "0";
    public String consumptionFormat = "0";
    public  String pressureUnit = "bar";
    public  String temperatureUnit = "C";
    public  String distanceUnit = "km";
    public  String heightUnit = "m";
    public String distanceTimeUnit = "KMH";
    public String consumptionUnit = "L/100";

    public boolean tenK = false;

    public boolean twelveK = false;
    public boolean fifteenK = false;

}
