package com.blackboxembedded.WunderLINQ.externalcamera.goproV1API;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public class ApiBase {

    private static Retrofit GoProV1MainAPI = null;

    public static Retrofit getMainClient() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        //interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        GoProV1MainAPI = new Retrofit.Builder()
                .baseUrl("http://10.5.5.9")
                .client(client)
                .build();
        return GoProV1MainAPI;
    }
}
