package com.blackboxembedded.WunderLINQ.externalcamera.goproV2API;

import com.blackboxembedded.WunderLINQ.externalcamera.goproV2API.model.GoProResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiClient {

    @GET("gp/gpControl/info")
    Call<ResponseBody> info();

    @GET("gp/gpControl/status")
    Call<ResponseBody> status();

    @GET("/gp/gpControl/setting/{param}/{option}")
    Call<GoProResponse> config(@Path("param") String param, @Path("option") String option);

    @GET("/gp/gpControl/command/{param}")
    Call<GoProResponse> command(@Path("param") String param, @Query("p") String option);

    @GET("gp/gpControl/execute")
    Call<GoProResponse> execute(@Query(value="p1", encoded = true) String p1,
                                @Query(value="a1", encoded = true) String a1,
                                @Query(value="c1", encoded = true) String c1);

}
