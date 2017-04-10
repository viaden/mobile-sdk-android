package com.viaden.sdk.script;

import java.io.IOException;

final class LexAnn {

    //general
    public static final int TT_WORD = 9000;
    public static final int TT_INTEGER = 9100;
    public static final int TT_DOUBLE = 9150;
    public static final int TT_EOF = 9200; //never set by this class
    public static final int TT_EOL = 9300;
    public static final int TT_STRING = 9500;
    public static final int TT_FUNC = 9600;
    public static final int TT_ARRAY = 9650;
    public static final int TT_OBJECT = 9660; //handle any king of object NOT CURRENTLY USED
    //keywords
    public static final int TT_IF = 9700;
    public static final int TT_EIF = 9800;
    public static final int TT_ELSE = 9850;
    public static final int TT_THEN = 9875;
    public static final int TT_DEFFUNC = 9900;
    public static final int TT_EDEFFUNC = 10000;
    public static final int TT_WHILE = 10100;
    public static final int TT_EWHILE = 10200;
    public static final int TT_DEFINT = 10300;
    public static final int TT_DEFSTRING = 10400;
    public static final int TT_DEFDOUBLE = 10425;
    public static final int TT_RETURN = 10450;
    public static final int TT_DEFOBJECT = 10451;
    public static final int TT_NEW = 10452;
    //math opts
    public static final int TT_PLUS = 10500;
    public static final int TT_MINUS = 10600;
    public static final int TT_MULT = 10700;
    public static final int TT_DIV = 10800;
    public static final int TT_MOD = 10850;
    public static final int TT_LEFT = 10851; // <<
    public static final int TT_RIGHT = 10852; // >>
    //logic
    public static final int TT_LAND = 10900;
    public static final int TT_LOR = 11000;
    public static final int TT_LEQ = 11100;
    public static final int TT_LNEQ = 11200;
    public static final int TT_LGR = 11300;
    public static final int TT_LLS = 11500;
    public static final int TT_LGRE = 11600;
    public static final int TT_LLSE = 11700;
    public static final int TT_NOT = 11800;
    //other
    public static final int TT_EQ = 11900;
    private static final int DELTA = 100;
    private static final int EOL = -1;
    /**
     * contains the current token type
     */
    public int ttype;
    /**
     * contient object value
     */
    public Object value;
    private boolean pBack;
    private char cBuf[], line[];
    private int c = 0;
    private int pos = 0;


    /**
     * Constructor
     */
    public LexAnn() {
        //note hard limit on how long a string can be
        cBuf = new char[DELTA];
    }


    /**
     * Convinience constructor which sets line as well
     */
    public LexAnn(final String firstLine) {
        cBuf = new char[DELTA];
        setString(firstLine);
    }

    /**
     * String representation of token (needs work)
     */
    public String toString() {

        final Class c = getClass();
        int n = 0;
        String tokenName = "", ret = "";

        final java.lang.reflect.Field[] fields = c.getFields();

        //try to get the human readable TT_* name via reflec magic
        for (n = 0; n < fields.length; n++) {
            final java.lang.reflect.Field f = fields[n];
            try {
                if (f.getName().startsWith("TT")) {
                    if (ttype == f.getInt(this)) {
                        tokenName = f.getName();
                    }
                }
            } catch (final Exception e) {
            }

        }

        if (!tokenName.equals("")) {
            ret = tokenName + ":" + value;
        } else {
            ret = String.valueOf((char) ttype) + ":" + value;
        }

        return ret;
    }

    /**
     * Sets the internal line buffer
     *
     * @param str - the string to use
     */
    public void setString(final String str) {
        line = str.toCharArray();
        pos = 0;
        c = 0;
    }

    /**
     * return the next char in the buffer
     */
    private int getChar() {
        if (pos < line.length) {
            return line[pos++];
        } else {
            return EOL;
        }
    }

    /**
     * return the character at a current line pos (+offset)
     * without affecting internal counters
     */
    private int peekChar(final int offset) {
        final int n;

        n = pos + offset - 1;
        if (n >= line.length) {
            return EOL;
        } else {
            return line[n];
        }
    }


    /**
     * Read the next token
     *
     * @return int - which is the charater read (not very useful)
     */
    public int nextToken() throws IOException {

        if (!pBack) {
            return nextT();
        } else {
            pBack = false;
            return ttype;
        }

    }

    /**
     * Causes next call to nextToken to return same value
     */
    public void pushBack() {
        pBack = true;
    }


