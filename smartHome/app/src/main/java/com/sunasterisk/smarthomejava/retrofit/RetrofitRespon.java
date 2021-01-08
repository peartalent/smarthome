package com.sunasterisk.smarthomejava.retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitRespon {
    private Retrofit mRetrofit;
    private static final RetrofitRespon outInstance = new RetrofitRespon();
    private RetrofitRespon(){
//        http://10.0.2.2:8080/
        String url ="http://192.168.43.38:3000/";
        String url2 ="http://192.168.1.160:3000/";
        String url3 = "http://192.168.43.141:3000/";
        mRetrofit=new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(url)
                .build();
    }
    public static RetrofitRespon getInstance(){
        return outInstance;
    }
    public Retrofit getRetrofit(){
        return mRetrofit;
    }
}
