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
package com.blackboxembedded.WunderLINQ.hardware.WLQ;

import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.preference.PreferenceManager;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.blackboxembedded.WunderLINQ.MemCache;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ca.rmen.sunrisesunset.SunriseSunset;

public class MotorcycleData {
    // Constants
    private final static long MILLISECOND_DELAY_CLUSTER_UPDATE = 15 * 1000; // Every 15 seconds
    private final static double CRITICAL_ENGINE_TEMP_C = 104.0; //219F hot engine
    private final static double CRITICAL_ENGINE_TEMP_LOW_C = 55.0; //130F cold engine

    private final static double CRITICAL_AIR_TEMP_HIGH_C = 37.5; //99.5F hot human
    private final static double CRITICAL_AIR_TEMP_LOW_C = 4.0; //39F cold human watch for frost
    private final static double CRITICAL_BATTERY_VOLTAGE_HIGH = 15.0;
    private final static double CRITICAL_BATTERY_VOLTAGE_LOW = 12.0;
    private final static double RANGE_CRITICAL = 5.0;
    private final static double RANGE_LOW = 50.0;


    // WunderLINQ HW
    public static WLQ wlq;
    public static String hardwareVersion;

    public enum DataType {
        GEAR(0),
        ENGINE_TEMP(1),
        AIR_TEMP(2),
        FRONT_RDC(3),
        REAR_RDC(4),
        ODOMETER(5),
        VOLTAGE(6),
        THROTTLE(7),
        FRONT_BRAKE(8),
        REAR_BRAKE(9),
        AMBIENT_LIGHT(10),
        TRIP_ONE(11),
        TRIP_TWO(12),
        TRIP_AUTO(13),
        SPEED(14),
        AVG_SPEED(15),
        CURRENT_CONSUMPTION(16),
        ECONOMY_ONE(17),
        ECONOMY_TWO(18),
        RANGE(19),
        SHIFTS(20),
        LEAN_DEVICE(21),
        GFORCE_DEVICE(22),
        BEARING_DEVICE(23),
        TIME_DEVICE(24),
        BAROMETRIC_DEVICE(25),
        SPEED_DEVICE(26),
        ALTITUDE_DEVICE(27),
        SUN_DEVICE(28),
        RPM(29),
        LEAN_BIKE(30),
        REAR_SPEED(31),
        CELL_SIGNAL(32),
        BATTERY_DEVICE(33);


        private final int value;

        // Constructor is private; cannot be called from outside the enum
        DataType(int value) {
            this.value = value;
        }

        // Getter method for the value
        public int getValue() {
            return value;
        }

        public String getString() {
            return "";
        }

    }

    // Focus
    private static boolean hasFocus;
    public static boolean getHasFocus() { return hasFocus; }
    public static void setHasFocus(boolean status) { MotorcycleData.hasFocus = status; }

    // Last Message
    private static byte[] lastMessage;
    public static byte[] getLastMessage() {
        return lastMessage;
    }
    public static void setLastMessage(byte[] message){
        MotorcycleData.lastMessage = message;
    }

    // Last Location
    private static Location lastLocation;
    public static Location getLastLocation() {
        return lastLocation;
    }
    public static void setLastLocation(Location location){
        MotorcycleData.lastLocation = location;
    }

    // Mobile device battery
    private static Double localBattery;
    public static Double getLocalBattery() {
        return localBattery;
    }
    public static void setLocalBattery(Double localBattery){
        MotorcycleData.localBattery = localBattery;
    }

    // Mobile device cell signal (dBm)
    private static Integer cellularSignal;
    public static Integer getCellularSignal() {
        return cellularSignal;
    }
    public static void setCellularSignal(Integer cellularSignal){
        MotorcycleData.cellularSignal = cellularSignal;
    }

    // Ignition Status
    private static Boolean ignitionStatus;
    public static Boolean getIgnitionStatus() {
        return ignitionStatus;
    }
    public static void setIgnitionStatus(Boolean ignitionStatus){
        MotorcycleData.ignitionStatus = ignitionStatus;
    }

    // VIN
    private static String vin;
    public static String getVin() {
        return vin;
    }
    public static void setVin(String vin){
        MotorcycleData.vin = vin;
    }

    // Next Service, km
    private static Integer nextService;
    public static Integer getNextService() {
        return nextService;
    }
    public static void setNextService(Integer nextService){
        MotorcycleData.nextService = nextService;
    }

    // Next Service, Date
    private static LocalDate nextServiceDate;
    public static LocalDate getNextServiceDate() {
        return nextServiceDate;
    }
    public static void setNextServiceDate(LocalDate nextServiceDate){
        MotorcycleData.nextServiceDate = nextServiceDate;
    }

    // Front Tire Pressure in bar
    private static Double frontTirePressure;
    public static Double getFrontTirePressure() {
        return frontTirePressure;
    }
    public static void setFrontTirePressure(Double pressure){
        MotorcycleData.frontTirePressure = pressure;
    }

    // Rear Tire Pressure in bar
    private static Double rearTirePressure;
    public static Double getRearTirePressure() {
        return rearTirePressure;
    }
    public static void setRearTirePressure(Double pressure){
        MotorcycleData.rearTirePressure = pressure;
    }

    // Ambient Temperature in C
    private static Double ambientTemperature;
    public static Double getAmbientTemperature() {
        return ambientTemperature;
    }
    public static void setAmbientTemperature(Double temperature){
        MotorcycleData.ambientTemperature = temperature;
    }

    // Engine Temperature in C
    private static Double engineTemperature;
    public static Double getEngineTemperature() {
        return engineTemperature;
    }
    public static void setEngineTemperature(Double temperature){
        MotorcycleData.engineTemperature = temperature;
    }

    // Odometer in km
    private static Double odometer;
    public static Double getOdometer() {
        return odometer;
    }
    public static void setOdometer(Double distance){
        MotorcycleData.odometer = distance;
    }

    // Trip One Distance in km
    private static Double tripOne;
    public static Double getTripOne() {
        return tripOne;
    }
    public static void setTripOne(Double distance){
        MotorcycleData.tripOne = distance;
    }

    // Trip Two Distance in km
    private static Double tripTwo;
    public static Double getTripTwo() {
        return tripTwo;
    }
    public static void setTripTwo(Double distance){
        MotorcycleData.tripTwo = distance;
    }

    // Trip Auto Distance in km
    private static Double tripAuto;
    public static Double getTripAuto() {
        return tripAuto;
    }
    public static void setTripAuto(Double distance){
        MotorcycleData.tripAuto = distance;
    }

    // Number of shifts
    private static Integer numberOfShifts = 0;
    public static Integer getNumberOfShifts() {
        return numberOfShifts;
    }
    public static void setNumberOfShifts(Integer shifts){
        MotorcycleData.numberOfShifts = shifts;
    }

    // RPM
    private static Integer rpm = 0;
    public static Integer getRPM() {
        return rpm;
    }
    public static void setRPM(Integer rpm){
        MotorcycleData.rpm = rpm;
    }

    // Gear
    private static String gear;
    public static String getGear() {
        return gear;
    }
    public static void setGear(String gear){
        MotorcycleData.gear = gear;
    }

    // Voltage
    private static Double voltage;
    public static Double getVoltage() {
        return voltage;
    }
    public static void setVoltage(Double voltage){
        MotorcycleData.voltage = voltage;
    }

    // Throttle Position
    private static Double throttlePosition;
    public static Double getThrottlePosition() {
        return throttlePosition;
    }
    public static void setThrottlePosition(Double throttlePosition){
        MotorcycleData.throttlePosition = throttlePosition;
    }

    // Front Brake
    private static Integer frontBrake = 0;
    public static Integer getFrontBrake() {
        return frontBrake;
    }
    public static void setFrontBrake(Integer frontBrake){
        MotorcycleData.frontBrake = frontBrake;
    }

    // Rear Brake
    private static Integer rearBrake = 0;
    public static Integer getRearBrake() {
        return rearBrake;
    }
    public static void setRearBrake(Integer rearBrake){
        MotorcycleData.rearBrake = rearBrake;
    }

    // Ambient Light
    private static Integer ambientLight;
    public static Integer getAmbientLight() {
        return ambientLight;
    }
    public static void setAmbientLight(Integer ambientLight){
        MotorcycleData.ambientLight = ambientLight;
    }

