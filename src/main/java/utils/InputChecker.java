package utils;

import org.jetbrains.annotations.NotNull;

public class InputChecker {
    public static boolean isValidMinecraftUsername(@NotNull String playerName) {
        return playerName.matches("[a-zA-Z0-9_]+");
    }

    public static boolean isValidWynncraftGuildName(@NotNull String guildName) {
        return guildName.matches("[a-zA-Z ]+");
    }

    /**
     * Parses positive integer from given String s.
     * @return Positive integer if success. -1 if given String s was not a positive integer.
     */
    public static int getPositiveInteger(String s) {
        try {
            int i = Integer.parseInt(s);
            return i > 0 ? i : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
