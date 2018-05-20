package com.blackboxembedded.WunderLINQ;

import android.location.Location;

/**
 * Created by keithconger on 9/10/17.
 */

public class Data {
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

    // Number of shifts
    private static Integer numberOfShifts;
    public static Integer getNumberOfShifts() {
        return numberOfShifts;
    }
    public static void setNumberOfShifts(Integer shifts){
        Data.numberOfShifts = shifts;
    }

    // Number of brakes
    private static Integer numberOfBrakes;
    public static Integer getNumberOfBrakes() {
        return numberOfBrakes;
    }
    public static void setNumberOfBrakes(Integer brakes){
        Data.numberOfBrakes = brakes;
    }

    // Gear
    private static String gear;
    public static String getGear() {
        return gear;
    }
    public static void setGear(String gear){
        Data.gear = gear;
    }

    // Utility functions
    public static void clear(){
        Data.lastLocation = null;
        Data.lastMessage = null;
        Data.frontTirePressure = null;
        Data.rearTirePressure = null;
        Data.ambientTemperature = null;
        Data.engineTemperature = null;
        Data.odometer = null;
        Data.tripOne = null;
        Data.tripTwo = null;
        Data.numberOfShifts = null;
        Data.numberOfBrakes = null;
        Data.gear = null;
    }
}
