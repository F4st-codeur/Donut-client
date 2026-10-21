package com.donutclient.module.hud;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.util.ClickTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

public class KeystrokesModule extends Module {
    private static final int SIZE = 20;
    private static final int GAP = 2;
    private static final int TOTAL = SIZE * 3 + GAP * 2;

    public KeystrokesModule() {
        super("Keystrokes", "Affiche les touches pressées en bas à droite.", Category.HUD);
    }

    @Override
    public void onRender(DrawContext context) {
        int x = context.getScaledWindowWidth() - TOTAL - 4;
        int y = context.getScaledWindowHeight() - (SIZE * 3 + GAP * 3 + 12) - 4;

        drawKey(context, mc.options.forwardKey, x + SIZE + GAP, y, SIZE, SIZE, label(mc.options.forwardKey));
        y += SIZE + GAP;
        drawKey(context, mc.options.leftKey, x, y, SIZE, SIZE, label(mc.options.leftKey));
        drawKey(context, mc.options.backKey, x + SIZE + GAP, y, SIZE, SIZE, label(mc.options.backKey));
        drawKey(context, mc.options.rightKey, x + (SIZE + GAP) * 2, y, SIZE, SIZE, label(mc.options.rightKey));
        y += SIZE + GAP;
        int half = (TOTAL - GAP) / 2;
        drawKey(context, mc.options.attackKey, x, y, half, SIZE, ClickTracker.getLeftCps() + " CPS");
        drawKey(context, mc.options.useKey, x + half + GAP, y, half, SIZE, ClickTracker.getRightCps() + " CPS");
        y += SIZE + GAP;
        drawKey(context, mc.options.jumpKey, x, y, TOTAL, 12, "—");
    }

    private static String label(KeyBinding key) {
        return key.getBoundKeyLocalizedText().getString().toUpperCase();
    }

    private void drawKey(DrawContext context, KeyBinding key, int x, int y, int w, int h, String text) {
        boolean pressed = key.isPressed();
        context.fill(x, y, x + w, y + h, pressed ? 0xB0FFFFFF : 0x80000000);
        int textX = x + (w - mc.textRenderer.getWidth(text)) / 2;
        int textY = y + (h - 8) / 2;
        context.drawText(mc.textRenderer, text, textX, textY, pressed ? 0xFF000000 : 0xFFFFFFFF, !pressed);
    }
}
