package com.sunasterisk.smarthomejava.unit;

import android.text.format.DateFormat;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FormatTimestamp {
    public static String formatTimestamp(int timestamp){
        Timestamp stamp = new Timestamp(timestamp);
        Date date = new Date(stamp.getTime());
        return new SimpleDateFormat("dd-M-yyyy hh:mm:ss").format(date);
    }
    public static String formatTimestamp(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        String date = DateFormat.format("dd-M-yyyy hh:mm", cal).toString();
        return date;
    }
}
