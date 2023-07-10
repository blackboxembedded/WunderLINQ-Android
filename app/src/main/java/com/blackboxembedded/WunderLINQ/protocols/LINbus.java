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

import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.blackboxembedded.WunderLINQ.FaultStatus;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.Utils.Utils;

import java.time.LocalDate;

public class LINbus {
    private static int prevBrakeValue = 0;
    public static void parseLINMessage(byte[] data){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        Data.setLastMessage(data);
        int msgID = (data[0] & 0xFF) ;
        switch (msgID) {
            case 0x00:
                byte[] vinValue = new byte[7];
                int sum = 0;
                for (int x = 1; x <= 7; x++){
                    vinValue[x - 1] = data[x];
                    sum = sum + data[x];
                }
                if (sum > 0) {
                    String vin = new String(vinValue);
                    Data.setVin(vin);
                }
                break;
            case 0x01:
                //Rear Speed
                if ((data[3] & 0xFF) != 0xFF && (data[4] & 0xFF) != 0xFF) {
                    double rearSpeed = ((data[3] & 0xFF) | (((data[4] & 0xFF) & 0x0f) << 8)) * 0.14;
                    Data.setRearSpeed(rearSpeed);
                }

                //Fuel Range
                if ((data[4] & 0xFF) != 0xFF && (data[5] & 0xFF) != 0xFF) {
                    double fuelRange = (((data[4] & 0xFF) >> 4) & 0x0f) + (((data[5] & 0xFF) & 0x0f) * 16) + ((((data[5] & 0xFF) >> 4) & 0x0f) * 256);
                    Data.setFuelRange(fuelRange);
                }
                // Ambient Light
                int ambientLightValue = (data[6] & 0xFF) & 0x0f; // the lowest 4 bits
                Data.setAmbientLight(ambientLightValue);
                break;
            case 0x05:
                // Lean Angle
                if((((data[2] & 0xFF) & 0x0f) != 0xF) && (data[1] & 0xFF) != 0xFF){
                    int lowNibble = (data[2] & 0xFF) & 0x0f;
                    double leanAngleBike = Utils.bytesToInt12((byte)lowNibble, data[1]);
                    double leanAngleBikeFixed;
                    if(leanAngleBike >= 2048){
                        leanAngleBikeFixed = leanAngleBike - 2048;
                    } else {
                        leanAngleBikeFixed = (2048 - leanAngleBike) * -1.0;
                    }
                    leanAngleBikeFixed = leanAngleBikeFixed * 0.045;
                    Data.setLeanAngleBike(leanAngleBikeFixed);
                    //Store Max L and R lean angle
                    if(leanAngleBikeFixed > 0){
                        if (Data.getLeanAngleMaxR() != null) {
                            if (leanAngleBikeFixed > Data.getLeanAngleMaxR()) {
                                Data.setLeanAngleMaxR(leanAngleBikeFixed);
                            }
                        } else {
                            Data.setLeanAngleMaxR(leanAngleBikeFixed);
                        }
                    } else if(leanAngleBikeFixed < 0){
                        if (Data.getLeanAngleMaxL() != null) {
                            if (Math.abs(leanAngleBikeFixed) > Data.getLeanAngleMaxL()) {
                                Data.setLeanAngleMaxL(Math.abs(leanAngleBikeFixed));
                            }
                        } else {
                            Data.setLeanAngleMaxL(Math.abs(leanAngleBikeFixed));
                        }
                    }
                }

                // Brakes
                int brakes = ((data[2] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                if(prevBrakeValue == 0){
                    prevBrakeValue = brakes;
                }
                if (prevBrakeValue != brakes) {
                    prevBrakeValue = brakes;
                    switch (brakes) {
                        case 0x6:
                            //Front
                            Data.setFrontBrake(Data.getFrontBrake() + 1);
                            break;
                        case 0x9:
                            //Back
                            Data.setRearBrake(Data.getRearBrake() + 1);
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
                // ABS Fault
                int absValue = (data[3] & 0xFF) & 0x0f; // the lowest 4 bits
                switch (absValue){
                    case 0x2: case 0x5: case 0x6: case 0x7: case 0xA: case 0xD: case 0xE:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(true);
                        break;
                    case 0x3: case 0xB:
                        FaultStatus.setAbsSelfDiagActive(true);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(false);
                        break;
                    case 0x8:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(true);
                        FaultStatus.setabsErrorActive(false);
                        break;
                    case 0xF: default:
                        FaultStatus.setAbsSelfDiagActive(false);
                        FaultStatus.setAbsDeactivatedActive(false);
                        FaultStatus.setabsErrorActive(false);
                        break;
                }

                // Tire Pressure
                if ((data[4] & 0xFF) != 0xFF) {
                    double rdcFront = (data[4] & 0xFF) / 50.0;
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
                if ((data[5] & 0xFF) != 0xFF){
                    double rdcRear = (data[5] & 0xFF) / 50.0;
                    Data.setRearTirePressure(rdcRear);
                    if (sharedPrefs.getBoolean("prefDebugLogging", false)) {
                        // Log data
                        Log.d("RearTirePressure","Value Received: " + rdcRear);
                    }
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
                } else {
                    if (sharedPrefs.getBoolean("prefDebugLogging", false)) {
                        // Log data
                        Log.d("RearTirePressure","Value Received: 0xFF");
                    }
                }

                if (!sharedPrefs.getBoolean("prefTPMSAlert",false)) {
                    // Tire Pressure Faults
                    switch (data[6] & 0xFF) {
                        case 0xC9:
                            FaultStatus.setfrontTirePressureWarningActive(true);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                        case 0xCA:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(true);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                        case 0xCB:
                            FaultStatus.setfrontTirePressureWarningActive(true);
                            FaultStatus.setrearTirePressureWarningActive(true);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                        case 0xD1:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(true);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                        case 0xD2:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(true);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (!(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                                }
                            }
                            break;
                        case 0xD3:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(true);
                            FaultStatus.setrearTirePressureCriticalActive(true);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(FaultStatus.getfrontTirePressureCriticalNotificationActive()) && !(FaultStatus.getrearTirePressureCriticalNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(true);
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(true);
                                }
                            }
                            break;
                        default:
                            FaultStatus.setfrontTirePressureWarningActive(false);
                            FaultStatus.setrearTirePressureWarningActive(false);
                            FaultStatus.setfrontTirePressureCriticalActive(false);
                            FaultStatus.setrearTirePressureCriticalActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (FaultStatus.getfrontTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setfrontTirePressureCriticalNotificationActive(false);
                                }
                                if (FaultStatus.getrearTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    FaultStatus.setrearTirePressureCriticalNotificationActive(false);
                                }
                            }
                            break;
                    }
                }
                break;
            case 0x06:
                //RPM
                int rpm = (((data[1] & 0xFF) + (((data[2] & 0xFF) & 0x0f) * 255)) * 5);
                Data.setRPM(rpm);

                //Gear
                String gear;
                int gearValue = ((data[2] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
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
                        Log.d("LINbus", "Unknown gear value");
                }
                if(Data.getGear() != null) {
                    if (!Data.getGear().equals(gear) && !gear.equals("-")) {
                        Data.setNumberOfShifts(Data.getNumberOfShifts() + 1);
                    }
                }
                Data.setGear(gear);

                // Throttle Position
                if ((data[3] & 0xFF) != 0xFF) {
                    int minPosition = 36;
                    int maxPosition = 236;
                    double throttlePosition = (((data[3] & 0xFF) - minPosition) * 100.0) / (maxPosition - minPosition);
                    Data.setThrottlePosition(throttlePosition);
                }

                // Engine Temperature
                if ((data[4] & 0xFF) != 0xFF) {
                    double engineTemp = ((data[4] & 0xFF) * 0.75) - 25;
                    Data.setEngineTemperature(engineTemp);
                }

                // ASC Fault
                int ascValue = ((data[5] & 0xFF)  >> 4) & 0x0f; // the highest 4 bits.
                switch (ascValue){
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

                //Oil Fault
                int oilValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                switch (oilValue){
                    case 0x2: case 0x6: case 0xA: case 0xE:
                        FaultStatus.setOilLowActive(true);
                        break;
                    default:
                        FaultStatus.setOilLowActive(false);
                        break;
                }

                break;
            case 0x07:
                //Average Speed
                if ((data[1] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF) {
                    double avgSpeed = ((((data[1] & 0xFF) >> 4) & 0x0f) * 2) + (((data[1] & 0xFF) & 0x0f) * 0.125) + (((data[2] & 0xFF) & 0x0f) * 32);
                    Data.setAvgSpeed(avgSpeed);
                }

                //Speed
                if ((data[3] & 0xFF) != 0xFF) {
                    double speed = (data[3] & 0xFF) * 2;
                    Data.setSpeed(speed);
                }

                //Voltage
                if ((data[4] & 0xFF) != 0xFF) {
                    double voltage = (data[4] & 0xFF) / 10.0;
                    Data.setvoltage(voltage);
                }

                // Fuel Fault
                int fuelValue = ((data[5] & 0xFF)  >> 4) & 0x0f; // the highest 4 bits.
                switch (fuelValue){
                    case 0x2: case 0x6: case 0xA: case 0xE:
                        FaultStatus.setfuelFaultActive(true);
                        BluetoothLeService.fuelAlert();
                        break;
                    default:
                        FaultStatus.setfuelFaultActive(false);
                        BluetoothLeService.fuelAlertSent = false;
                        break;
                }
                // General Fault
                int generalFault = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                switch (generalFault){
                    case 0x1: case 0xD:
                        FaultStatus.setGeneralFlashingYellowActive(true);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x2: case 0xE:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x4: case 0x7:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(true);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x5:
                        FaultStatus.setGeneralFlashingYellowActive(true);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(true);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x6:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (!(FaultStatus.getgeneralFlashingRedNotificationActive())) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(true);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                    case 0x8: case 0xB:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (!(FaultStatus.getgeneralShowsRedNotificationActive())) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(true);
                            }
                        }
                        break;
                    case 0x9:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(true);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (!FaultStatus.getgeneralShowsRedNotificationActive() && !FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(true);
                                FaultStatus.setGeneralShowsRedNotificationActive(true);
                            }
                        }
                        break;
                    case 0xA:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(true);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(true);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (!(FaultStatus.getgeneralShowsRedNotificationActive())) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(true);
                            }
                        }
                        break;
                    default:
                        FaultStatus.setGeneralFlashingYellowActive(false);
                        FaultStatus.setGeneralShowsYellowActive(false);
                        FaultStatus.setGeneralFlashingRedActive(false);
                        FaultStatus.setGeneralShowsRedActive(false);
                        if (sharedPrefs.getBoolean("prefNotifications", true)) {
                            if (FaultStatus.getgeneralFlashingRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralFlashingRedNotificationActive(false);
                            }
                            if (FaultStatus.getgeneralShowsRedNotificationActive()) {
                                BluetoothLeService.updateNotification();
                                FaultStatus.setGeneralShowsRedNotificationActive(false);
                            }
                        }
                        break;
                }
                break;
            case 0x08:
                if ((data[1] & 0xFF) != 0xFF) {
                    double ambientTemp = ((data[1] & 0xFF) * 0.50) - 40;
                    Data.setAmbientTemperature(ambientTemp);
                    if(ambientTemp <= 0.0){
                        FaultStatus.seticeWarnActive(true);
                    } else {
                        FaultStatus.seticeWarnActive(false);
                    }
                }

                // LAMP Faults
                if (((data[3] & 0xFF) != 0xFF) ) {
                    // LAMPF 1
                    int lampfOneValue = ((data[3] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfOneValue) {
                        case 0x1: case 0x5: case 0x9: case 0xD:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(false);
                            break;
                        case 0x2: case 0x6: case 0xA: case 0xE:
                            FaultStatus.setAddFrontLightOneActive(false);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        case 0x3: case 0xB:
                            FaultStatus.setAddFrontLightOneActive(true);
                            FaultStatus.setAddFrontLightTwoActive(true);
                            break;
                        default:
                            FaultStatus.setAddFrontLightOneActive(false);
                            FaultStatus.setAddFrontLightTwoActive(false);
                            break;
                    }
                }
                // LAMPF 2
                if (((data[4] & 0xFF) != 0xFF) ) {
                    int lampfTwoHighValue = ((data[4] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfTwoHighValue) {
                        case 0x1: case 0x9:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x2: case 0xA:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x3: case 0xB:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(false);
                            break;
                        case 0x4: case 0xC:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x5: case 0xD:
                            FaultStatus.setDaytimeRunningActive(true);
                            FaultStatus.setfrontLeftSignalActive(false);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x6: case 0xE:
                            FaultStatus.setDaytimeRunningActive(false);
                            FaultStatus.setfrontLeftSignalActive(true);
                            FaultStatus.setfrontRightSignalActive(true);
                            break;
                        case 0x7: case 0xF:
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
                    int lampfTwoLowValue = (data[4] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfTwoLowValue) {
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
                }

                // LAMPF 3
                if (((data[5] & 0xFF) != 0xFF) ) {
                    int lampfThreeHighValue = ((data[5] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfThreeHighValue) {
                        case 0x1: case 0x3: case 0x5: case 0x7: case 0x9: case 0xB: case 0xD: case 0xF:
                            FaultStatus.setrearRightSignalActive(true);
                            break;
                        default:
                            FaultStatus.setrearRightSignalActive(false);
                            break;
                    }
                    int lampfThreeLowValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfThreeLowValue) {
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
                }

                // LAMPF 4
                if (((data[6] & 0xFF) != 0xFF) ) {
                    int lampfFourHighValue = ((data[6] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfFourHighValue) {
                        case 0x1: case 0x3: case 0x5: case 0x9: case 0xB: case 0xD: case 0xF:
                            FaultStatus.setRearFogLightActive(true);
                            break;
                        default:
                            FaultStatus.setRearFogLightActive(false);
                            break;
                    }
                    int lampfFourLowValue = (data[6] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfFourLowValue) {
                        case 0x1:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x2:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x3:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x4:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x5:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x6:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x7:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                        case 0x8:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0x9:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xA:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xB:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xC:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xD:
                            FaultStatus.setAddDippedLightActive(true);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        case 0xE:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(true);
                            FaultStatus.setFrontLampOneLightActive(true);
                            FaultStatus.setFrontLampTwoLightActive(true);
                            break;
                        default:
                            FaultStatus.setAddDippedLightActive(false);
                            FaultStatus.setAddBrakeLightActive(false);
                            FaultStatus.setFrontLampOneLightActive(false);
                            FaultStatus.setFrontLampTwoLightActive(false);
                            break;
                    }
                }
                break;
            case 0x09:
                //Fuel Economy 1
                if ((data[2] & 0xFF) != 0xFF) {
                    double fuelEconomyOne = ((((data[2] & 0xFF) >> 4) & 0x0f) * 1.6) + (((data[2] & 0xFF) & 0x0f) * 0.1);
                    Data.setFuelEconomyOne(fuelEconomyOne);
                } else {
                    Data.setFuelEconomyOne(null);
                }
                //Fuel Economy 2
                if ((data[3] & 0xFF) != 0xFF) {
                    double fuelEconomyTwo = ((((data[3] & 0xFF) >> 4) & 0x0f) * 1.6) + (((data[3] & 0xFF) & 0x0f) * 0.1);
                    Data.setFuelEconomyTwo(fuelEconomyTwo);
                } else {
                    Data.setFuelEconomyTwo(null);
                }
                //Current Consumption
                if ((data[4] & 0xFF) != 0xFF) {
                    double cConsumption = ((((data[4] & 0xFF) >> 4) & 0x0f) * 1.6) + (((data[4] & 0xFF) & 0x0f) * 0.1);
                    Data.setCurrentConsumption(cConsumption);
                } else {
                    Data.setCurrentConsumption(null);
                }
                break;
            case 0x0a:
                if ((data[3] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF && (data[1] & 0xFF) != 0xFF) {
                    double odometer = Utils.bytesToInt16(data[3], data[2], data[1]);
                    Data.setOdometer(odometer);
                }

                if ((data[6] & 0xFF) != 0xFF && (data[5] & 0xFF) != 0xFF && (data[4] & 0xFF) != 0xFF) {
                    double tripAuto = Utils.bytesToInt16(data[6], data[5], data[4]) / 10.0;
                    Data.setTripAuto(tripAuto);
                }
                break;
            case 0x0b:
                if ((data[3] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF && (data[1] & 0xFF) != 0xFF) {
                    int year = (((data[2] & 0xFF) & 0x0f) << 8) |(data[1] & 0xFF);
                    int month = ((data[2] & 0xFF) >> 4 & 0x0f) - 1;
                    int day = (data[3] & 0xFF);
                    LocalDate nextServiceDate = LocalDate.of(year, month, day);
                    Data.setNextServiceDate(nextServiceDate);

                    // Getting the current date
                    LocalDate currentDate = LocalDate.now();

                    // Comparing the two dates
                    int comparison = nextServiceDate.compareTo(currentDate);
                    if (comparison <= 0) {
                        FaultStatus.setServiceActive(true);
                    } else {
                        FaultStatus.setServiceActive(false);
                    }
                }
                if ((data[4] & 0xFF) != 0xFF){
                    int nextService = data[4] * 100;
                    Data.setNextService(nextService);
                }
                break;
            case 0x0c:
                if ((data[3] & 0xFF) != 0xFF && (data[2] & 0xFF) != 0xFF && (data[1] & 0xFF) != 0xFF) {
                    double trip1 = Utils.bytesToInt16(data[3], data[2], data[1]) / 10.0;
                    Data.setTripOne(trip1);
                }
                if ((data[6] & 0xFF) != 0xFF && (data[5] & 0xFF) != 0xFF && (data[4] & 0xFF) != 0xFF) {
                    double trip2 = Utils.bytesToInt16(data[6], data[5], data[4]) / 10.0;
                    Data.setTripTwo(trip2);
                }
                break;
            default:
                break;
        }
    }
}
