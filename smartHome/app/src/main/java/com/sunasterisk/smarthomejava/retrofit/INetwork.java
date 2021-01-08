package com.sunasterisk.smarthomejava.retrofit;

import com.sunasterisk.smarthomejava.model.* ;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface INetwork {
    @GET("getListBulb")
    Call<List<Led>> getLeds();
    @GET("turnOnOffBulb")
    Call<Void> turnLed(@Query("id") int id,@Query("value") int value);
    @GET("getAirQuality")
    Call<List<Air>> getAirs();
    @GET("getEntranceHistory?id=96")
    Call<List<Door>> getHistory();
    @GET("addRFID")
    Call<String> addCard(@Query("name") String name);
    @GET("getCardList")
    Call<List<Card>> getCards();
    @GET("deleteCard")
    Call<String> deleteCard(@Query("id") int id);
    @GET("login")
    Call<Boolean> login(@Query("username") String username,@Query("password") String password);
}
