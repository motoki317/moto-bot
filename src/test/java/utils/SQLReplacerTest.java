package utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SQLReplacerTest {
    @Test
    void replaceSql() {
        class TestCase {
            private String sql;
            private Object[] objects;
            private String want;

            private TestCase(String sql, Object[] objects, String want) {
                this.sql = sql;
                this.objects = objects;
                this.want = want;
            }
        }

        TestCase[] cases = new TestCase[]{
                new TestCase(
                        "SELECT * FROM war_player WHERE player_name = ? AND player_uuid = ?",
                        new Object[]{"Salted", "UUID_TEST"},
                        "SELECT * FROM war_player WHERE player_name = \"Salted\" AND player_uuid = \"UUID_TEST\""
                ),
                new TestCase(
                        "No replace",
                        new Object[]{},
                        "No replace"
                ),
                new TestCase(
                        "SELECT * FROM war_player WHERE player_name = ?",
                        new Object[]{"bad input\""},
                        "SELECT * FROM war_player WHERE player_name = \"bad input\\\"\""
                ),
                new TestCase(
                        "SELECT * FROM war_player WHERE player_name = ?",
                        new Object[]{"escape char \\"},
                        "SELECT * FROM war_player WHERE player_name = \"escape char \\\\\""
                ),
                new TestCase(
                        "SELECT * FROM war_player WHERE player_name = ?",
                        new Object[]{"question char ?"},
                        "SELECT * FROM war_player WHERE player_name = \"question char ?\""
                )
        };

        for (TestCase c : cases) {
            assertEquals(c.want, SQLReplacer.replaceSql(c.sql, c.objects));
        }
    }
}