    // Speed
    private static Double speed;
    public static Double getSpeed() {
        return speed;
    }
    public static void setSpeed(Double speed){
        MotorcycleData.speed = speed;
    }

    // Average Speed
    private static Double avgSpeed;
    public static Double getAvgSpeed() {
        return avgSpeed;
    }
    public static void setAvgSpeed(Double avgSpeed){
        MotorcycleData.avgSpeed = avgSpeed;
    }

    // Current Consumption
    private static Double currentConsumption;
    public static Double getCurrentConsumption() {
        return currentConsumption;
    }
    public static void setCurrentConsumption(Double currentConsumption){
        MotorcycleData.currentConsumption = currentConsumption;
    }

    // Fuel Economy 1
    private static Double fuelEconomyOne;
    public static Double getFuelEconomyOne() {
        return fuelEconomyOne;
    }
    public static void setFuelEconomyOne(Double fuelEconomyOne){
        MotorcycleData.fuelEconomyOne = fuelEconomyOne;
    }

    // Fuel Economy 2
    private static Double fuelEconomyTwo;
    public static Double getFuelEconomyTwo() {
        return fuelEconomyTwo;
    }
    public static void setFuelEconomyTwo(Double fuelEconomyTwo){
        MotorcycleData.fuelEconomyTwo = fuelEconomyTwo;
    }

    // Fuel Range
    private static Double fuelRange;
    public static Double getFuelRange() {
        return fuelRange;
    }
    public static void setFuelRange(Double fuelRange){
        MotorcycleData.fuelRange = fuelRange;
    }

    // Lean Angle
    private static Double leanAngleDevice;
    public static Double getLeanAngleDevice() {
        return leanAngleDevice;
    }
    public static void setLeanAngleDevice(Double leanAngle){
        MotorcycleData.leanAngleDevice = leanAngle;

        //Store Max L and R lean angle
        if (leanAngle > 0) {
            if (MotorcycleData.getLeanAngleDeviceMaxR() == null) {
                MotorcycleData.setLeanAngleDeviceMaxR(leanAngle);
            } else if (leanAngle > MotorcycleData.getLeanAngleDeviceMaxR())  {
                MotorcycleData.setLeanAngleDeviceMaxR(leanAngle);
            }
        } else if (leanAngle < 0) {
            if (MotorcycleData.getLeanAngleDeviceMaxL() == null) {
                MotorcycleData.setLeanAngleDeviceMaxL(Math.abs(leanAngle));
            } else if (Math.abs(leanAngle) > MotorcycleData.getLeanAngleDeviceMaxL()) {
                MotorcycleData.setLeanAngleDeviceMaxL(Math.abs(leanAngle));
            }
        }
    }

    // Lean Angle Max
    private static Double leanAngleDeviceMaxL;
    public static Double getLeanAngleDeviceMaxL() {
        return leanAngleDeviceMaxL;
    }
    public static void setLeanAngleDeviceMaxL(Double leanAngle){
        MotorcycleData.leanAngleDeviceMaxL = leanAngle;
    }
    private static Double leanAngleDeviceMaxR;
    public static Double getLeanAngleDeviceMaxR() {
        return leanAngleDeviceMaxR;
    }
    public static void setLeanAngleDeviceMaxR(Double leanAngle){
        MotorcycleData.leanAngleDeviceMaxR = leanAngle;
    }

    // g-force
    private static Double gForce;
    public static Double getGForce() {
        return gForce;
    }
    public static void setGForce(Double gForce){
        MotorcycleData.gForce = gForce;
    }

    // bearing
    private static Integer bearing;
    public static Integer getBearing() {
        return bearing;
    }
    public static void setBearing(Integer bearing){
        MotorcycleData.bearing = bearing;
    }

    // time
    private static Date time;
    private static Date lastUpdateClusterClock;
    public static Date getTime() {
        return time;
    }
    public static void setTime(Date time){
        MotorcycleData.time = time;

        if ((MotorcycleData.lastUpdateClusterClock == null) || ((MotorcycleData.time.getTime() - MotorcycleData.lastUpdateClusterClock.getTime()) > MILLISECOND_DELAY_CLUSTER_UPDATE)) {
            try
            {
                com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService.setClusterClock(time);
            } finally {
                MotorcycleData.lastUpdateClusterClock = MotorcycleData.time;
            }
        }
    }

    // barometric pressure
    private static Double barometricPressure;
    public static Double getBarometricPressure() {
        return barometricPressure;
    }
    public static void setBarometricPressure(Double barometricPressure){
        MotorcycleData.barometricPressure = barometricPressure;
    }

    // Lean Angle Bike
    private static Double leanAngleBike;
    public static Double getLeanAngleBike() {
        return leanAngleBike;
    }
    public static void setLeanAngleBike(Double leanAngleBike){
        MotorcycleData.leanAngleBike = leanAngleBike;

        //Store Max L and R lean angle
        double leanAngleBikeFixed = leanAngleBike * 1;
        if(leanAngleBikeFixed > 0){
            if (MotorcycleData.getLeanAngleBikeMaxR() == null) {
                MotorcycleData.setLeanAngleBikeMaxR(leanAngleBikeFixed);
            } else if (leanAngleBikeFixed > MotorcycleData.getLeanAngleBikeMaxR()) {
                MotorcycleData.setLeanAngleBikeMaxR(leanAngleBikeFixed);
            }
        } else if(leanAngleBikeFixed < 0){
            if (MotorcycleData.getLeanAngleBikeMaxL() == null) {
                MotorcycleData.setLeanAngleBikeMaxL(Math.abs(leanAngleBikeFixed));
            } else if (Math.abs(leanAngleBikeFixed) > MotorcycleData.getLeanAngleBikeMaxL()) {
                MotorcycleData.setLeanAngleBikeMaxL(Math.abs(leanAngleBikeFixed));
            }
        }
    }

    // Lean Angle Bike Max
    private static Double leanAngleBikeMaxL;
    public static Double getLeanAngleBikeMaxL() {
        return leanAngleBikeMaxL;
    }
    public static void setLeanAngleBikeMaxL(Double leanAngleBikeMaxL){
        MotorcycleData.leanAngleBikeMaxL = leanAngleBikeMaxL;
    }
    private static Double leanAngleBikeMaxR;
    public static Double getLeanAngleBikeMaxR() {
        return leanAngleBikeMaxR;
    }
    public static void setLeanAngleBikeMaxR(Double leanAngleBikeMaxR){
        MotorcycleData.leanAngleBikeMaxR = leanAngleBikeMaxR;
    }

    // Rear Speed
    private static Double rearSpeed;
    public static Double getRearSpeed() {
        return rearSpeed;
    }
    public static void setRearSpeed(Double rearSpeed){
        MotorcycleData.rearSpeed = rearSpeed;
    }

