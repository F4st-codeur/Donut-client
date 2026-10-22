package com.donutclient.util;

import com.donutclient.DonutClient;
import com.donutclient.module.Module;
import com.donutclient.module.ModuleManager;
import com.donutclient.module.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Sauvegarde l'état des modules dans config/donutclient.json. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("donutclient.json");

    private Config() {
    }

    public static void load(ModuleManager manager) {
        if (!Files.exists(PATH)) {
            applyDefaults(manager);
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();
            JsonObject modules = root.getAsJsonObject("modules");
            if (modules == null) return;
            boolean legitMode = root.has("legitMode") && root.get("legitMode").getAsBoolean();
            for (Module module : manager.getModules()) {
                JsonObject entry = modules.getAsJsonObject(module.getName());
                if (entry == null) continue;
                if (entry.has("key")) module.setKey(entry.get("key").getAsInt());
                JsonObject settings = entry.getAsJsonObject("settings");
                if (settings != null) {
                    for (Setting setting : module.getSettings()) {
                        if (settings.has(setting.getName())) setting.fromJson(settings.get(setting.getName()));
                    }
                }
                if (entry.has("enabled")) module.setEnabled(entry.get("enabled").getAsBoolean());
            }
            manager.setLegitMode(legitMode);
        } catch (Exception e) {
            DonutClient.LOGGER.error("Impossible de lire la config, valeurs par défaut utilisées", e);
            applyDefaults(manager);
        }
    }

    public static void save(ModuleManager manager) {
        JsonObject modules = new JsonObject();
        for (Module module : manager.getModules()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("enabled", module.isEnabled());
            entry.addProperty("key", module.getKey());
            JsonObject settings = new JsonObject();
            for (Setting setting : module.getSettings()) settings.add(setting.getName(), setting.toJson());
            entry.add("settings", settings);
            modules.add(module.getName(), entry);
        }
        JsonObject root = new JsonObject();
        root.addProperty("legitMode", manager.isLegitMode());
        root.add("modules", modules);
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(root));
        } catch (Exception e) {
            DonutClient.LOGGER.error("Impossible de sauvegarder la config", e);
        }
    }

    private static void applyDefaults(ModuleManager manager) {
        for (String name : new String[]{"Watermark", "FPS", "Ping", "Coordinates", "ArrayList", "Keystrokes", "Zoom"}) {
            Module module = manager.getByName(name);
            if (module != null) module.setEnabled(true);
        }
    }
}
