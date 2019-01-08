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

    // VIN
    // Last Location
    private static String vin;
    public static String getVin() {
        return vin;
    }
    public static void setVin(String vin){
        Data.vin = vin;
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

    // Utility functions
    public static void clear(){
        Data.lastLocation = null;
        Data.lastMessage = null;
        Data.vin = null;
        Data.frontTirePressure = null;
        Data.rearTirePressure = null;
        Data.ambientTemperature = null;
        Data.engineTemperature = null;
        Data.odometer = null;
        Data.tripOne = null;
        Data.tripTwo = null;
        Data.numberOfShifts = 0;
        Data.gear = null;
        Data.voltage = null;
        Data.throttlePosition = null;
        Data.frontBrake = 0;
        Data.rearBrake = 0;
        Data.speed = null;
        Data.avgSpeed = null;
        Data.currentConsumption = null;
        Data.fuelEconomyOne = null;
        Data.fuelEconomyTwo = null;
        Data.fuelRange = null;
    }
}
