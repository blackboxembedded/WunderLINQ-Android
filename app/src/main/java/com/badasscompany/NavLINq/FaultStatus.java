package com.badasscompany.NavLINq;

import android.content.Context;
import android.content.ContextWrapper;

import java.util.ArrayList;

/**
 * Created by keithconger on 12/29/17.
 */

public class FaultStatus extends ContextWrapper {
    private static boolean absFaultActive = false;
    private static String absFaultDesc = "";

    private static boolean fuelFaultActive = false;
    private static String fuelFaultDesc = "";

    private static boolean frontTirePressureActive = false;
    private static String frontTirePressureDesc = "";

    private static boolean rearTirePressureActive = false;
    private static String rearTirePressureDesc = "";

    private static boolean frontLeftSignalActive = false;
    private static String frontLeftSignalDesc = "";

    private static boolean frontRightSignalActive = false;
    private static String frontRightSignalDesc = "";

    private static boolean rearLeftSignalActive = false;
    private static String rearLeftSignalDesc = "";

    private static boolean rearRightSignalActive = false;
    private static String rearRightSignalDesc = "";

    public FaultStatus(Context base) {
        super(base);
        absFaultDesc = MainActivity.getContext().getResources().getString(R.string.fault_ABSF);
        fuelFaultDesc = MainActivity.getContext().getResources().getString(R.string.fault_FUELF);
        frontTirePressureDesc = MainActivity.getContext().getResources().getString(R.string.fault_TIREFF);
        rearTirePressureDesc = MainActivity.getContext().getResources().getString(R.string.fault_TIRERF);
        frontLeftSignalDesc = MainActivity.getContext().getResources().getString(R.string.fault_SIGFLF);
        frontRightSignalDesc = MainActivity.getContext().getResources().getString(R.string.fault_SIGFRF);
        rearLeftSignalDesc = MainActivity.getContext().getResources().getString(R.string.fault_SIGRLF);
        rearRightSignalDesc = MainActivity.getContext().getResources().getString(R.string.fault_SIGRRF);
    }

    public static ArrayList<String> getallActiveDesc() {
        ArrayList<String> allActiveDesc = new ArrayList<String>();
        if(absFaultActive){
            allActiveDesc.add(absFaultDesc);
        }
        if(fuelFaultActive){
            allActiveDesc.add(fuelFaultDesc);
        }
        if(frontTirePressureActive){
            allActiveDesc.add(frontTirePressureDesc);
        }
        if(rearTirePressureActive){
            allActiveDesc.add(rearTirePressureDesc);
        }
        if(frontLeftSignalActive){
            allActiveDesc.add(frontLeftSignalDesc);
        }
        if(frontRightSignalActive){
            allActiveDesc.add(frontRightSignalDesc);
        }
        if(rearLeftSignalActive){
            allActiveDesc.add(rearLeftSignalDesc);
        }
        if(rearRightSignalActive){
            allActiveDesc.add(rearRightSignalDesc);
        }

        return allActiveDesc;
    }

    public static void setabsFaultActive(boolean absFaultActive){
        FaultStatus.absFaultActive = absFaultActive;
    }
    public static boolean getabsFaultActive() {
        return absFaultActive;
    }
    public static String getabsFaultDesc() {
        return absFaultDesc;
    }

    public static void setfuelFaultActive(boolean fuelFaultActive){
        FaultStatus.fuelFaultActive = fuelFaultActive;
    }
    public static boolean getfuelFaultActive() {
        return fuelFaultActive;
    }
    public static String getfuelFaultDesc() {
        return fuelFaultDesc;
    }

    public static void setfrontTirePressureActive(boolean frontTirePressureActive){
        FaultStatus.frontTirePressureActive = frontTirePressureActive;
    }
    public static boolean getfrontTirePressureActive() {
        return frontTirePressureActive;
    }
    public static String getfrontTirePressureDesc() {
        return frontTirePressureDesc;
    }

    public static void setrearTirePressureActive(boolean rearTirePressureActive){
        FaultStatus.rearTirePressureActive = rearTirePressureActive;
    }
    public static boolean getrearTirePressureActive() {
        return rearTirePressureActive;
    }
    public static String getrearTirePressureDesc() {
        return rearTirePressureDesc;
    }

    public static void setfrontLeftSignalActive(boolean frontLeftSignalActive){
        FaultStatus.frontLeftSignalActive = frontLeftSignalActive;
    }
    public static boolean getfrontLeftSignalActive() {
        return frontLeftSignalActive;
    }
    public static String getfrontLeftSignalDesc() {
        return frontLeftSignalDesc;
    }

    public static void setfrontRightSignalActive(boolean frontRightSignalActive){
        FaultStatus.frontRightSignalActive = frontRightSignalActive;
    }
    public static boolean getfrontRightSignalActive() {
        return frontRightSignalActive;
    }
    public static String getfrontRightSignalDesc() {
        return frontRightSignalDesc;
    }

    public static void setrearLeftSignalActive(boolean rearLeftSignalActive){
        FaultStatus.rearLeftSignalActive = rearLeftSignalActive;
    }
    public static boolean getrearLeftSignalActive() {
        return rearLeftSignalActive;
    }
    public static String getrearLeftSignalDesc() {
        return rearLeftSignalDesc;
    }

    public static void setrearRightSignalActive(boolean rearRightSignalActive){
        FaultStatus.rearRightSignalActive = rearRightSignalActive;
    }
    public static boolean getrearRightSignalActive() {
        return rearRightSignalActive;
    }
    public static String rearRightSignalDesc() {
        return rearRightSignalDesc;
    }
}

