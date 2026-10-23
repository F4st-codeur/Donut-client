package com.donutclient.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Analyse une commande "/pay <pseudo> <montant>" (sans le slash). Indépendant de Minecraft
 * pour pouvoir être testé facilement.
 */
public final class PayCommandParser {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern AMOUNT = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)([kKmMbBtT]?)");

    public enum Status {
        /** Ce n'est pas une commande /pay : on ne touche à rien. */
        NOT_PAY,
        /** /pay sans les bons arguments. */
        USAGE,
        INVALID_PLAYER,
        INVALID_AMOUNT,
        OK
    }

    public record Result(Status status, String player, BigDecimal amount) {
        /** Montant formaté avec séparateurs : 1000000 -> "1,000,000". */
        public String formattedAmount() {
            DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
            return format.format(amount);
        }
    }

    private PayCommandParser() {
    }

    public static Result parse(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0 || !parts[0].equalsIgnoreCase("pay")) return new Result(Status.NOT_PAY, null, null);
        if (parts.length != 3) return new Result(Status.USAGE, null, null);
        if (!USERNAME.matcher(parts[1]).matches()) return new Result(Status.INVALID_PLAYER, parts[1], null);

        BigDecimal amount = parseAmount(parts[2]);
        if (amount == null) return new Result(Status.INVALID_AMOUNT, parts[1], null);
        return new Result(Status.OK, parts[1], amount);
    }

    /** "1000", "1,000,000", "2.5k", "1m"... ou {@code null} si invalide ou nul. */
    static BigDecimal parseAmount(String text) {
        var matcher = AMOUNT.matcher(text);
        if (!matcher.matches()) return null;
        BigDecimal value = new BigDecimal(matcher.group(1).replace(",", ""));
        BigDecimal multiplier = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "k" -> BigDecimal.valueOf(1_000L);
            case "m" -> BigDecimal.valueOf(1_000_000L);
            case "b" -> BigDecimal.valueOf(1_000_000_000L);
            case "t" -> BigDecimal.valueOf(1_000_000_000_000L);
            default -> BigDecimal.ONE;
        };
        BigDecimal amount = value.multiply(multiplier);
        return amount.signum() > 0 ? amount : null;
    }
}
