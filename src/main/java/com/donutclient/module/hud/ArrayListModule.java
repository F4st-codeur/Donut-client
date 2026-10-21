package com.donutclient.module.hud;

import com.donutclient.DonutClient;
import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.util.ColorUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;
import java.util.List;

public class ArrayListModule extends Module {
    public ArrayListModule() {
        super("ArrayList", "Liste les modules actifs en haut à droite.", Category.HUD);
    }

    @Override
    public void onRender(DrawContext context) {
        TextRenderer tr = mc.textRenderer;
        List<Module> active = DonutClient.getModuleManager().getModules().stream()
                .filter(m -> m.isEnabled() && m.getCategory() != Category.HUD)
                .sorted(Comparator.comparingInt((Module m) -> tr.getWidth(m.getName())).reversed())
                .toList();

        int screenWidth = context.getScaledWindowWidth();
        int y = 2;
        for (int i = 0; i < active.size(); i++) {
            String name = active.get(i).getName();
            int width = tr.getWidth(name);
            int x = screenWidth - width - 4;
            int color = ColorUtil.rainbow(i * 200L);
            context.fill(x - 2, y - 1, screenWidth, y + 10, 0x90000000);
            context.fill(screenWidth - 1, y - 1, screenWidth, y + 10, color);
            context.drawTextWithShadow(tr, name, x, y + 1, color);
            y += 11;
        }
    }
}
