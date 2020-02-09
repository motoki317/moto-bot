package utils;

import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentParserTest {
    private static class TestCase {
        private String[] input;
        private Map<String, String> expectOutput;

        private TestCase(String[] input, Map<String, String> expectOutput) {
            this.input = input;
            this.expectOutput = expectOutput;
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

            ret.add(new TestCase(input.split(" "), expectOutput));
        }

        {
            String input = "-g Hax --total -sr";
            Map<String, String> expectOutput = new HashMap<>();
            expectOutput.put("g", "Hax");
            expectOutput.put("-total", "");
            expectOutput.put("sr", "");

            ret.add(new TestCase(input.split(" "), expectOutput));
        }

        {
            String input = "--total -sr -g Hax";
            Map<String, String> expectOutput = new HashMap<>();
            expectOutput.put("g", "Hax");
            expectOutput.put("-total", "");
            expectOutput.put("sr", "");

            ret.add(new TestCase(input.split(" "), expectOutput));
        }

        return ret;
    }

    @Test
    void testHyphenParser() {
        for (TestCase testCase : prepareTestCases()) {
            ArgumentParser parser = new ArgumentParser(testCase.input);
            assertEquals(parser.getArgumentMap(), testCase.expectOutput);
        }
    }
}
