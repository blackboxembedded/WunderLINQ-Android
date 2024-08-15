package com.blackboxembedded.WunderLINQ;


import com.blackboxembedded.WunderLINQ.Utils.Utils;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

//Reduce frequent access to SharedPreferences (disk IO) with static class.
public class MemCache {
    private final static String TAG = "MemCache";

    private static NumberFormat cachedLocalizedNumberFormat;
    private static SimpleDateFormat cachedLocalizedDateFormat;

    private static Map<String, Object> cache;

    public static Map<String, Object> getCache() {
        if (cache == null) {
            cache = new HashMap<>();
            setupUnitsCache();
        }

        return cache;
    }

    public static boolean exists(String key) {
        return getCache().containsKey(key);
    }

    public static String getString(String key, String defaultValue) {
        String retValue;
        if (getCache().containsKey(key)) {
            retValue = (String) getCache().get(key);

            if (retValue.isBlank()) {
                Log.d("MemCache GetString", "Not Found: " + key);
            }

            return retValue;
        } else {
            Log.d("MemCache GetString", "Not Found: " + key);
            return defaultValue;
        }
    }

    public static String getString(String key) {
        String defaultValue = "";
        return getString(key, defaultValue);
    }

    public static void putString(String key, String value) {
        getCache().put(key, value);
    }

    public static void invalidate() {
        // Clear the cache
        getCache().clear();
        cachedLocalizedDateFormat = null;
        cachedLocalizedNumberFormat = null;

        getCachedLocalizedDateFormat();
        getLocalizedNumberFormat();
        setupUnitsCache();

    }


    //Cached LocalizedNumberFormat on first retrieval.  Value is unlikely to change while app is running and retrieval is expensive
    public static NumberFormat getLocalizedNumberFormat() {
        if (cachedLocalizedNumberFormat == null) {
            cachedLocalizedNumberFormat = NumberFormat.getNumberInstance(Utils.getCurrentLocale());
        }
        return cachedLocalizedNumberFormat;
    }

    //Cached LocalizedNumberFormat on first retrieval.  Value is unlikely to change while app is running and retrieval is expensive
    public static SimpleDateFormat getCachedLocalizedDateFormat() {
        if (cachedLocalizedDateFormat == null) {
            String pref =  PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).getString("prefTime", "0");
            String tf;

            if (!pref.equals("0")) {
                tf = "HH:mm";
            } else {
                tf = "h:mm aa";
            }

            cachedLocalizedDateFormat = new SimpleDateFormat(tf, Utils.getCurrentLocale());
        }

