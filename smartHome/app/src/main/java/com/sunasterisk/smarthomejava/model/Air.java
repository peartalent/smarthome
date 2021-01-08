package com.sunasterisk.smarthomejava.model;

import com.google.gson.annotations.SerializedName;

public class Air {
    @SerializedName("time_stamp")
    public String timeStamp;

    @SerializedName("value")
    public String value;
//    public String getTimeDate(){
//        return
//    }
    public Air(String timeStamp, String value) {
        this.timeStamp = timeStamp;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Air{" +
                "timeStamp='" + timeStamp + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public static String URL = "localhost:3000/getStateBulb?id=1";

}
