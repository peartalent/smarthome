package com.sunasterisk.smarthomejava;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sunasterisk.smarthomejava.mqtt.*;
import com.sunasterisk.smarthomejava.mqtt.MqttClientConnect;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import static com.sunasterisk.smarthomejava.config.Config.TOPIC_LED;
import static com.sunasterisk.smarthomejava.config.Config.TOPIC_WARNING;

public class MqttConnectBroadcast extends BroadcastReceiver implements IMqttController {
    static public String BROAD_CAST_NAME = "MqttConnectBroadcast";
    static public String NOTIFI_ID = "BROAD_CARD_NAME";

    private MqttAndroidClient client;
    String topic = TOPIC_WARNING;
    public String TAG = topic;
    Context mContext;
    int idNotify = 3;
    MqttClientConnect mqttClientConnect;

    void initMqtt() {
        mqttClientConnect = MqttClientConnect.getInstance();
        mqttClientConnect.setContext(mContext);
        try {
            IMqttToken token = mqttClientConnect.mqttConnect();
            client = mqttClientConnect.getClient();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    sub(topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(topic, message.toString() + "=====");
                JSONObject json = new JSONObject(message.toString());
                if (topic.equals(TOPIC_WARNING)) {
                    if (json.has("code") && json.has("message")) {
                        if (json.getInt("code") == 113) {
                            NotificationSmartHouse.callNotify(mContext, idNotify++, NOTIFI_ID, "Warnning", "Có người đang cố ý mở của nhà bạn");
                        } else if (json.getInt("code") == 114) {
                            NotificationSmartHouse.callNotify(mContext, idNotify++, NOTIFI_ID, "Warnning", "Không khí trong nhà bạn có gì đó không ổn");
                        } else if (json.getInt("code") == 110) {
                            if (json.getString("message").equals("failed"))
                                NotificationSmartHouse.callNotify(mContext, idNotify++, NOTIFI_ID, "Thêm thẻ", "Thêm thẻ thất bại");
                            else if (json.getString("message").equals("success"))
                                NotificationSmartHouse.callNotify(mContext, idNotify++, NOTIFI_ID, "Thêm thẻ", "Thêm thẻ thành công");
                        }
                    }
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

    }

    static public void start(Context context) {
        Intent intent = new Intent();
        intent.setAction(BROAD_CAST_NAME);
        context.sendBroadcast(intent);
    }

    @Override
    public void pub(String toppic, String content) {
        String payload = content;
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sub(String topic) {
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Sub thành công");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Log.d(TAG, "Sub Thất bại");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        NotificationSmartHouse.createChannel(context, NOTIFI_ID, NOTIFI_ID, NotificationManager.IMPORTANCE_HIGH);
        initMqtt();
    }
}
