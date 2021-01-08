package com.sunasterisk.smarthomejava.mqtt;

import android.content.Context;
import android.util.Log;

import com.sunasterisk.smarthomejava.MainActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

import static com.sunasterisk.smarthomejava.config.Config.*;

public class MqttClientConnect implements IMqttController {
    static public String BROAD_CAST_NAME = "MqttConnectBroadcast";
    static public String NOTIFI_ID = "BROAD_CARD_NAME";
    static public String URI_SERVER = URL_MQTT;

    private MqttAndroidClient client;
    private MqttAndroidClient client1;
    private String topic= TOPIC_LED;
    public String TAG = topic;
    private Context mContext;
    private byte[] payload = "Xin chào nhé".getBytes();

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

    private static volatile MqttClientConnect instance;

    private MqttClientConnect() {
    }

    public static synchronized MqttClientConnect getInstance() {
        if (instance == null) {
            instance = new MqttClientConnect();
        }
        return instance;
    }
    //    hàm kết nối
    public IMqttToken mqttConnect() throws MqttException {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(mContext.getApplicationContext(), URI_SERVER, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
//        options.setWill(topic, payload, 1, false);
        IMqttToken token = client.connect(options);
        return token;
    }
    public IMqttToken mqttConnect1() throws MqttException {
        String clientId = MqttClient.generateClientId();
        client1 = new MqttAndroidClient(mContext.getApplicationContext(), URI_SERVER, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
//        options.setWill(topic, payload, 1, false);
        IMqttToken token = client1.connect(options);
        return token;
    }
    public MqttAndroidClient getClient(){
        return client;
    }
    public MqttAndroidClient getClient1(){
        return client1;
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
        int qos = 2;
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
                    if(exception != null){
                        Log.d(TAG, "Sub Thất bại " + exception.getMessage());
                    }
                    Log.d(TAG, "Sub Thất bại "+asyncActionToken.toString());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void sub1(String topic) {
        int qos = 2;
        try {
            IMqttToken subToken = client1.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Sub thành công");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    if(exception != null){
                        Log.d(TAG, "Sub Thất bại " + exception.getMessage());
                    }
                    Log.d(TAG, "Sub Thất bại "+asyncActionToken.toString());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
