package com.blackboxembedded.WunderLINQ.externalcamera.goproV1API.model;


import com.blackboxembedded.WunderLINQ.externalcamera.goproV1API.ApiBase;
import com.blackboxembedded.WunderLINQ.externalcamera.goproV1API.ApiClient;

import okhttp3.ResponseBody;
import retrofit2.Call;

public class GPConstants {
    static ApiClient GoProV1Api = ApiBase.getMainClient().create(ApiClient.class);
    public static class Commands {
        public static class Power {
            public static Call<ResponseBody> on = GoProV1Api.command("bacpac", "PW", "%01");
            public static Call<ResponseBody> off = GoProV1Api.command("bacpac", "PW", "%00");
        }
        public static class Shutter {
            public static Call<ResponseBody> shutter = GoProV1Api.command("bacpac", "SH", "%01");
            public static Call<ResponseBody> stop = GoProV1Api.command("bacpac", "SH", "%00");
        }
    }
}
