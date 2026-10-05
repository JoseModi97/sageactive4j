package io.github.josemodi97.sageactive4j.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** A plain fixed-width text table. */
final class Table {

    private final String[] headers;
    private final List<String[]> rows = new ArrayList<String[]>();

    Table(String... headers) {
        this.headers = headers;
    }

    Table row(Object... cells) {
        String[] row = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            Object cell = i < cells.length ? cells[i] : null;
            row[i] = cell == null ? "" : cell.toString();
        }
        rows.add(row);
        return this;
    }

    boolean isEmpty() {
        return rows.isEmpty();
    }

    void print(PrintStream out) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
            for (String[] row : rows) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        printRow(out, headers, widths);
        StringBuilder rule = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            rule.append(i == 0 ? "" : "  ").append(repeat('-', widths[i]));
        }
        out.println(rule);
        for (String[] row : rows) {
            printRow(out, row, widths);
        }
    }

    private static void printRow(PrintStream out, String[] cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                line.append("  ");
            }
            line.append(cells[i]);
            if (i < cells.length - 1) {
                line.append(repeat(' ', widths[i] - cells[i].length()));
            }
        }
        out.println(line);
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