    // Utility functions
    public static String getLabel(DataType dataPoint){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
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
        String temperatureUnit = "C";
        String temperatureFormat = sharedPrefs.getString("prefTempF", "0");
        if (temperatureFormat.contains("1")) {
            // F
            temperatureUnit = "F";
        }
        String distanceUnit = "km";
        String heightUnit = "m";
        String distanceTimeUnit = "kmh";
        String distanceFormat = sharedPrefs.getString("prefDistance", "0");
        if (distanceFormat.contains("1")) {
            distanceUnit = "mi";
            heightUnit = "ft";
            distanceTimeUnit = "mph";
        }
        String consumptionUnit = "L/100";
        String consumptionFormat = sharedPrefs.getString("prefConsumption", "0");
        if (consumptionFormat.contains("1")) {
            consumptionUnit = "mpg";
        } else if (consumptionFormat.contains("2")) {
            consumptionUnit = "mpg";
        } else if (consumptionFormat.contains("3")) {
            consumptionUnit = "km/L";
        }
        String voltageUnit = "V";
        String throttleUnit = "%";
        String signalUnit = "dBm";
        String batteryUnit = "%";
        String barometricUnit = "mBar";

        String label = " ";
        switch (dataPoint){
            case GEAR:
                label = MyApplication.getContext().getString(R.string.gear_label);
                break;
            case ENGINE_TEMP:
                label = MyApplication.getContext().getString(R.string.engine_temp_label) + "(" + temperatureUnit + ")";
                break;
            case AIR_TEMP:
                label = MyApplication.getContext().getString(R.string.ambient_temp_label) + "(" + temperatureUnit + ")";
                break;
            case FRONT_RDC:
                label = MyApplication.getContext().getString(R.string.frontpressure_header) + "(" + pressureUnit + ")";
                break;
            case REAR_RDC:
                label = MyApplication.getContext().getString(R.string.rearpressure_header) + "(" + pressureUnit + ")";
                break;
            case ODOMETER:
                label = MyApplication.getContext().getString(R.string.odometer_label) + "(" + distanceUnit + ")";
                break;
            case VOLTAGE:
                label = MyApplication.getContext().getString(R.string.voltage_label) + "(" + voltageUnit + ")";
                break;
            case THROTTLE:
                label = MyApplication.getContext().getString(R.string.throttle_label) + "(" + throttleUnit + ")";
                break;
            case FRONT_BRAKE:
                label = MyApplication.getContext().getString(R.string.frontbrakes_label);
                break;
            case REAR_BRAKE:
                label = MyApplication.getContext().getString(R.string.rearbrakes_label);
                break;
            case AMBIENT_LIGHT:
                label = MyApplication.getContext().getString(R.string.ambientlight_label);
                break;
            case TRIP_ONE:
                label = MyApplication.getContext().getString(R.string.trip1_label) + "(" + distanceUnit + ")";
                break;
            case TRIP_TWO:
                label = MyApplication.getContext().getString(R.string.trip2_label) + "(" + distanceUnit + ")";
                break;
            case TRIP_AUTO:
                label = MyApplication.getContext().getString(R.string.tripauto_label) + "(" + distanceUnit + ")";
                break;
            case SPEED:
                label = MyApplication.getContext().getString(R.string.speed_label) + "(" + distanceTimeUnit + ")";
                break;
            case AVG_SPEED:
                label = MyApplication.getContext().getString(R.string.avgspeed_label) + "(" + distanceTimeUnit + ")";
                break;
            case CURRENT_CONSUMPTION:
                label = MyApplication.getContext().getString(R.string.cconsumption_label) + "(" + consumptionUnit + ")";
                break;
            case ECONOMY_ONE:
                label = MyApplication.getContext().getString(R.string.fueleconomyone_label) + "(" + consumptionUnit + ")";
                break;
            case ECONOMY_TWO:
                label = MyApplication.getContext().getString(R.string.fueleconomytwo_label) + "(" + consumptionUnit + ")";
                break;
            case RANGE:
                label = MyApplication.getContext().getString(R.string.fuelrange_label) + "(" + distanceUnit + ")";
                break;
            case SHIFTS:
                label = MyApplication.getContext().getString(R.string.shifts_header);
                break;
            case LEAN_DEVICE:
                label = MyApplication.getContext().getString(R.string.leanangle_header);
                break;
            case GFORCE_DEVICE:
                label = MyApplication.getContext().getString(R.string.gforce_header);
                break;
            case BEARING_DEVICE:
                label = MyApplication.getContext().getString(R.string.bearing_header);
                break;
            case TIME_DEVICE:
                label = MyApplication.getContext().getString(R.string.time_header);
                break;
            case BAROMETRIC_DEVICE:
                label = MyApplication.getContext().getString(R.string.barometricpressure_header) + "(" + barometricUnit + ")";
                break;
            case SPEED_DEVICE:
                label = MyApplication.getContext().getString(R.string.gpsspeed_header) + "(" + distanceTimeUnit + ")";
                break;
            case ALTITUDE_DEVICE:
                label = MyApplication.getContext().getString(R.string.altitude_header) + "(" + heightUnit + ")";
                break;
            case SUN_DEVICE:
                label = MyApplication.getContext().getString(R.string.sunrisesunset_header);
                break;
            case RPM:
                label = MyApplication.getContext().getString(R.string.rpm_header);
                break;
            case LEAN_BIKE:
                label = MyApplication.getContext().getString(R.string.leanangle_bike_header);
                break;
            case REAR_SPEED:
                label = MyApplication.getContext().getString(R.string.rearwheel_speed_header) + "(" + distanceTimeUnit + ")";
                break;
            case CELL_SIGNAL:
                label = MyApplication.getContext().getString(R.string.cellular_signal_header) + "(" + signalUnit + ")";
                break;
            case BATTERY_DEVICE:
                label = MyApplication.getContext().getString(R.string.local_battery_header) + "(" + batteryUnit + ")";
                break;
            default:
                label = " ";
        }
        return label;
    }

    public static String getExtraKey(DataType dataPoint){
        String key = "";
        switch (dataPoint){
            case GEAR:
                key = "gear";
                break;
            case ENGINE_TEMP:
                key = "engineTemperature";
                break;
            case AIR_TEMP:
                key = "ambientTemperature";
                break;
            case FRONT_RDC:
                key = "frontTirePressure";
                break;
            case REAR_RDC:
                key = "rearTirePressure";
                break;
            case ODOMETER:
                key = "odometer";
                break;
            case VOLTAGE:
                key = "voltage";
                break;
            case THROTTLE:
                key = "throttlePosition";
                break;
            case FRONT_BRAKE:
                key = "frontBrake";
                break;
            case REAR_BRAKE:
                key = "rearBrake";
                break;
            case AMBIENT_LIGHT:
                key = "ambientLight";
                break;
            case TRIP_ONE:
                key = "tripOne";
                break;
            case TRIP_TWO:
                key = "tripTwo";
                break;
            case TRIP_AUTO:
                key = "tripAuto";
                break;
            case SPEED:
                key = "speed";
                break;
            case AVG_SPEED:
                key = "avgSpeed";
                break;
            case CURRENT_CONSUMPTION:
                key = "currentConsumption";
                break;
            case ECONOMY_ONE:
                key = "fuelEconomyOne";
                break;
            case ECONOMY_TWO:
                key = "fuelEconomyTwo";
                break;
            case RANGE:
                key = "fuelRange";
                break;
            case SHIFTS:
                key = "numberOfShifts";
                break;
            case LEAN_DEVICE:
                key = "leanAngleDevice";
                break;
            case GFORCE_DEVICE:
                key = "gForce";
                break;
            case BEARING_DEVICE:
                key = "bearing";
                break;
            case TIME_DEVICE:
                key = "time";
                break;
            case BAROMETRIC_DEVICE:
                key = "barometricPressure";
                break;
            case SPEED_DEVICE:
                key = "gpsSpeed";
                break;
            case ALTITUDE_DEVICE:
                key = "gpsAltitude";
                break;
            case SUN_DEVICE:
                key = "sunRiseSet";
                break;
            case RPM:
                key = "rpm";
                break;
            case LEAN_BIKE:
                key = "leanAngleDevice";
                break;
            case REAR_SPEED:
                key = "rearSpeed";
                break;
            case CELL_SIGNAL:
                key = "cellSignal";
                break;
            case BATTERY_DEVICE:
                key = "battery";
                break;
            default:
                key = "?";
        }
        return key;
    }

