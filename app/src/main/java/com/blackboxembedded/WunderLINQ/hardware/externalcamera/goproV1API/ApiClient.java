package com.blackboxembedded.WunderLINQ.hardware.externalcamera.goproV1API;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiClient {

    @GET("/camera/sx")
    Call<ResponseBody> status();

    @GET("/camera/sx")
    Call<ResponseBody> statusPwd(@Query("t") String pwd);

    @GET("/bacpac/sd")
    Call<ResponseBody> getPwd();

    @GET("/{param}/{option}")
    Call<ResponseBody> command(@Path("param") String param, @Path("option") String option, @Query(value = "p", encoded = true) String argument);

    @GET("/{param}/{option}")
    Call<ResponseBody> commandPwd(@Path("param") String param, @Path("option") String option, @Query("t") String pwd, @Query(value = "p", encoded = true) String argument);
}
