package com.sunasterisk.smarthomejava.mqtt;

public interface IMqttController {
//    Hàm để đẩy lên mqtt
    public void pub(String toppic, String content);
//    Hàm đăng ký nhận những thay đổi từ mqtt
    public void sub(String toppic);
}
