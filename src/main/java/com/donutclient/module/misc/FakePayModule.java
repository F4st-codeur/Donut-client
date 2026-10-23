package com.donutclient.module.misc;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.ModuleManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Intercepte /pay quand le module est actif : aucun argent n'est envoyé au serveur.
 * Quand le module est désactivé, la commande /pay fonctionne normalement.
 */
public class FakePayModule extends Module {
    public FakePayModule() {
        super("FakePay", "Simule /pay localement sans envoyer d'argent au serveur.", Category.MISC);
    }

    @Override
    public boolean onCommand(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0 || !parts[0].equalsIgnoreCase("pay")) return false;

        if (parts.length != 3) {
            feedback(Text.literal("Utilisation : /pay <pseudo> <montant>").formatted(Formatting.RED));
            return true;
        }

        try {
            int amount = Integer.parseInt(parts[2]);
            if (amount < 0) throw new NumberFormatException();
            showPayment(mc, parts[1], amount);
        } catch (NumberFormatException exception) {
            feedback(Text.literal("Montant invalide : " + parts[2]).formatted(Formatting.RED));
        }

        return true;
    }

    static void showPayment(MinecraftClient client, String player, int amount) {
        if (client == null) return;

        String target = player == null || player.isBlank() ? "Steve" : player;
        Text overlay = Text.literal("You paid " + target + " $" + amount)
                .formatted(Formatting.GOLD, Formatting.BOLD);
        Text chatMessage = Text.literal("[FakePay] You paid " + target + " $" + amount)
                .formatted(Formatting.GOLD, Formatting.BOLD);

        if (client.inGameHud != null) {
            client.inGameHud.setOverlayMessage(overlay, false);
        }
        if (client.player != null) {
            client.player.sendMessage(chatMessage, false);
        } else if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(chatMessage);
        }
    }

    private static void feedback(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) client.inGameHud.getChatHud().addMessage(message);
    }

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, ModuleManager manager) {
        dispatcher.register(literal("fakepay")
                .executes(context -> toggle(context.getSource(), manager))
                .then(literal("toggle").executes(context -> toggle(context.getSource(), manager)))
                .then(literal("on").executes(context -> setEnabled(context.getSource(), manager, true)))
                .then(literal("off").executes(context -> setEnabled(context.getSource(), manager, false)))
                .then(argument("player", StringArgumentType.word())
                        .then(argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    FakePayModule module = manager.get(FakePayModule.class);
                                    if (module == null || !module.isEnabled()) {
                                        context.getSource().sendFeedback(Text.literal("Activez FakePay avec /fakepay on.").formatted(Formatting.RED));
                                        return 0;
                                    }
                                    String player = StringArgumentType.getString(context, "player");
                                    int amount = IntegerArgumentType.getInt(context, "amount");
                                    showPayment(MinecraftClient.getInstance(), player, amount);
                                    return 1;
                                }))));
    }

    private static int toggle(FabricClientCommandSource source, ModuleManager manager) {
        FakePayModule module = manager.get(FakePayModule.class);
        if (module == null) return 0;
        module.toggle();
        source.sendFeedback(status(module));
        return 1;
    }

    private static int setEnabled(FabricClientCommandSource source, ModuleManager manager, boolean enabled) {
        FakePayModule module = manager.get(FakePayModule.class);
        if (module == null) return 0;
        module.setEnabled(enabled);
        source.sendFeedback(status(module));
        return 1;
    }

    private static Text status(FakePayModule module) {
        return Text.literal("FakePay " + (module.isEnabled() ? "activé" : "désactivé"))
                .formatted(module.isEnabled() ? Formatting.GREEN : Formatting.GRAY);
    }
}
