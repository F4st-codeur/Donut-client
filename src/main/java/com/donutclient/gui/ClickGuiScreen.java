package com.donutclient.gui;

import com.donutclient.DonutClient;
import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.util.ColorUtil;
import com.donutclient.util.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface des modules : clic gauche = activer/désactiver, clic droit = assigner une touche,
 * glisser l'en-tête d'un panneau pour le déplacer.
 */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 110;
    private static final int HEADER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 14;

    // Static pour garder la position des panneaux entre deux ouvertures.
    private static final List<Panel> PANELS = new ArrayList<>();

    private Panel dragging;
    private int dragOffsetX, dragOffsetY;
    private Module binding;

    public ClickGuiScreen() {
        super(Text.literal(DonutClient.NAME));
        if (PANELS.isEmpty()) {
            int x = 10;
            for (Category category : Category.values()) {
                PANELS.add(new Panel(category, x, 30));
                x += PANEL_WIDTH + 10;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        Module hovered = null;
        for (Panel panel : PANELS) {
            Module m = panel.render(context, mouseX, mouseY);
            if (m != null) hovered = m;
        }

        String footer = binding != null
                ? "Appuie sur une touche pour " + binding.getName() + " (Échap/Suppr = aucune)"
                : hovered != null ? hovered.getDescription()
                : "Clic gauche : activer  •  Clic droit : touche  •  Glisser l'en-tête : déplacer";
        context.drawCenteredTextWithShadow(textRenderer, footer, width / 2, height - 20, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, DonutClient.NAME + " v" + DonutClient.VERSION, 6, 6, ColorUtil.rainbow(0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (binding != null) return true;
        for (Panel panel : PANELS) {
            if (panel.isOverHeader(mouseX, mouseY)) {
                if (button == 0) {
                    dragging = panel;
                    dragOffsetX = (int) mouseX - panel.x;
                    dragOffsetY = (int) mouseY - panel.y;
                } else if (button == 1) {
                    panel.expanded = !panel.expanded;
                }
                return true;
            }
            Module module = panel.moduleAt(mouseX, mouseY);
            if (module != null) {
                if (button == 0) module.toggle();
                else if (button == 1) binding = module;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            dragging.x = (int) mouseX - dragOffsetX;
            dragging.y = (int) mouseY - dragOffsetY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE
                    || keyCode == GLFW.GLFW_KEY_BACKSPACE;
            binding.setKey(clear ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            binding = null;
            return true;
        }
        if (DonutClient.guiKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        Config.save(DonutClient.getModuleManager());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private class Panel {
        final Category category;
        int x, y;
        boolean expanded = true;

        Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        List<Module> modules() {
            return DonutClient.getModuleManager().getByCategory(category);
        }

        /** Dessine le panneau et renvoie le module survolé, s'il y en a un. */
        Module render(DrawContext context, int mouseX, int mouseY) {
            context.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, 0xF0141414);
            context.fill(x, y + HEADER_HEIGHT - 1, x + PANEL_WIDTH, y + HEADER_HEIGHT, ColorUtil.rainbow(x * 10L));
            context.drawTextWithShadow(textRenderer, category.getDisplayName(), x + 5, y + 4, 0xFFFFFFFF);
            String arrow = expanded ? "-" : "+";
            context.drawTextWithShadow(textRenderer, arrow, x + PANEL_WIDTH - 10, y + 4, 0xFFAAAAAA);
            if (!expanded) return null;

            Module hovered = null;
            int rowY = y + HEADER_HEIGHT;
            for (Module module : modules()) {
                boolean hover = mouseX >= x && mouseX < x + PANEL_WIDTH && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                if (hover) hovered = module;
                int bg = hover ? 0xE0303030 : 0xD0202020;
                context.fill(x, rowY, x + PANEL_WIDTH, rowY + ROW_HEIGHT, bg);
                if (module.isEnabled()) context.fill(x, rowY, x + 2, rowY + ROW_HEIGHT, ColorUtil.rainbow(rowY * 10L));

                int color = module.isEnabled() ? 0xFFFFFFFF : 0xFF888888;
                context.drawTextWithShadow(textRenderer, module.getName(), x + 6, rowY + 3, color);

                String key = module == binding ? "..." : keyName(module.getKey());
                if (!key.isEmpty()) {
                    context.drawTextWithShadow(textRenderer, key,
                            x + PANEL_WIDTH - textRenderer.getWidth(key) - 4, rowY + 3, 0xFF777777);
                }
                rowY += ROW_HEIGHT;
            }
            return hovered;
        }

        boolean isOverHeader(double mx, double my) {
            return mx >= x && mx < x + PANEL_WIDTH && my >= y && my < y + HEADER_HEIGHT;
        }

        Module moduleAt(double mx, double my) {
            if (!expanded || mx < x || mx >= x + PANEL_WIDTH) return null;
            int index = (int) ((my - y - HEADER_HEIGHT) / ROW_HEIGHT);
            List<Module> modules = modules();
            if (my < y + HEADER_HEIGHT || index >= modules.size()) return null;
            return modules.get(index);
        }
    }

    private static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "";
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase();
    }
}
