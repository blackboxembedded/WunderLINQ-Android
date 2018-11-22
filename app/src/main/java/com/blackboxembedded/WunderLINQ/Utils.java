package com.blackboxembedded.WunderLINQ;

public class Utils {

    public static String ByteArraytoHex(byte[] bytes) {
        if(bytes!=null){
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString();
        }
        return "";
    }

    public static int bytesToInt(byte a, byte b, byte c) {
        return (a & 0xFF) << 16 | (b & 0xFF) << 8 | (c & 0xFF);
    }

    // Unit Conversion Functions
    // bar to psi
    public static double barToPsi(double bar){
        return bar * 14.5037738;
    }
    // bar to kpa
    public static double barTokPa(double bar){
        return bar * 100;
    }
    // bar to kg-f
    public static double barTokgf(double bar){
        return bar * 1.0197162129779;
    }
    // kilometers to miles
    public static double kmToMiles(double kilometers){
        return kilometers * 0.6214;
    }
    // Celsius to Fahrenheit
    public static double celsiusToFahrenheit(double celsius){
        return (celsius * 1.8) + 32.0;
    }
}
