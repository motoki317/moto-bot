package utils;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SQLReplacer {
    public static String replaceSql(String sql, @NotNull Object... objects) {
        if (numQuestionChars(sql) != objects.length) {
            throw new Error(String.format("Number of replacements (\"?\", %s) and number of given objects (%s) do not match.\n" +
                    "SQL before replacement: %s", numQuestionChars(sql), objects.length, sql));
        }

        // Save '?' char points in the original sql
        int[] replacePoints = new int[objects.length];
        char[] charArray = sql.toCharArray();
        int nextIndex = 0;
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == '?') {
                replacePoints[nextIndex] = i;
                nextIndex++;
                if (nextIndex == replacePoints.length) {
                    break;
                }
            }
        }

        // Replace '?' chars from last
        String ret = sql;
        for (int i = objects.length - 1; i >= 0; i--) {
            Object o = objects[i];
            int point = replacePoints[i];
            ret = ret.substring(0, point) + objectToString(o) + ret.substring(point + 1);
        }
        return ret;
    }

    private static int numQuestionChars(String sql) {
        int ret = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?')
                ret++;
        }
        return ret;
    }

    @NotNull
    private static String objectToString(@Nullable Object o) {
        return o == null ? "NULL" : "\"" + escapeString(o.toString()) + "\"";
    }

    @NotNull
    private static String escapeString(@NotNull String s) {
        // Escape escape character ('\')
        // Escape double quotation ('"') as it is used to represent a value
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
