package com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.model;


import com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.ApiClient;
import com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.ApiBase;

import retrofit2.Call;

/**
 * Created by konrad on 1/2/17.
 */

public class GPConstants {
    static ApiClient GoProV2Api = ApiBase.getMainClient().create(ApiClient.class);
    public static class Status{
        public static Call<GoProResponse> Status = GoProV2Api.execute("gpStream", "proto_v2", "restart");
    }
    public static class Commands {
    public static class Stream{
        public static Call<GoProResponse> Restart = GoProV2Api.execute("gpStream", "proto_v2", "restart");
        public static Call<GoProResponse> Stop = GoProV2Api.execute("gpStream", "proto_v2", "stop");
    }
    public static class Shutter {
        public static Call<GoProResponse> shutter = GoProV2Api.command("shutter","1");
        public static Call<GoProResponse> stop = GoProV2Api.command("shutter","0");
        public static Call<GoProResponse> hilight = GoProV2Api.command("storage/tag_moment","");
    }

    public static class Modes {
        public static Call<GoProResponse> videoMode = GoProV2Api.command("mode","0");
        public static Call<GoProResponse> photoMode = GoProV2Api.command("mode","1");
        public static Call<GoProResponse> multishotMode = GoProV2Api.command("mode","2");
        public static class videoSubModes {
            public static Call<GoProResponse> Video = GoProV2Api.command("sub_mode","mode=0&sub_mode=0");
            public static Call<GoProResponse> TimeLapseVideo = GoProV2Api.command("sub_mode","mode=0&sub_mode=1");
            public static Call<GoProResponse> VideoPlusPhoto = GoProV2Api.command("sub_mode","mode=0&sub_mode=2");
            public static Call<GoProResponse> Looping = GoProV2Api.command("sub_mode","mode=0&sub_mode=3");
        }

        public static class photoSubModes {
            public static Call<GoProResponse> Single = GoProV2Api.command("sub_mode","mode=1&sub_mode=0");
            public static Call<GoProResponse> Continuous = GoProV2Api.command("sub_mode","mode=1&sub_mode=1");
            public static Call<GoProResponse> Night = GoProV2Api.command("sub_mode","mode=1&sub_mode=2");
        }

        public static class multiShotSubModes {
            public static Call<GoProResponse> Burst = GoProV2Api.command("sub_mode","mode=2&sub_mode=0");
            public static Call<GoProResponse> Timelapse = GoProV2Api.command("sub_mode","mode=2&sub_mode=1");
            public static Call<GoProResponse> NightLapse = GoProV2Api.command("sub_mode","mode=2&sub_mode=2");
        }
    }


}
    public static class Setup {
        public static class Orientation {
            public static Call<GoProResponse> Up = GoProV2Api.config("52", "1");
            public static Call<GoProResponse> Down = GoProV2Api.config("52", "2");
            public static Call<GoProResponse> Auto = GoProV2Api.config("52", "0");
        }

        public static class Locate {

            public static Call<GoProResponse> On = GoProV2Api.command("system/locate","1");
            public static Call<GoProResponse> Off = GoProV2Api.command("system/locate","0");

        }

        public static class Delete {
            public static Call<GoProResponse> DeleteFile = GoProV2Api.command("storage/delete","file");
            public static Call<GoProResponse> DeleteLast = GoProV2Api.command("storage/delete/last","");
            public static Call<GoProResponse> Format = GoProV2Api.command("storage/delete/all","");
        }

        public static class QuickCapture {
            public static Call<GoProResponse> On = GoProV2Api.config("54", "1");
            public static Call<GoProResponse> Off = GoProV2Api.config("54", "0");

        }

        public static class LEDBlink {
            public static Call<GoProResponse> Two = GoProV2Api.config("55", "1");
            public static Call<GoProResponse> Four = GoProV2Api.config("55", "2");
            public static Call<GoProResponse> Off = GoProV2Api.config("55", "0");

        }

