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
}
