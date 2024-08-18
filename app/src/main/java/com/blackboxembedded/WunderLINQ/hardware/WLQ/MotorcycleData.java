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

import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.R;
import com.blackboxembedded.WunderLINQ.Utils.Utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MotorcycleData {
    // WunderLINQ HW
    public static WLQ wlq;
    public static String hardwareVersion;

    public static final int DATA_GEAR = 0;
    public static final int DATA_ENGINE_TEMP = 1;
    public static final int DATA_AIR_TEMP = 2;
    public static final int DATA_FRONT_RDC = 3;
    public static final int DATA_REAR_RDC = 4;
    public static final int DATA_ODOMETER = 5;
    public static final int DATA_VOLTAGE = 6;
    public static final int DATA_THROTTLE = 7;
    public static final int DATA_FRONT_BRAKE = 8;
    public static final int DATA_REAR_BRAKE = 9;
    public static final int DATA_AMBIENT_LIGHT = 10;
    public static final int DATA_TRIP_ONE = 11;
    public static final int DATA_TRIP_TWO = 12;
    public static final int DATA_TRIP_AUTO = 13;
    public static final int DATA_SPEED = 14;
    public static final int DATA_AVG_SPEED = 15;
    public static final int DATA_CURRENT_CONSUMPTION = 16;
    public static final int DATA_ECONOMY_ONE = 17;
    public static final int DATA_ECONOMY_TWO = 18;
    public static final int DATA_RANGE = 19;
    public static final int DATA_SHIFTS = 20;
    public static final int DATA_LEAN_DEVICE = 21;
    public static final int DATA_GFORCE_DEVICE = 22;
    public static final int DATA_BEARING_DEVICE = 23;
    public static final int DATA_TIME_DEVICE = 24;
    public static final int DATA_BAROMETRIC_DEVICE = 25;
    public static final int DATA_SPEED_DEVICE = 26;
    public static final int DATA_ALTITUDE_DEVICE = 27;
    public static final int DATA_SUN_DEVICE = 28;
    public static final int DATA_RPM = 29;
    public static final int DATA_LEAN = 30;
    public static final int DATA_REAR_SPEED = 31;
    public static final int DATA_CELL_SIGNAL= 32;
    public static final int DATA_BATTERY_DEVICE = 33;

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
    private static Double leanAngle;
    public static Double getLeanAngle() {
        return leanAngle;
    }
    public static void setLeanAngle(Double leanAngle){
        MotorcycleData.leanAngle = leanAngle;
    }

    // Lean Angle Max
    private static Double leanAngleMaxL;
    public static Double getLeanAngleMaxL() {
        return leanAngleMaxL;
    }
    public static void setLeanAngleMaxL(Double leanAngleMaxL){
        MotorcycleData.leanAngleMaxL = leanAngleMaxL;
    }
    private static Double leanAngleMaxR;
    public static Double getLeanAngleMaxR() {
        return leanAngleMaxR;
    }
    public static void setLeanAngleMaxR(Double leanAngleMaxR){
        MotorcycleData.leanAngleMaxR = leanAngleMaxR;
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
    public static Date getTime() {
        return time;
    }
    public static void setTime(Date time){
        MotorcycleData.time = time;
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
    public static String getLabel(int dataPoint){
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
            case DATA_GEAR:
                label = MyApplication.getContext().getString(R.string.gear_label);
                break;
            case DATA_ENGINE_TEMP:
                label = MyApplication.getContext().getString(R.string.engine_temp_label) + "(" + temperatureUnit + ")";
                break;
            case DATA_AIR_TEMP:
                label = MyApplication.getContext().getString(R.string.ambient_temp_label) + "(" + temperatureUnit + ")";
                break;
            case DATA_FRONT_RDC:
                label = MyApplication.getContext().getString(R.string.frontpressure_header) + "(" + pressureUnit + ")";
                break;
            case DATA_REAR_RDC:
                label = MyApplication.getContext().getString(R.string.rearpressure_header) + "(" + pressureUnit + ")";
                break;
            case DATA_ODOMETER:
                label = MyApplication.getContext().getString(R.string.odometer_label) + "(" + distanceUnit + ")";
                break;
            case DATA_VOLTAGE:
                label = MyApplication.getContext().getString(R.string.voltage_label) + "(" + voltageUnit + ")";
                break;
            case DATA_THROTTLE:
                label = MyApplication.getContext().getString(R.string.throttle_label) + "(" + throttleUnit + ")";
                break;
            case DATA_FRONT_BRAKE:
                label = MyApplication.getContext().getString(R.string.frontbrakes_label);
                break;
            case DATA_REAR_BRAKE:
                label = MyApplication.getContext().getString(R.string.rearbrakes_label);
                break;
            case DATA_AMBIENT_LIGHT:
                label = MyApplication.getContext().getString(R.string.ambientlight_label);
                break;
            case DATA_TRIP_ONE:
                label = MyApplication.getContext().getString(R.string.trip1_label) + "(" + distanceUnit + ")";
                break;
            case DATA_TRIP_TWO:
                label = MyApplication.getContext().getString(R.string.trip2_label) + "(" + distanceUnit + ")";
                break;
            case DATA_TRIP_AUTO:
                label = MyApplication.getContext().getString(R.string.tripauto_label) + "(" + distanceUnit + ")";
                break;
            case DATA_SPEED:
                label = MyApplication.getContext().getString(R.string.speed_label) + "(" + distanceTimeUnit + ")";
                break;
            case DATA_AVG_SPEED:
                label = MyApplication.getContext().getString(R.string.avgspeed_label) + "(" + distanceTimeUnit + ")";
                break;
            case DATA_CURRENT_CONSUMPTION:
                label = MyApplication.getContext().getString(R.string.cconsumption_label) + "(" + consumptionUnit + ")";
                break;
            case DATA_ECONOMY_ONE:
                label = MyApplication.getContext().getString(R.string.fueleconomyone_label) + "(" + consumptionUnit + ")";
                break;
            case DATA_ECONOMY_TWO:
                label = MyApplication.getContext().getString(R.string.fueleconomytwo_label) + "(" + consumptionUnit + ")";
                break;
            case DATA_RANGE:
                label = MyApplication.getContext().getString(R.string.fuelrange_label) + "(" + distanceUnit + ")";
                break;
            case DATA_SHIFTS:
                label = MyApplication.getContext().getString(R.string.shifts_header);
                break;
            case DATA_LEAN_DEVICE:
                label = MyApplication.getContext().getString(R.string.leanangle_header);
                break;
            case DATA_GFORCE_DEVICE:
                label = MyApplication.getContext().getString(R.string.gforce_header);
                break;
            case DATA_BEARING_DEVICE:
                label = MyApplication.getContext().getString(R.string.bearing_header);
                break;
            case DATA_TIME_DEVICE:
                label = MyApplication.getContext().getString(R.string.time_header);
                break;
            case DATA_BAROMETRIC_DEVICE:
                label = MyApplication.getContext().getString(R.string.barometricpressure_header) + "(" + barometricUnit + ")";
                break;
            case DATA_SPEED_DEVICE:
                label = MyApplication.getContext().getString(R.string.gpsspeed_header) + "(" + distanceTimeUnit + ")";
                break;
            case DATA_ALTITUDE_DEVICE:
                label = MyApplication.getContext().getString(R.string.altitude_header) + "(" + heightUnit + ")";
                break;
            case DATA_SUN_DEVICE:
                label = MyApplication.getContext().getString(R.string.sunrisesunset_header);
                break;
            case DATA_RPM:
                label = MyApplication.getContext().getString(R.string.rpm_header);
                break;
            case DATA_LEAN:
                label = MyApplication.getContext().getString(R.string.leanangle_bike_header);
                break;
            case DATA_REAR_SPEED:
                label = MyApplication.getContext().getString(R.string.rearwheel_speed_header) + "(" + distanceTimeUnit + ")";
                break;
            case DATA_CELL_SIGNAL:
                label = MyApplication.getContext().getString(R.string.cellular_signal_header) + "(" + signalUnit + ")";
                break;
            case DATA_BATTERY_DEVICE:
                label = MyApplication.getContext().getString(R.string.local_battery_header) + "(" + batteryUnit + ")";
                break;
            default:
                label = " ";
        }
        return label;
    }

    public static String getExtraKey(int dataPoint){
        String key = "";
        switch (dataPoint){
            case DATA_GEAR:
                key = "gear";
                break;
            case DATA_ENGINE_TEMP:
                key = "engineTemperature";
                break;
            case DATA_AIR_TEMP:
                key = "ambientTemperature";
                break;
            case DATA_FRONT_RDC:
                key = "frontTirePressure";
                break;
            case DATA_REAR_RDC:
                key = "rearTirePressure";
                break;
            case DATA_ODOMETER:
                key = "odometer";
                break;
            case DATA_VOLTAGE:
                key = "voltage";
                break;
            case DATA_THROTTLE:
                key = "throttlePosition";
                break;
            case DATA_FRONT_BRAKE:
                key = "frontBrake";
                break;
            case DATA_REAR_BRAKE:
                key = "rearBrake";
                break;
            case DATA_AMBIENT_LIGHT:
                key = "ambientLight";
                break;
            case DATA_TRIP_ONE:
                key = "tripOne";
                break;
            case DATA_TRIP_TWO:
                key = "tripTwo";
                break;
            case DATA_TRIP_AUTO:
                key = "tripAuto";
                break;
            case DATA_SPEED:
                key = "speed";
                break;
            case DATA_AVG_SPEED:
                key = "avgSpeed";
                break;
            case DATA_CURRENT_CONSUMPTION:
                key = "currentConsumption";
                break;
            case DATA_ECONOMY_ONE:
                key = "fuelEconomyOne";
                break;
            case DATA_ECONOMY_TWO:
                key = "fuelEconomyTwo";
                break;
            case DATA_RANGE:
                key = "fuelRange";
                break;
            case DATA_SHIFTS:
                key = "numberOfShifts";
                break;
            case DATA_LEAN_DEVICE:
                key = "leanAngleDevice";
                break;
            case DATA_GFORCE_DEVICE:
                key = "gForce";
                break;
            case DATA_BEARING_DEVICE:
                key = "bearing";
                break;
            case DATA_TIME_DEVICE:
                key = "time";
                break;
            case DATA_BAROMETRIC_DEVICE:
                key = "barometricPressure";
                break;
            case DATA_SPEED_DEVICE:
                key = "gpsSpeed";
                break;
            case DATA_ALTITUDE_DEVICE:
                key = "gpsAltitude";
                break;
            case DATA_SUN_DEVICE:
                key = "sunRiseSet";
                break;
            case DATA_RPM:
                key = "rpm";
                break;
            case DATA_LEAN:
                key = "leanAngle";
                break;
            case DATA_REAR_SPEED:
                key = "rearSpeed";
                break;
            case DATA_CELL_SIGNAL:
                key = "cellSignal";
                break;
            case DATA_BATTERY_DEVICE:
                key = "battery";
                break;
            default:
                key = "?";
        }
        return key;
    }

    public static Drawable getIcon(int dataPoint){
        Drawable icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_cog);
        switch (dataPoint){
            case DATA_GEAR:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_cog);
                break;
            case DATA_ENGINE_TEMP:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_engine_temp);
                if(MotorcycleData.getEngineTemperature() != null ){
                    double engineTemp = MotorcycleData.getEngineTemperature();
                    if (engineTemp >= 104.0){
                        icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                    }
                }
                break;
            case DATA_AIR_TEMP:
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
            case DATA_FRONT_RDC:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire);
                if (Faults.getFrontTirePressureCriticalActive()){
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                } else if (Faults.getFrontTirePressureWarningActive()){
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                }
                break;
            case DATA_REAR_RDC:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire);
                if (Faults.getRearTirePressureCriticalActive()){
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.motorrad_red), PorterDuff.Mode.SRC_ATOP);
                } else if (Faults.getRearTirePressureWarningActive()){
                    icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tire_alert);
                    icon.setColorFilter(ContextCompat.getColor(MyApplication.getContext().getApplicationContext(), R.color.yellow), PorterDuff.Mode.SRC_ATOP);
                }
                break;
            case DATA_ODOMETER:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_dashboard_meter);
                break;
            case DATA_VOLTAGE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_car_battery);
                break;
            case DATA_THROTTLE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_signature);
                break;
            case DATA_FRONT_BRAKE:
            case DATA_REAR_BRAKE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_brakes);
                break;
            case DATA_AMBIENT_LIGHT:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_lightbulb);
                break;
            case DATA_TRIP_ONE:
            case DATA_TRIP_TWO:
            case DATA_TRIP_AUTO:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_suitcase);
                break;
            case DATA_SPEED:
            case DATA_AVG_SPEED:
            case DATA_SPEED_DEVICE:
            case DATA_RPM:
            case DATA_REAR_SPEED:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_tachometer_alt);
                break;
            case DATA_CURRENT_CONSUMPTION:
            case DATA_ECONOMY_ONE:
            case DATA_ECONOMY_TWO:
            case DATA_RANGE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_gas_pump);
                break;
            case DATA_SHIFTS:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_arrows_alt_v);
                break;
            case DATA_LEAN_DEVICE:
            case DATA_LEAN:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_angle);
                break;
            case DATA_GFORCE_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_accelerometer);
                break;
            case DATA_BEARING_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_compass);
                break;
            case DATA_TIME_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_clock);
                break;
            case DATA_BAROMETRIC_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_barometer);
                break;
            case DATA_ALTITUDE_DEVICE:
                icon = AppCompatResources.getDrawable(MyApplication.getContext().getApplicationContext(), R.drawable.ic_mountain);
                break;
            case DATA_SUN_DEVICE:
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
            case DATA_CELL_SIGNAL:
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
            case DATA_BATTERY_DEVICE:
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

    public static CarIcon getCarIcon(int dataPoint){
        IconCompat icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_cog);
        CarColor carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.white),MyApplication.getContext().getResources().getColor(R.color.black));

        switch (dataPoint){
            case DATA_GEAR:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_cog);
                break;
            case DATA_ENGINE_TEMP:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_engine_temp);
                if(MotorcycleData.getEngineTemperature() != null ){
                    double engineTemp = MotorcycleData.getEngineTemperature();
                    if (engineTemp >= 104.0){
                        carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.motorrad_red),MyApplication.getContext().getResources().getColor(R.color.motorrad_red));
                    }
                }
                break;
            case DATA_AIR_TEMP:
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
            case DATA_FRONT_RDC:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire);
                if (Faults.getFrontTirePressureCriticalActive()){
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire_alert);
                    carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.motorrad_red),MyApplication.getContext().getResources().getColor(R.color.motorrad_red));
                } else if (Faults.getFrontTirePressureWarningActive()){
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire_alert);
                    carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.yellow),MyApplication.getContext().getResources().getColor(R.color.yellow));
                }
                break;
            case DATA_REAR_RDC:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire);
                if (Faults.getRearTirePressureCriticalActive()){
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire_alert);
                    carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.motorrad_red),MyApplication.getContext().getResources().getColor(R.color.motorrad_red));
                } else if (Faults.getRearTirePressureWarningActive()){
                    icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tire_alert);
                    carColor = CarColor.createCustom(MyApplication.getContext().getResources().getColor(R.color.yellow),MyApplication.getContext().getResources().getColor(R.color.yellow));
                }
                break;
            case DATA_ODOMETER:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_dashboard_meter);
                break;
            case DATA_VOLTAGE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_car_battery);
                break;
            case DATA_THROTTLE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_signature);
                break;
            case DATA_FRONT_BRAKE:
            case DATA_REAR_BRAKE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_brakes);
                break;
            case DATA_AMBIENT_LIGHT:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_lightbulb);
                break;
            case DATA_TRIP_ONE:
            case DATA_TRIP_TWO:
            case DATA_TRIP_AUTO:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_suitcase);
                break;
            case DATA_SPEED:
            case DATA_AVG_SPEED:
            case DATA_SPEED_DEVICE:
            case DATA_RPM:
            case DATA_REAR_SPEED:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_tachometer_alt);
                break;
            case DATA_CURRENT_CONSUMPTION:
            case DATA_ECONOMY_ONE:
            case DATA_ECONOMY_TWO:
            case DATA_RANGE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_gas_pump);
                break;
            case DATA_SHIFTS:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_arrows_alt_v);
                break;
            case DATA_LEAN_DEVICE:
            case DATA_LEAN:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_angle);
                break;
            case DATA_GFORCE_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_accelerometer);
                break;
            case DATA_BEARING_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_compass);
                break;
            case DATA_TIME_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_clock);
                break;
            case DATA_BAROMETRIC_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_barometer);
                break;
            case DATA_ALTITUDE_DEVICE:
                icon = IconCompat.createWithResource(MyApplication.getContext(), R.drawable.ic_mountain);
                break;
            case DATA_SUN_DEVICE:
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
            case DATA_CELL_SIGNAL:
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
            case DATA_BATTERY_DEVICE:
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

    public static String getValue(int dataPoint){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
        String temperatureFormat = sharedPrefs.getString("prefTempF", "0");
        String distanceFormat = sharedPrefs.getString("prefDistance", "0");
        String consumptionFormat = sharedPrefs.getString("prefConsumption", "0");

        String value = "";
        switch (dataPoint){
            case DATA_GEAR:
                if(MotorcycleData.getGear() != null){
                    value = MotorcycleData.getGear();
                }
                break;
            case DATA_ENGINE_TEMP:
                if(MotorcycleData.getEngineTemperature() != null ){
                    double engineTemp = MotorcycleData.getEngineTemperature();
                    if (temperatureFormat.contains("1")) {
                        // F
                        engineTemp = Utils.celsiusToFahrenheit(engineTemp);
                    }
                    value = String.valueOf(Math.round(engineTemp));
                }
                break;
            case DATA_AIR_TEMP:
                if(MotorcycleData.getAmbientTemperature() != null ){
                    double ambientTemp = MotorcycleData.getAmbientTemperature();
                    if (temperatureFormat.contains("1")) {
                        // F
                        ambientTemp = Utils.celsiusToFahrenheit(ambientTemp);
                    }
                    value = String.valueOf(Math.round(ambientTemp));
                }
                break;
            case DATA_FRONT_RDC:
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
                    value = Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(rdcFront);
                }
                break;
            case DATA_REAR_RDC:
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
                    value = Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(rdcRear);
                }
                break;
            case DATA_ODOMETER:
                if(MotorcycleData.getOdometer() != null){
                    double odometer = MotorcycleData.getOdometer();
                    if (distanceFormat.contains("1")) {
                        odometer = Utils.kmToMiles(odometer);
                    }
                    value = String.valueOf(Math.round(odometer));
                }
                break;
            case DATA_VOLTAGE:
                if(MotorcycleData.getVoltage() != null){
                    Double voltage = MotorcycleData.getVoltage();
                    value = String.valueOf(Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(voltage));
                }
                break;
            case DATA_THROTTLE:
                if(MotorcycleData.getThrottlePosition() != null){
                    Double throttlePosition = MotorcycleData.getThrottlePosition();
                    value = String.valueOf(Math.round(throttlePosition));
                }
                break;
            case DATA_FRONT_BRAKE:
                if((MotorcycleData.getFrontBrake() != null) && (MotorcycleData.getFrontBrake() != 0)){
                    Integer frontBrakes = MotorcycleData.getFrontBrake();
                    value = String.valueOf(frontBrakes);
                }
                break;
            case DATA_REAR_BRAKE:
                if((MotorcycleData.getRearBrake() != null) && (MotorcycleData.getRearBrake() != 0)){
                    Integer rearBrakes = MotorcycleData.getRearBrake();
                    value = String.valueOf(rearBrakes);
                }
                break;
            case DATA_AMBIENT_LIGHT:
                if(MotorcycleData.getAmbientLight() != null){
                    Integer ambientLight = MotorcycleData.getAmbientLight();
                    value = String.valueOf(ambientLight);
                }
                break;
            case DATA_TRIP_ONE:
                if(MotorcycleData.getTripOne() != null) {
                    double trip1 = MotorcycleData.getTripOne();
                    if (distanceFormat.contains("1")) {
                        trip1 = Utils.kmToMiles(trip1);
                    }
                    value = Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(trip1);
                }
                break;
            case DATA_TRIP_TWO:
                if(MotorcycleData.getTripTwo() != null){
                    double trip2 = MotorcycleData.getTripTwo();
                    if (distanceFormat.contains("1")) {
                        trip2 = Utils.kmToMiles(trip2);
                    }
                    value = Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(trip2);
                }
                break;
            case DATA_TRIP_AUTO:
                if(MotorcycleData.getTripAuto() != null){
                    double tripAuto = MotorcycleData.getTripAuto();
                    if (distanceFormat.contains("1")) {
                        tripAuto = Utils.kmToMiles(tripAuto);
                    }
                    value = Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(tripAuto);
                }
                break;
            case DATA_SPEED:
                if(MotorcycleData.getSpeed() != null){
                    double speed = MotorcycleData.getSpeed();
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    value = String.valueOf(Math.round(speed));
                }
                break;
            case DATA_AVG_SPEED:
                if(MotorcycleData.getAvgSpeed() != null){
                    double avgSpeed = MotorcycleData.getAvgSpeed();
                    if (distanceFormat.contains("1")) {
                        avgSpeed = Utils.kmToMiles(avgSpeed);
                    }
                    value = Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(avgSpeed);
                }
                break;
            case DATA_CURRENT_CONSUMPTION:
                if(MotorcycleData.getCurrentConsumption() != null){
                    double currentConsumption = MotorcycleData.getCurrentConsumption();
                    if (consumptionFormat.contains("1")) {
                        currentConsumption = Utils.l100ToMpg(currentConsumption);
                    } else if (consumptionFormat.contains("2")) {
                        currentConsumption = Utils.l100ToMpgI(currentConsumption);
                    } else if (consumptionFormat.contains("3")) {
                        currentConsumption = Utils.l100ToKmL(currentConsumption);
                    }
                    value = String.valueOf(Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(currentConsumption));
                }
                break;
            case DATA_ECONOMY_ONE:
                if(MotorcycleData.getFuelEconomyOne() != null){
                    double fuelEconomyOne = MotorcycleData.getFuelEconomyOne();
                    if (consumptionFormat.contains("1")) {
                        fuelEconomyOne = Utils.l100ToMpg(fuelEconomyOne);
                    } else if (consumptionFormat.contains("2")) {
                        fuelEconomyOne = Utils.l100ToMpgI(fuelEconomyOne);
                    } else if (consumptionFormat.contains("3")) {
                        fuelEconomyOne = Utils.l100ToKmL(fuelEconomyOne);
                    }
                    value = String.valueOf(Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(fuelEconomyOne));
                }
                break;
            case DATA_ECONOMY_TWO:
                if(MotorcycleData.getFuelEconomyTwo() != null){
                    double fuelEconomyTwo = MotorcycleData.getFuelEconomyTwo();
                    if (consumptionFormat.contains("1")) {
                        fuelEconomyTwo = Utils.l100ToMpg(fuelEconomyTwo);
                    } else if (consumptionFormat.contains("2")) {
                        fuelEconomyTwo  = Utils.l100ToMpgI(fuelEconomyTwo);
                    } else if (consumptionFormat.contains("3")) {
                        fuelEconomyTwo  = Utils.l100ToKmL(fuelEconomyTwo);
                    }
                    value = String.valueOf(Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(fuelEconomyTwo));
                }
                break;
            case DATA_RANGE:
                if(MotorcycleData.getFuelRange() != null){
                    double fuelRange = MotorcycleData.getFuelRange();
                    if (distanceFormat.contains("1")) {
                        fuelRange = Utils.kmToMiles(fuelRange);
                    }
                    value = String.valueOf(Math.round(fuelRange));
                }
                break;
            case DATA_SHIFTS:
                if(MotorcycleData.getNumberOfShifts() != null){
                    int shifts = MotorcycleData.getNumberOfShifts();
                    value = String.valueOf(shifts);
                }
                break;
            case DATA_LEAN_DEVICE:
                if(MotorcycleData.getLeanAngle() != null){
                    Double leanAngle = MotorcycleData.getLeanAngle();
                    value = String.valueOf(Math.round(leanAngle));
                }
                break;
            case DATA_GFORCE_DEVICE:
                if(MotorcycleData.getGForce() != null){
                    Double gForce = MotorcycleData.getGForce();
                    value = String.valueOf(Utils.getLocalizedOneDigitFormat(Utils.getCurrentLocale()).format(gForce));
                }
                break;
            case DATA_BEARING_DEVICE:
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
            case DATA_TIME_DEVICE:
                if (MotorcycleData.getTime() != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                    if (!sharedPrefs.getString("prefTime", "0").equals("0")) {
                        dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    }
                    value = dateFormat.format(MotorcycleData.getTime());
                }
                break;
            case DATA_BAROMETRIC_DEVICE:
                if (MotorcycleData.getBarometricPressure() != null) {
                    value = String.valueOf(Math.round(MotorcycleData.getBarometricPressure()));
                }
                break;
            case DATA_SPEED_DEVICE:
                String gpsSpeed = MyApplication.getContext().getString(R.string.gps_nofix);
                if (MotorcycleData.getLastLocation() != null){
                    gpsSpeed = String.valueOf(Math.round(MotorcycleData.getLastLocation().getSpeed() * 3.6));
                    if (distanceFormat.contains("1")) {
                        gpsSpeed = String.valueOf(Math.round(Utils.kmToMiles(MotorcycleData.getLastLocation().getSpeed() * 3.6)));
                    }
                }
                value = gpsSpeed;
                break;
            case DATA_ALTITUDE_DEVICE:
                String altitude = MyApplication.getContext().getString(R.string.gps_nofix);
                if (MotorcycleData.getLastLocation() != null){
                    altitude = String.valueOf(Math.round(MotorcycleData.getLastLocation().getAltitude()));
                    if (distanceFormat.contains("1")) {
                        altitude = String.valueOf(Math.round(Utils.mToFeet(MotorcycleData.getLastLocation().getAltitude())));
                    }
                }
                value = altitude;
                break;
            case DATA_SUN_DEVICE:
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
            case DATA_RPM:
                if (MotorcycleData.getRPM() > 0){
                    value = String.valueOf(MotorcycleData.getRPM());
                }
                break;
            case DATA_LEAN:
                if(MotorcycleData.getLeanAngleBike() != null){
                    Double leanAngleBike = MotorcycleData.getLeanAngleBike();
                    value = String.valueOf(Math.round(leanAngleBike));
                }
                break;
            case DATA_REAR_SPEED:
                if(MotorcycleData.getRearSpeed() != null){
                    double speed = MotorcycleData.getRearSpeed();
                    if (distanceFormat.contains("1")) {
                        speed = Utils.kmToMiles(speed);
                    }
                    value = String.valueOf(Math.round(speed));
                }
                break;
            case DATA_CELL_SIGNAL:
                if(MotorcycleData.getCellularSignal() != null){
                    value = String.valueOf(MotorcycleData.getCellularSignal());
                }
                break;
            case DATA_BATTERY_DEVICE:
                if(MotorcycleData.getLocalBattery() != null){
                    value = String.valueOf(Math.round(MotorcycleData.getLocalBattery()));
                }
                break;
            default:
                value = MyApplication.getContext().getString(R.string.blank_field);
        }
        return value;
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
        Data.leanAngle = null;
        Data.gForce = null;
        Data.bearing = null;
        Data.barometricPressure = null;
        Data.leanAngleBike = null;
        Data.rearSpeed = null;
    }
     */
}
