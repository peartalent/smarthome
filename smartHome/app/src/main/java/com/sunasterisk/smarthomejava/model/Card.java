package com.sunasterisk.smarthomejava.model;

import com.google.gson.annotations.SerializedName;

public class Card {
    @SerializedName("id")
    private int id;
    @SerializedName("nameCard")
    private String name;

    public Card() {
    }

    public Card(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
                                   
