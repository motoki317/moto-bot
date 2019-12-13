package utils;

import java.awt.*;

public enum MinecraftColor {
    BLACK(new Color(0x000000), "Black", "black"),
    DARK_BLUE(new Color(0x0000AA), "Dark Blue", "dark_blue"),
    DARK_GREEN(new Color(0x00AA00), "Dark Green", "dark_green"),
    DARK_AQUA(new Color(0x00AAAA), "Dark Aqua", "dark_aqua"),
    DARK_RED(new Color(0xAA0000), "Dark Red", "dark_red"),
    DARK_PURPLE(new Color(0xAA00AA), "Dark Purple", "dark_purple"),
    GOLD(new Color(0xFFAA00), "Gold", "gold"),
    GRAY(new Color(0xAAAAAA), "Gray", "gray"),
    DARK_GRAY(new Color(0x555555), "Dark Gray", "dark_gray"),
    BLUE(new Color(0x5555FF), "Blue", "blue"),
    GREEN(new Color(0x55FF55), "Green", "green"),
    AQUA(new Color(0x55FFFF), "Aqua", "aqua"),
    RED(new Color(0xFF5555), "Red", "red"),
    LIGHT_PURPLE(new Color(0xFF55FF), "Light Purple", "light_purple"),
    YELLOW(new Color(0xFFFF55), "Yellow", "yellow"),
    WHITE(new Color(0xFFFFFF), "White", "white");

    private Color color;
    private String officialName;
    private String technicalName;

    MinecraftColor(Color color, String officialName, String technicalName) {
        this.color = color;
        this.officialName = officialName;
        this.technicalName = technicalName;
    }

    public Color getColor() {
        return color;
    }

    public String getOfficialName() {
        return officialName;
    }

    public String getTechnicalName() {
        return technicalName;
    }

    public Color getBackgroundColor() {
        Color color = this.color;
        return new Color(
                color.getRGB() / 4,
                color.getGreen() / 4,
                color.getBlue() / 4
        );
    }
}