    public static Drawable getIcon(DataType dataPoint){
        Drawable icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_cog);
        switch (dataPoint){
            case GEAR:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_cog);
                break;
            case ENGINE_TEMP:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_engine_temp);
                if(MotorcycleData.getEngineTemperature() != null ){
                    double engineTemp = MotorcycleData.getEngineTemperature();
                    if (engineTemp >= 104.0){
                        icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                    }
                }
                break;
            case AIR_TEMP:
                if(MotorcycleData.getAmbientTemperature() != null ){
                    double ambientTemp = MotorcycleData.getAmbientTemperature();
                    if(ambientTemp <= 0){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_snowflake);
                        icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_blue), PorterDuff.Mode.SRC_ATOP);
                    } else {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_thermometer_half);
                    }
                } else {
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_thermometer_half);
                }
                break;
            case FRONT_RDC:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire);
                if (Faults.getFrontTirePressureCriticalActive()){
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                } else if (Faults.getFrontTirePressureWarningActive()){
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                }
                break;
            case REAR_RDC:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire);
                if (Faults.getRearTirePressureCriticalActive()){
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                } else if (Faults.getRearTirePressureWarningActive()){
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                }
                break;
            case ODOMETER:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_dashboard_meter);
                break;
            case VOLTAGE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_car_battery);
                break;
            case THROTTLE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_signature);
                break;
            case FRONT_BRAKE:
            case REAR_BRAKE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_brakes);
                break;
            case AMBIENT_LIGHT:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_lightbulb);
                break;
            case TRIP_ONE:
            case TRIP_TWO:
            case TRIP_AUTO:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_suitcase);
                break;
            case SPEED:
            case AVG_SPEED:
            case SPEED_DEVICE:
            case RPM:
            case REAR_SPEED:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tachometer_alt);
                break;
            case CURRENT_CONSUMPTION:
            case ECONOMY_ONE:
            case ECONOMY_TWO:
            case RANGE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_gas_pump);
                break;
            case SHIFTS:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_arrows_alt_v);
                break;
            case LEAN_DEVICE:
            case LEAN_BIKE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_angle);
                break;
            case GFORCE_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_accelerometer);
                break;
            case BEARING_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_compass);
                break;
            case TIME_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_clock);
                break;
            case BAROMETRIC_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_barometer);
                break;
            case ALTITUDE_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_mountain);
                break;
            case SUN_DEVICE:
                if (MotorcycleData.getLastLocation() != null && MotorcycleData.getTime() != null) {
                    Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(Calendar.getInstance(), MotorcycleData.getLastLocation().getLatitude(), MotorcycleData.getLastLocation().getLongitude());
                    Date sunrise = sunriseSunset[0].getTime();
                    Date sunset = sunriseSunset[1].getTime();
                    if(MotorcycleData.getTime().compareTo(sunrise) > 0 && MotorcycleData.getTime().compareTo(sunset) < 0){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_sun);
                    } else {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_moon);
                    }

                } else {
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_sun);
                }
                break;
            case CELL_SIGNAL:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_0);
                if(MotorcycleData.getCellularSignal() != null){
                    int signal = MotorcycleData.getCellularSignal();
                    if (signal > -79) {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_4);
                    } else if (signal > -89 && signal < -80) {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_3);
                    } else if (signal > -99 && signal < -90) {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_2);
                    } else if (signal > -109 && signal < -100) {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_1);
                    } else if (signal < -110) {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_0);
                    }
                }
                break;
            case BATTERY_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_empty);
                if(MotorcycleData.getLocalBattery() != null){
                    double battery = MotorcycleData.getLocalBattery();
                    if(battery > 95){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_full);
                    } else if(battery > 75 && battery < 95){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_three_quarters);
                    } else if(battery > 50 && battery < 75){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_half);
                    } else if(battery > 25 && battery < 50){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_quarter);
                    } else if(battery > 0 && battery < 25){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_empty);
                        icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                    }
                }
                break;
            default:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_cog);
        }
        return icon;
    }

    public static CarIcon getCarIcon(DataType dataPoint){
        IconCompat icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_cog);
        CarColor carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.white),MyApplication.getContext().getResources().getColor(R.color.black));

        switch (dataPoint){
            case GEAR:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_cog);
                break;
            case ENGINE_TEMP:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_engine_temp);
                if(MotorcycleData.getEngineTemperature() != null ){
                    double engineTemp = MotorcycleData.getEngineTemperature();
                    if (engineTemp >= 104.0){
                        carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.motorrad_red),MyApplication.getContext().getResources().getColor(R.color.motorrad_red));
                    }
                }
                break;
            case AIR_TEMP:
                if(MotorcycleData.getAmbientTemperature() != null ){
                    double ambientTemp = MotorcycleData.getAmbientTemperature();
                    if(ambientTemp <= 0){
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_snowflake);
                        carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.motorrad_blue),MyApplication.getContext().getResources().getColor(R.color.motorrad_blue));
                    } else {
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_thermometer_half);
                    }
                } else {
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_thermometer_half);
                }
                break;
            case FRONT_RDC:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire);
                if (Faults.getFrontTirePressureCriticalActive()){
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire_alert);
                    carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.motorrad_red),MyApplication.getContext().getResources().getColor(R.color.motorrad_red));
                } else if (Faults.getFrontTirePressureWarningActive()){
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire_alert);
                    carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.yellow),MyApplication.getContext().getResources().getColor(R.color.yellow));
                }
                break;
            case REAR_RDC:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire);
                if (Faults.getRearTirePressureCriticalActive()){
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire_alert);
                    carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.motorrad_red),MyApplication.getContext().getResources().getColor(R.color.motorrad_red));
                } else if (Faults.getRearTirePressureWarningActive()){
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire_alert);
                    carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.yellow),MyApplication.getContext().getResources().getColor(R.color.yellow));
                }
                break;
            case ODOMETER:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_dashboard_meter);
                break;
            case VOLTAGE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_car_battery);
                break;
            case THROTTLE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_signature);
                break;
            case FRONT_BRAKE:
            case REAR_BRAKE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_brakes);
                break;
            case AMBIENT_LIGHT:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_lightbulb);
                break;
            case TRIP_ONE:
            case TRIP_TWO:
            case TRIP_AUTO:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_suitcase);
                break;
            case SPEED:
            case AVG_SPEED:
            case SPEED_DEVICE:
            case RPM:
            case REAR_SPEED:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tachometer_alt);
                break;
            case CURRENT_CONSUMPTION:
            case ECONOMY_ONE:
            case ECONOMY_TWO:
            case RANGE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_gas_pump);
                break;
            case SHIFTS:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_arrows_alt_v);
                break;
            case LEAN_DEVICE:
            case LEAN_BIKE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_angle);
                break;
            case GFORCE_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_accelerometer);
                break;
            case BEARING_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_compass);
                break;
            case TIME_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_clock);
                break;
            case BAROMETRIC_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_barometer);
                break;
            case ALTITUDE_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_mountain);
                break;
            case SUN_DEVICE:
                if (MotorcycleData.getLastLocation() != null && MotorcycleData.getTime() != null) {
                    Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(Calendar.getInstance(), MotorcycleData.getLastLocation().getLatitude(), MotorcycleData.getLastLocation().getLongitude());
                    Date sunrise = sunriseSunset[0].getTime();
                    Date sunset = sunriseSunset[1].getTime();
                    if(MotorcycleData.getTime().compareTo(sunrise) > 0 && MotorcycleData.getTime().compareTo(sunset) < 0){
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_sun);
                    } else {
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_moon);
                    }

                } else {
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_sun);
                }
                break;
            case CELL_SIGNAL:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.signal_bar_0);
                if(MotorcycleData.getCellularSignal() != null){
                    int signal = MotorcycleData.getCellularSignal();
                    if (signal > -79) {
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.signal_bar_4);
                    } else if (signal > -89 && signal < -80) {
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.signal_bar_3);
                    } else if (signal > -99 && signal < -90) {
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.signal_bar_2);
                    } else if (signal > -109 && signal < -100) {
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.signal_bar_1);
                    } else if (signal < -110) {
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.signal_bar_0);
                    }
                }
                break;
            case BATTERY_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.battery_empty);
                if(MotorcycleData.getLocalBattery() != null){
                    double battery = MotorcycleData.getLocalBattery();
                    if(battery > 95){
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.battery_full);
                    } else if(battery > 75 && battery < 95){
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.battery_three_quarters);
                    } else if(battery > 50 && battery < 75){
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.battery_half);
                    } else if(battery > 25 && battery < 50){
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.battery_quarter);
                    } else if(battery > 0 && battery < 25){
                        icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.battery_empty);
                        carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.motorrad_red),MyApplication.getContext().getResources().getColor(R.color.motorrad_red));
                    }
                }
                break;
            default:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_cog);
        }
        return new CarIcon.Builder(icon).setTint(carColor).build();
    }

    public static String getValue(DataType dataPoint){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
        String temperatureFormat = sharedPrefs.getString("prefTempF", "0");
        String distanceFormat = sharedPrefs.getString("prefDistance", "0");
        String consumptionFormat = sharedPrefs.getString("prefConsumption", "0");

        String value = "";
        switch (dataPoint){
            case GEAR:
                if(MotorcycleData.getGear() != null){
                    value = MotorcycleData.getGear();
                }
                break;
            case ENGINE_TEMP:
                if(MotorcycleData.getEngineTemperature() != null ){
                    double engineTemp = MotorcycleData.getEngineTemperature();
                    if (temperatureFormat.contains("1")) {
                        // F
                        engineTemp = Utils.celsiusToFahrenheit(engineTemp);
                    }
                    value = (Utils.toZeroDecimalString(engineTemp));
                }
                break;
            case AIR_TEMP:
                if(MotorcycleData.getAmbientTemperature() != null ){
                    double ambientTemp = MotorcycleData.getAmbientTemperature();
                    if (temperatureFormat.contains("1")) {
                        // F
                        ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                    }
                    value = (Utils.toZeroDecimalString(ambientTemp));
                }
                break;
            case FRONT_RDC:
                if(MotorcycleData.getFrontTirePressure() != null){
                    double rdcFront = MotorcycleData.getFrontTirePressure();
                    if (pressureFormat.contains("1")) {
                        // KPa
                        rdcFront = Utils.barTokPa(rdcFront);
                    } else if (pressureFormat.contains("2")) {
                        // Kg-f
                        rdcFront = Utils.barToKgF(rdcFront);
                    } else if (pressureFormat.contains("3")) {
                        // Psi
                        rdcFront = Utils.barToPsi(rdcFront);
                    }
                    value = Utils.toOneDecimalString(rdcFront);
                }
                break;
            case REAR_RDC:
                if(MotorcycleData.getRearTirePressure() != null){
                    double rdcRear = MotorcycleData.getRearTirePressure();
                    if (pressureFormat.contains("1")) {
                        // KPa
                        rdcRear = Utils.barTokPa(rdcRear);
                    } else if (pressureFormat.contains("2")) {
                        // Kg-f
                        rdcRear = Utils.barToKgF(rdcRear);
                    } else if (pressureFormat.contains("3")) {
                        // Psi
                        rdcRear = Utils.barToPsi(rdcRear);
                    }
                    value = Utils.toOneDecimalString(rdcRear);
                }
                break;
            case ODOMETER:
                if(MotorcycleData.getOdometer() != null){
                    double odometer = MotorcycleData.getOdometer();
                    if (distanceFormat.contains("1")) {
                        odometer = Utils.kmToMiles(odometer);
                    }
                    value = (Utils.toZeroDecimalString(odometer));
                }
                break;
            case VOLTAGE:
                if(MotorcycleData.getVoltage() != null){
                    Double voltage = MotorcycleData.getVoltage();
                    value = (Utils.toOneDecimalString(voltage));
                }
                break;
            case THROTTLE:
                if(MotorcycleData.getThrottlePosition() != null){
                    Double throttlePosition = MotorcycleData.getThrottlePosition();
                    value = (Utils.toZeroDecimalString(throttlePosition));
                }
                break;
            case FRONT_BRAKE:
                if((MotorcycleData.getFrontBrake() != null) && (MotorcycleData.getFrontBrake() != 0)){
                    Integer frontBrakes = MotorcycleData.getFrontBrake();
                    value = String.valueOf(frontBrakes);
                }
                break;
            case REAR_BRAKE:
                if((MotorcycleData.getRearBrake() != null) && (MotorcycleData.getRearBrake() != 0)){
                    Integer rearBrakes = MotorcycleData.getRearBrake();
                    value = String.valueOf(rearBrakes);
                }
                break;
            case AMBIENT_LIGHT:
                if(MotorcycleData.getAmbientLight() != null){
                    Integer ambientLight = MotorcycleData.getAmbientLight();
                    value = String.valueOf(ambientLight);
                }
                break;
            case TRIP_ONE:
                if(MotorcycleData.getTripOne() != null) {
                    double trip1 = MotorcycleData.getTripOne();
                    if (distanceFormat.contains("1")) {
                        trip1 = Utils.kmToMiles(trip1);
                    }
                    value = Utils.toOneDecimalString(trip1);
                }
                break;
            case TRIP_TWO:
                if(MotorcycleData.getTripTwo() != null){
                    double trip2 = MotorcycleData.getTripTwo();
                    if (distanceFormat.contains("1")) {
                        trip2 = Utils.kmToMiles(trip2);
                    }
                    value = Utils.toOneDecimalString(trip2);
                }
                break;
            case TRIP_AUTO:
                if(MotorcycleData.getTripAuto() != null){
                    double tripAuto = MotorcycleData.getTripAuto();
                    if (distanceFormat.contains("1")) {
                        tripAuto = Utils.kmToMiles(tripAuto);
                    }
                    value = Utils.toOneDecimalString(tripAuto);
                }
                break;
            case SPEED:
                if(MotorcycleData.getSpeed() != null){
                    double speed = MotorcycleData.getSpeed();
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    value = (Utils.toZeroDecimalString(speed));
                }
                break;
            case AVG_SPEED:
                if(MotorcycleData.getAvgSpeed() != null){
                    double avgSpeed = MotorcycleData.getAvgSpeed();
                    if (distanceFormat.contains("1")) {
                        avgSpeed = Utils.kmToMiles(avgSpeed);
                    }
                    value = Utils.toOneDecimalString(avgSpeed);
                }
                break;
            case CURRENT_CONSUMPTION:
                if(MotorcycleData.getCurrentConsumption() != null){
                    double currentConsumption = MotorcycleData.getCurrentConsumption();
                    if (consumptionFormat.contains("1")) {
                        currentConsumption = Utils.l100ToMpg(currentConsumption);
                    } else if (consumptionFormat.contains("2")) {
                        currentConsumption = Utils.l100ToMpgI(currentConsumption);
                    } else if (consumptionFormat.contains("3")) {
                        currentConsumption = Utils.l100ToKmL(currentConsumption);
                    }
                    value = (Utils.toOneDecimalString(currentConsumption));
                }
                break;
            case ECONOMY_ONE:
                if(MotorcycleData.getFuelEconomyOne() != null){
                    double fuelEconomyOne = MotorcycleData.getFuelEconomyOne();
                    if (consumptionFormat.contains("1")) {
                        fuelEconomyOne = Utils.l100ToMpg(fuelEconomyOne);
                    } else if (consumptionFormat.contains("2")) {
                        fuelEconomyOne = Utils.l100ToMpgI(fuelEconomyOne);
                    } else if (consumptionFormat.contains("3")) {
                        fuelEconomyOne = Utils.l100ToKmL(fuelEconomyOne);
                    }
                    value = (Utils.toOneDecimalString(fuelEconomyOne));
                }
                break;
            case ECONOMY_TWO:
                if(MotorcycleData.getFuelEconomyTwo() != null){
                    double fuelEconomyTwo = MotorcycleData.getFuelEconomyTwo();
                    if (consumptionFormat.contains("1")) {
                        fuelEconomyTwo = Utils.l100ToMpg(fuelEconomyTwo);
                    } else if (consumptionFormat.contains("2")) {
                        fuelEconomyTwo  = Utils.l100ToMpgI(fuelEconomyTwo);
                    } else if (consumptionFormat.contains("3")) {
                        fuelEconomyTwo  = Utils.l100ToKmL(fuelEconomyTwo);
                    }
                    value = (Utils.toOneDecimalString(fuelEconomyTwo));
                }
                break;
            case RANGE:
                if(MotorcycleData.getFuelRange() != null){
                    double fuelRange = MotorcycleData.getFuelRange();
                    if (distanceFormat.contains("1")) {
                        fuelRange = Utils.kmToMiles(fuelRange);
                    }
                    value = (Utils.toZeroDecimalString(fuelRange));
                }
                break;
            case SHIFTS:
                if(MotorcycleData.getNumberOfShifts() != null){
                    int shifts = MotorcycleData.getNumberOfShifts();
                    value = String.valueOf(shifts);
                }
                break;
            case LEAN_DEVICE:
                if(MotorcycleData.getLeanAngleDevice() != null){
                    Double leanAngle = MotorcycleData.getLeanAngleDevice();
                    value = (Utils.toZeroDecimalString(leanAngle));
                }
                break;
            case GFORCE_DEVICE:
                if(MotorcycleData.getGForce() != null){
                    Double gForce = MotorcycleData.getGForce();
                    value = (Utils.toOneDecimalString(gForce));
                }
                break;
            case BEARING_DEVICE:
                if (MotorcycleData.getBearing() != null) {
                    Integer bearingValue = MotorcycleData.getBearing();
                    String bearing = bearingValue.toString() + "°";
                    if (!sharedPrefs.getString("prefBearing", "0").contains("0")) {
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
                        }
                    }
                    value = bearing;
                }
                break;
            case TIME_DEVICE:
                if (MotorcycleData.getTime() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    if (!sharedPrefs.getString("prefTime", "0").equals("0")) {
                        dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    }
                    value = dateFormat.format(MotorcycleData.getTime());
                }
                break;
            case BAROMETRIC_DEVICE:
                if (MotorcycleData.getBarometricPressure() != null) {
                    value = (Utils.toZeroDecimalString(MotorcycleData.getBarometricPressure()));
                }
                break;
            case SPEED_DEVICE:
                String gpsSpeed = MyApplication.getContext().getString(R.string.gps_nofix);
                if (MotorcycleData.getLastLocation() != null){
                    gpsSpeed = (Utils.toZeroDecimalString(MotorcycleData.getLastLocation().getSpeed() * 3.6));
                    if (distanceFormat.contains("1")) {
                        gpsSpeed = (Utils.toZeroDecimalString(Utils.kmToMiles(MotorcycleData.getLastLocation().getSpeed() * 3.6)));
                    }
                }
                value = gpsSpeed;
                break;
            case ALTITUDE_DEVICE:
                String altitude = MyApplication.getContext().getString(R.string.gps_nofix);
                if (MotorcycleData.getLastLocation() != null){
                    altitude = (Utils.toZeroDecimalString(MotorcycleData.getLastLocation().getAltitude()));
                    if (distanceFormat.contains("1")) {
                        altitude = (Utils.toZeroDecimalString(Utils.mToFeet(MotorcycleData.getLastLocation().getAltitude())));
                    }
                }
                value = altitude;
                break;
            case SUN_DEVICE:
                value = MyApplication.getContext().getString(R.string.gps_nofix);
                if (MotorcycleData.getLastLocation() != null) {
                    Calendar[] sunriseSunset = ca.rmen.sunrisesunset.SunriseSunset.getSunriseSunset(Calendar.getInstance(), MotorcycleData.getLastLocation().getLatitude(), MotorcycleData.getLastLocation().getLongitude());
                    Date sunrise = sunriseSunset[0].getTime();
                    Date sunset = sunriseSunset[1].getTime();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    if (!sharedPrefs.getString("prefTime", "0").equals("0")) {
                        dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    }
                    String sunriseString = dateFormat.format(sunrise);
                    String sunsetString = dateFormat.format(sunset);
                    value = sunriseString + "/" + sunsetString;
                }
                break;
            case RPM:
                if (MotorcycleData.getRPM() > 0){
                    value = String.valueOf(MotorcycleData.getRPM());
                }
                break;
            case LEAN_BIKE:
                if(MotorcycleData.getLeanAngleBike() != null){
                    Double leanAngleBike = MotorcycleData.getLeanAngleBike();
                    value = (Utils.toZeroDecimalString(leanAngleBike));
                }
                break;
            case REAR_SPEED:
                if(MotorcycleData.getRearSpeed() != null){
                    double speed = MotorcycleData.getRearSpeed();
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    value = (Utils.toZeroDecimalString(speed));
                }
                break;
            case CELL_SIGNAL:
                if(MotorcycleData.getCellularSignal() != null){
                    value = String.valueOf(MotorcycleData.getCellularSignal());
                }
                break;
            case BATTERY_DEVICE:
                if(MotorcycleData.getLocalBattery() != null){
                    value = (Utils.toZeroDecimalString(MotorcycleData.getLocalBattery()));
                }
                break;
            default:
                value = MyApplication.getContext().getString(R.string.blank_field);
        }
        return value;
    }



    public static String  getLabel(int dataPoint) {
        return getLabel(DataType.values()[dataPoint]);
    }

    public static String  getExtraKey(int dataPoint) {
        return getExtraKey(DataType.values()[dataPoint]);
    }

    public static Drawable  getIcon(int dataPoint) {
        return getIcon(DataType.values()[dataPoint]);
    }

    public static CarIcon getCarIcon(int dataPoint){
        return getCarIcon(DataType.values()[dataPoint]);
    }
    public static String getValue(int dataPoint) {
        return getValue(DataType.values()[dataPoint]);
    }

    /*
    public static void clear(){
        Data.lastLocation = null;
        Data.lastMessage = null;
        Data.vin = null;
        Data.nextService = null;
        Data.nextServiceDate = null;
        Data.frontTirePressure = null;
        Data.rearTirePressure = null;
        Data.ambientTemperature = null;
        Data.engineTemperature = null;
        Data.odometer = null;
        Data.tripOne = null;
        Data.tripTwo = null;
        Data.gear = null;
        Data.voltage = null;
        Data.throttlePosition = null;
        Data.ambientLight = null;
        Data.speed = null;
        Data.avgSpeed = null;
        Data.currentConsumption = null;
        Data.fuelEconomyOne = null;
        Data.fuelEconomyTwo = null;
        Data.fuelRange = null;
        Data.leanAngleDevice = null;
        Data.gForce = null;
        Data.bearing = null;
        Data.barometricPressure = null;
        Data.leanAngleBike = null;
        Data.rearSpeed = null;
    }
     */






    public static  Object[] getCombinedData(DataType dataPoint){
        String value = "";
        String label = "  "; //default label is empty
        Drawable icon = null; //default icon is always null
        Integer valueColor = null; //default text color is null

        try {
            switch ( dataPoint){

                case GEAR:
                    value = MotorcycleData.getGear();
                    label = MyApplication.getContext().getString(R.string.gear_label);
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_cog);

                    if(!StringUtils.isEmpty(value)){
                        if (value.equals("N")) {
                            valueColor =  ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_green) ;
                        } else if ("123456".contains(value)) {
                            valueColor =  ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_yellow);
                        }
                    }
                    break;


                case ENGINE_TEMP:
                    Double engineTemp = MotorcycleData.getEngineTemperature();
                    label = MemCache.temperatureUnitEngine();
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_engine_temp);

                    if(engineTemp != null ) {
                        if (engineTemp >= CRITICAL_ENGINE_TEMP_C){
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                            valueColor =  ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red);
                        } else if (engineTemp <= CRITICAL_ENGINE_TEMP_LOW_C) {
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_blue), PorterDuff.Mode.SRC_ATOP);
                            valueColor =  ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_blue);
                        }
                        if (MemCache.temperatureFormat().equals("1")) {// F
                            engineTemp = Utils.celsiusToFahrenheit(engineTemp);
                        }
                        value = Utils.toZeroDecimalString(engineTemp);
                    }
                    break;


                case AIR_TEMP:
                    Double ambientTemp = MotorcycleData.getAmbientTemperature();
                    label = MemCache.temperatureUnitAir();

                    if(ambientTemp != null ){
                        if(ambientTemp <= CRITICAL_AIR_TEMP_LOW_C){
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_snowflake);
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_blue), PorterDuff.Mode.SRC_ATOP);
                            valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_blue);
                        } else {
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_thermometer_half);
                            if (ambientTemp > CRITICAL_AIR_TEMP_HIGH_C) {
                                icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                                valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red);
                            }
                        }
                        if (MemCache.temperatureFormat().equals("1")) {// F
                            ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                        }
                        value = Utils.toZeroDecimalString(ambientTemp);
                    }
                    break;


                case FRONT_RDC:
                    Double rdcFront = MotorcycleData.getFrontTirePressure();
                    label = MemCache.pressureUnitLabelF();
                    if (Faults.getFrontTirePressureCriticalActive()){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                        icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                        valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red);
                    } else if (Faults.getFrontTirePressureWarningActive()){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                        icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow);
                    } else {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire);
                    }
                    if(rdcFront != null) {
                        switch (MemCache.pressureFormat()) {
                            case "1": // KPa
                                rdcFront = Utils.barTokPa(rdcFront); break;
                            case "2": // Kg-f
                                rdcFront = Utils.barToKgF(rdcFront); break;
                            case "3": // Psi
                                rdcFront = Utils.barToPsi(rdcFront); break;
                        }
                        value = Utils.toOneDecimalString(rdcFront);
                    }
                    break;


                case REAR_RDC:
                    Double rdcRear = MotorcycleData.getRearTirePressure();
                    label = MemCache.pressureUnitLabelR();
                    if (Faults.getRearTirePressureCriticalActive()){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                        icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                        valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red);
                    } else if (Faults.getRearTirePressureWarningActive()){
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                        icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow);
                    } else {
                        icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire);
                    }
                    if(rdcRear != null) {
                        switch (MemCache.pressureFormat()) {
                            case "1": // KPa
                                rdcRear = Utils.barTokPa(rdcRear);break;
                            case "2":// Kg-f
                                rdcRear = Utils.barToKgF(rdcRear);break;
                            case "3":// Psi
                                rdcRear = Utils.barToPsi(rdcRear);break;
                        }
                        value = Utils.toOneDecimalString(rdcRear);
                    }
                    break;


                case ODOMETER:
                    Double odometer = MotorcycleData.getOdometer();
                    label = MemCache.odometerLabel();
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_dashboard_meter);
                    if(odometer != null){
                        if (MemCache.distanceFormat().equals("1")) {
                            odometer = Utils.kmToMiles(odometer);
                        }

                        // Wrap odometer across two lines for easier reading in cell
                        // consider checking if the display is in portrait mode as well
                        value = Utils.toZeroDecimalString(odometer,true);
                    }
                    break;



                case VOLTAGE:
                    Double voltage = MotorcycleData.getVoltage();
                    label = MemCache.voltageUnitLabel();
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_car_battery);
                    if(voltage != null){
                        value = Utils.toOneDecimalString(voltage);

                        if (voltage >= CRITICAL_BATTERY_VOLTAGE_HIGH) {
                            valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red);
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                        } else if (voltage < CRITICAL_BATTERY_VOLTAGE_LOW) {
                            valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow);
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        }
                    }
                    break;



                case THROTTLE:
                    Double throttlePosition = MotorcycleData.getThrottlePosition();
                    label = MemCache.throttleUnitLabel();
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_signature);
                    if (throttlePosition != null){
                        value = Utils.toZeroDecimalString(throttlePosition);
                    }
                    break;



                case FRONT_BRAKE:
                    Integer frontBrakes = MotorcycleData.getFrontBrake();
                    label = MemCache.brakeLabelF();
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_brakes);

                    if((frontBrakes != null) && (frontBrakes != 0)){
                        value = String.valueOf(frontBrakes);
                    }
                    break;


                case REAR_BRAKE:
                    Integer rearBrakes = MotorcycleData.getRearBrake();
                    label = MemCache.brakeLabelR();
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_brakes);
                    if((rearBrakes != null) && (rearBrakes != 0)){
                        value = String.valueOf(rearBrakes);
                    }
                    break;



                case AMBIENT_LIGHT:
                    Integer ambientLight = MotorcycleData.getAmbientLight();
                    label = MyApplication.getContext().getString(R.string.ambientlight_label);
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_lightbulb);
                    if(ambientLight != null){
                        value = String.valueOf(ambientLight);
                    }
                    break;



                case TRIP_ONE:
                    Double trip1 = MotorcycleData.getTripOne();
                    label = MemCache.distanceUnitLabel(); // MyApplication.getContext().getString(R.string.trip1_label) + " (" + distanceUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_suitcase);
                    if(trip1 != null) {
                        if (MemCache. distanceFormat().equals("1")) {
                            trip1 = Utils.kmToMiles(trip1);
                        }
                        value = Utils.toZeroDecimalString(trip1);
                    }
                    break;



                case TRIP_TWO:
                    Double trip2 = MotorcycleData.getTripTwo();
                    label = MemCache.trip2Label(); // MyApplication.getContext().getString(R.string.trip2_label) + " (" + distanceUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_suitcase);
                    if(trip2 != null){
                        if (MemCache. distanceFormat().equals("1")) {
                            trip2 = Utils.kmToMiles(trip2);
                        }

                        value = Utils.toZeroDecimalString(trip2);
                    }
                    break;



                case TRIP_AUTO:
                    Double tripAuto = MotorcycleData.getTripAuto();
                    label = MemCache.tripAutoLabel(); // MyApplication.getContext().getString(R.string.trip_auto_label) + " (" + distanceUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_suitcase);
                    if(tripAuto != null){
                        if (MemCache.distanceFormat().equals("1")) {
                            tripAuto = Utils.kmToMiles(tripAuto);
                        }

                        value = Utils.toZeroDecimalString(tripAuto);
                    }
                    break;



                case SPEED:
                    Double speedo = MotorcycleData.getSpeed();
                    label = MemCache.speedLabel(); // MyApplication.getContext().getString(R.string.speed_label) + " (" + distanceTimeUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tachometer_alt);

                    if (speedo != null){
                        if (MemCache.distanceFormat().equals("1")) {
                            speedo = Utils.kmToMiles(speedo);
                        }
                        value = Utils.toZeroDecimalString(speedo);
                    }
                    break;



                case AVG_SPEED:
                    Double avgSpeed = MotorcycleData.getAvgSpeed();
                    label = MemCache.avgSpeedLabel(); // MyApplication.getContext().getString(R.string.avg_speed_label) + " (" + distanceTimeUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tachometer_alt);

                    if (avgSpeed != null) {
                        if (MemCache.distanceFormat().equals("1")) {
                            avgSpeed = Utils.kmToMiles(avgSpeed);
                        }
                        value = Utils.toOneDecimalString(avgSpeed);
                    }
                    break;


                case CURRENT_CONSUMPTION:
                    Double currentConsumption = MotorcycleData.getCurrentConsumption();
                    label = MemCache.consumptionLabel(); // MyApplication.getContext().getString(R.string.cConsumption_label) + " (" + consumptionUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_gas_pump);

                    if (currentConsumption != null){
                        switch (MemCache.consumptionFormat()) {
                            case "1":
                                currentConsumption = Utils.l100ToMpg(currentConsumption); break;
                            case "2":
                                currentConsumption = Utils.l100ToMpgI(currentConsumption); break;
                            case "3":
                                currentConsumption = Utils.l100ToKmL(currentConsumption); break;
                        }
                        value = Utils.toOneDecimalString(currentConsumption);
                    }
                    break;


                case ECONOMY_ONE:
                    Double fuelEconomyOne = MotorcycleData.getFuelEconomyOne();
                    label = MemCache.economy1Label(); // MyApplication.getContext().getString(R.string.fuel_economy_one_label) + " (" + consumptionUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_gas_pump);

                    if( MotorcycleData.getFuelEconomyOne() != null){
                        switch (MemCache.consumptionFormat()) {
                            case "1":
                                fuelEconomyOne = Utils.l100ToMpg(fuelEconomyOne); break;
                            case "2":
                                fuelEconomyOne = Utils.l100ToMpgI(fuelEconomyOne); break;
                            case "3":
                                fuelEconomyOne = Utils.l100ToKmL(fuelEconomyOne); break;
                        }
                        value = Utils.toOneDecimalString(fuelEconomyOne);
                    }
                    break;


                case ECONOMY_TWO:
                    Double fuelEconomyTwo = MotorcycleData.getFuelEconomyTwo();
                    label = MemCache.economy2Label(); // MyApplication.getContext().getString(R.string.fuel_economy_two_label) + " (" + consumptionUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_gas_pump);

                    if(fuelEconomyTwo != null){
                        switch (MemCache.consumptionFormat()) {
                            case "1":
                                fuelEconomyTwo = Utils.l100ToMpg(fuelEconomyTwo); break;
                            case "2":
                                fuelEconomyTwo = Utils.l100ToMpgI(fuelEconomyTwo); break;
                            case "3":
                                fuelEconomyTwo = Utils.l100ToKmL(fuelEconomyTwo); break;
                        }
                        value = Utils.toOneDecimalString(fuelEconomyTwo);
                    }
                    break;


                case RANGE:
                    Double fuelRange = MotorcycleData.getFuelRange();
                    label = MemCache.rangeLabel(); // MyApplication.getContext().getString(R.string.fuel_range_label) + " (" + distanceUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_gas_pump);

                    if(fuelRange != null){
                        if (MemCache.distanceFormat().equals("1")) {
                            fuelRange = Utils.kmToMiles(fuelRange);
                        }
                        if (fuelRange < RANGE_CRITICAL) {
                            valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red);
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                        } else if (fuelRange < RANGE_LOW) {
                            valueColor = ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow);
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        }
                        value = Utils.toZeroDecimalString(fuelRange);
                    }
                    break;


                case SHIFTS:
                    Integer shifts = MotorcycleData.getNumberOfShifts();
                    label = MyApplication.getContext().getString(R.string.shifts_header);
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_arrows_alt_v);

                    if(shifts != null){
                        value = Utils.toZeroDecimalString(shifts);
                    }
                    break;


                case LEAN_DEVICE:
                    Double leanAngle = MotorcycleData.getLeanAngleDevice();
                    label = MyApplication.getContext().getString(R.string.leanangle_header);
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_angle);

                    if(leanAngle != null){
                        value = Utils.toZeroDecimalString(leanAngle);
                    }
                    break;


                case GFORCE_DEVICE:
                    Double gForce = MotorcycleData.getGForce();
                    label = MyApplication.getContext().getString(R.string.gforce_header);
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_accelerometer);
                    if(gForce != null){
                        value = Utils.toOneDecimalString(gForce);
                    }
                    break;


                case BEARING_DEVICE:
                    Integer bearingValue = MotorcycleData.getBearing();
                    label = MemCache.bearingLabel(); // MyApplication.getContext().getString(R.string.bearing_header);
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_compass);

                    if (bearingValue != null) {
                        String bearing = String.format("%s°",bearingValue);
                        if (!MemCache.bearingPref().equals("0")) {
                            bearing = Utils.bearingToCardinal(bearingValue);
                        }
                        value = bearing;
                    }
                    break;


                case TIME_DEVICE:
                    label = MyApplication.getContext().getString(R.string.time_header);
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_clock);
                    if ( MotorcycleData.getTime() != null) {
                        SimpleDateFormat dateFormat = Utils.getCachedLocalizedDateFormat();
                        value = dateFormat.format( MotorcycleData.getTime());
                        if (value.contains(" "))
                            value = value.replace(" ","\n");
                    }
                    break;


                case BAROMETRIC_DEVICE:
                    label = MemCache.barometricLabel(); // MyApplication.getContext().getString(R.string.barometric_pressure_header) + "(" + barometricUnit + ")"
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_barometer);
                    if ( MotorcycleData.getBarometricPressure() != null) {
                        value = Utils.toZeroDecimalString( MotorcycleData.getBarometricPressure());
                    }
                    break;



                case SPEED_DEVICE:
                    label = MemCache.speedLabel(); // MyApplication.getContext().getString(R.string.gps_speed_header) + "(" + distanceTimeUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tachometer_alt);
                    String gpsSpeed = MyApplication.getContext().getString(R.string.gps_nofix);

                    if ( MotorcycleData.getLastLocation() != null){
                        gpsSpeed = Utils.toZeroDecimalString( MotorcycleData.getLastLocation().getSpeed() * 3.6);
                        if (MemCache.distanceFormat().equals("1")) {
                            gpsSpeed = Utils.toZeroDecimalString(Utils.kmToMiles(MotorcycleData.getLastLocation().getSpeed() * 3.6));
                        }
                    }
                    value = gpsSpeed;
                    break;



                case ALTITUDE_DEVICE:
                    String altitude = MyApplication.getContext().getString(R.string.gps_nofix);
                    label = MemCache.altitudeLabel(); // MyApplication.getContext().getString(R.string.altitude_header) + "(" + heightUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_mountain);

                    if (MotorcycleData.getLastLocation() != null){
                        altitude = Utils.toZeroDecimalString(MotorcycleData.getLastLocation().getAltitude());
                        if (MemCache.distanceFormat().equals("1")) {
                            altitude = Utils.toZeroDecimalString(Utils.mToFeet(MotorcycleData.getLastLocation().getAltitude()));
                        }
                    }
                    value = altitude;
                    break;



                case SUN_DEVICE:
                    Location loc = MotorcycleData.getLastLocation();
                    label = MyApplication.getContext().getString(R.string.sunrisesunset_header);
                    value = MyApplication.getContext().getString(R.string.gps_nofix);
                    if (loc != null) {
                        Calendar[] sunriseSunset = SunriseSunset.getSunriseSunset(Calendar.getInstance(), loc.getLatitude(), loc.getLongitude());

                        Date sunrise = sunriseSunset[0].getTime();
                        Date sunset = sunriseSunset[1].getTime();
                        Date current = MotorcycleData.getTime();

                        Duration sunriseDur = Duration.between(current.toInstant(), sunrise.toInstant());
                        Duration sunsetDur = Duration.between(current.toInstant(), sunset.toInstant());

                        float sunriseHrs = sunriseDur.toMinutes() / 60.0f;
                        float sunsetHrs = sunsetDur.toMinutes() / 60.0f;



                        SimpleDateFormat dateFormat = Utils.getCachedLocalizedDateFormat();

                        String sunriseString = dateFormat.format(sunrise) + " (" + Utils.toOneDecimalString(sunriseHrs) + ")";
                        String sunsetString = dateFormat.format(sunset) + " (" + Utils.toOneDecimalString(sunsetHrs) + ")";
                        value = sunriseString + "\n" + sunsetString;

                        if(current.compareTo(sunrise) > 0 && current.compareTo(sunset) < 0){
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_sun);
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                        } else {
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_moon);
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_blue), PorterDuff.Mode.SRC_ATOP);
                        }
                    }
                    break;



                case RPM:
                    Integer rpm = MotorcycleData.getRPM();
                    label = MyApplication.getContext().getString(R.string.rpm_header) + " (x1000)";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tachometer_alt);
                    if ((rpm != null) && (rpm > 0)){
                        value = Utils.toOneDecimalString(rpm / 1000d);
                    }
                    break;


                case LEAN_BIKE:
                    Double leanAngleBike = MotorcycleData.getLeanAngleBike();
                    label = MyApplication.getContext().getString(R.string.leanangle_bike_header);
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_angle);
                    if (leanAngleBike != null){
                        value = Utils.toZeroDecimalString(leanAngleBike);
                    }
                    break;


                case REAR_SPEED:
                    Double rSpeed = MotorcycleData.getRearSpeed();
                    label = MemCache.speedLabelW(); // MyApplication.getContext().getString(R.string.rear_wheel_speed_header) + "(" + distanceTimeUnit + ")";
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tachometer_alt);

                    if(rSpeed != null){
                        if (MemCache.distanceFormat().equals("1")) {
                            rSpeed = Utils.kmToMiles(rSpeed);
                        }
                        value = Utils.toZeroDecimalString(rSpeed);
                    }
                    break;



                case CELL_SIGNAL:
                    Integer signal = MotorcycleData.getCellularSignal();
                    label = MemCache.signalLabel(); // MyApplication.getContext().getString(R.string.cellular_signal_header) + "(" + signalUnit + ")";
                    if(signal != null){
                        if (signal > -79) {
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_4);
                        } else if (signal > -89 && signal < -80) {
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_3);
                        } else if (signal > -99 && signal < -90) {
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_2);
                        } else if (signal > -109 && signal < -100) {
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_1);
                        } else if (signal < -110) {
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.signal_bar_0);
                        }
                        value = Utils.toZeroDecimalString(signal);
                    }
                    break;


                case BATTERY_DEVICE:
                    Double battery = MotorcycleData.getLocalBattery();
                    label = MemCache.batteryUnitLabel(); // MyApplication.getContext().getString(R.string.local_battery_header) + "(" + batteryUnit + ")";
                    if(battery != null){
                        if(battery > 95){
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_full);
                        } else if(battery > 75 && battery < 95){
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_three_quarters);
                        } else if(battery > 50 && battery < 75){
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_half);
                        } else if(battery > 25 && battery < 50){
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_quarter);
                        } else if(battery > 0 && battery < 25){
                            icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.battery_empty);
                            icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                        }
                        value = Utils.toZeroDecimalString(battery);
                    }
                    break;
            }
        } catch (Exception e) {
            value = "";
        }
        if ((value == null) || (value.isBlank())) {
            value = MyApplication.getContext().getString(R.string.blank_field);
        }

        return new Object[]{value, label, icon, valueColor};
    }

}
