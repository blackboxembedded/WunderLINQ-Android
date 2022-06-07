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
package com.blackboxembedded.WunderLINQ.protocols;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.blackboxembedded.WunderLINQ.FaultStatus;
import com.blackboxembedded.WunderLINQ.Utils;

public class CANbus {
    private static int prevBrakeValue = 0;

    public static void parseCANMessage(byte[] data){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        Data.setLastMessage(data);
        int msgID = ((data[0] & 0xFF)<<3) + ((data[1] & 0xFF)>>5);
        //Log.d("CANbus","CANID: " + msgID + "  CANMSG: " + Utils.ByteArraytoHexNoDelim(data));
        switch (msgID){
            case 268:
                // RPM
                int rpm = (((data[4] & 0xFF) + (((data[5] & 0xFF) & 0x0f) * 255)) * 5);
                Data.setRPM(rpm);

                // Lean Angle
                double leanAngle = ((data[6] & 0xFF) + (((data[5] & 0xFF) >> 4) & 0x0f) * 0.1) * (Math.sqrt(2) / 2);
                Data.setLeanAngleBike(leanAngle);
                break;
            case 272:
                // ASC Status - Needs testing
                int ascStatusValue = ((data[5] & 0xFF)  >> 4) & 0x0f; // the highest 4 bits.
                switch (ascStatusValue){
                    case 0x1: case 0x9:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(true);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0x2: case 0x5: case 0x6: case 0x7: case 0xA: case 0xD: case 0xE:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(true);
                        break;
                    case 0x3: case 0xB:
                        FaultStatus.setAscSelfDiagActive(true);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                    case 0x8:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(true);
                        FaultStatus.setascErrorActive(false);
                        break;
                    default:
                        FaultStatus.setAscSelfDiagActive(false);
                        FaultStatus.setAscInterventionActive(false);
                        FaultStatus.setAscDeactivatedActive(false);
                        FaultStatus.setascErrorActive(false);
                        break;
                }

                // Throttle
                int minPosition = 36;
                int maxPosition = 236;
                double throttlePosition = (((data[7] & 0xFF) - minPosition) * 100.0) / (maxPosition - minPosition);
                Data.setThrottlePosition(throttlePosition);
                break;
            case 656:
                // Speed
                double speed = (data[4] * 255 + data[3]) / 118;
                Data.setSpeed(speed);

                // Brakes
                int brakes = ((data[6] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                if(prevBrakeValue == 0){
                    prevBrakeValue = brakes;
                }
                if (prevBrakeValue != brakes) {
                    prevBrakeValue = brakes;
                    switch (brakes) {
                        case 0x6:
                            //Rear
                            Data.setRearBrake(Data.getFrontBrake() + 1);
                            break;
                        case 0x9:
                            //Front
                            Data.setFrontBrake(Data.getRearBrake() + 1);
                            break;
                        case 0xA:
                            //Both
                            Data.setFrontBrake(Data.getFrontBrake() + 1);
                            Data.setRearBrake(Data.getRearBrake() + 1);
                            break;
                        default:
                            break;
                    }
                }
                break;
            case 700:

                // Engine Temperature
                if ((data[4] & 0xFF) != 0xFF) {
                    double engineTemp = ((data[4] & 0xFF) * 0.75) - 25;
                    Data.setEngineTemperature(engineTemp);
                }
                //Gear
                String gear;
                int gearValue = ((data[7] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                switch (gearValue) {
                    case 0x1:
                        gear = "1";
                        break;
                    case 0x2:
                        gear = "N";
                        break;
                    case 0x4:
                        gear = "2";
                        break;
                    case 0x7:
                        gear = "3";
                        break;
                    case 0x8:
                        gear = "4";
                        break;
                    case 0xB:
                        gear = "5";
                        break;
                    case 0xD:
                        gear = "6";
                        break;
                    case 0xF:
                        // Inbetween Gears
                        gear = "-";
                        break;
                    default:
                        gear = "-";
                        Log.d("CANbus", "Unknown gear value");
                }
                if(Data.getGear() != null) {
                    if (!Data.getGear().equals(gear) && !gear.equals("-")) {
                        Data.setNumberOfShifts(Data.getNumberOfShifts() + 1);
                    }
                }
                Data.setGear(gear);

                // ABS Fault - Needs testing
                int absValue = (data[8] & 0xFF);
                switch (absValue){
                    case 0x59:
                        FaultStatus.setAbsSelfDiagActive(true);
                        FaultStatus.setAbsDeactivatedActive(false);
                        break;
                    case 0x41:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(true);
                        break;
                    default:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        break;
                }
                break;
            case 720:
                //Ambient Temp
                double ambientTemp = ((data[4] & 0xFF) * 0.50) - 40;
                Data.setAmbientTemperature(ambientTemp);
                if(ambientTemp <= 0.0){
                    FaultStatus.seticeWarnActive(true);
                } else {
                    FaultStatus.seticeWarnActive(false);
                }

                //LAMPF - Needs testing
                int lampfOneValue = ((data[5] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                switch (lampfOneValue) {
                    case 0x1:
                        FaultStatus.setDaytimeRunningActive(true);
                        FaultStatus.setfrontLeftSignalActive(false);
                        FaultStatus.setfrontRightSignalActive(false);
                        break;
                    case 0x2:
                        FaultStatus.setDaytimeRunningActive(false);
                        FaultStatus.setfrontLeftSignalActive(true);
                        FaultStatus.setfrontRightSignalActive(false);
                        break;
                    case 0x3:
                        FaultStatus.setDaytimeRunningActive(true);
                        FaultStatus.setfrontLeftSignalActive(true);
                        FaultStatus.setfrontRightSignalActive(false);
                        break;
                    case 0x4:
                        FaultStatus.setDaytimeRunningActive(false);
                        FaultStatus.setfrontLeftSignalActive(false);
                        FaultStatus.setfrontRightSignalActive(true);
                        break;
                    case 0x5:
                        FaultStatus.setDaytimeRunningActive(true);
                        FaultStatus.setfrontLeftSignalActive(false);
                        FaultStatus.setfrontRightSignalActive(true);
                        break;
                    case 0x6:
                        FaultStatus.setDaytimeRunningActive(false);
                        FaultStatus.setfrontLeftSignalActive(true);
                        FaultStatus.setfrontRightSignalActive(true);
                        break;
                    case 0x7:
                        FaultStatus.setDaytimeRunningActive(true);
                        FaultStatus.setfrontLeftSignalActive(true);
                        FaultStatus.setfrontRightSignalActive(true);
                        break;
                    default:
                        FaultStatus.setDaytimeRunningActive(false);
                        FaultStatus.setfrontLeftSignalActive(false);
                        FaultStatus.setfrontRightSignalActive(false);
                        break;
                }
                int lampfOneLowValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                switch (lampfOneLowValue) {
                    case 0x1:
                        FaultStatus.setFrontParkingLightOneActive(true);
                        FaultStatus.setFrontParkingLightTwoActive(false);
                        FaultStatus.setLowBeamActive(false);
                        FaultStatus.setHighBeamActive(false);
                        break;
                    case 0x2:
                        FaultStatus.setFrontParkingLightOneActive(false);
                        FaultStatus.setFrontParkingLightTwoActive(true);
                        FaultStatus.setLowBeamActive(false);
                        FaultStatus.setHighBeamActive(false);
                        break;
                    case 0x3:
                        FaultStatus.setFrontParkingLightOneActive(true);
                        FaultStatus.setFrontParkingLightTwoActive(true);
                        FaultStatus.setLowBeamActive(false);
                        FaultStatus.setHighBeamActive(false);
                        break;
                    case 0x4:
                        FaultStatus.setFrontParkingLightOneActive(false);
                        FaultStatus.setFrontParkingLightTwoActive(false);
                        FaultStatus.setLowBeamActive(true);
                        FaultStatus.setHighBeamActive(false);
                        break;
                    case 0x5:
                        FaultStatus.setFrontParkingLightOneActive(true);
                        FaultStatus.setFrontParkingLightTwoActive(false);
                        FaultStatus.setLowBeamActive(true);
                        FaultStatus.setHighBeamActive(false);
                        break;
                    case 0x6:
                        FaultStatus.setFrontParkingLightOneActive(false);
                        FaultStatus.setFrontParkingLightTwoActive(true);
                        FaultStatus.setLowBeamActive(true);
                        FaultStatus.setHighBeamActive(false);
                        break;
                    case 0x7:
                        FaultStatus.setFrontParkingLightOneActive(true);
                        FaultStatus.setFrontParkingLightTwoActive(true);
                        FaultStatus.setLowBeamActive(true);
                        FaultStatus.setHighBeamActive(false);
                        break;
                    case 0x8:
                        FaultStatus.setFrontParkingLightOneActive(false);
                        FaultStatus.setFrontParkingLightTwoActive(false);
                        FaultStatus.setLowBeamActive(false);
                        FaultStatus.setHighBeamActive(true);
                        break;
                    case 0x9:
                        FaultStatus.setFrontParkingLightOneActive(true);
                        FaultStatus.setFrontParkingLightTwoActive(false);
                        FaultStatus.setLowBeamActive(false);
                        FaultStatus.setHighBeamActive(true);
                        break;
                    case 0xA:
                        FaultStatus.setFrontParkingLightOneActive(false);
                        FaultStatus.setFrontParkingLightTwoActive(true);
                        FaultStatus.setLowBeamActive(false);
                        FaultStatus.setHighBeamActive(true);
                        break;
                    case 0xB:
                        FaultStatus.setFrontParkingLightOneActive(true);
                        FaultStatus.setFrontParkingLightTwoActive(true);
                        FaultStatus.setLowBeamActive(false);
                        FaultStatus.setHighBeamActive(true);
                        break;
                    case 0xC:
                        FaultStatus.setFrontParkingLightOneActive(false);
                        FaultStatus.setFrontParkingLightTwoActive(false);
                        FaultStatus.setLowBeamActive(true);
                        FaultStatus.setHighBeamActive(true);
                        break;
                    case 0xD:
                        FaultStatus.setFrontParkingLightOneActive(true);
                        FaultStatus.setFrontParkingLightTwoActive(false);
                        FaultStatus.setLowBeamActive(true);
                        FaultStatus.setHighBeamActive(true);
                        break;
                    case 0xE:
                        FaultStatus.setFrontParkingLightOneActive(false);
                        FaultStatus.setFrontParkingLightTwoActive(true);
                        FaultStatus.setLowBeamActive(true);
                        FaultStatus.setHighBeamActive(true);
                        break;
                    case 0xF:
                        FaultStatus.setFrontParkingLightOneActive(true);
                        FaultStatus.setFrontParkingLightTwoActive(true);
                        FaultStatus.setLowBeamActive(true);
                        FaultStatus.setHighBeamActive(true);
                        break;
                    default:
                        FaultStatus.setFrontParkingLightOneActive(false);
                        FaultStatus.setFrontParkingLightTwoActive(false);
                        FaultStatus.setLowBeamActive(false);
                        FaultStatus.setHighBeamActive(false);
                        break;
                }

                int lampfTwoHighValue = ((data[6] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                switch (lampfTwoHighValue) {
                    case 0x1: case 0x3: case 0x5: case 0x7: case 0x9: case 0xB: case 0xD: case 0xF:
                        FaultStatus.setrearRightSignalActive(true);
                        break;
                    default:
                        FaultStatus.setrearRightSignalActive(false);
                        break;
                }
                int lampfTwoLowValue = (data[6] & 0xFF) & 0x0f; // the lowest 4 bits
                switch (lampfTwoLowValue) {
                    case 0x1:
                        FaultStatus.setrearLeftSignalActive(false);
                        FaultStatus.setRearLightActive(true);
                        FaultStatus.setBrakeLightActive(false);
                        FaultStatus.setLicenseLightActive(false);
                        break;
                    case 0x2:
                        FaultStatus.setrearLeftSignalActive(false);
                        FaultStatus.setRearLightActive(false);
                        FaultStatus.setBrakeLightActive(true);
                        FaultStatus.setLicenseLightActive(false);
                        break;
                    case 0x3:
                        FaultStatus.setrearLeftSignalActive(false);
                        FaultStatus.setRearLightActive(true);
                        FaultStatus.setBrakeLightActive(true);
                        FaultStatus.setLicenseLightActive(false);
                        break;
                    case 0x4:
                        FaultStatus.setrearLeftSignalActive(false);
                        FaultStatus.setRearLightActive(false);
                        FaultStatus.setBrakeLightActive(false);
                        FaultStatus.setLicenseLightActive(true);
                        break;
                    case 0x5: case 0xC:
                        FaultStatus.setrearLeftSignalActive(true);
                        FaultStatus.setRearLightActive(false);
                        FaultStatus.setBrakeLightActive(false);
                        FaultStatus.setLicenseLightActive(true);
                        break;
                    case 0x6:
                        FaultStatus.setrearLeftSignalActive(false);
                        FaultStatus.setRearLightActive(false);
                        FaultStatus.setBrakeLightActive(true);
                        FaultStatus.setLicenseLightActive(true);
                        break;
                    case 0x7:
                        FaultStatus.setrearLeftSignalActive(false);
                        FaultStatus.setRearLightActive(true);
                        FaultStatus.setBrakeLightActive(true);
                        FaultStatus.setLicenseLightActive(true);
                        break;
                    case 0x8:
                        FaultStatus.setrearLeftSignalActive(true);
                        FaultStatus.setRearLightActive(false);
                        FaultStatus.setBrakeLightActive(false);
                        FaultStatus.setLicenseLightActive(false);
                        break;
                    case 0x9:
                        FaultStatus.setrearLeftSignalActive(true);
                        FaultStatus.setRearLightActive(true);
                        FaultStatus.setBrakeLightActive(false);
                        FaultStatus.setLicenseLightActive(false);
                        break;
                    case 0xA:
                        FaultStatus.setrearLeftSignalActive(true);
                        FaultStatus.setRearLightActive(false);
                        FaultStatus.setBrakeLightActive(true);
                        FaultStatus.setLicenseLightActive(false);
                        break;
                    case 0xD:
                        FaultStatus.setrearLeftSignalActive(true);
                        FaultStatus.setRearLightActive(true);
                        FaultStatus.setBrakeLightActive(true);
                        FaultStatus.setLicenseLightActive(false);
                        break;
                    case 0xE:
                        FaultStatus.setrearLeftSignalActive(true);
                        FaultStatus.setRearLightActive(false);
                        FaultStatus.setBrakeLightActive(true);
                        FaultStatus.setLicenseLightActive(true);
                        break;
                    case 0xF:
                        FaultStatus.setrearLeftSignalActive(true);
                        FaultStatus.setRearLightActive(true);
                        FaultStatus.setBrakeLightActive(true);
                        FaultStatus.setLicenseLightActive(true);
                        break;
                    default:
                        FaultStatus.setrearLeftSignalActive(false);
                        FaultStatus.setRearLightActive(false);
                        FaultStatus.setBrakeLightActive(false);
                        FaultStatus.setLicenseLightActive(false);
                        break;
                }

                //Heated Grip Status
                int heatedGripsValue = (data[7] & 0xFF);
                switch (heatedGripsValue) {
                    case 0x49:
                        Data.setHeatedGrips(Data.GRIP_LOW);
                        break;
                    case 0x51:
                        Data.setHeatedGrips(Data.GRIP_HIGH);
                        break;
                    default:
                        Data.setHeatedGrips(Data.GRIP_OFF);
                        break;
                }

                //High Beam Status
                int highBeamValue = ((data[8] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                switch (highBeamValue) {
                    case 0x6:
                        Data.setHighBeam(true);
                        break;
                    default:
                        Data.setHighBeam(false);
                        break;
                }

                //Fog Light Status - Need confirmation
                int fogLightValue = (data[9] & 0xFF) & 0x0f; // the lowest 4 bits
                switch (fogLightValue) {
                    case 0x2:
                        Data.setFogLight(true);
                        break;
                    default:
                        Data.setFogLight(false);
                        break;
                }
                break;
            case 928:
                // Tire Pressure - Needs testing
                if ((data[8] & 0xFF) != 0xFF) {
                    double rdcFront = (data[8] & 0xFF) / 50.0;
                    Data.setFrontTirePressure(rdcFront);
                    if (sharedPrefs.getBoolean("prefTPMSAlert", false)) {
                        int pressureThreshold = Integer.parseInt(sharedPrefs.getString("prefTPMSAlertThreshold", "-1"));
                        if (pressureThreshold >= 0) {
                            String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
                            if (pressureFormat.contains("1")) {
                                // KPa
                                if (pressureThreshold >= Utils.barTokPa(rdcFront)) {
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("2")) {
                                // Kg-f
                                if (pressureThreshold >= Utils.barTokgf(rdcFront)) {
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("3")) {
                                // Psi
                                if (pressureThreshold >= Utils.barToPsi(rdcFront)) {
                                    FaultStatus.setfrontTirePressureCriticalActive(true);
                                }
                            }
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                                }
                            }
                        } else {
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                }
                            }
                        }
                    }
                }
                if ((data[9] & 0xFF) != 0xFF){
                    double rdcRear = (data[9] & 0xFF) / 50.0;
                    Data.setRearTirePressure(rdcRear);
                    if (sharedPrefs.getBoolean("prefTPMSAlert",false)) {
                        int pressureThreshold = Integer.parseInt(sharedPrefs.getString("prefTPMSAlertThreshold","-1"));
                        if (pressureThreshold >= 0) {
                            String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
                            if (pressureFormat.contains("1")) {
                                // KPa
                                if (pressureThreshold >= Utils.barTokPa(rdcRear)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("2")) {
                                // Kg-f
                                if (pressureThreshold >= Utils.barTokgf(rdcRear)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("3")) {
                                // Psi
                                if (pressureThreshold >= Utils.barToPsi(rdcRear)){
                                    FaultStatus.setrearTirePressureCriticalActive(true);
                                }
                            }
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                                }
                            }
                        } else {
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                }
                            }
                        }
                    }
                }
                break;
            case 1023:
                // Cluster Ambient Light
                int ambientLightValue = (data[3] & 0xFF) & 0x0f; // the lowest 4 bits
                Data.setAmbientLight(ambientLightValue);

                // Odometer
                double odometer = Utils.bytesToInt16(data[9], data[8], data[7]);
                Data.setOdometer(odometer);
                break;
            default:
                break;
        }
    }
}
