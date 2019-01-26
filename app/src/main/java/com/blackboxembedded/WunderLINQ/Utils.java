package com.blackboxembedded.WunderLINQ;

import java.text.DecimalFormat;
import java.util.Date;

public class Utils {

    public static String ByteArraytoHex(byte[] bytes) {
        if(bytes!=null){
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString();
        }
        return "";
    }

    public static String ByteArraytoHexNoDelim(byte[] bytes) {
        if(bytes!=null){
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        }
        return "";
    }

    public static int bytesToInt(byte a, byte b, byte c) {
        return (a & 0xFF) << 16 | (b & 0xFF) << 8 | (c & 0xFF);
    }

    // Take two dates and calulate the duration in hours, min, sec
    public static long [] calculateDuration(Date startDate, Date endDate){
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        //long elapsedDays = different / daysInMilli;
        //different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;
        long[] duration = new long[]{elapsedSeconds,elapsedMinutes,elapsedHours};
        return duration;
    }

    //Normalize a degree from 0 to 360 instead of -180 to 180
    public static int normalizeDegrees(double rads){
        return (int)((rads+360)%360);
    }

    // Unit Conversion Functions
    // bar to psi
    public static double barToPsi(double bar){
        return bar * 14.5037738;
    }
    // bar to kpa
    public static double barTokPa(double bar){
        return bar * 100;
    }
    // bar to kg-f
    public static double barTokgf(double bar){
        return bar * 1.0197162129779;
    }
    // kilometers to miles
    public static double kmToMiles(double kilometers){
        return kilometers * 0.62137;
    }
    // meters to feet
    public static double mToFeet(double meters){
        return meters / 0.3048;
    }
    // Celsius to Fahrenheit
    public static double celsiusToFahrenheit(double celsius){
        return (celsius * 1.8) + 32.0;
    }
    // L/100 to MPG
    public static double l100Tompg(double l100){
        return 235.215 / l100;
    }

    //format to 1 decimal place
    public static DecimalFormat oneDigit = new DecimalFormat("###0.0");
}
