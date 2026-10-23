package com.donutclient.module.misc;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.ModeSetting;
import com.donutclient.util.PayCommandParser;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Quand il est actif, "/pay" n'est plus envoyé au serveur : le client affiche à la place une notification
 * humoristique, toujours marquée « [PaySimulator] Simulation » pour ne pas ressembler à un vrai paiement.
 */
public class PaySimulatorModule extends Module {
    private static final String PRETEND = "Prétend";
    private static final String DREAM = "Rêve";
    private static final String STINGY = "Radin";

    private final ModeSetting message = add(new ModeSetting("Message", PRETEND, PRETEND, DREAM, STINGY));

    public PaySimulatorModule() {
        super("PaySimulator", "Bloque /pay et affiche une simulation humoristique (rien n'est envoyé).", Category.MISC);
    }

    /** Renvoie {@code true} si la commande est une commande /pay interceptée (donc à ne pas envoyer). */
    @Override
    public boolean onCommand(String command) {
        PayCommandParser.Result result = PayCommandParser.parse(command);
        switch (result.status()) {
            case NOT_PAY -> {
                return false;
            }
            case USAGE -> notify(Text.literal("Utilisation : /pay <pseudo> <montant>").formatted(Formatting.RED));
            case INVALID_PLAYER -> notify(Text.literal("Pseudo invalide : " + result.player()).formatted(Formatting.RED));
            case INVALID_AMOUNT -> notify(Text.literal("Montant invalide (exemples : 1000, 1,000,000, 2.5k, 1m)").formatted(Formatting.RED));
            case OK -> notify(Text.literal(body(result.formattedAmount(), result.player())).formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        // Tant que le module est actif, aucune commande /pay ne part, même invalide.
        return true;
    }

    private String body(String amount, String player) {
        return switch (message.get()) {
            case DREAM -> "dans tes rêves, tu envoies $" + amount + " à " + player + ".";
            case STINGY -> "$" + amount + " pour " + player + " ? Même pas en rêve, rien n'a été envoyé.";
            default -> "tu prétends envoyer $" + amount + " à " + player + " !";
        };
    }

    /** Préfixe fixe et coloré, impossible à retirer : la notification ne peut pas passer pour un message du serveur. */
    private void notify(Text content) {
        MutableText text = Text.literal("[PaySimulator] ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal("Simulation : ").formatted(Formatting.YELLOW))
                .append(content);
        mc.inGameHud.getChatHud().addMessage(text);
    }
}
