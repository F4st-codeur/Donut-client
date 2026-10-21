package com.donutclient.module;

public enum Category {
    HUD("HUD"),
    RENDER("Render"),
    MOVEMENT("Movement");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
