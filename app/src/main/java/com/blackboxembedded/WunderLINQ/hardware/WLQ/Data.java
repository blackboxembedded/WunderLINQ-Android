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

import android.location.Location;
import android.util.Log;

import java.time.LocalDate;
import java.util.Date;

public class Data {
    // WunderLINQ HW
    public static WLQ wlq;
    public static String hardwareVersion;

    // Last Message
    private static byte[] lastMessage;
    public static byte[] getLastMessage() {
        return lastMessage;
    }
    public static void setLastMessage(byte[] message){
        Data.lastMessage = message;
    }

    // Last Location
    private static Location lastLocation;
    public static Location getLastLocation() {
        return lastLocation;
    }
    public static void setLastLocation(Location location){
        Data.lastLocation = location;
    }

    // Mobile device battery
    private static Double localBattery;
    public static Double getLocalBattery() {
        return localBattery;
    }
    public static void setLocalBattery(Double localBattery){
        Data.localBattery = localBattery;
    }

    // Mobile device cell signal (dBm)
    private static Integer cellularSignal;
    public static Integer getCellularSignal() {
        return cellularSignal;
    }
    public static void setCellularSignal(Integer cellularSignal){
        Data.cellularSignal = cellularSignal;
    }

    // VIN
    private static String vin;
    public static String getVin() {
        return vin;
    }
    public static void setVin(String vin){
        Data.vin = vin;
    }

    // Next Service, km
    private static Integer nextService;
    public static Integer getNextService() {
        return nextService;
    }
    public static void setNextService(Integer nextService){
        Data.nextService = nextService;
    }

    // Next Service, Date
    private static LocalDate nextServiceDate;
    public static LocalDate getNextServiceDate() {
        return nextServiceDate;
    }
    public static void setNextServiceDate(LocalDate nextServiceDate){
        Data.nextServiceDate = nextServiceDate;
    }

    // Front Tire Pressure in bar
    private static Double frontTirePressure;
    public static Double getFrontTirePressure() {
        return frontTirePressure;
    }
    public static void setFrontTirePressure(Double pressure){
        Data.frontTirePressure = pressure;
    }

    // Rear Tire Pressure in bar
    private static Double rearTirePressure;
    public static Double getRearTirePressure() {
        return rearTirePressure;
    }
    public static void setRearTirePressure(Double pressure){
        Data.rearTirePressure = pressure;
        Log.d("RearTirePressure","Value Set: " + Data.rearTirePressure);
    }

    // Ambient Temperature in C
    private static Double ambientTemperature;
    public static Double getAmbientTemperature() {
        return ambientTemperature;
    }
    public static void setAmbientTemperature(Double temperature){
        Data.ambientTemperature = temperature;
    }

    // Engine Temperature in C
    private static Double engineTemperature;
    public static Double getEngineTemperature() {
        return engineTemperature;
    }
    public static void setEngineTemperature(Double temperature){
        Data.engineTemperature = temperature;
    }

    // Odometer in km
    private static Double odometer;
    public static Double getOdometer() {
        return odometer;
    }
    public static void setOdometer(Double distance){
        Data.odometer = distance;
    }

    // Trip One Distance in km
    private static Double tripOne;
    public static Double getTripOne() {
        return tripOne;
    }
    public static void setTripOne(Double distance){
        Data.tripOne = distance;
    }

    // Trip Two Distance in km
    private static Double tripTwo;
    public static Double getTripTwo() {
        return tripTwo;
    }
    public static void setTripTwo(Double distance){
        Data.tripTwo = distance;
    }

    // Trip Auto Distance in km
    private static Double tripAuto;
    public static Double getTripAuto() {
        return tripAuto;
    }
    public static void setTripAuto(Double distance){
        Data.tripAuto = distance;
    }

    // Number of shifts
    private static Integer numberOfShifts = 0;
    public static Integer getNumberOfShifts() {
        return numberOfShifts;
    }
    public static void setNumberOfShifts(Integer shifts){
        Data.numberOfShifts = shifts;
    }

    // RPM
    private static Integer rpm = 0;
    public static Integer getRPM() {
        return rpm;
    }
    public static void setRPM(Integer rpm){
        Data.rpm = rpm;
    }

    // Gear
    private static String gear;
    public static String getGear() {
        return gear;
    }
    public static void setGear(String gear){
        Data.gear = gear;
    }

    // Voltage
    private static Double voltage;
    public static Double getvoltage() {
        return voltage;
    }
    public static void setvoltage(Double voltage){
        Data.voltage = voltage;
    }

    // Throttle Position
    private static Double throttlePosition;
    public static Double getThrottlePosition() {
        return throttlePosition;
    }
    public static void setThrottlePosition(Double throttlePosition){
        Data.throttlePosition = throttlePosition;
    }

    // Front Brake
    private static Integer frontBrake = 0;
    public static Integer getFrontBrake() {
        return frontBrake;
    }
    public static void setFrontBrake(Integer frontBrake){
        Data.frontBrake = frontBrake;
    }

    // Rear Brake
    private static Integer rearBrake = 0;
    public static Integer getRearBrake() {
        return rearBrake;
    }
    public static void setRearBrake(Integer rearBrake){
        Data.rearBrake = rearBrake;
    }