        public static class Beeps {
            public static Call<GoProResponse> Off = GoProV2Api.config("56", "2");
            public static Call<GoProResponse> SeventyPercent = GoProV2Api.config("56", "1");
            public static Call<GoProResponse> Full = GoProV2Api.config("56", "0");

        }

        public static class VideoFormat {
            public static Call<GoProResponse> NTSC = GoProV2Api.config("57", "0");
            public static Call<GoProResponse> PAL = GoProV2Api.config("57", "1");

        }

        public static class LCDDisplay {
            public static Call<GoProResponse> On = GoProV2Api.config("72", "1");
            public static Call<GoProResponse> Off = GoProV2Api.config("72", "0");

        }

        public static class OnScreenDisplay {
            public static Call<GoProResponse> On = GoProV2Api.config("58", "1");
            public static Call<GoProResponse> Off = GoProV2Api.config("58", "0");

        }

        public static class LCDBrightness {
            public static Call<GoProResponse> High = GoProV2Api.config("49", "0");
            public static Call<GoProResponse> Medium = GoProV2Api.config("49", "1");
            public static Call<GoProResponse> Low = GoProV2Api.config("49", "2");

        }

        public static class LCDLock {
            public static Call<GoProResponse> On = GoProV2Api.config("50", "1");
            public static Call<GoProResponse> Off = GoProV2Api.config("50", "0");

        }

        public static class LCDTimeout {

            public static Call<GoProResponse> Never = GoProV2Api.config("51", "0");
            public static Call<GoProResponse> LCD1min = GoProV2Api.config("51", "1");
            public static Call<GoProResponse> LCD2min = GoProV2Api.config("51", "2");
            public static Call<GoProResponse> LCD3min = GoProV2Api.config("51", "3");

        }

        public static class AutoOff {
            public static Call<GoProResponse> Never = GoProV2Api.config("59", "0");
            public static Call<GoProResponse> AO1m = GoProV2Api.config("59", "1");
            public static Call<GoProResponse> AO2m = GoProV2Api.config("59", "2");
            public static Call<GoProResponse> AO3m = GoProV2Api.config("59", "3");
            public static Call<GoProResponse> AO5m = GoProV2Api.config("59", "4");

        }
    }
    public static class Video{
        public static class Resolutions {
            public static Call<GoProResponse> Res4K= GoProV2Api.config("2","1");
            public static Call<GoProResponse> Res4K_SuperView= GoProV2Api.config("2","2");
            public static Call<GoProResponse> Res2point7K= GoProV2Api.config("2","4");
            public static Call<GoProResponse> Res2point7KSuperView= GoProV2Api.config("2","5");
            public static Call<GoProResponse> Res2point7K_4by3AR= GoProV2Api.config("2","6");
            public static Call<GoProResponse> Res1440p= GoProV2Api.config("2","7");
            public static Call<GoProResponse> Res1080pSuperView= GoProV2Api.config("2","8");
            public static Call<GoProResponse> Res1080p= GoProV2Api.config("2","9");
            public static Call<GoProResponse> Res960p= GoProV2Api.config("2","10");
            public static Call<GoProResponse> Res720pSuperView= GoProV2Api.config("2","11");
            public static Call<GoProResponse> Res720p= GoProV2Api.config("2","12");
            public static Call<GoProResponse> ResWVGA= GoProV2Api.config("2","13");
        }
        public static class FrameRate {
            public static Call<GoProResponse> FR240fps= GoProV2Api.config("3","0");
            public static Call<GoProResponse> FR120fps= GoProV2Api.config("3","1");
            public static Call<GoProResponse> FR100fps= GoProV2Api.config("3","2");
            public static Call<GoProResponse> FR60fps= GoProV2Api.config("3","5");
            public static Call<GoProResponse> FR50fps= GoProV2Api.config("3","6");
            public static Call<GoProResponse> FR48fps= GoProV2Api.config("3","7");
            public static Call<GoProResponse> FR30fps= GoProV2Api.config("3","8");
            public static Call<GoProResponse> FR25fps= GoProV2Api.config("3","9");
            public static Call<GoProResponse> FR24fps= GoProV2Api.config("3","10");
            public static Call<GoProResponse> FR15fps= GoProV2Api.config("3","11");
            public static Call<GoProResponse> FR12point5fps= GoProV2Api.config("3","12");
        }
        public static class FOV {

