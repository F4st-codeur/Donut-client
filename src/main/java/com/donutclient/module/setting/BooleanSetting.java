package com.donutclient.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting {
    private boolean value;

    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public boolean get() {
        return value;
    }

    @Override
    public String getDisplayValue() {
        return value ? "ON" : "OFF";
    }

    @Override
    public void onClick(int button) {
        value = !value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        value = json.getAsBoolean();
    }
}
