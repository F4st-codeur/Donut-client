package com.donutclient.module.movement;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import net.minecraft.client.option.KeyBinding;

public class ToggleSprintModule extends Module {
    public ToggleSprintModule() {
        super("ToggleSprint", "Sprinte automatiquement sans maintenir la touche.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        mc.options.sprintKey.setPressed(true);
    }

    @Override
    protected void onDisable() {
        // Resynchronise les touches avec l'état réel du clavier.
        KeyBinding.updatePressedStates();
    }

    @Override
    public boolean isLegit() {
        return true;
    }
}
