package com.donutclient.module.setting;

import com.google.gson.JsonElement;

public abstract class Setting {
    private final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String getDisplayValue();

    /** Clic dans la ClickGUI : 0 = gauche, 1 = droit. */
    public abstract void onClick(int button);

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement json);
}
