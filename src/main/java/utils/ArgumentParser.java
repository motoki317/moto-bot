package utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Parses arguments and returns their corresponding string in form of map.
 * <br>Examples:
 * <br>Input {@code -g Kingdom Foxes -t -sr} produces:
 * <br>{@code {g: "Kingdom Foxes", t: "", sr: ""}}
 * <br>
 * <br>Input {@code -g Hax --total -sr} produces:
 * <br>{@code {g: "Hax", -total: "", sr: ""}}
 */
public class ArgumentParser {
    private Map<String, String> argumentMap;

    public ArgumentParser(@NotNull String[] arguments) {
        this.argumentMap = parseHyphenArguments(arguments);
    }

    private static final String HYPHEN = "-";

    private static Map<String, String> parseHyphenArguments(@NotNull String[] arguments) {
        Map<String, String> ret = new HashMap<>();

        int[] argStarts = IntStream.range(0, arguments.length)
                .filter(i -> arguments[i].startsWith(HYPHEN))
                .sorted()
                .toArray();

        for (int i = 0; i < argStarts.length; i++) {
            int argStart = argStarts[i];
            int end;
            if (i == argStarts.length - 1) {
                end = arguments.length;
            } else {
                end = argStarts[i + 1];
            }

            ret.put(arguments[argStart].substring(HYPHEN.length()),
                    Arrays.stream(arguments, argStart + 1, end)
                    .collect(Collectors.joining(" "))
            );
        }

        return ret;
    }

    public Map<String, String> getArgumentMap() {
        return argumentMap;
    }
}
