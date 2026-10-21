package com.donutclient.util;

public final class ColorUtil {
    private ColorUtil() {
    }

    /** Couleur arc-en-ciel animée, décalée de {@code offset} millisecondes. */
    public static int rainbow(long offset) {
        float hue = ((System.currentTimeMillis() + offset) % 4000L) / 4000f;
        return hsvToArgb(hue, 0.6f, 1f);
    }

    public static int hsvToArgb(float h, float s, float v) {
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
