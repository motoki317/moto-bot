package utils;

import org.jetbrains.annotations.NotNull;

public class InputChecker {
    public static boolean isValidMinecraftUsername(@NotNull String playerName) {
        return playerName.matches("[a-zA-Z0-9_]+");
    }

    public static boolean isValidWynncraftGuildName(@NotNull String guildName) {
        return guildName.matches("[a-zA-Z ]+");
    }
}