        return cachedLocalizedDateFormat;
    }


    private static void setupUnitsCache() {
        if (getCache().isEmpty()) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
            // putString("",);

            putString("gear",MyApplication.getContext().getString(R.string.gear_label));
            putString("brakeLabelF", MyApplication.getContext().getString(R.string.frontbrakes_label));
            putString("brakeLabelR", MyApplication.getContext().getString(R.string.rearbrakes_label));

            // Pressure Unit
            String pressureUnit = "bar";
            String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
            if (pressureFormat.contains("1")) {
                // KPa
                pressureUnit = "KPa";
            } else if (pressureFormat.contains("2")) {
                // Kg-f
                pressureUnit = "Kg-f";
            } else if (pressureFormat.contains("3")) {
                // Psi
                pressureUnit = "psi";
            }
            putString("pressureFormat",pressureFormat);
            putString("pressureUnit",pressureUnit);
            putString("pressureUnitLabelF",
                    MyApplication.getContext().getString(R.string.frontpressure_header) + " (" + pressureUnit + ")");
            putString("pressureUnitLabelR",
                    MyApplication.getContext().getString(R.string.rearpressure_header) + " (" + pressureUnit + ")");



            //Temperature Unit
            String temperatureUnit = "C";
            String temperatureFormat = sharedPrefs.getString("prefTempF", "0");
            if (temperatureFormat.contains("1")) {
                // F
                temperatureUnit = "F";
            }
            putString("temperatureUnit", temperatureUnit);
            putString("temperatureFormat",temperatureFormat);
            putString("temperatureUnitAir",MyApplication.getContext().getString(R.string.ambient_temp_label) + " (" + temperatureUnit + ")");
            putString("temperatureUnitEngine",MyApplication.getContext().getString(R.string.engine_temp_label) + " (" + temperatureUnit + ")");

            //Distance
            String distanceUnit = "km";
            String heightUnit = "m";
            String distanceTimeUnit = "kmh";
            String distanceFormat = sharedPrefs.getString("prefDistance", "0");
            if (distanceFormat.contains("1")) {
                distanceUnit = "mi";
                heightUnit = "ft";
                distanceTimeUnit = "mph";
            }
            putString("distanceFormat",distanceFormat);
            putString("distanceUnit",distanceUnit);
            putString("heightUnit",heightUnit);
            putString("distanceTimeUnit",distanceTimeUnit);
            putString("odometerLabel",MyApplication.getContext().getString(R.string.odometer_label) + " (" + distanceUnit + ")");
            putString("trip1Label",MyApplication.getContext().getString(R.string.trip1_label) + " (" + distanceUnit + ")");
            putString("trip2Label",MyApplication.getContext().getString(R.string.trip2_label) + " (" + distanceUnit + ")");
            putString("tripAutoLabel",MyApplication.getContext().getString(R.string.tripauto_label) + " (" + distanceUnit + ")");
            putString("speedLabel",MyApplication.getContext().getString(R.string.speed_label) + " (" + distanceTimeUnit + ")");

            //Consumption Rate
            String consumptionUnit = "L/100";
            String consumptionFormat = sharedPrefs.getString("prefConsumption", "0");
            switch (consumptionFormat){
                case "1":
                case "2":
                    consumptionUnit = "mpg"; break;
                case "3":
                    consumptionUnit = "km/L"; break;
            }
            putString("consumptionFormat",consumptionFormat);
            putString("consumptionUnit",consumptionUnit);

            String voltageUnit = "V";
            putString("voltageUnit",voltageUnit);
            putString("voltageUnitLabel",MyApplication.getContext().getString(R.string.voltage_label) + " (" + voltageUnit + ")");

            String throttleUnit = "%";
            putString("throttleUnit", throttleUnit  );
            putString("throttleUnitLabel",MyApplication.getContext().getString(R.string.throttle_label) + " (" + throttleUnit + ")");

            String signalUnit = "dBm";
            putString("", "");

            String batteryUnit = "%";
            putString("", "");

            String barometricUnit = "mBar";
            putString("", "");


            //AVG_SPEED:
            putString("avgSpeedLabel", MyApplication.getContext().getString(R.string.avgspeed_label) + " (" + distanceTimeUnit + ")");

            //CURRENT_CONSUMPTION:
            putString("consumptionLabel", MyApplication.getContext().getString(R.string.cconsumption_label) + " (" + consumptionUnit + ")");

            //ECONOMY_ONE:
            putString("economy1Label", MyApplication.getContext().getString(R.string.fueleconomyone_label) + " (" + consumptionUnit + ")");

            //ECONOMY_TWO:
            putString("economy2Label", MyApplication.getContext().getString(R.string.fueleconomytwo_label) + " (" + consumptionUnit + ")");

            //RANGE:
            putString("rangeLabel", MyApplication.getContext().getString(R.string.fuelrange_label) + " (" + distanceUnit + ")");

            //SHIFTS:
            putString("shiftsLabel", MyApplication.getContext().getString(R.string.shifts_header));

            //LEAN_BT:
            putString("leanLabelBt", MyApplication.getContext().getString(R.string.leanangle_header));

            //GFORCE_DEVICE:
            putString("gforceLabel", MyApplication.getContext().getString(R.string.gforce_header));

            //BEARING_DEVICE:
            putString("bearingLabel", MyApplication.getContext().getString(R.string.bearing_header));
            putString("bearingPref", sharedPrefs.getString("prefBearing", "0"));

            //TIME_DEVICE:
            putString("timeLabel", MyApplication.getContext().getString(R.string.time_header));

            //BAROMETRIC_DEVICE:
            putString("barometricLabel", MyApplication.getContext().getString(R.string.barometricpressure_header) + "(" + barometricUnit + ")");

            //SPEED_DEVICE:
            putString("speedLabelG", MyApplication.getContext().getString(R.string.gpsspeed_header) + "(" + distanceTimeUnit + ")");

            //ALTITUDE_DEVICE:
            putString("altitudeLabel", MyApplication.getContext().getString(R.string.altitude_header) + "(" + heightUnit + ")");

            //SUN_DEVICE:
            putString("sunLabel", MyApplication.getContext().getString(R.string.sunrisesunset_header));

            //RPM:
            putString("rpmLabel", MyApplication.getContext().getString(R.string.rpm_header));

            //LEAN_BIKE:
            putString("leanLabel", MyApplication.getContext().getString(R.string.leanangle_bike_header));

            //REAR_SPEED:
            putString("speedLabelW", MyApplication.getContext().getString(R.string.rearwheel_speed_header) + "(" + distanceTimeUnit + ")");

            //CELL_SIGNAL:
            putString("signalLabel", MyApplication.getContext().getString(R.string.cellular_signal_header) + "(" + signalUnit + ")");

            //BATTERY_DEVICE:
            putString("batteryLabel", MyApplication.getContext().getString(R.string.local_battery_header) + "(" + batteryUnit + ")");
        }
    }


    public static String barometricUnit(){
        return getString("barometricUnit");
    }
    public static String barometricUnitLabel(){
        return getString("barometricUnitLabel");
    }
    public static String batteryUnit(){
        return getString("batteryUnit");
    }
    public static String batteryUnitLabel(){
        return getString("batteryUnitLabel");
    }
    public static String consumptionUnit(){
        return getString("consumptionUnit");
    }
    public static String consumptionFormat(){
        return getString("consumptionFormat");
    }
    public static String consumptionUnitLabel(){
        return getString("consumptionUnitLabel");
    }
    public static String distanceTimeUnit(){
        return getString("distanceTimeUnit");
    }
    public static String distanceTimeUnitLabel(){
        return getString("distanceTimeUnitLabel");
    }
    public static String distanceUnit(){
        return getString("distanceUnit");
    }
    public static String distanceFormat(){
        return getString("distanceFormat");
    }
    public static String odometerLabel(){
        return getString("odometerLabel");
    }
    public static String trip1Label(){
        return getString("trip1Label");
    }
    public static String trip2Label(){
        return getString("trip2Label");
    }
    public static String tripAutoLabel(){
        return getString("tripAutoLabel");
    }
    public static String speedLabel() {
        return getString("speedLabel");
    }
    public static String distanceUnitLabel(){
        return getString("distanceUnitLabel");
    }
    public static String heightUnit(){
        return getString("heightUnit");
    }
    public static String heightUnitLabel(){
        return getString("heightUnitLabel");
    }
    public static String pressureUnit(){
        return getString("pressureUnit");
    }
    public static String pressureFormat(){
        return getString("pressureFormat");
    }
    public static String pressureUnitLabelF(){
        return getString("pressureUnitLabelF");
    }
    public static String pressureUnitLabelR(){
        return getString("pressureUnitLabelR");
    }
    public static String pressureUnitLabel(){
        return getString("pressureUnitLabel");
    }
    public static String signalUnit(){
        return getString("signalUnit");
    }
    public static String signalUnitLabel(){
        return getString("signalUnitLabel");
    }
    public static String temperatureUnit(){
        return getString("temperatureUnit");
    }
    public static String temperatureFormat(){
        return getString("temperatureFormat");
    }
    public static String temperatureUnitEngine(){
        return getString("temperatureUnitEngine");
    }
    public static String temperatureUnitAir(){
        return getString("temperatureUnitAir");
    }
    public static String throttleUnit(){
        return getString("throttleUnit");
    }
    public static String throttleUnitLabel(){
        return getString("throttleUnitLabel");
    }
    public static String voltageUnit(){
        return getString("voltageUnit");
    }
    public static String voltageUnitLabel(){
        return getString("voltageUnitLabel");
    }
    public static String brakeLabelF() {
        return getString("brakeLabelF");
    }
    public static String brakeLabelR() {
        return getString("brakeLabelR");
    }

    public static String gear() {
        return getString("gear");
    }





    //AVG_SPEED:
    public static String avgSpeedLabel(){
        return getString("avgSpeedLabel");
    }

    //CURRENT_CONSUMPTION:
    public static String consumptionLabel(){
        return getString("consumptionLabel");
    }

    //ECONOMY_ONE:
    public static String economy1Label(){
        return getString("economy1Label");
    }

    //ECONOMY_TWO:
    public static String economy2Label(){
        return getString("economy2Label");
    }

    //RANGE:
    public static String rangeLabel(){
        return getString("rangeLabel");
    }

    //SHIFTS:
    public static String shiftsLabel(){
        return getString("shiftsLabel");
    }

    //LEAN_BT:
    public static String leanLabelBt(){
        return getString("leanLabelBt");
    }

    //GFORCE_DEVICE:
    public static String gforceLabel(){
        return getString("gforceLabel");
    }

    //BEARING_DEVICE:
    public static String bearingLabel(){
        return getString("bearingLabel");
    }

    public static String bearingPref(){
        return getString("bearingPref");
    }

    //TIME_DEVICE:
    public static String timeLabel(){
        return getString("timeLabel");
    }

    //BAROMETRIC_DEVICE:
    public static String barometricLabel(){
        return getString("barometricLabel");
    }

    //SPEED_DEVICE:
    public static String speedLabelG(){
        return getString("speedLabelG");
    }

    //ALTITUDE_DEVICE:
    public static String altitudeLabel(){
        return getString("altitudeLabel");
    }

    //SUN_DEVICE:
    public static String sunLabel(){
        return getString("sunLabel");
    }

    //RPM:
    public static String rpmLabel(){
        return getString("rpmLabel");
    }

    //LEAN_BIKE:
    public static String leanLabel(){
        return getString("leanLabel");
    }

    //REAR_SPEED:
    public static String speedLabelW(){
        return getString("speedLabelW");
    }

    //CELL_SIGNAL:
    public static String signalLabel(){
        return getString("signalLabel");
    }

    //BATTERY_DEVICE:
    public static String batteryLabel(){
        return getString("batteryLabel");
    }
}
