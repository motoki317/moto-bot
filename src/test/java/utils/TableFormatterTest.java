package utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static utils.TableFormatter.Justify.Left;
import static utils.TableFormatter.Justify.Right;

class TableFormatterTest {
    @Test
    public void testNoFooter() {
        TableFormatter tf = new TableFormatter(false);

        tf.addColumn("", Left, Left)
                .addColumn("Server", Left, Left)
                .addColumn("Players", Left, Right)
                .addColumn("Uptime", Left, Right);

        for (int i = 0; i < 10; i++) {
            tf.addRow((i + 1) + ".", "WC" + (i + 1), "" + i * 5, i * 10 + " m");
        }

        assertEquals("""
                            | Server | Players | Uptime
                        ----+--------+---------+--------
                        1.  | WC1    |       0 |    0 m
                        2.  | WC2    |       5 |   10 m
                        3.  | WC3    |      10 |   20 m
                        4.  | WC4    |      15 |   30 m
                        5.  | WC5    |      20 |   40 m
                        6.  | WC6    |      25 |   50 m
                        7.  | WC7    |      30 |   60 m
                        8.  | WC8    |      35 |   70 m
                        9.  | WC9    |      40 |   80 m
                        10. | WC10   |      45 |   90 m
                        """,
                tf.toString());
        assertEquals(tf.widthAt(0), 3);
        assertEquals(tf.widthAt(1), 6);
        assertEquals(tf.widthAt(2), 7);
        assertEquals(tf.widthAt(3), 6);
    }

    @Test
    public void testWithFooter() {
        TableFormatter tf = new TableFormatter(true);

        tf.addColumn("", Left, Left)
                .addColumn("Server", Left, Left)
                .addColumn("Players", "" + 225, Left, Left)
                .addColumn("Uptime", Left, Right);

        for (int i = 0; i < 10; i++) {
            tf.addRow((i + 1) + ".", "WC" + (i + 1), "" + i * 5, i * 10 + " m");
        }

        assertEquals("""
                            | Server | Players | Uptime
                        ----+--------+---------+--------
                        1.  | WC1    | 0       |    0 m
                        2.  | WC2    | 5       |   10 m
                        3.  | WC3    | 10      |   20 m
                        4.  | WC4    | 15      |   30 m
                        5.  | WC5    | 20      |   40 m
                        6.  | WC6    | 25      |   50 m
                        7.  | WC7    | 30      |   60 m
                        8.  | WC8    | 35      |   70 m
                        9.  | WC9    | 40      |   80 m
                        10. | WC10   | 45      |   90 m
                        ----+--------+---------+--------
                            |        | 225     |\s
                        """,
                tf.toString());
        assertEquals(tf.widthAt(0), 3);
        assertEquals(tf.widthAt(1), 6);
        assertEquals(tf.widthAt(2), 7);
        assertEquals(tf.widthAt(3), 6);
    }
}
