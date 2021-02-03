package utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents Minecraft username UUID (Universal Unique Identifier).
 */

public class UUID {
    private final byte[] data;

    public UUID(byte[] data) {
        if (data.length != 16) {
            throw new IllegalArgumentException("Length of the given data (" + data.length + ") is not correct (want 16).");
        }

        this.data = data;
    }

    public UUID(@NotNull String uuid) {
        if (!isUUID(uuid)) {
            throw new IllegalArgumentException("Given argument (" + uuid + ") is not a valid UUID.");
        }

        this.data = convertUUID(uuid.replace("-", ""));
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    /**
     * Returns UUID without hyphens.
     * @return Example `123e4567e89b12d3a456426655440000`
     */
    public String toString() {
        char[] hexChars = new char[data.length * 2];
        for ( int j = 0; j < data.length; j++ ) {
            int v = data[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Returns UUID with hyphens.
     * @return Example `123e4567-e89b-12d3-a456-426655440000`
     */
    public String toStringWithHyphens() {
        String str = this.toString();
        String hy = "-";
        return str.substring(0, 8) + hy
                + str.substring(8, 12) + hy
                + str.substring(12, 16) + hy
                + str.substring(16, 20) + hy
                + str.substring(20, 32);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UUID && Arrays.equals(data, ((UUID) obj).data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(data));
    }

    /**
     * Checks if the string is a valid Minecraft UUID (8-4-4-4-12 length, letter 0-9a-f a.k.a. hexadecimal).
     * Checks both cases of if UUID is trimmed or with hyphens.
     * @param possibleUUID Possible UUID string. Can be either with hyphens or not.
     * @return True if it was a valid UUID.
     */
    public static boolean isUUID(@NotNull String possibleUUID) {
        return isTrimmedUUID(possibleUUID) || isUUIDWithHyphens(possibleUUID);
    }

    private static boolean isTrimmedUUID(@NotNull String possibleUUID) {
        return possibleUUID.matches("[0-9a-f]{32}");
    }

    private static boolean isUUIDWithHyphens(@NotNull String possibleUUID) {
        return possibleUUID.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    /**
     * Convert String UUID to byte array data, to save memory space.
     * @param s Trimmed UUID (i.e. without hyphens).
     * @return Byte array data of the UUID.
     */
    private static byte[] convertUUID(String s) {
        if (!isTrimmedUUID(s)) {
            throw new IllegalArgumentException("Given argument (" + s + ") is not a valid UUID.");
        }

        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
