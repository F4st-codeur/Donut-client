package com.donutclient.module;

import com.donutclient.module.setting.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;
    private int key = GLFW.GLFW_KEY_UNKNOWN;
    boolean keyWasDown;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Appelé à chaque tick client quand le module est actif et qu'un joueur existe. */
    public void onTick() {
    }

    /** Appelé à chaque frame du HUD quand le module est actif. */
    public void onRender(DrawContext context) {
    }

    /** Appelé pendant le rendu du monde quand le module est actif. */
    public void onWorldRender(WorldRenderContext context) {
    }

    /** Info courte affichée à côté du nom dans l'ArrayList, ou {@code null}. */
    public String getInfo() {
        return null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public List<Setting> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }
}
