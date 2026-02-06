package com.minesms.launcher3;

public class MenuOption {
    private String title;
    private int iconResId;
    private Runnable action;

    public MenuOption(String title, int iconResId, Runnable action) {
        this.title = title;
        this.iconResId = iconResId;
        this.action = action;
    }

    // Getters
    public String getTitle() { return title; }
    public int getIconResId() { return iconResId; }
    public Runnable getAction() { return action; }
}

