package utils;

import java.util.ArrayList;
import java.util.List;

public class TableFormatter {
    public enum Justify {
        Left,
        Right;

        private void append(StringBuilder sb, String data, int columnWidth, boolean isLast) {
            switch (this) {
                case Left:
                    sb.append(data);
                    if (!isLast) {
                        pad(sb, ' ', columnWidth - data.length());
                    }
                    break;
                case Right:
                    pad(sb, ' ', columnWidth - data.length());
                    sb.append(data);
                    break;
                default:
                    throw new Error("implement me");
            }
        }
    }

    private static class Column {
        private final String name;
        private final String footer;
        private final Justify colJustify;
        private final Justify rowJustify;

        private Column(String name, String footer, Justify colJustify, Justify rowJustify) {
            this.name = name;
            this.footer = footer;
            this.colJustify = colJustify;
            this.rowJustify = rowJustify;
        }

        private Column(String name, Justify colJustify, Justify rowJustify) {
            this.name = name;
            this.footer = "";
            this.colJustify = colJustify;
            this.rowJustify = rowJustify;
        }
    }

    private final boolean includeFooter;
    private final List<Column> columns;
    private final List<String[]> data;
    private boolean colFinalized;
    private boolean dataFinalized;
    private int[] columnWidths;

    public TableFormatter(boolean includeFooter) {
        this.includeFooter = includeFooter;
        this.columns = new ArrayList<>();
        this.data = new ArrayList<>();
        this.colFinalized = false;
        this.dataFinalized = false;
    }

    public TableFormatter addColumn(String name, Justify colJustify, Justify rowJustify) {
        if (this.colFinalized || this.dataFinalized) {
            throw new RuntimeException("TableFormatter: Cannot add columns after adding rows");
        }
        this.columns.add(new Column(name, colJustify, rowJustify));
        return this;
    }

    public TableFormatter addColumn(String name, String footer, Justify colJustify, Justify rowJustify) {
        if (this.colFinalized || this.dataFinalized) {
            throw new RuntimeException("TableFormatter: Cannot add columns after adding rows");
        }
        this.columns.add(new Column(name, footer, colJustify, rowJustify));
        return this;
    }

    public void addRow(String... data) {
        if (data.length != this.columns.size()) {
            throw new RuntimeException("TableFormatter: Given data length (" + data.length + ") differs from the defined column size (" + this.columns.size() + ")");
        }
        if (this.dataFinalized) {
            throw new RuntimeException("TableFormatter: Cannot add data after calling format() or widthAt()");
        }
        this.colFinalized = true;
        this.data.add(data);
    }

    private void computeWidths() {
        if (this.columnWidths != null) return;
        this.colFinalized = true;
        this.dataFinalized = true;

        int cols = this.columns.size();
        this.columnWidths = new int[cols];
        for (int i = 0; i < this.columns.size(); i++) {
            Column col = this.columns.get(i);
            this.columnWidths[i] = Math.max(col.name.length(), col.footer.length());
        }
        for (String[] datum : this.data) {
            for (int i = 0; i < datum.length; i++) {
                this.columnWidths[i] = Math.max(
                        this.columnWidths[i],
                        datum[i].length()
                );
            }
        }
    }

    public void toString(StringBuilder sb) {
        this.computeWidths();

        int cols = this.columns.size();
        // header
        for (int i = 0; i < cols; i++) {
            Column col = this.columns.get(i);
            boolean isLast = i == cols - 1;
            col.colJustify.append(sb, col.name, this.columnWidths[i], isLast);
            if (!isLast) {
                sb.append(" | ");
            }
        }
        sb.append("\n");
        // separator header - data
        addSeparator(sb);
        // data
        for (String[] datum : this.data) {
            for (int i = 0; i < datum.length; i++) {
                Column col = this.columns.get(i);
                boolean isLast = i == cols - 1;
                col.rowJustify.append(sb, datum[i], this.columnWidths[i], isLast);
                if (!isLast) {
                    sb.append(" | ");
                }
            }
            sb.append("\n");
        }
        if (this.includeFooter) {
            // separator data - footer (if needed)
            addSeparator(sb);
            // footer (if needed)
            for (int i = 0; i < cols; i++) {
                Column col = this.columns.get(i);
                boolean isLast = i == cols - 1;
                col.colJustify.append(sb, col.footer, this.columnWidths[i], isLast);
                if (!isLast) {
                    sb.append(" | ");
                }
            }
            sb.append("\n");
        }

    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.toString(sb);
        return sb.toString();
    }

    public void addSeparator(StringBuilder sb) {
        this.computeWidths();

        int cols = this.columns.size();
        for (int i = 0; i < cols; i++) {
            pad(sb, '-', this.columnWidths[i]);
            if (i < cols - 1) {
                sb.append("-+-");
            } else {
                sb.append('-');
            }
        }
        sb.append("\n");
    }

    private static void pad(StringBuilder sb, char ch, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(ch);
        }
    }

    public int widthAt(int column) {
        this.computeWidths();
        return this.columnWidths[column];
    }
}
