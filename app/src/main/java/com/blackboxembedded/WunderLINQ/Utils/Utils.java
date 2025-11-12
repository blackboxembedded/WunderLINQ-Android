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
package com.blackboxembedded.WunderLINQ.Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import com.blackboxembedded.WunderLINQ.MemCache;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.AltitudeData;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class Utils {

    private static final String TAG = "Utils";

    public static final String LATITUDE_PATTERN="^(\\+|-)?(?:90(?:(?:\\.0{1,8})?)|(?:[0-9]|[1-8][0-9])(?:(?:\\.[0-9]{1,8})?))$";
    public static final String LONGITUDE_PATTERN="^(\\+|-)?(?:180(?:(?:\\.0{1,8})?)|(?:[0-9]|[1-9][0-9]|1[0-7][0-9])(?:(?:\\.[0-9]{1,8})?))$";

    public static boolean isSet(byte value, byte bit){
        return ( (value & bit) == bit );
    }

    public static String ByteArrayToHex(byte[] bytes) {
        if(bytes!=null){
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString();
        }
        return "";
    }

    public static String ByteArrayToHexNoDelim(byte[] bytes) {
        if(bytes!=null){
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        }
        return "";
    }

    public static int bytesToInt16(byte a, byte b, byte c) {
        return (a & 0xFF) << 16 | (b & 0xFF) << 8 | (c & 0xFF);
    }

    public static int bytesToInt12(byte a, byte b) {
        return (a & 0xFF) << 8 | (b & 0xFF);
    }

    // Take two dates and calculate the duration in hours, min, sec
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

    // return rate of climb
    public static double calculateRateOfClimb(Queue<AltitudeData> altitudeWindow) {
        if (altitudeWindow.size() < 2) return 0.0; // Not enough data to calculate

        // Get the oldest and newest data points
        AltitudeData oldestData = altitudeWindow.peek(); // Oldest data in the window
        AltitudeData newestData = ((LinkedList<AltitudeData>) altitudeWindow).getLast(); // Most recent data

        // Calculate time and altitude differences
        double altitudeChange = newestData.altitude - oldestData.altitude;
        double timeChangeInSeconds = (newestData.timestamp - oldestData.timestamp) / 1000.0;

        // Rate of climb (m/window)
        return altitudeChange / timeChangeInSeconds;
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
    public static double barToKgF(double bar){
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
    public static double l100ToMpg(double l100){
        return 235.215 / l100;
    }
    // L/100 to MPG Imperial
    public static double l100ToMpgI(double l100){
        return (235.215 / l100) * 1.20095;
    }
    // L/100 to km/L
    public static double l100ToKmL(double l100){
        return l100 / 100;
    }

    /*
     * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
     */
    // lowPass filter
    public static float[] lowPass(float[] input, float[] output, float ALPHA) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    //Get Devices current Locale
    public static Locale getCurrentLocale() {
        return MyApplication.getContext().getResources().getConfiguration().getLocales().get(0);
    }


    //format to 1 decimal place
    private static NumberFormat getLocalizedOneDigitFormat() {
        NumberFormat oneDigit = MemCache.getLocalizedNumberFormat();

        oneDigit.setMinimumFractionDigits(1);
        oneDigit.setMaximumFractionDigits(1);

        return oneDigit;
    }

    //format to 0 decimal place
    private static NumberFormat getLocalizedZeroDecimalFormat() {
        NumberFormat zeroDecimal = MemCache.getLocalizedNumberFormat();

        zeroDecimal.setMaximumFractionDigits(0);

        return zeroDecimal;
    }

    public static String toOneDecimalString(double num) {
        DecimalFormat decimalFormat =  (DecimalFormat) Utils.getLocalizedOneDigitFormat();
        String value  = decimalFormat.format(Math.round(num * 100d)/100d );

        return value;
    }



    public static String toZeroDecimalString(double num) {
        return toZeroDecimalString(num, false);
    }

    // Outputs big number to string with consistent formatting.  Use when number scale can vary greatly eg trip can go from 0.1 - 999,999
    public static String toZeroDecimalString(double num, boolean wrapGrouping) {
        String groupChar = "";
        DecimalFormat decimalFormat =  (DecimalFormat) Utils.getLocalizedOneDigitFormat();
        String value;

        if ((num > 0 && num < 10) || (num < 0 && num > -10)) {
            value = decimalFormat.format(Math.round(num * 100d)/100d );
        } else if (((num > 0 && num < 10000) || (num < 0 && num > -10000))) {
            decimalFormat.setMaximumFractionDigits(0);
            value = decimalFormat.format(Math.round(num * 10d)/10d );
        } else {
            decimalFormat.setMaximumFractionDigits(0);
            value = decimalFormat.format(Math.round(num * 10d)/10d );
            if (wrapGrouping && decimalFormat.isGroupingUsed()) {
                DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
                groupChar =  String.valueOf(symbols.getGroupingSeparator());
                value = value.replace(groupChar, "\n" + groupChar);
            }
        }

        return value;
    }

    public static SimpleDateFormat getCachedLocalizedDateFormat() {
        return MemCache.getCachedLocalizedDateFormat();
    }

    public static String bearingToCardinal(Integer bearingValue) {
        String bearing;

        if (bearingValue > 331 || bearingValue <= 28) {
            bearing = MyApplication.getContext().getString(R.string.north);
        } else if (bearingValue > 28 && bearingValue <= 73) {
            bearing = MyApplication.getContext().getString(R.string.north_east);
        } else if (bearingValue > 73 && bearingValue <= 118) {
            bearing = MyApplication.getContext().getString(R.string.east);
        } else if (bearingValue > 118 && bearingValue <= 163) {
            bearing = MyApplication.getContext().getString(R.string.south_east);
        } else if (bearingValue > 163 && bearingValue <= 208) {
            bearing = MyApplication.getContext().getString(R.string.south);
        } else if (bearingValue > 208 && bearingValue <= 253) {
            bearing = MyApplication.getContext().getString(R.string.south_west);
        } else if (bearingValue > 253 && bearingValue <= 298) {
            bearing = MyApplication.getContext().getString(R.string.west);
        } else if (bearingValue > 298 && bearingValue <= 331) {
            bearing = MyApplication.getContext().getString(R.string.north_west);
        } else {
            bearing = MyApplication.getContext().getString(R.string.blank_field);
        }

        return bearing;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {

        if (drawable == null) {
            Log.e(TAG, "drawableToBitmap: drawable is null!");
            return null;
        }

        // Determine the size of the Drawable
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        // Ensure a valid size
        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }

        // Create a Bitmap to hold the drawable content
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Create a Canvas and draw the Drawable onto it
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static Bitmap drawableToBitmap(Drawable drawable, int height) {
        if (drawable == null) {
            Log.e(TAG, "drawableToBitmap: drawable is null!");
            return null;
        }

        // Ensure a valid height
        if (height <= 0) {
            height = 1;
        }

        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        // Default width if no intrinsic size
        int width = (intrinsicWidth > 0 && intrinsicHeight > 0)
                ? (int) ((float) height * intrinsicWidth / intrinsicHeight)
                : height; // fallback to square

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
