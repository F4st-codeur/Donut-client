package com.donutclient.module.render;

import com.donutclient.mixin.SimpleOptionAccessor;
import com.donutclient.module.Category;
import com.donutclient.module.Module;

public class FullbrightModule extends Module {
    private static final double BRIGHT_GAMMA = 15.0;

    private Double previousGamma;

    public FullbrightModule() {
        super("Fullbright", "Éclaire au maximum les zones sombres.", Category.RENDER);
    }

    @Override
    public void onTick() {
        // Appliqué au tick (et pas dans onEnable) pour que les options du jeu soient déjà chargées.
        if (previousGamma == null) previousGamma = mc.options.getGamma().getValue();
        setGamma(BRIGHT_GAMMA);
    }

    @Override
    protected void onDisable() {
        if (previousGamma != null) {
            setGamma(previousGamma);
            previousGamma = null;
        }
    }

    private void setGamma(double value) {
        // Contourne la validation 0..1 de SimpleOption#setValue.
        ((SimpleOptionAccessor) (Object) mc.options.getGamma()).donut$setValue(value);
    }
}
