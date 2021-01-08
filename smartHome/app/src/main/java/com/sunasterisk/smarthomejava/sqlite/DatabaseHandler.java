package com.sunasterisk.smarthomejava.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "IOT";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME_DOOR = "Door";
    private static final String KEY_DOOR_ID = "id";
    private static final String KEY_DOOR_NAME = "name";
    private static final String KEY_DOOR_TIME = "time_stamp";
    private static final String KEY_DOOR_PERMISSION = "permission";

    private static final String TABLE_NAME_AIR = "Door";
    private static final String KEY_AIR_ID = "id";
    private static final String KEY_AIR_NAME = "name";
    private static final String KEY_AIR_TIME = "time_stamp";
    private static final String KEY_AIR_VALUE = "value";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlDoor = String.format("CREATE TABLE %s(%s INTEGER PRIMARY KEY, %s TEXT, %s TEXT, %s TEXT)", TABLE_NAME_DOOR, KEY_DOOR_ID, KEY_DOOR_NAME, KEY_DOOR_TIME, KEY_DOOR_PERMISSION);
        db.execSQL(sqlDoor);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
