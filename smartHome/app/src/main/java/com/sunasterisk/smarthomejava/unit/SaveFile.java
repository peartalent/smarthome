package com.sunasterisk.smarthomejava.unit;

import android.content.Context;
import android.content.SharedPreferences;

public class SaveFile{
    public static void saveSharedInt(String key, int value,Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(key,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        // Save.
        editor.apply();
    }
    public static int loadInt(String key, Context context) {
        // 1 là dark, 0 while
        SharedPreferences sharedPreferences = context.getSharedPreferences(key,Context.MODE_PRIVATE);
        if (sharedPreferences != null) {
            return sharedPreferences.getInt(key, 0);
        }
        return 0;
    }
}
