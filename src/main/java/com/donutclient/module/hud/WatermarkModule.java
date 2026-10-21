package com.donutclient.module.hud;

import com.donutclient.DonutClient;
import com.donutclient.module.TextHudModule;
import com.donutclient.util.ColorUtil;

public class WatermarkModule extends TextHudModule {
    public WatermarkModule() {
        super("Watermark", "Affiche le nom du client.");
    }

    @Override
    public String getText() {
        return DonutClient.NAME + " v" + DonutClient.VERSION;
    }

    @Override
    public int getColor() {
        return ColorUtil.rainbow(0);
    }
}
