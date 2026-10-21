package com.donutclient.module.hud;

import com.donutclient.module.TextHudModule;
import com.donutclient.util.ClickTracker;

public class CpsModule extends TextHudModule {
    public CpsModule() {
        super("CPS", "Affiche tes clics par seconde (gauche | droit).");
    }

    @Override
    public String getText() {
        return "CPS: " + ClickTracker.getLeftCps() + " | " + ClickTracker.getRightCps();
    }
}
