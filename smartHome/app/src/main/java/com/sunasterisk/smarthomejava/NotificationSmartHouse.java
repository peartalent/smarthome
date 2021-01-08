package com.sunasterisk.smarthomejava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

public class NotificationSmartHouse {
    public static  void  createChannel (Context mContext, String chId, String chName, int impt){
        NotificationManager m =(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel nc = new NotificationChannel(chId,chName,impt);
            nc.enableLights(true);
            nc.setLightColor(Color.GREEN);
            m.createNotificationChannel(nc);
        }
    }

    public static void callNotify(Context mContext, int id,String chId, String title, String text) {
        NotificationManager m =(NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder n = new Notification.Builder(mContext)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_donut_small_black_24dp);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            n.setChannelId(chId);
        }

        m.notify(id,n.build());
    }
    public static Notification callNotifyService(Context mContext, String chId, String title, String text) {

        Notification.Builder n = new Notification.Builder(mContext)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_donut_small_black_24dp);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            n.setChannelId(chId);
        }

        return n.build();
    }
}
