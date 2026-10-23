
package com.donutclient.module.misc;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.ModeSetting;
import com.donutclient.util.PayCommandParser;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FakePayModule extends Module {

    private static final String PRETEND = "Prétend";
    private static final String DREAM = "Rêve";
    private static final String STINGY = "Radin";
    private static final String FAKE = "Pour de faux";

    private final ModeSetting message = add(
            new ModeSetting(
                    "Message",
                    PRETEND,
                    PRETEND,
                    DREAM,
                    STINGY,
                    FAKE
            )
    );

    public FakePayModule() {
        super(
                "FakePay",
                "Simule /pay avec un message local dans le chat et l'action bar.",
                Category.MISC
        );
    }

    @Override
    public boolean onCommand(String command) {
        PayCommandParser.Result result = PayCommandParser.parse(command);

        switch (result.status()) {
            case NOT_PAY -> {
                return false;
            }

            case USAGE -> notify(
                    Text.literal("Utilisation : /pay <pseudo> <montant>")
                            .formatted(Formatting.RED)
            );

            case INVALID_PLAYER -> notify(
                    Text.literal("Pseudo invalide : " + result.player())
                            .formatted(Formatting.RED)
            );

            case INVALID_AMOUNT -> notify(
                    Text.literal(
                            "Montant invalide (exemples : 1000, 1,000,000, 2.5k, 1m)"
                    ).formatted(Formatting.RED)
            );

            case OK -> {
                String player = result.player();
                String amount = result.formattedAmount();

                MutableText text = Text.literal("You paid ")
                        .formatted(Formatting.GREEN)
                        .append(
                                Text.literal(player)
                                        .formatted(Formatting.WHITE)
                        )
                        .append(
                                Text.literal(" $" + amount)
                                        .formatted(Formatting.GREEN)
                        );

                notify(text);
            }
        }

        // La commande /pay interceptée n'est pas envoyée au serveur.
        return true;
    }

    private String body(String amount, String player) {
        return switch (message.get()) {
            case DREAM ->
                    "Dans tes rêves, tu envoies $" + amount + " à " + player + ".";

            case STINGY ->
                    "$" + amount + " pour " + player
                            + " ? Rien n'a été envoyé.";

            case FAKE ->
                    "$" + amount + " ont été simulés pour " + player + ".";

            default ->
                    "Tu prétends envoyer $" + amount + " à " + player + " !";
        };
    }

    /**
     * Affiche une notification locale dans le chat et l'action bar.
     * Aucun message n'est envoyé au serveur.
     */
    private void notify(Text content) {
        mc.inGameHud.getChatHud().addMessage(content);
        mc.inGameHud.setOverlayMessage(content, false);
    }
}
