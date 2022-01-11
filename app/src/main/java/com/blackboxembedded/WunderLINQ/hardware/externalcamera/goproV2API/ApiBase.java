package com.blackboxembedded.WunderLINQ.hardware.externalcamera.goproV2API;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiBase {

    private static Retrofit GoProV2MainAPI = null;

    public static Retrofit getMainClient() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        GoProV2MainAPI = new Retrofit.Builder()
                .baseUrl("http://10.5.5.9")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        return GoProV2MainAPI;
    }
}
