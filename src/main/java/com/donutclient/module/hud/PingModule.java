package com.donutclient.module.hud;

import com.donutclient.module.TextHudModule;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule extends TextHudModule {
    public PingModule() {
        super("Ping", "Affiche la latence avec le serveur.");
    }

    @Override
    public String getText() {
        if (mc.getNetworkHandler() == null || mc.player == null || mc.isInSingleplayer()) return null;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? null : "Ping: " + entry.getLatency() + " ms";
    }
}