    // Ambient Light
    private static Integer ambientLight;
    public static Integer getAmbientLight() {
        return ambientLight;
    }
    public static void setAmbientLight(Integer ambientLight){
        Data.ambientLight = ambientLight;
    }

    // Speed
    private static Double speed;
    public static Double getSpeed() {
        return speed;
    }
    public static void setSpeed(Double speed){
        Data.speed = speed;
    }

    // Average Speed
    private static Double avgSpeed;
    public static Double getAvgSpeed() {
        return avgSpeed;
    }
    public static void setAvgSpeed(Double avgSpeed){
        Data.avgSpeed = avgSpeed;
    }

    // Current Consumption
    private static Double currentConsumption;
    public static Double getCurrentConsumption() {
        return currentConsumption;
    }
    public static void setCurrentConsumption(Double currentConsumption){
        Data.currentConsumption = currentConsumption;
    }

    // Fuel Economy 1
    private static Double fuelEconomyOne;
    public static Double getFuelEconomyOne() {
        return fuelEconomyOne;
    }
    public static void setFuelEconomyOne(Double fuelEconomyOne){
        Data.fuelEconomyOne = fuelEconomyOne;
    }

    // Fuel Economy 2
    private static Double fuelEconomyTwo;
    public static Double getFuelEconomyTwo() {
        return fuelEconomyTwo;
    }
    public static void setFuelEconomyTwo(Double fuelEconomyTwo){
        Data.fuelEconomyTwo = fuelEconomyTwo;
    }

    // Fuel Range
    private static Double fuelRange;
    public static Double getFuelRange() {
        return fuelRange;
    }
    public static void setFuelRange(Double fuelRange){
        Data.fuelRange = fuelRange;
    }

    // Lean Angle
    private static Double leanAngle;
    public static Double getLeanAngle() {
        return leanAngle;
    }
    public static void setLeanAngle(Double leanAngle){
        Data.leanAngle = leanAngle;
    }

    // Lean Angle Max
    private static Double leanAngleMaxL;
    public static Double getLeanAngleMaxL() {
        return leanAngleMaxL;
    }
    public static void setLeanAngleMaxL(Double leanAngleMaxL){
        Data.leanAngleMaxL = leanAngleMaxL;
    }
    private static Double leanAngleMaxR;
    public static Double getLeanAngleMaxR() {
        return leanAngleMaxR;
    }
    public static void setLeanAngleMaxR(Double leanAngleMaxR){
        Data.leanAngleMaxR = leanAngleMaxR;
    }

    // g-force
    private static Double gForce;
    public static Double getGForce() {
        return gForce;
    }
    public static void setGForce(Double gForce){
        Data.gForce = gForce;
    }

    // bearing
    private static Integer bearing;
    public static Integer getBearing() {
        return bearing;
    }
    public static void setBearing(Integer bearing){
        Data.bearing = bearing;
    }

    // time
    private static Date time;
    public static Date getTime() {
        return time;
    }
    public static void setTime(Date time){
        Data.time = time;
    }

    // barometric pressure
    private static Double barometricPressure;
    public static Double getBarometricPressure() {
        return barometricPressure;
    }
    public static void setBarometricPressure(Double barometricPressure){
        Data.barometricPressure = barometricPressure;
    }

    // Lean Angle Bike
    private static Double leanAngleBike;
    public static Double getLeanAngleBike() {
        return leanAngleBike;
    }
    public static void setLeanAngleBike(Double leanAngleBike){
        Data.leanAngleBike = leanAngleBike;
    }

    // Lean Angle Bike Max
    private static Double leanAngleBikeMaxL;
    public static Double getLeanAngleBikeMaxL() {
        return leanAngleBikeMaxL;
    }
    public static void setLeanAngleBikeMaxL(Double leanAngleBikeMaxL){
        Data.leanAngleBikeMaxL = leanAngleBikeMaxL;
    }
    private static Double leanAngleBikeMaxR;
    public static Double getLeanAngleBikeMaxR() {
        return leanAngleBikeMaxR;
    }
    public static void setLeanAngleBikeMaxR(Double leanAngleBikeMaxR){
        Data.leanAngleBikeMaxR = leanAngleBikeMaxR;
    }

    // Rear Speed
    private static Double rearSpeed;
    public static Double getRearSpeed() {
        return rearSpeed;
    }
    public static void setRearSpeed(Double rearSpeed){
        Data.rearSpeed = rearSpeed;
    }

    // START of CAN only Data
    // High Beam Status
    private static boolean highBeam;
    public static boolean getHighBeam() {
        return highBeam;
    }
    public static void setHighBeam(boolean highBeam){
        Data.highBeam = highBeam;
    }

    // Fog Light Status
    private static boolean fogLight;
    public static boolean getFogLight() {
        return fogLight;
    }
    public static void setFogLight(boolean fogLight){
        Data.fogLight = fogLight;
    }

    // Heated Grip Status
    public static int GRIP_HIGH = 2;
    public static int GRIP_LOW = 1;
    public static int GRIP_OFF = 0;
    private static int heatedGrips;
    public static int getHeatedGrips() {
        return heatedGrips;
    }
    public static void setHeatedGrips(int heatedGrips){
        Data.heatedGrips = heatedGrips;
    }

    // END of CAN only Data

    // Utility functions
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
