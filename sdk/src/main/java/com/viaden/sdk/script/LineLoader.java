package com.viaden.sdk.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

final class LineLoader {
    private Vector lines;
    private int curLine;

    /**
     * Constructor
     */
    LineLoader() {
        lines = new Vector(200);
        curLine = 0;
    }

    /**
     * load with script from InputStreamReader
     *
     * @param is - the input stream to read from
     */
    final void load(final Reader is) throws IOException {
        final BufferedReader in = new BufferedReader(is);
        String s = in.readLine();
        while (s != null) {
            addLine(s);
            s = in.readLine();
        }
        in.close();
        curLine = 0;
    }

    /**
     * resets the LineLoader
     */
    final void reset() {
        lines = new Vector(200);
        curLine = 0;
    }

    /**
     * method to incrementally add lines to buffer
     *
     * @param s the line to load
     */
    final void addLine(final String s) {
        if (!s.trim().equals("")) {
            lines.addElement(s);
        } else {
            //need to add blank lines to keep error msg lines
            //in sync with file lines.
            lines.addElement("");
        }
    }

    /**
     * Returns the current execution line
     */
    final int getCurLine() {
        return curLine;
    }

    /**
     * Sets the current execution line
     *
     * @param n the line number
     */
    final void setCurLine(int n) {
        if (n > lines.size()) {
            n = lines.size() - 1;
        } else if (n < 0) {
            n = 0;
        }
        curLine = n;
    }

    /**
     * Returns the total number of lines in buffer
     */
    final int lineCount() {
        return lines.size();
    }

    /**
     * Returns the text of the current line
     */
    final String getLine() {
        return (String) lines.elementAt(curLine);
    }

    /**
     * Returns the text of the requested line
     */
    final String getLine(final int n) {
        if (n < 0 || n >= lines.size()) return "";
        return (String) lines.elementAt(n);
    }
}
