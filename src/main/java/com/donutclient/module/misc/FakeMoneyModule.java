package com.donutclient.module.misc;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.ModeSetting;
import com.donutclient.module.setting.NumberSetting;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remplace l'argent affiché à l'écran (scoreboard, tab...) par un faux montant.
 * Purement visuel et local : le vrai solde ne change pas, personne d'autre ne le voit.
 */
public class FakeMoneyModule extends Module {
    private static final String KEYWORD_LINES = "Lignes argent";
    private static final String EVERYWHERE = "Tous les $";
    private static final Pattern KEYWORDS = Pattern.compile("(?i)money|balance|\\bbal\\b|argent|solde|coins?");
    private static final Pattern AMOUNT = Pattern.compile("(\\$\\s*)[0-9][0-9.,]*+\\s*+[KkMmBbTt]?+(?![A-Za-z])");
    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\s*)[0-9][0-9.,]*+\\s*+[KkMmBbTt]?+(?![A-Za-z])");

    private final NumberSetting amount = add(new NumberSetting("Montant", 100, 1, 999, 1));
    private final ModeSetting unit = add(new ModeSetting("Unité", "M", "Aucune", "K", "M", "B", "T"));
    private final ModeSetting where = add(new ModeSetting("Où", KEYWORD_LINES, KEYWORD_LINES, EVERYWHERE));

    public FakeMoneyModule() {
        super("FakeMoney", "Affiche un faux solde à l'écran (visuel uniquement, rien ne change sur le serveur).", Category.MISC);
    }

    private String fakeValue() {
        return amount.get() + (unit.is("Aucune") ? "" : unit.get());
    }

    /** Renvoie le texte avec les montants remplacés, ou le même texte s'il n'y a rien à changer. */
    public Text apply(Text text) {
        if (!isEnabled() || text == null) return text;
        String plain = text.getString();
        if (plain.indexOf('$') < 0) return text;
        if (where.is(KEYWORD_LINES) && !KEYWORDS.matcher(plain).find()) return text;

        String fake = fakeValue();
        MutableText result = Text.empty();
        // "$" et le nombre peuvent être dans deux morceaux de texte de couleurs différentes.
        boolean[] afterDollar = {false};
        text.visit((style, segment) -> {
            String replaced = segment;
            if (afterDollar[0]) {
                Matcher leading = LEADING_NUMBER.matcher(replaced);
                if (leading.find()) replaced = leading.group(1) + fake + replaced.substring(leading.end());
            }
            replaced = AMOUNT.matcher(replaced).replaceAll(m -> Matcher.quoteReplacement(m.group(1) + fake));
            if (!segment.isBlank()) afterDollar[0] = segment.stripTrailing().endsWith("$");
            result.append(Text.literal(replaced).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}
