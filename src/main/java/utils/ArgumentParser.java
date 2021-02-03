package utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ArgumentParser {
    private final Map<String, String> argumentMap;
    private final String normalArgument;

    public ArgumentParser(@NotNull String[] arguments) {
        this.argumentMap = new HashMap<>();

        int[] argStarts = IntStream.range(0, arguments.length)
                .filter(i -> arguments[i].startsWith(HYPHEN))
                .sorted()
                .toArray();

        // No hyphen argument
        this.normalArgument = Arrays.stream(arguments, 0, argStarts.length > 0 ? argStarts[0] : arguments.length)
                .collect(Collectors.joining(" "));

        for (int i = 0; i < argStarts.length; i++) {
            int argStart = argStarts[i];
            int end;
            if (i == argStarts.length - 1) {
                end = arguments.length;
            } else {
                end = argStarts[i + 1];
            }

            this.argumentMap.put(arguments[argStart].substring(HYPHEN.length()),
                    Arrays.stream(arguments, argStart + 1, end)
                            .collect(Collectors.joining(" "))
            );
        }
    }

    private static final String HYPHEN = "-";

    /**
     * Parses arguments and returns their corresponding string in form of map.
     * <br>Examples:
     * <br>Input {@code -g Kingdom Foxes -t -sr} produces:
     * <br>{@code {g: "Kingdom Foxes", t: "", sr: ""}}
     * <br>
     * <br>Input {@code -g Hax --total -sr} produces:
     * <br>{@code {g: "Hax", -total: "", sr: ""}}
     * <br>
     * <br>Input {@code test -g Hax} produces:
     * <br>{@code {g: "Hax"}}
     */
    public Map<String, String> getArgumentMap() {
        return argumentMap;
    }

    /**
     * Returns an argument without hyphen.
     * For example, input {@code test -g Hax} produces "test"
     * @return Argument
     */
    public String getNormalArgument() {
        return normalArgument;
    }
}
