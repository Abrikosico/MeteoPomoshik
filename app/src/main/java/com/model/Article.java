package com.model;

public class Article {
    public String title;
    public String description;
    public int iconResId; // ID картинки (например, R.drawable.ic_pressure)
    public boolean isWarning; // Если true - выделим красным

    public Article(String title, String description, int iconResId, boolean isWarning) {
        this.title = title;
        this.description = description;
        this.iconResId = iconResId;
        this.isWarning = isWarning;
    }
}