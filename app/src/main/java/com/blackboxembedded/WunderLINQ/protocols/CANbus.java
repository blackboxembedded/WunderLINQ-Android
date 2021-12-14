package com.blackboxembedded.WunderLINQ.protocols;

import android.util.Log;

import com.blackboxembedded.WunderLINQ.Data;
import com.blackboxembedded.WunderLINQ.FaultStatus;
import com.blackboxembedded.WunderLINQ.Utils;

public class CANbus {
    public static void parseCANMessage(byte[] data){
        Data.setLastMessage(data);
        int msgID = ((data[0] & 0xFF)<<3) + ((data[1] & 0xFF)>>5);
        Log.d("CANbus","CANID: " + msgID + "  CANMSG: " + Utils.ByteArraytoHexNoDelim(data));

        switch (msgID){
            case 268:
                int rpm = (((data[4] & 0xFF) + (((data[5] & 0xFF) & 0x0f) * 255)) * 5);
                Data.setRPM(rpm);
                break;
            case 272:
                int minPosition = 36;
                int maxPosition = 236;
                double throttlePosition = (((data[7] & 0xFF) - minPosition) * 100.0) / (maxPosition - minPosition);
                Data.setThrottlePosition(throttlePosition);
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
                break;
            case 1023:
                // Ambient Light - Not Confirmed
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
