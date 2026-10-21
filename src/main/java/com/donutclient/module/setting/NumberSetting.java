package com.donutclient.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Nombre entier : clic gauche = +step, clic droit = -step. */
public class NumberSetting extends Setting {
    private final int min, max, step;
    private int value;

    public NumberSetting(String name, int defaultValue, int min, int max, int step) {
        super(name);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = defaultValue;
    }

    public int get() {
        return value;
    }

    private void set(int value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    @Override
    public String getDisplayValue() {
        return String.valueOf(value);
    }

    @Override
    public void onClick(int button) {
        set(button == 1 ? value - step : value + step);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        set(json.getAsInt());
    }
}