            public static Call<GoProResponse> Wide= GoProV2Api.config("4","0");
            public static Call<GoProResponse> Medium= GoProV2Api.config("4","1");
            public static Call<GoProResponse> Narrow= GoProV2Api.config("4","2");
            public static Call<GoProResponse> Linear= GoProV2Api.config("4","4");
        }
        public static class LowLight{
            public static Call<GoProResponse> On= GoProV2Api.config("8","1");
            public static Call<GoProResponse> Off= GoProV2Api.config("8","0");
        }
        public static class LoopingDuration{

            public static Call<GoProResponse> Max= GoProV2Api.config("6","0");
            public static Call<GoProResponse> Loop5Min= GoProV2Api.config("6","1");
            public static Call<GoProResponse> Loop20Min= GoProV2Api.config("6","2");
            public static Call<GoProResponse> Loop60Min= GoProV2Api.config("6","3");
            public static Call<GoProResponse> Loop120Min= GoProV2Api.config("6","4");
        }
        public static class VideoPlusPhotoInterval{

            public static Call<GoProResponse> Interval5Min= GoProV2Api.config("7","1");
            public static Call<GoProResponse> Interval10Min= GoProV2Api.config("7","2");
            public static Call<GoProResponse> Interval30Min= GoProV2Api.config("7","3");
            public static Call<GoProResponse> Interval60Min= GoProV2Api.config("7","4");
        }
        public static class SpotMeter {

            public static Call<GoProResponse> Off = GoProV2Api.config("9", "0");
            public static Call<GoProResponse> On = GoProV2Api.config("9", "1");
        }
        public static class ProTune{
            public static Call<GoProResponse> On = GoProV2Api.config("10","1");
            public static Call<GoProResponse> Off = GoProV2Api.config("10","0");
        }
        public static class WhiteBalance{

            public static Call<GoProResponse> WBAuto = GoProV2Api.config("11","0");
            public static Call<GoProResponse> WB3000k = GoProV2Api.config("11","1");
            public static Call<GoProResponse> WB4000k = GoProV2Api.config("11","5");
            public static Call<GoProResponse> WB4800k = GoProV2Api.config("11","6");
            public static Call<GoProResponse> WB5500k = GoProV2Api.config("11","2");
            public static Call<GoProResponse> WB6000k = GoProV2Api.config("11","7");
            public static Call<GoProResponse> WB6500k = GoProV2Api.config("11","3");
            public static Call<GoProResponse> WBNative = GoProV2Api.config("11","4");
        }
        public static class Color{

