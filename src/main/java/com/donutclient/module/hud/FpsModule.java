package com.donutclient.module.hud;

import com.donutclient.module.TextHudModule;

public class FpsModule extends TextHudModule {
    public FpsModule() {
        super("FPS", "Affiche les images par seconde.");
    }

    @Override
    public String getText() {
        return "FPS: " + mc.getCurrentFps();
    }
}
