package com.donutclient.module;

public enum Category {
    HUD("HUD"),
    RENDER("Render"),
    WORLD("World"),
    MOVEMENT("Movement");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