            public static Call<GoProResponse> GOPRO = GoProV2Api.config("12","0");
            public static Call<GoProResponse> Flat = GoProV2Api.config("12","1");
        }
        public static class ISOLimit{
            public static Call<GoProResponse> ISO6400 = GoProV2Api.config("13","0");
            public static Call<GoProResponse> ISO1600 = GoProV2Api.config("13","1");
            public static Call<GoProResponse> ISO400 = GoProV2Api.config("13","2");
            public static Call<GoProResponse> ISO3200 = GoProV2Api.config("13","3");
            public static Call<GoProResponse> ISO800 = GoProV2Api.config("13","4");
            public static Call<GoProResponse> ISO200 = GoProV2Api.config("13","7");
            public static Call<GoProResponse> ISO100 = GoProV2Api.config("13","8");
        }
        public static class ISOMode{
            public static Call<GoProResponse> Max = GoProV2Api.config("74","0");
            public static Call<GoProResponse> Lock = GoProV2Api.config("74","1");
        }
        public static class Sharpness{
            public static Call<GoProResponse> High = GoProV2Api.config("14","0");
            public static Call<GoProResponse> Med = GoProV2Api.config("14","1");
            public static Call<GoProResponse> Low = GoProV2Api.config("14","2");
        }
        public static class ManualVideoExposure{
            public static Call<GoProResponse> MVE_AutoMode = GoProV2Api.config("73","0");
            public static class FR24FPS{
                public static Call<GoProResponse> MVE_1_24 = GoProV2Api.config("73","3");
                public static Call<GoProResponse> MVE_1_48 = GoProV2Api.config("73","6");
                public static Call<GoProResponse> MVE_1_96 = GoProV2Api.config("73","11");
            }
            public static class FR30FPS{
                public static Call<GoProResponse> MVE_1_30 = GoProV2Api.config("73","5");
                public static Call<GoProResponse> MVE_1_60 = GoProV2Api.config("73","8");
                public static Call<GoProResponse> MVE_1_120 = GoProV2Api.config("73","13");
            }
            public static class FR48FPS{
                public static Call<GoProResponse> MVE_1_48 = GoProV2Api.config("73","6");
                public static Call<GoProResponse> MVE_1_96 = GoProV2Api.config("73","11");
                public static Call<GoProResponse> MVE_1_192 = GoProV2Api.config("73","16");
            }
            public static class FR60FPS{
                public static Call<GoProResponse> MVE_1_60 = GoProV2Api.config("73","8");
                public static Call<GoProResponse> MVE_1_120 = GoProV2Api.config("73","13");
                public static Call<GoProResponse> MVE_1_240 = GoProV2Api.config("73","18");
            }
            public static class FR90FPS{
                public static Call<GoProResponse> MVE_1_90 = GoProV2Api.config("73","10");
                public static Call<GoProResponse> MVE_1_180 = GoProV2Api.config("73","15");
                public static Call<GoProResponse> MVE_1_360 = GoProV2Api.config("73","20");
            }
            public static class FR120FPS{
                public static Call<GoProResponse> MVE_1_120 = GoProV2Api.config("73","13");
                public static Call<GoProResponse> MVE_1_240 = GoProV2Api.config("73","18");
                public static Call<GoProResponse> MVE_1_480 = GoProV2Api.config("73","22");
            }
            public static class FR240FPS{
                public static Call<GoProResponse> MVE_1_120 = GoProV2Api.config("73","18");
                public static Call<GoProResponse> MVE_1_240 = GoProV2Api.config("73","22");
                public static Call<GoProResponse> MVE_1_480 = GoProV2Api.config("73","23");

            }
        }
        public static class EVComp{
            static String param="15";
            public static Call<GoProResponse> EVMinus2_0 = GoProV2Api.config(param,"8");
            public static Call<GoProResponse> EVMinus1_5 = GoProV2Api.config(param,"7");
            public static Call<GoProResponse> EVMinus1_0 = GoProV2Api.config(param,"6");
            public static Call<GoProResponse> EVMinus0_5 = GoProV2Api.config(param,"5");
            public static Call<GoProResponse> EV0_0 = GoProV2Api.config(param,"4");
            public static Call<GoProResponse> EV0_5 = GoProV2Api.config(param,"3");
            public static Call<GoProResponse> EV1_0 = GoProV2Api.config(param,"2");
            public static Call<GoProResponse> EV1_5 = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> EV2_0 = GoProV2Api.config(param,"0");

        }
    }

    public static class Photo{

        public static class ContinuousRate{
            static String param="18";
            public static Call<GoProResponse> C3PPS = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> C5PPS = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> C10PPS = GoProV2Api.config(param,"2");

        }

