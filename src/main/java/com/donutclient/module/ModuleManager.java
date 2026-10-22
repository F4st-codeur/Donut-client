package com.donutclient.module;

import com.donutclient.module.hud.ArrayListModule;
import com.donutclient.module.hud.CoordinatesModule;
import com.donutclient.module.hud.CpsModule;
import com.donutclient.module.hud.FpsModule;
import com.donutclient.module.hud.KeystrokesModule;
import com.donutclient.module.hud.PingModule;
import com.donutclient.module.hud.WatermarkModule;
import com.donutclient.module.misc.FakeMoneyModule;
import com.donutclient.module.movement.ToggleSprintModule;
import com.donutclient.module.render.ChestEspModule;
import com.donutclient.module.render.FreecamModule;
import com.donutclient.module.render.FullbrightModule;
import com.donutclient.module.render.PlayerEspModule;
import com.donutclient.module.render.ZoomModule;
import com.donutclient.module.world.BaseFinderModule;
import com.donutclient.module.world.BaseScannerModule;
import com.donutclient.module.world.SchemBuilderModule;
import com.donutclient.module.world.SchemDiggerModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private boolean legitMode;

    public ModuleManager() {
        // HUD (l'ordre détermine l'empilement des lignes de texte)
        add(new WatermarkModule());
        add(new FpsModule());
        add(new PingModule());
        add(new CoordinatesModule());
        add(new CpsModule());
        add(new ArrayListModule());
        add(new KeystrokesModule());
        // Render
        add(new FullbrightModule());
        add(new ZoomModule());
        add(new ChestEspModule());
        add(new PlayerEspModule());
        add(new FreecamModule());
        // World
        add(new BaseFinderModule());
        add(new BaseScannerModule());
        add(new SchemDiggerModule());
        add(new SchemBuilderModule());
        // Movement
        add(new ToggleSprintModule());
        // Misc
        add(new FakeMoneyModule());
    }

    private void add(Module module) {
        modules.add(module);
    }

    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (Module module : modules) {
            int key = module.getKey();
            if (key == GLFW.GLFW_KEY_UNKNOWN || (legitMode && !module.isLegit())) continue;
            boolean down = mc.currentScreen == null && InputUtil.isKeyPressed(mc.getWindow(), key);
            if (down && !module.keyWasDown) module.toggle();
            module.keyWasDown = down;
        }

        if (mc.player == null) return;
        for (Module module : modules) {
            if (module.isEnabled()) module.onTick();
        }
    }

    public void onWorldRender(WorldRenderContext context) {
        if (MinecraftClient.getInstance().player == null) return;
        for (Module module : modules) {
            if (module.isEnabled()) module.onWorldRender(context);
        }
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    /** Modules à afficher dans la ClickGUI : en mode legit, seulement les modules legit. */
    public List<Module> getVisible(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category && (!legitMode || m.isLegit())).toList();
    }

    public boolean isLegitMode() {
        return legitMode;
    }

    /** Active le mode legit : cache et coupe les modules non legit. */
    public void setLegitMode(boolean legitMode) {
        this.legitMode = legitMode;
        if (!legitMode) return;
        for (Module module : modules) {
            if (!module.isLegit()) module.setEnabled(false);
        }
    }

    public Module getByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) return module;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        for (Module module : modules) {
            if (type.isInstance(module)) return (T) module;
        }
        return null;
    }
}
