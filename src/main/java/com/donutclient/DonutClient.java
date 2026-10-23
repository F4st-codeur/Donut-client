package com.donutclient;

import com.donutclient.gui.ClickGuiScreen;
import com.donutclient.gui.HudRenderer;
import com.donutclient.module.ModuleManager;
import com.donutclient.util.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DonutClient implements ClientModInitializer {
    public static final String MOD_ID = "donutclient";
    public static final String NAME = "Donut Client";
    public static final String VERSION = "1.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static KeyBinding guiKey;
    public static KeyBinding zoomKey;

    private static ModuleManager moduleManager;

    @Override
    public void onInitializeClient() {
        moduleManager = new ModuleManager();
        Config.load(moduleManager);

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.donutclient.gui", GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.donutclient.zoom", GLFW.GLFW_KEY_C, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGuiScreen());
                }
            }
            moduleManager.onTick();
        });

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "hud"), (context, tickCounter) -> HudRenderer.render(context));
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(moduleManager::onWorldRender);
        // Appelé avant l'envoi de chaque commande au serveur : un module peut l'annuler.
        ClientSendMessageEvents.ALLOW_COMMAND.register(moduleManager::allowCommand);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save(moduleManager));

        LOGGER.info("{} v{} chargé avec {} modules", NAME, VERSION, moduleManager.getModules().size());
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }
}
