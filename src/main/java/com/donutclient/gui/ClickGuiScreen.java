package com.donutclient.gui;

import com.donutclient.DonutClient;
import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.Setting;
import com.donutclient.util.ColorUtil;
import com.donutclient.util.Config;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interface des modules :
 * clic gauche = activer/désactiver, clic droit = afficher les réglages,
 * glisser l'en-tête d'un panneau pour le déplacer.
 */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 130;
    private static final int HEADER_HEIGHT = 16;
    private static final int ROW_HEIGHT = 14;

    // Static pour garder l'état des panneaux entre deux ouvertures.
    private static final List<Panel> PANELS = new ArrayList<>();
    private static final Set<Module> EXPANDED = new HashSet<>();

    private Panel dragging;
    private int dragOffsetX, dragOffsetY;
    private Module binding;
    // Position de la souris à la dernière frame, utilisée pour les clics.
    private int mouseX, mouseY;

    /** Une ligne d'un panneau : un module, un de ses réglages, ou sa touche. */
    private record Row(Module module, Setting setting, boolean isBind, int y) {
    }

    public ClickGuiScreen() {
        super(Text.literal(DonutClient.NAME));
        if (PANELS.isEmpty()) {
            int x = 10;
            for (Category category : Category.values()) {
                PANELS.add(new Panel(category, x, 30));
                x += PANEL_WIDTH + 8;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        if (dragging != null) {
            dragging.x = mouseX - dragOffsetX;
            dragging.y = mouseY - dragOffsetY;
        }

        Module hovered = null;
        for (Panel panel : PANELS) {
            Module m = panel.render(context, mouseX, mouseY);
            if (m != null) hovered = m;
        }

        String footer = binding != null
                ? "Appuie sur une touche pour " + binding.getName() + " (Échap/Suppr = aucune)"
                : hovered != null ? hovered.getDescription()
                : "Clic gauche : activer  •  Clic droit : réglages  •  Glisser l'en-tête : déplacer";
        context.drawCenteredTextWithShadow(textRenderer, footer, width / 2, height - 20, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, DonutClient.NAME + " v" + DonutClient.VERSION, 6, 6, ColorUtil.rainbow(0));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (binding != null) return true;
        int button = click.button();
        for (Panel panel : PANELS) {
            if (panel.isOverHeader(mouseX, mouseY)) {
                if (button == 0) {
                    dragging = panel;
                    dragOffsetX = mouseX - panel.x;
                    dragOffsetY = mouseY - panel.y;
                } else if (button == 1) {
                    panel.expanded = !panel.expanded;
                }
                return true;
            }
            Row row = panel.rowAt(mouseX, mouseY);
            if (row == null) continue;
            if (row.isBind()) {
                binding = row.module();
            } else if (row.setting() != null) {
                row.setting().onClick(button);
            } else if (button == 0) {
                row.module().toggle();
            } else if (button == 1 && !EXPANDED.remove(row.module())) {
                EXPANDED.add(row.module());
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.getKeycode();
        if (binding != null) {
            boolean clear = input.isEscape() || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE;
            binding.setKey(clear ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            binding = null;
            return true;
        }
        if (DonutClient.guiKey.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
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

        List<Row> rows() {
            List<Row> rows = new ArrayList<>();
            if (!expanded) return rows;
            int rowY = y + HEADER_HEIGHT;
            for (Module module : DonutClient.getModuleManager().getByCategory(category)) {
                rows.add(new Row(module, null, false, rowY));
                rowY += ROW_HEIGHT;
                if (!EXPANDED.contains(module)) continue;
                for (Setting setting : module.getSettings()) {
                    rows.add(new Row(module, setting, false, rowY));
                    rowY += ROW_HEIGHT;
                }
                rows.add(new Row(module, null, true, rowY));
                rowY += ROW_HEIGHT;
            }
            return rows;
        }

        /** Dessine le panneau et renvoie le module survolé, s'il y en a un. */
        Module render(DrawContext context, int mouseX, int mouseY) {
            context.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, 0xF0141414);
            context.fill(x, y + HEADER_HEIGHT - 1, x + PANEL_WIDTH, y + HEADER_HEIGHT, ColorUtil.rainbow(x * 10L));
            context.drawTextWithShadow(textRenderer, category.getDisplayName(), x + 5, y + 4, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, expanded ? "-" : "+", x + PANEL_WIDTH - 10, y + 4, 0xFFAAAAAA);

            Module hovered = null;
            for (Row row : rows()) {
                int rowY = row.y();
                boolean hover = mouseX >= x && mouseX < x + PANEL_WIDTH && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                if (hover) hovered = row.module();
                boolean isModuleRow = row.setting() == null && !row.isBind();

                int bg = isModuleRow ? (hover ? 0xE0303030 : 0xD0202020) : (hover ? 0xE0262626 : 0xD0181818);
                context.fill(x, rowY, x + PANEL_WIDTH, rowY + ROW_HEIGHT, bg);

                if (isModuleRow) {
                    Module module = row.module();
                    if (module.isEnabled()) context.fill(x, rowY, x + 2, rowY + ROW_HEIGHT, ColorUtil.rainbow(rowY * 10L));
                    int color = module.isEnabled() ? 0xFFFFFFFF : 0xFF888888;
                    context.drawTextWithShadow(textRenderer, module.getName(), x + 6, rowY + 3, color);
                    if (!module.getSettings().isEmpty() || EXPANDED.contains(module)) {
                        String arrow = EXPANDED.contains(module) ? "v" : ">";
                        context.drawTextWithShadow(textRenderer, arrow, x + PANEL_WIDTH - 10, rowY + 3, 0xFF777777);
                    }
                } else {
                    String label = row.isBind() ? "Touche" : row.setting().getName();
                    String value = row.isBind()
                            ? (row.module() == binding ? "..." : keyName(row.module().getKey()))
                            : row.setting().getDisplayValue();
                    context.drawTextWithShadow(textRenderer, label, x + 12, rowY + 3, 0xFFBBBBBB);
                    int valueColor = "ON".equals(value) ? 0xFF55FF55 : "OFF".equals(value) ? 0xFFFF5555 : 0xFFFFFFFF;
                    context.drawTextWithShadow(textRenderer, value,
                            x + PANEL_WIDTH - textRenderer.getWidth(value) - 4, rowY + 3, valueColor);
                }
            }
            return hovered;
        }

        boolean isOverHeader(int mx, int my) {
            return mx >= x && mx < x + PANEL_WIDTH && my >= y && my < y + HEADER_HEIGHT;
        }

        Row rowAt(int mx, int my) {
            if (mx < x || mx >= x + PANEL_WIDTH) return null;
            for (Row row : rows()) {
                if (my >= row.y() && my < row.y() + ROW_HEIGHT) return row;
            }
            return null;
        }
    }

    private static String keyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "Aucune";
        return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toUpperCase();
    }
}
