package com.donutclient.gui;

import com.donutclient.DonutClient;
import com.donutclient.module.Module;
import com.donutclient.module.TextHudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class HudRenderer {
    private HudRenderer() {
    }

    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        int y = 4;
        for (Module module : DonutClient.getModuleManager().getModules()) {
            if (!module.isEnabled()) continue;
            if (module instanceof TextHudModule text) {
                String line = text.getText();
                if (line == null) continue;
                context.drawTextWithShadow(mc.textRenderer, line, 4, y, text.getColor());
                y += 10;
            } else {
                module.onRender(context);
            }
        }
    }
}