        public static class Resolution{
            static String param="17";
            public static Call<GoProResponse> R12MPW = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> R12MPL= GoProV2Api.config(param,"10");
            public static Call<GoProResponse> R12MPM= GoProV2Api.config(param,"8");
            public static Call<GoProResponse> R12MPN= GoProV2Api.config(param,"9");
            public static Call<GoProResponse> R10MPW= GoProV2Api.config(param,"4");
            public static Call<GoProResponse> R10MPL= GoProV2Api.config(param,"11");
            public static Call<GoProResponse> R8MPW = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> R7MPW = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> R7MPM = GoProV2Api.config(param,"2");
            public static Call<GoProResponse> R5MPM = GoProV2Api.config(param,"3");

        }
        public static class Shutter{
            static String param="19";
            public static Call<GoProResponse> ShutterAuto = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> Shutter2S = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> Shutter5S = GoProV2Api.config(param,"2");
            public static Call<GoProResponse> Shutter10S = GoProV2Api.config(param,"3");
            public static Call<GoProResponse> Shutter15S = GoProV2Api.config(param,"4");
            public static Call<GoProResponse> Shutter20S = GoProV2Api.config(param,"5");
            public static Call<GoProResponse> Shutter30S = GoProV2Api.config(param,"6");

        }
        public static class SpotMeter{
            static String param="20";
            public static Call<GoProResponse> ON = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> OFF = GoProV2Api.config(param,"0");

        }
        public static class WDR{
            static String param="77";
            public static Call<GoProResponse> ON = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> OFF = GoProV2Api.config(param,"0");

        }
        public static class RAW{
            static String param="82";
            public static Call<GoProResponse> ON = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> OFF = GoProV2Api.config(param,"0");

        }
        public static class Protune{
            static String param="21";
            public static Call<GoProResponse> ON = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> OFF = GoProV2Api.config(param,"0");

        }
        public static class WhiteBalance{
            static String param="22";
            public static Call<GoProResponse> WBAuto = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> WB3000K = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> WB4000K = GoProV2Api.config(param,"5");
            public static Call<GoProResponse> WB4800K = GoProV2Api.config(param,"6");
            public static Call<GoProResponse> WB5500K = GoProV2Api.config(param,"2");
            public static Call<GoProResponse> WB6000K = GoProV2Api.config(param,"7");
            public static Call<GoProResponse> WB6500K = GoProV2Api.config(param,"3");
            public static Call<GoProResponse> WBNative = GoProV2Api.config(param,"4");

        }
        public static class Color{
            static String param="23";
            public static Call<GoProResponse> GoProColor = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> Flat = GoProV2Api.config(param,"1");

        }
        public static class Sharpness{
            static String param="25";
            public static Call<GoProResponse> High = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> Medium = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> Low = GoProV2Api.config(param,"2");

        }
        public static class EVComp{
            static String param="26";
            public static Call<GoProResponse> EVMinus2_0 = GoProV2Api.config(param,"8");
            public static Call<GoProResponse> EVMinus1_5 = GoProV2Api.config(param,"7");
            public static Call<GoProResponse> EVMinus1_0 = GoProV2Api.config(param,"6");
            public static Call<GoProResponse> EVMinus0_5 = GoProV2Api.config(param,"5");
            public static Call<GoProResponse> EV0_0 = GoProV2Api.config(param,"4");
            public static Call<GoProResponse> EV0_5 = GoProV2Api.config(param,"3");
            public static Call<GoProResponse> EV1_0 = GoProV2Api.config(param,"2");
            public static Call<GoProResponse> EV1_5 = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> EV2_0 = GoProV2Api.config(param,"0");

        }
        public static class ISOMin{
            static String param="75";
            public static Call<GoProResponse> ISO800 = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> ISO400 = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> ISO200 = GoProV2Api.config(param,"2");
            public static Call<GoProResponse> ISO100 = GoProV2Api.config(param,"3");

        }
        public static class ISOMax{
            static String param="24";
            public static Call<GoProResponse> ISO800 = GoProV2Api.config(param,"0");
            public static Call<GoProResponse> ISO400 = GoProV2Api.config(param,"1");
            public static Call<GoProResponse> ISO200 = GoProV2Api.config(param,"2");
            public static Call<GoProResponse> ISO100 = GoProV2Api.config(param,"3");
        }
    }
}
