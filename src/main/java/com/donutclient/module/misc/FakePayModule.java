package com.donutclient.module.misc;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * FakePay est un troll visuel local : il reproduit un faux retour de paiement
 * côté client sans jamais envoy er une vraie commande de paiement au serveur.
 */
public class FakePayModule extends Module {
    public FakePayModule() {
        super("FakePay", "Simule un paiement localement sans toucher à l'économie du serveur.", Category.MISC);
    }

    @Override
    public boolean onCommand(String command) {
        if (!isEnabled()) return false;

        String trimmed = command == null ? "" : command.trim();
        if (trimmed.isEmpty()) return false;

        String[] parts = trimmed.split("\\s+");
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("pay")) {
            return false;
        }

        int amount;
        try {
            amount = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {
            return false;
        }

        String player = parts[1];
        showPayment(mc, player, amount);
        return true;
    }

    static void showPayment(MinecraftClient client, String player, int amount) {
        if (client == null) return;

        String target = player == null || player.isBlank() ? "Steve" : player;
        String formattedAmount = formatAmount(amount);
        Text message = Text.literal("Successfully sent $" + formattedAmount + " to " + target)
                .formatted(Formatting.GOLD, Formatting.BOLD);

        if (client.inGameHud != null) {
            client.inGameHud.setOverlayMessage(message, false);
        }

        if (client.player != null) {
            client.player.sendMessage(message, false);
        } else if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(message);
        }
    }

    private static String formatAmount(int amount) {
        DecimalFormat formatter = new DecimalFormat("#,##0");
        return formatter.format(amount).replace(',', '.');
    }
}
