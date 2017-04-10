package com.viaden.sdk.script;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

public class BasicIO extends FScript {

    private Object files[];

    /**
     * Constructor
     */
    public BasicIO() {
        super();
        files = new Object[25];
    }

    /**
     * <p> Overridden from FScript implements the following FScript functions </p>
     * <p>
     * <p> note that this only provides very basic IO facilities,
     * line by line read/write
     * to files, and stdio read write.  There is a maximum of 25 open files</p>
     * <p> <b>(void) println(param...)</b> - write to stdout -
     * takes variable parameter list </p>
     * <p> <b>string readln() </b> - reads a string from stdin </p>
     * <p> <b>int open(string filename,string mode) </b> -
     * opens a file 'filename' for
     * reading (mode="r") or writing (mode="w") returns an integer which is
     * used in future calls. Returns -1 on >25 files opened </p>
     * <p> <b>string read(fp) </b> - reads one line from previously openened file
     * </p>
     * <p> <b>void write(fp,param...) - writes concatination of all params to one
     * line of file </p>
     */
    public Object callFunction(final String name, final Vector param)/*ArrayList param)*/ throws FSException {

        //(void) println(param.....)
        if (name.equals("println")) {
            int n;
            String s = "";
            for (n = 0; n < param.size(); n++) {
                s = s + param.elementAt(n);
            }
            System.out.println(s);
        }
        //string readln()
        else if (name.equals("readln")) {
            try {
                return new BufferedReader(
                        new InputStreamReader(System.in)).readLine();

            } catch (final IOException e) {
                throw new FSException(e.getMessage());
            }
        }
        //int open(string file,string mode)
        else if (name.equals("open")) {
            int n;

            try {
                for (n = 0; n < 25; n++) {
                    if (files[n] == null) {
                        if (param.elementAt(1).equals("r")) {
                            files[n] = new BufferedReader(
                                    new FileReader((String) param.elementAt(0)));
                            break;
                        } else if (param.elementAt(1).equals("w")) {
                            files[n] = new BufferedWriter(
                                    new FileWriter((String) param.elementAt(0)));
                            break;
                        } else {
                            throw new FSException(
                                    "open expects 'r' or 'w' for modes");
                        }
                    }
                }
            } catch (final IOException e) {
                throw new FSException(e.getMessage());
            }
            if (n < 25) return new Integer(n);
            else return new Integer(-1);
        }
        //(void)close(int fp)
        else if (name.equals("close")) {
            final int n;
            n = ((Integer) param.elementAt(0)).intValue();
            if (files[n] == null) {
                throw new FSException("Invalid file number passed to close");
            }
            try {
                if (files[n] instanceof BufferedWriter) {
                    ((BufferedWriter) files[n]).close();
                } else {
                    ((BufferedReader) files[n]).close();
                }
                files[n] = null;
            } catch (final IOException e) {
                throw new FSException(e.getMessage());
            }
        }
        //(void) write(params....)
        else if (name.equals("write")) {
            int n;
            String s = "";
            for (n = 1; n < param.size(); n++) {
                s = s + param.elementAt(n);
            }
            n = ((Integer) param.elementAt(0)).intValue();
            if (files[n] == null) {
                throw new FSException("Invalid file number passed to write");
            }
            if (!(files[n] instanceof BufferedWriter)) {
                throw new FSException("Invalid file mode for write");
            }
            try {
                ((BufferedWriter) files[n]).write(s, 0, s.length());
                ((BufferedWriter) files[n]).newLine();
            } catch (final IOException e) {
                throw new FSException(e.getMessage());
            }
        }
        //string read(int fp)
        else if (name.equals("read")) {
            final int n;
            String s;
            n = ((Integer) param.elementAt(0)).intValue();
            if (files[n] == null) {
                throw new FSException("Invalid file number passed to read");
            }
            if (!(files[n] instanceof BufferedReader)) {
                throw new FSException("Invalid file mode for read");
            }
            try {
                s = ((BufferedReader) files[n]).readLine();
                //dodge eof problems
                if (s == null) s = "";
                return s;
            } catch (final IOException e) {
                throw new FSException(e.getMessage());
            }
        }
        //int eof(fp)
        else if (name.equals("eof")) {
            final int n;
            n = ((Integer) param.elementAt(0)).intValue();
            if (files[n] == null) {
                throw new FSException("Invalid file number passed to eof");
            }
            final BufferedReader br = (BufferedReader) files[n];
            try {
                br.mark(1024);
                if (br.readLine() == null) {
                    return new Integer(1);
                } else {
                    br.reset();
                    return new Integer(0);
                }
            } catch (final IOException e) {
                throw new FSException(e.getMessage());
            }
        } else {
            super.callFunction(name, param);
        }
        return new Integer(0);
    }
}
