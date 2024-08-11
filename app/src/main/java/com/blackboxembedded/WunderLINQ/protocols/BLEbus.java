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

import com.blackboxembedded.WunderLINQ.comms.BLE.BluetoothLeService;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Data;
import com.blackboxembedded.WunderLINQ.hardware.WLQ.Faults;
import com.blackboxembedded.WunderLINQ.MyApplication;
import com.blackboxembedded.WunderLINQ.Utils.Utils;

import java.time.LocalDate;

public class BLEbus {
    private static int prevBrakeValue = 0;
    public static void parseBLEMessage(byte[] data){
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        Data.setLastMessage(data);
        int msgID = (data[0] & 0xFF) ;
        switch (msgID) {
            case 0x00:
                if ((data[1] & 0xFF) != 0xFF || (data[2] & 0xFF) != 0xFF || (data[3] & 0xFF) != 0xFF || (data[4] & 0xFF) != 0xFF
                        || (data[5] & 0xFF) != 0xFF || (data[6] & 0xFF) != 0xFF || (data[7] & 0xFF) != 0xFF) {
                    byte[] vinValue = new byte[7];
                    for (int x = 1; x <= 7; x++) {
                        vinValue[x - 1] = data[x];
                    }
                    String vin = new String(vinValue);
                    Data.setVin(vin);
                }
                break;
            case 0x01:
                //Ignition
                int ignitionValue = ((data[2] & 0xFF) >> 4) & 0x0f;
                switch (ignitionValue){
                    case 0x0: case 0x1: case 0x2: case 0x3:
                        //Ignition Off
                        if (Data.getIgnitionStatus()){
                            BluetoothLeService.ignitionAlert();
                        }
                        Data.setIgnitionStatus(false);
                        break;
                    case 0x4: case 0x5: case 0x6: case 0x7:
                        //Ignition On
                        Data.setIgnitionStatus(true);
                        break;
                    default:
                        //Unknown
                        Data.setIgnitionStatus(false);
                        break;
                }

                //Rear Speed
                if (((data[3] & 0xFF) != 0xFF) || ((data[4] & 0xFF) & 0x0f) != 0xF) {
                    double rearSpeed = ((data[3] & 0xFF) | (((data[4] & 0xFF) & 0x0f) << 8)) * 0.14;
                    Data.setRearSpeed(rearSpeed);
                }

                //Fuel Range
                if ((((data[4] & 0xFF) >> 4) & 0x0f)  != 0xF && (data[5] & 0xFF) != 0xFF) {
                    double fuelRange = (((data[4] & 0xFF) >> 4) & 0x0f) + (((data[5] & 0xFF) & 0x0f) * 16) + ((((data[5] & 0xFF) >> 4) & 0x0f) * 256);
                    Data.setFuelRange(fuelRange);
                }
                // Ambient Light
                if (((data[6] & 0xFF) & 0x0f) != 0xF) {
                    int ambientLightValue = (data[6] & 0xFF) & 0x0f; // the lowest 4 bits
                    Data.setAmbientLight(ambientLightValue);
                }
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
                if ((((data[2] & 0xFF) >> 4) & 0x0f) != 0xF) {
                    int brakes = ((data[2] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    if (prevBrakeValue == 0) {
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
                }
                // ABS Fault
                if (((data[3] & 0xFF) & 0x0f) != 0xF) {
                    int absValue = (data[3] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (absValue) {
                        case 0x2:
                        case 0x5:
                        case 0x6:
                        case 0x7:
                        case 0xA:
                        case 0xD:
                        case 0xE:
                            Faults.setAbsSelfDiagActive(false);
                            Faults.setAbsDeactivatedActive(false);
                            Faults.setabsErrorActive(true);
                            break;
                        case 0x3:
                        case 0xB:
                            Faults.setAbsSelfDiagActive(true);
                            Faults.setAbsDeactivatedActive(false);
                            Faults.setabsErrorActive(false);
                            break;
                        case 0x8:
                            Faults.setAbsSelfDiagActive(false);
                            Faults.setAbsDeactivatedActive(true);
                            Faults.setabsErrorActive(false);
                            break;
                        case 0xF:
                        default:
                            Faults.setAbsSelfDiagActive(false);
                            Faults.setAbsDeactivatedActive(false);
                            Faults.setabsErrorActive(false);
                            break;
                    }
                }
                // Tire Pressure
                if ((data[4] & 0xFF) != 0xFF) {
                    double rdcFront = (data[4] & 0xFF) / 50.0;
                    Data.setFrontTirePressure(rdcFront);
                    if (sharedPrefs.getBoolean("prefTPMSAlert", false)) {
                        Double pressureThreshold = Double.parseDouble(sharedPrefs.getString("prefTPMSAlertThreshold", "30.0"));
                        if (pressureThreshold >= 0) {
                            String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
                            if (pressureFormat.contains("1")) {
                                // KPa
                                if (pressureThreshold >= Utils.barTokPa(rdcFront)) {
                                    Faults.setfrontTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("2")) {
                                // Kg-f
                                if (pressureThreshold >= Utils.barTokgf(rdcFront)) {
                                    Faults.setfrontTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("3")) {
                                // Psi
                                if (pressureThreshold >= Utils.barToPsi(rdcFront)) {
                                    Faults.setfrontTirePressureCriticalActive(true);
                                }
                            }
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(Faults.getfrontTirePressureCriticalNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setfrontTirePressureCriticalNotificationActive(true);
                                }
                            }
                        } else {
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                Faults.setfrontTirePressureCriticalNotificationActive(false);
                                if (Faults.getfrontTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                }
                            }
                        }
                    }
                }
                if ((data[5] & 0xFF) != 0xFF){
                    double rdcRear = (data[5] & 0xFF) / 50.0;
                    Data.setRearTirePressure(rdcRear);
                    if (sharedPrefs.getBoolean("prefTPMSAlert",false)) {
                        Double pressureThreshold = Double.parseDouble(sharedPrefs.getString("prefTPMSAlertThreshold", "30.0"));
                        if (pressureThreshold >= 0) {
                            String pressureFormat = sharedPrefs.getString("prefPressureF", "0");
                            if (pressureFormat.contains("1")) {
                                // KPa
                                if (pressureThreshold >= Utils.barTokPa(rdcRear)){
                                    Faults.setrearTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("2")) {
                                // Kg-f
                                if (pressureThreshold >= Utils.barTokgf(rdcRear)){
                                    Faults.setrearTirePressureCriticalActive(true);
                                }
                            } else if (pressureFormat.contains("3")) {
                                // Psi
                                if (pressureThreshold >= Utils.barToPsi(rdcRear)){
                                    Faults.setrearTirePressureCriticalActive(true);
                                }
                            }
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(Faults.getrearTirePressureCriticalNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setrearTirePressureCriticalNotificationActive(true);
                                }
                            }
                        } else {
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                Faults.setrearTirePressureCriticalNotificationActive(false);
                                if (Faults.getrearTirePressureCriticalNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                }
                            }
                        }
                    }
                }
                if ((data[6] & 0xFF) != 0xFF) {
                    if (!sharedPrefs.getBoolean("prefTPMSAlert", false)) {
                        // Tire Pressure Faults
                        switch (data[6] & 0xFF) {
                            case 0xC9:
                                Faults.setfrontTirePressureWarningActive(true);
                                Faults.setrearTirePressureWarningActive(false);
                                Faults.setfrontTirePressureCriticalActive(false);
                                Faults.setrearTirePressureCriticalActive(false);
                                if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                    if (Faults.getfrontTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setfrontTirePressureCriticalNotificationActive(false);
                                    }
                                    if (Faults.getrearTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setrearTirePressureCriticalNotificationActive(false);
                                    }
                                }
                                break;
                            case 0xCA:
                                Faults.setfrontTirePressureWarningActive(false);
                                Faults.setrearTirePressureWarningActive(true);
                                Faults.setfrontTirePressureCriticalActive(false);
                                Faults.setrearTirePressureCriticalActive(false);
                                if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                    if (Faults.getfrontTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setfrontTirePressureCriticalNotificationActive(false);
                                    }
                                    if (Faults.getrearTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setrearTirePressureCriticalNotificationActive(false);
                                    }
                                }
                                break;
                            case 0xCB:
                                Faults.setfrontTirePressureWarningActive(true);
                                Faults.setrearTirePressureWarningActive(true);
                                Faults.setfrontTirePressureCriticalActive(false);
                                Faults.setrearTirePressureCriticalActive(false);
                                if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                    if (Faults.getfrontTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setfrontTirePressureCriticalNotificationActive(false);
                                    }
                                    if (Faults.getrearTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setrearTirePressureCriticalNotificationActive(false);
                                    }
                                }
                                break;
                            case 0xD1:
                                Faults.setfrontTirePressureWarningActive(false);
                                Faults.setrearTirePressureWarningActive(false);
                                Faults.setfrontTirePressureCriticalActive(true);
                                Faults.setrearTirePressureCriticalActive(false);
                                if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                    if (!(Faults.getfrontTirePressureCriticalNotificationActive())) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setfrontTirePressureCriticalNotificationActive(true);
                                    }
                                    if (Faults.getrearTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setrearTirePressureCriticalNotificationActive(false);
                                    }
                                }
                                break;
                            case 0xD2:
                                Faults.setfrontTirePressureWarningActive(false);
                                Faults.setrearTirePressureWarningActive(false);
                                Faults.setfrontTirePressureCriticalActive(false);
                                Faults.setrearTirePressureCriticalActive(true);
                                if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                    if (Faults.getfrontTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setfrontTirePressureCriticalNotificationActive(false);
                                    }
                                    if (!(Faults.getrearTirePressureCriticalNotificationActive())) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setrearTirePressureCriticalNotificationActive(true);
                                    }
                                }
                                break;
                            case 0xD3:
                                Faults.setfrontTirePressureWarningActive(false);
                                Faults.setrearTirePressureWarningActive(false);
                                Faults.setfrontTirePressureCriticalActive(true);
                                Faults.setrearTirePressureCriticalActive(true);
                                if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                    if (!(Faults.getfrontTirePressureCriticalNotificationActive()) && !(Faults.getrearTirePressureCriticalNotificationActive())) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setfrontTirePressureCriticalNotificationActive(true);
                                        Faults.setrearTirePressureCriticalNotificationActive(true);
                                    }
                                }
                                break;
                            default:
                                Faults.setfrontTirePressureWarningActive(false);
                                Faults.setrearTirePressureWarningActive(false);
                                Faults.setfrontTirePressureCriticalActive(false);
                                Faults.setrearTirePressureCriticalActive(false);
                                if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                    if (Faults.getfrontTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setfrontTirePressureCriticalNotificationActive(false);
                                    }
                                    if (Faults.getrearTirePressureCriticalNotificationActive()) {
                                        BluetoothLeService.updateNotification();
                                        Faults.setrearTirePressureCriticalNotificationActive(false);
                                    }
                                }
                                break;
                        }
                    }
                }
                break;
            case 0x06:
                //RPM
                if (((data[1] & 0xFF) != 0xFF) || ((data[2] & 0xFF) & 0x0f) != 0xF) {
                    int rpm = (((data[1] & 0xFF) + (((data[2] & 0xFF) & 0x0f) * 255)) * 5);
                    Data.setRPM(rpm);
                }
                //Gear
                if ((((data[2] & 0xFF) >> 4) & 0x0f) != 0xF) {
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
                        default:
                            gear = "-";
                    }
                    if (Data.getGear() != null) {
                        if (!Data.getGear().equals(gear) && !gear.equals("-")) {
                            Data.setNumberOfShifts(Data.getNumberOfShifts() + 1);
                        }
                    }
                    Data.setGear(gear);
                }
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
                if ((((data[5] & 0xFF)  >> 4) & 0x0f) != 0xF) {
                    int ascValue = ((data[5] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (ascValue) {
                        case 0x1:
                        case 0x9:
                            Faults.setAscSelfDiagActive(false);
                            Faults.setAscInterventionActive(true);
                            Faults.setAscDeactivatedActive(false);
                            Faults.setascErrorActive(false);
                            break;
                        case 0x2:
                        case 0x5:
                        case 0x6:
                        case 0x7:
                        case 0xA:
                        case 0xD:
                        case 0xE:
                            Faults.setAscSelfDiagActive(false);
                            Faults.setAscInterventionActive(false);
                            Faults.setAscDeactivatedActive(false);
                            Faults.setascErrorActive(true);
                            break;
                        case 0x3:
                        case 0xB:
                            Faults.setAscSelfDiagActive(true);
                            Faults.setAscInterventionActive(false);
                            Faults.setAscDeactivatedActive(false);
                            Faults.setascErrorActive(false);
                            break;
                        case 0x8:
                            Faults.setAscSelfDiagActive(false);
                            Faults.setAscInterventionActive(false);
                            Faults.setAscDeactivatedActive(true);
                            Faults.setascErrorActive(false);
                            break;
                        default:
                            Faults.setAscSelfDiagActive(false);
                            Faults.setAscInterventionActive(false);
                            Faults.setAscDeactivatedActive(false);
                            Faults.setascErrorActive(false);
                            break;
                    }
                }
                //Oil Fault
                if (((data[5] & 0xFF) & 0x0f) != 0xF) {
                    int oilValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (oilValue) {
                        case 0x2:
                        case 0x6:
                        case 0xA:
                        case 0xE:
                            Faults.setOilLowActive(true);
                            break;
                        default:
                            Faults.setOilLowActive(false);
                            break;
                    }
                }
                break;
            case 0x07:
                //Average Speed
                if ((data[1] & 0xFF) != 0xFF || ((data[2] & 0xFF) & 0x0f) != 0xF) {
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
                if ((((data[5] & 0xFF)  >> 4) & 0x0f) != 0xF) {
                    int fuelValue = ((data[5] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (fuelValue) {
                        case 0x2:
                        case 0x6:
                        case 0xA:
                        case 0xE:
                            Faults.setfuelFaultActive(true);
                            BluetoothLeService.fuelAlert();
                            break;
                        default:
                            Faults.setfuelFaultActive(false);
                            BluetoothLeService.fuelAlertSent = false;
                            break;
                    }
                }
                // General Fault
                if (((data[5] & 0xFF) & 0x0f) != 0xF) {
                    int generalFault = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (generalFault) {
                        case 0x1:
                        case 0xD:
                            Faults.setGeneralFlashingYellowActive(true);
                            Faults.setGeneralShowsYellowActive(false);
                            Faults.setGeneralFlashingRedActive(false);
                            Faults.setGeneralShowsRedActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (Faults.getgeneralFlashingRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if (Faults.getgeneralShowsRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralShowsRedNotificationActive(false);
                                }
                            }
                            break;
                        case 0x2:
                        case 0xE:
                            Faults.setGeneralFlashingYellowActive(false);
                            Faults.setGeneralShowsYellowActive(true);
                            Faults.setGeneralFlashingRedActive(false);
                            Faults.setGeneralShowsRedActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (Faults.getgeneralFlashingRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if (Faults.getgeneralShowsRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralShowsRedNotificationActive(false);
                                }
                            }
                            break;
                        case 0x4:
                        case 0x7:
                            Faults.setGeneralFlashingYellowActive(false);
                            Faults.setGeneralShowsYellowActive(false);
                            Faults.setGeneralFlashingRedActive(true);
                            Faults.setGeneralShowsRedActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(Faults.getgeneralFlashingRedNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if (Faults.getgeneralShowsRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralShowsRedNotificationActive(false);
                                }
                            }
                            break;
                        case 0x5:
                            Faults.setGeneralFlashingYellowActive(true);
                            Faults.setGeneralShowsYellowActive(false);
                            Faults.setGeneralFlashingRedActive(true);
                            Faults.setGeneralShowsRedActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(Faults.getgeneralFlashingRedNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if (Faults.getgeneralShowsRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralShowsRedNotificationActive(false);
                                }
                            }
                            break;
                        case 0x6:
                            Faults.setGeneralFlashingYellowActive(false);
                            Faults.setGeneralShowsYellowActive(true);
                            Faults.setGeneralFlashingRedActive(true);
                            Faults.setGeneralShowsRedActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!(Faults.getgeneralFlashingRedNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(true);
                                }
                                if (Faults.getgeneralShowsRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralShowsRedNotificationActive(false);
                                }
                            }
                            break;
                        case 0x8:
                        case 0xB:
                            Faults.setGeneralFlashingYellowActive(false);
                            Faults.setGeneralShowsYellowActive(false);
                            Faults.setGeneralFlashingRedActive(false);
                            Faults.setGeneralShowsRedActive(true);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (Faults.getgeneralFlashingRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if (!(Faults.getgeneralShowsRedNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralShowsRedNotificationActive(true);
                                }
                            }
                            break;
                        case 0x9:
                            Faults.setGeneralFlashingYellowActive(false);
                            Faults.setGeneralShowsYellowActive(false);
                            Faults.setGeneralFlashingRedActive(true);
                            Faults.setGeneralShowsRedActive(true);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (!Faults.getgeneralShowsRedNotificationActive() && !Faults.getgeneralFlashingRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(true);
                                    Faults.setGeneralShowsRedNotificationActive(true);
                                }
                            }
                            break;
                        case 0xA:
                            Faults.setGeneralFlashingYellowActive(false);
                            Faults.setGeneralShowsYellowActive(true);
                            Faults.setGeneralFlashingRedActive(false);
                            Faults.setGeneralShowsRedActive(true);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (Faults.getgeneralFlashingRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if (!(Faults.getgeneralShowsRedNotificationActive())) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralShowsRedNotificationActive(true);
                                }
                            }
                            break;
                        default:
                            Faults.setGeneralFlashingYellowActive(false);
                            Faults.setGeneralShowsYellowActive(false);
                            Faults.setGeneralFlashingRedActive(false);
                            Faults.setGeneralShowsRedActive(false);
                            if (sharedPrefs.getBoolean("prefNotifications", true)) {
                                if (Faults.getgeneralFlashingRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralFlashingRedNotificationActive(false);
                                }
                                if (Faults.getgeneralShowsRedNotificationActive()) {
                                    BluetoothLeService.updateNotification();
                                    Faults.setGeneralShowsRedNotificationActive(false);
                                }
                            }
                            break;
                    }
                }
                break;
            case 0x08:
                if ((data[1] & 0xFF) != 0xFF) {
                    double ambientTemp = ((data[1] & 0xFF) * 0.50) - 40;
                    Data.setAmbientTemperature(ambientTemp);
                    if(ambientTemp <= 0.0){
                        Faults.seticeWarnActive(true);
                    } else {
                        Faults.seticeWarnActive(false);
                    }
                }

                // LAMP Faults
                if ((((data[3] & 0xFF) >> 4) & 0x0f) != 0xF) {
                    // LAMPF 1
                    int lampfOneValue = ((data[3] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfOneValue) {
                        case 0x1: case 0x5: case 0x9: case 0xD:
                            Faults.setAddFrontLightOneActive(true);
                            Faults.setAddFrontLightTwoActive(false);
                            break;
                        case 0x2: case 0x6: case 0xA: case 0xE:
                            Faults.setAddFrontLightOneActive(false);
                            Faults.setAddFrontLightTwoActive(true);
                            break;
                        case 0x3: case 0xB:
                            Faults.setAddFrontLightOneActive(true);
                            Faults.setAddFrontLightTwoActive(true);
                            break;
                        default:
                            Faults.setAddFrontLightOneActive(false);
                            Faults.setAddFrontLightTwoActive(false);
                            break;
                    }
                }
                // LAMPF 2
                if (((((data[4] & 0xFF) >> 4) & 0x0f) != 0xF) ) {
                    int lampfTwoHighValue = ((data[4] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfTwoHighValue) {
                        case 0x1:
                        case 0x9:
                            Faults.setDaytimeRunningActive(true);
                            Faults.setfrontLeftSignalActive(false);
                            Faults.setfrontRightSignalActive(false);
                            break;
                        case 0x2:
                        case 0xA:
                            Faults.setDaytimeRunningActive(false);
                            Faults.setfrontLeftSignalActive(true);
                            Faults.setfrontRightSignalActive(false);
                            break;
                        case 0x3:
                        case 0xB:
                            Faults.setDaytimeRunningActive(true);
                            Faults.setfrontLeftSignalActive(true);
                            Faults.setfrontRightSignalActive(false);
                            break;
                        case 0x4:
                        case 0xC:
                            Faults.setDaytimeRunningActive(false);
                            Faults.setfrontLeftSignalActive(false);
                            Faults.setfrontRightSignalActive(true);
                            break;
                        case 0x5:
                        case 0xD:
                            Faults.setDaytimeRunningActive(true);
                            Faults.setfrontLeftSignalActive(false);
                            Faults.setfrontRightSignalActive(true);
                            break;
                        case 0x6:
                        case 0xE:
                            Faults.setDaytimeRunningActive(false);
                            Faults.setfrontLeftSignalActive(true);
                            Faults.setfrontRightSignalActive(true);
                            break;
                        case 0x7:
                        case 0xF:
                            Faults.setDaytimeRunningActive(true);
                            Faults.setfrontLeftSignalActive(true);
                            Faults.setfrontRightSignalActive(true);
                            break;
                        default:
                            Faults.setDaytimeRunningActive(false);
                            Faults.setfrontLeftSignalActive(false);
                            Faults.setfrontRightSignalActive(false);
                            break;
                    }
                }
                if (((data[4] & 0xFF) & 0x0f) != 0xF) {
                    int lampfTwoLowValue = (data[4] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfTwoLowValue) {
                        case 0x1:
                            Faults.setFrontParkingLightOneActive(true);
                            Faults.setFrontParkingLightTwoActive(false);
                            Faults.setLowBeamActive(false);
                            Faults.setHighBeamActive(false);
                            break;
                        case 0x2:
                            Faults.setFrontParkingLightOneActive(false);
                            Faults.setFrontParkingLightTwoActive(true);
                            Faults.setLowBeamActive(false);
                            Faults.setHighBeamActive(false);
                            break;
                        case 0x3:
                            Faults.setFrontParkingLightOneActive(true);
                            Faults.setFrontParkingLightTwoActive(true);
                            Faults.setLowBeamActive(false);
                            Faults.setHighBeamActive(false);
                            break;
                        case 0x4:
                            Faults.setFrontParkingLightOneActive(false);
                            Faults.setFrontParkingLightTwoActive(false);
                            Faults.setLowBeamActive(true);
                            Faults.setHighBeamActive(false);
                            break;
                        case 0x5:
                            Faults.setFrontParkingLightOneActive(true);
                            Faults.setFrontParkingLightTwoActive(false);
                            Faults.setLowBeamActive(true);
                            Faults.setHighBeamActive(false);
                            break;
                        case 0x6:
                            Faults.setFrontParkingLightOneActive(false);
                            Faults.setFrontParkingLightTwoActive(true);
                            Faults.setLowBeamActive(true);
                            Faults.setHighBeamActive(false);
                            break;
                        case 0x7:
                            Faults.setFrontParkingLightOneActive(true);
                            Faults.setFrontParkingLightTwoActive(true);
                            Faults.setLowBeamActive(true);
                            Faults.setHighBeamActive(false);
                            break;
                        case 0x8:
                            Faults.setFrontParkingLightOneActive(false);
                            Faults.setFrontParkingLightTwoActive(false);
                            Faults.setLowBeamActive(false);
                            Faults.setHighBeamActive(true);
                            break;
                        case 0x9:
                            Faults.setFrontParkingLightOneActive(true);
                            Faults.setFrontParkingLightTwoActive(false);
                            Faults.setLowBeamActive(false);
                            Faults.setHighBeamActive(true);
                            break;
                        case 0xA:
                            Faults.setFrontParkingLightOneActive(false);
                            Faults.setFrontParkingLightTwoActive(true);
                            Faults.setLowBeamActive(false);
                            Faults.setHighBeamActive(true);
                            break;
                        case 0xB:
                            Faults.setFrontParkingLightOneActive(true);
                            Faults.setFrontParkingLightTwoActive(true);
                            Faults.setLowBeamActive(false);
                            Faults.setHighBeamActive(true);
                            break;
                        case 0xC:
                            Faults.setFrontParkingLightOneActive(false);
                            Faults.setFrontParkingLightTwoActive(false);
                            Faults.setLowBeamActive(true);
                            Faults.setHighBeamActive(true);
                            break;
                        case 0xD:
                            Faults.setFrontParkingLightOneActive(true);
                            Faults.setFrontParkingLightTwoActive(false);
                            Faults.setLowBeamActive(true);
                            Faults.setHighBeamActive(true);
                            break;
                        case 0xE:
                            Faults.setFrontParkingLightOneActive(false);
                            Faults.setFrontParkingLightTwoActive(true);
                            Faults.setLowBeamActive(true);
                            Faults.setHighBeamActive(true);
                            break;
                        case 0xF:
                            Faults.setFrontParkingLightOneActive(true);
                            Faults.setFrontParkingLightTwoActive(true);
                            Faults.setLowBeamActive(true);
                            Faults.setHighBeamActive(true);
                            break;
                        default:
                            Faults.setFrontParkingLightOneActive(false);
                            Faults.setFrontParkingLightTwoActive(false);
                            Faults.setLowBeamActive(false);
                            Faults.setHighBeamActive(false);
                            break;
                    }
                }

                // LAMPF 3
                if ((((data[5] & 0xFF) >> 4) & 0x0f) != 0xF) {
                    int lampfThreeHighValue = ((data[5] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfThreeHighValue) {
                        case 0x1:
                        case 0x3:
                        case 0x5:
                        case 0x7:
                        case 0x9:
                        case 0xB:
                        case 0xD:
                        case 0xF:
                            Faults.setrearRightSignalActive(true);
                            break;
                        default:
                            Faults.setrearRightSignalActive(false);
                            break;
                    }
                }
                if (((data[5] & 0xFF) & 0x0f) != 0xF) {
                    int lampfThreeLowValue = (data[5] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfThreeLowValue) {
                        case 0x1:
                            Faults.setrearLeftSignalActive(false);
                            Faults.setRearLightActive(true);
                            Faults.setBrakeLightActive(false);
                            Faults.setLicenseLightActive(false);
                            break;
                        case 0x2:
                            Faults.setrearLeftSignalActive(false);
                            Faults.setRearLightActive(false);
                            Faults.setBrakeLightActive(true);
                            Faults.setLicenseLightActive(false);
                            break;
                        case 0x3:
                            Faults.setrearLeftSignalActive(false);
                            Faults.setRearLightActive(true);
                            Faults.setBrakeLightActive(true);
                            Faults.setLicenseLightActive(false);
                            break;
                        case 0x4:
                            Faults.setrearLeftSignalActive(false);
                            Faults.setRearLightActive(false);
                            Faults.setBrakeLightActive(false);
                            Faults.setLicenseLightActive(true);
                            break;
                        case 0x5: case 0xC:
                            Faults.setrearLeftSignalActive(true);
                            Faults.setRearLightActive(false);
                            Faults.setBrakeLightActive(false);
                            Faults.setLicenseLightActive(true);
                            break;
                        case 0x6:
                            Faults.setrearLeftSignalActive(false);
                            Faults.setRearLightActive(false);
                            Faults.setBrakeLightActive(true);
                            Faults.setLicenseLightActive(true);
                            break;
                        case 0x7:
                            Faults.setrearLeftSignalActive(false);
                            Faults.setRearLightActive(true);
                            Faults.setBrakeLightActive(true);
                            Faults.setLicenseLightActive(true);
                            break;
                        case 0x8:
                            Faults.setrearLeftSignalActive(true);
                            Faults.setRearLightActive(false);
                            Faults.setBrakeLightActive(false);
                            Faults.setLicenseLightActive(false);
                            break;
                        case 0x9:
                            Faults.setrearLeftSignalActive(true);
                            Faults.setRearLightActive(true);
                            Faults.setBrakeLightActive(false);
                            Faults.setLicenseLightActive(false);
                            break;
                        case 0xA:
                            Faults.setrearLeftSignalActive(true);
                            Faults.setRearLightActive(false);
                            Faults.setBrakeLightActive(true);
                            Faults.setLicenseLightActive(false);
                            break;
                        case 0xD:
                            Faults.setrearLeftSignalActive(true);
                            Faults.setRearLightActive(true);
                            Faults.setBrakeLightActive(true);
                            Faults.setLicenseLightActive(false);
                            break;
                        case 0xE:
                            Faults.setrearLeftSignalActive(true);
                            Faults.setRearLightActive(false);
                            Faults.setBrakeLightActive(true);
                            Faults.setLicenseLightActive(true);
                            break;
                        case 0xF:
                            Faults.setrearLeftSignalActive(true);
                            Faults.setRearLightActive(true);
                            Faults.setBrakeLightActive(true);
                            Faults.setLicenseLightActive(true);
                            break;
                        default:
                            Faults.setrearLeftSignalActive(false);
                            Faults.setRearLightActive(false);
                            Faults.setBrakeLightActive(false);
                            Faults.setLicenseLightActive(false);
                            break;
                    }
                }

                // LAMPF 4
                if ((((data[6] & 0xFF) >> 4) & 0x0f) != 0xF) {
                    int lampfFourHighValue = ((data[6] & 0xFF) >> 4) & 0x0f; // the highest 4 bits.
                    switch (lampfFourHighValue) {
                        case 0x1:
                        case 0x3:
                        case 0x5:
                        case 0x9:
                        case 0xB:
                        case 0xD:
                        case 0xF:
                            Faults.setRearFogLightActive(true);
                            break;
                        default:
                            Faults.setRearFogLightActive(false);
                            break;
                    }
                }
                if (((data[6] & 0xFF) & 0x0f) != 0xF) {
                    int lampfFourLowValue = (data[6] & 0xFF) & 0x0f; // the lowest 4 bits
                    switch (lampfFourLowValue) {
                        case 0x1:
                            Faults.setAddDippedLightActive(true);
                            Faults.setAddBrakeLightActive(false);
                            Faults.setFrontLampOneLightActive(false);
                            Faults.setFrontLampTwoLightActive(false);
                            break;
                        case 0x2:
                            Faults.setAddDippedLightActive(false);
                            Faults.setAddBrakeLightActive(true);
                            Faults.setFrontLampOneLightActive(false);
                            Faults.setFrontLampTwoLightActive(false);
                            break;
                        case 0x3:
                            Faults.setAddDippedLightActive(true);
                            Faults.setAddBrakeLightActive(true);
                            Faults.setFrontLampOneLightActive(false);
                            Faults.setFrontLampTwoLightActive(false);
                            break;
                        case 0x4:
                            Faults.setAddDippedLightActive(false);
                            Faults.setAddBrakeLightActive(false);
                            Faults.setFrontLampOneLightActive(true);
                            Faults.setFrontLampTwoLightActive(false);
                            break;
                        case 0x5:
                            Faults.setAddDippedLightActive(true);
                            Faults.setAddBrakeLightActive(false);
                            Faults.setFrontLampOneLightActive(true);
                            Faults.setFrontLampTwoLightActive(false);
                            break;
                        case 0x6:
                            Faults.setAddDippedLightActive(false);
                            Faults.setAddBrakeLightActive(true);
                            Faults.setFrontLampOneLightActive(true);
                            Faults.setFrontLampTwoLightActive(false);
                            break;
                        case 0x7:
                            Faults.setAddDippedLightActive(true);
                            Faults.setAddBrakeLightActive(true);
                            Faults.setFrontLampOneLightActive(true);
                            Faults.setFrontLampTwoLightActive(false);
                            break;
                        case 0x8:
                            Faults.setAddDippedLightActive(false);
                            Faults.setAddBrakeLightActive(false);
                            Faults.setFrontLampOneLightActive(false);
                            Faults.setFrontLampTwoLightActive(true);
                            break;
                        case 0x9:
                            Faults.setAddDippedLightActive(true);
                            Faults.setAddBrakeLightActive(false);
                            Faults.setFrontLampOneLightActive(false);
                            Faults.setFrontLampTwoLightActive(true);
                            break;
                        case 0xA:
                            Faults.setAddDippedLightActive(false);
                            Faults.setAddBrakeLightActive(true);
                            Faults.setFrontLampOneLightActive(false);
                            Faults.setFrontLampTwoLightActive(true);
                            break;
                        case 0xB:
                            Faults.setAddDippedLightActive(true);
                            Faults.setAddBrakeLightActive(true);
                            Faults.setFrontLampOneLightActive(false);
                            Faults.setFrontLampTwoLightActive(true);
                            break;
                        case 0xC:
                            Faults.setAddDippedLightActive(false);
                            Faults.setAddBrakeLightActive(false);
                            Faults.setFrontLampOneLightActive(true);
                            Faults.setFrontLampTwoLightActive(true);
                            break;
                        case 0xD:
                            Faults.setAddDippedLightActive(true);
                            Faults.setAddBrakeLightActive(false);
                            Faults.setFrontLampOneLightActive(true);
                            Faults.setFrontLampTwoLightActive(true);
                            break;
                        case 0xE:
                            Faults.setAddDippedLightActive(false);
                            Faults.setAddBrakeLightActive(true);
                            Faults.setFrontLampOneLightActive(true);
                            Faults.setFrontLampTwoLightActive(true);
                            break;
                        default:
                            Faults.setAddDippedLightActive(false);
                            Faults.setAddBrakeLightActive(false);
                            Faults.setFrontLampOneLightActive(false);
                            Faults.setFrontLampTwoLightActive(false);
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
                if ((data[3] & 0xFF) != 0xFF || (data[2] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xFF) {
                    double odometer = Utils.bytesToInt16(data[3], data[2], data[1]);
                    Data.setOdometer(odometer);
                }

                if ((data[6] & 0xFF) != 0xFF || (data[5] & 0xFF) != 0xFF || (data[4] & 0xFF) != 0xFF) {
                    double tripAuto = Utils.bytesToInt16(data[6], data[5], data[4]) / 10.0;
                    Data.setTripAuto(tripAuto);
                }
                break;
            case 0x0b:
                if ((data[3] & 0xFF) != 0xFF || (data[2] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xFF) {
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
                        Faults.setServiceActive(true);
                    } else {
                        Faults.setServiceActive(false);
                    }
                }
                if ((data[4] & 0xFF) != 0xFF){
                    int nextService = data[4] * 100;
                    Data.setNextService(nextService);
                }
                break;
            case 0x0c:
                if ((data[3] & 0xFF) != 0xFF || (data[2] & 0xFF) != 0xFF || (data[1] & 0xFF) != 0xFF) {
                    double trip1 = Utils.bytesToInt16(data[3], data[2], data[1]) / 10.0;
                    Data.setTripOne(trip1);
                }
                if ((data[6] & 0xFF) != 0xFF || (data[5] & 0xFF) != 0xFF || (data[4] & 0xFF) != 0xFF) {
                    double trip2 = Utils.bytesToInt16(data[6], data[5], data[4]) / 10.0;
                    Data.setTripTwo(trip2);
                }
                break;
            default:
                break;
        }
    }
}