    //Internal next token function
    private int nextT() throws IOException {

        int cPos = 0;

        if (c == 0) c = getChar();

        value = null;

        while (Character.isWhitespace((char) c)) c = getChar();

        if (c == EOL) {
            ttype = TT_EOL;
        }
        //Comments
        else if (c == '#') {
            while (c != EOL) c = getChar();
            //get the next item, will be an eol marker
            nextT();
            //then the 'real' next token
            nextT();
        }
        //Quoted Strings
        else if (c == '"') {
            c = getChar();

            while ((c != EOL) && (c != '"')) {
                if (cPos == cBuf.length) {
                    final char[] tmp = new char[cPos + DELTA];
                    System.arraycopy(cBuf, 0, tmp, 0, cPos);
                    cBuf = tmp;
                }

                if (c == '\\') {
                    final int c2 = getChar();
                    if (c2 == '\\') {
                        cBuf[cPos++] = '\\';
                        c = getChar();
                    } else if (c2 == 'n') {
                        cBuf[cPos++] = '\n';
                        c = getChar();
                    } else if (c2 == 'r') {
                        cBuf[cPos++] = '\r';
                        c = getChar();
                    } else if (c2 == 't') {
                        cBuf[cPos++] = '\t';
                        c = getChar();
                    } else if (c2 == '\"') {
                        cBuf[cPos++] = '\"';
                        c = getChar();
                    } else {
                        cBuf[cPos++] = (char) c;
                        c = c2;
                    }
                } else {
                    cBuf[cPos++] = (char) c;
                    c = getChar();
                }
            }

            value = new String(cBuf, 0, cPos);
            c = getChar();
            ttype = TT_STRING;
        }
        //Words
        else if (Character.isJavaIdentifierStart((char) c)) {

            if (c == '$') //used like object or var
            {
                ttype = TT_DEFOBJECT;
                value = "$";
                c = getChar();
                return ttype;
            }

            while (Character.isJavaIdentifierPart((char) c) || c == '.' || c == '_') {
                if (cPos == cBuf.length) {
                    final char[] tmp = new char[cPos + DELTA];
                    System.arraycopy(cBuf, 0, tmp, 0, cPos);
                    cBuf = tmp;
                }
                cBuf[cPos++] = (char) c;
                c = getChar();
            }

            final String keyword = new String(cBuf, 0, cPos);

            if (keyword.equals("if")) {
                ttype = TT_IF;
            } else if (keyword.equals("then")) {
                ttype = TT_THEN;
            } else if (keyword.equals("endif")) {
                ttype = TT_EIF;
            } else if (keyword.equals("else")) {
                ttype = TT_ELSE;
            } else if (keyword.equals("while")) {
                ttype = TT_WHILE;
            } else if (keyword.equals("endwhile")) {
                ttype = TT_EWHILE;
            } else if (keyword.equals("func")) {
                ttype = TT_DEFFUNC;
            } else if (keyword.equals("endfunc")) {
                ttype = TT_EDEFFUNC;
            } else if (keyword.equals("return")) {
                ttype = TT_RETURN;
            } else if (keyword.equals("new")) {
                ttype = TT_NEW;
            } else if (keyword.equals("int")) {
                ttype = TT_DEFINT;
            } else if (keyword.equals("string")) {
                ttype = TT_DEFSTRING;
            } else if (keyword.equals("double")) {
                ttype = TT_DEFDOUBLE;
            } else if (keyword.equals("object") || keyword.equals("var")) {
                ttype = TT_DEFOBJECT;
            } else if (c == '(') {
                ttype = TT_FUNC;
            } else if (c == '[') {
                ttype = TT_ARRAY;
            } else if (keyword.equals("true")) {
                ttype = TT_INTEGER;
                value = new Integer(1);
                return ttype;
            } else if (keyword.equals("false")) {
                ttype = TT_INTEGER;
                value = new Integer(0);
                return ttype;
            } else {
                ttype = TT_WORD;
            }

            value = keyword;
        }
        //Numbers
        else if (Character.isDigit((char) c)) {
            while (Character.isDigit((char) c) || c == '.') {
                if (cPos == cBuf.length) {
                    final char[] tmp = new char[cPos + DELTA];
                    System.arraycopy(cBuf, 0, tmp, 0, cPos);
                    cBuf = tmp;
                }
                cBuf[cPos++] = (char) c;
                c = getChar();
            }
            final String str = new String(cBuf, 0, cPos);
            if (str.indexOf('.') > 0) {
                ttype = TT_DOUBLE;
                try {
                    value = Double.valueOf(str);
                } catch (final NumberFormatException nfexception) {
                    value = new Double(0.0);
                }
            } else {
                ttype = TT_INTEGER;
                try {
                    value = Integer.valueOf(str);
                } catch (final NumberFormatException nfexception) {
                    value = new Integer(0);
                }
            }
        }
        //others
        else {
            if (c == '+') {
                ttype = TT_PLUS;
            } else if (c == '-') {
                ttype = TT_MINUS;
            } else if (c == '*') {
                ttype = TT_MULT;
            } else if (c == '/') {
                ttype = TT_DIV;
            } else if (c == '%') {
                ttype = TT_MOD;
            } else if (c == '>') {
                if (peekChar(1) == '=') {
                    getChar();
                    ttype = TT_LGRE;
                } else if (peekChar(1) == '>') {
                    getChar();
                    ttype = TT_RIGHT;
                } else {
                    ttype = TT_LGR;
                }
            } else if (c == '<') {
                if (peekChar(1) == '=') {
                    getChar();
                    ttype = TT_LLSE;
                } else if (peekChar(1) == '<') {
                    getChar();
                    ttype = TT_LEFT;
                } else {
                    ttype = TT_LLS;
                }
            } else if (c == '=') {
                if (peekChar(1) == '=') {
                    getChar();
                    ttype = TT_LEQ;
                } else {
                    ttype = TT_EQ;
                }
            } else if (c == '!') {
                if (peekChar(1) == '=') {
                    getChar();
                    ttype = TT_LNEQ;
                } else {
                    ttype = TT_NOT;
                }
            } else if ((c == '|') && (peekChar(1) == '|')) {
                getChar();
                ttype = TT_LOR;
            } else if ((c == '&') && (peekChar(1) == '&')) {
                getChar();
                ttype = TT_LAND;
            } else {
                ttype = c;
            }
            c = getChar();

        }

        return ttype;
    }

}



