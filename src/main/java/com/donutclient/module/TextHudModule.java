package com.donutclient.module;

/** Module HUD affichant une ligne de texte, empilée en haut à gauche de l'écran. */
public abstract class TextHudModule extends Module {
    protected TextHudModule(String name, String description) {
        super(name, description, Category.HUD);
    }

    /** Texte à afficher, ou {@code null} pour ne rien afficher cette frame. */
    public abstract String getText();

    public int getColor() {
        return 0xFFFFFFFF;
    }
}
