package utils;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("OverlyLongMethod")
class ArgumentParserTest {
    private static class TestCase {
        private final String[] input;
        private final Map<String, String> expectOutput;
        private final String expectedNormalArg;

        private TestCase(String[] input, Map<String, String> expectOutput, String expectedNormalArg) {
            this.input = input;
            this.expectOutput = expectOutput;
            this.expectedNormalArg = expectedNormalArg;
        }
    }

    @TestOnly
    private static List<TestCase> prepareTestCases() {
        List<TestCase> ret = new ArrayList<>();

        {
            String input = "-g Kingdom Foxes -t -sr";
            Map<String, String> expectOutput = new HashMap<>();
            expectOutput.put("g", "Kingdom Foxes");
            expectOutput.put("t", "");
            expectOutput.put("sr", "");

            ret.add(new TestCase(input.split(" "), expectOutput, ""));
        }

        {
            String input = "-g Hax --total -sr";
            Map<String, String> expectOutput = new HashMap<>();
            expectOutput.put("g", "Hax");
            expectOutput.put("-total", "");
            expectOutput.put("sr", "");

            ret.add(new TestCase(input.split(" "), expectOutput, ""));
        }

        {
            String input = "--total -sr -g Hax";
            Map<String, String> expectOutput = new HashMap<>();
            expectOutput.put("g", "Hax");
            expectOutput.put("-total", "");
            expectOutput.put("sr", "");

            ret.add(new TestCase(input.split(" "), expectOutput, ""));
        }

        {
            String input = "Kingdom Foxes --total -sr -g Hax";
            Map<String, String> expectOutput = new HashMap<>();
            expectOutput.put("g", "Hax");
            expectOutput.put("-total", "");
            expectOutput.put("sr", "");

            ret.add(new TestCase(input.split(" "), expectOutput, "Kingdom Foxes"));
        }

        {
            String input = "aaa -t -sr -g Hax";
            Map<String, String> expectOutput = new HashMap<>();
            expectOutput.put("g", "Hax");
            expectOutput.put("t", "");
            expectOutput.put("sr", "");

            ret.add(new TestCase(input.split(" "), expectOutput, "aaa"));
        }

        return ret;
    }

    @Test
    void testHyphenParser() {
        for (TestCase testCase : prepareTestCases()) {
            ArgumentParser parser = new ArgumentParser(testCase.input);
            assertEquals(testCase.expectOutput, parser.getArgumentMap());
            assertEquals(testCase.expectedNormalArg, parser.getNormalArgument());
        }
    }
}
