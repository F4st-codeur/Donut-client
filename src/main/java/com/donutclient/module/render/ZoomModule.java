package com.donutclient.module.render;

import com.donutclient.DonutClient;
import com.donutclient.module.Category;
import com.donutclient.module.Module;

public class ZoomModule extends Module {
    private static final float FACTOR = 4f;

    public ZoomModule() {
        super("Zoom", "Maintiens la touche Zoom (C par défaut) pour zoomer.", Category.RENDER);
    }

    public boolean isZooming() {
        return isEnabled() && mc.currentScreen == null && DonutClient.zoomKey != null && DonutClient.zoomKey.isPressed();
    }

    public float apply(float fov) {
        return isZooming() ? fov / FACTOR : fov;
    }

    @Override
    public boolean isLegit() {
        return true;
    }
}
