package com.sunasterisk.smarthomejava;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class  MainService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        innitFore();
        registerReceiver(new MqttConnectBroadcast(), new IntentFilter(MqttConnectBroadcast.BROAD_CAST_NAME));
        MqttConnectBroadcast.start(this);
        Log.d("mqtt", "on service");
    }
    public void innitFore(){
        NotificationSmartHouse.createChannel(this,"MainService","main_service", NotificationManager.IMPORTANCE_HIGH);
//        NotificationSmartHouse.callNotify(this, 1 ,"MainService","smartHouse","Xin chào");
        startForeground(1,NotificationSmartHouse.callNotifyService(this,"MainService","Nhà thông minh","IOT"));
//        NotificationSmartHouse.callNotify(this,2,"MainService","có dữ liệu","tets");
    }
   
}
