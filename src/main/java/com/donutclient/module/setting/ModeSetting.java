package com.donutclient.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

/** Choix parmi plusieurs valeurs : clic gauche = suivante, clic droit = précédente. */
public class ModeSetting extends Setting {
    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name);
        this.modes = List.of(modes);
        this.index = Math.max(0, this.modes.indexOf(defaultMode));
    }

    public String get() {
        return modes.get(index);
    }

    public boolean is(String mode) {
        return get().equals(mode);
    }

    @Override
    public String getDisplayValue() {
        return get();
    }

    @Override
    public void onClick(int button) {
        index = Math.floorMod(index + (button == 1 ? -1 : 1), modes.size());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement json) {
        int i = modes.indexOf(json.getAsString());
        if (i >= 0) index = i;
    }
}
