package com.donutclient.module.hud;

import com.donutclient.module.TextHudModule;

public class CoordinatesModule extends TextHudModule {
    public CoordinatesModule() {
        super("Coordinates", "Affiche ta position et ta direction.");
    }

    @Override
    public String getText() {
        if (mc.player == null) return null;
        return String.format("XYZ: %.1f / %.1f / %.1f (%s)",
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                mc.player.getHorizontalFacing().asString());
    }
}
