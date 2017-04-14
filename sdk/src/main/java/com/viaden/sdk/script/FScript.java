package com.viaden.sdk.script;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;

public class FScript implements FSExtension {
    protected LineLoader code;
    /**
     * If true then an attempt is made to cast varaibles
     * of dissimilar types for expressions/assignments.
     * If false an FSException occurs if dissimilar types
     * are used (e.g int + string). Defaults to false
     **/
    boolean softType = false;
    /**
     * If true there is no need to define variables before use.
     * The default behaviour (preDefineVar=true) is to throw
     * an FSException in this situation
     **/
    boolean preDefineVar = true;
    /**
     * If this variable is true (the default) it is possible to
     * to create java objects within FScript - it is also
     * possible to access static classes e.g. java.lang.System.
     * If false object creation is not possible and static classes cannot
     * be accessed - however objects returned by extensions/subclasses
     * are still available
     **/
    boolean javaObjects = true;
    private Parser parser;
    private Vector extensions;

    public FScript() {
        parser = new Parser(this);
        code = new LineLoader();
        parser.setCode(code);
        extensions = new Vector();
    }

    /**
     * Loads FScript parser with text from an InputStream
     *
     * @param is, the input stream
     */
    protected void load(final InputStream is) throws IOException {
        code.load(new InputStreamReader(is));
    }

    /**
     * Loads FScript parser with text from an InputStreamReader
     *
     * @param is, the input stream
     */
    public void load(final Reader is) throws IOException {
        code.load(is);
    }
    
/*    public void save(OutputStream is) throws IOException {
        code.save(new OutputStreamWriter(is), 0, code.lineCount());
    }
    
    public void save(Writer is) throws IOException {
        code.save(is, 0, code.lineCount());
    }
    
    public void save(Writer is, int from, int to) throws IOException {
        code.save(is, from, to);
    }*/

    /**
     * Load an individual line into the parser, intended for
     * document processing applications
     *
     * @param line the line to load
     */
    public void loadLine(final String line) {
        code.addLine(line);
    }

    /**
     * Registers language extensions
     *
     * @param extension the extension to register
     **/
    public void registerExtension(final FSExtension extension) {
        extensions.addElement(extension);
    }

    /**
     * Removes a previously registered extenison
     *
     * @param extension the extension to remove
     **/
    public void unRegisterExtension(final FSExtension extension) {
        extensions.removeElement(extension);
    }

    /**
     * Run the parser over currently loaded code
     *
     * @return any return value of the script's execution (will be one of
     * FScript's supported type objects, Integer,String,Double)
     */
    protected Object run() throws IOException, FSException {
        //reset the internal variable state
        parser.reset();
        return parser.parse(0, code.lineCount() - 1);
    }

    /**
     * Resets the internal code store
     */
    protected void reset() {
        code.reset();
        parser.reset();
    }

    /**
     * Continues execution from current point - only really
     * useful in a document processing application where you may
     * wish to add code, execute, add some more code..etc..
     *
     * @return any return value of the script's execution (will be one of
     * FScript's supported type objects, Integer,String,Double)
     */
    public Object cont() throws IOException, FSException {
        if (code.getCurLine() == 0) {
            return run();
        } else {
            return parser.parse(code.getCurLine() + 1, code.lineCount() - 1);
        }
    }

    /**
     * Returns more details on any error states, indicated by
     * FSExceptions.
     *
     * @return String, see below <br>
     * s[0]=the error text <BR>
     * s[1]=the line number <BR>
     * s[2]=the line text <BR>
     * s[3]=the current token <BR>
     * s[4]=a variable dump (current scope) <BR>
     * s[5]=a global variable dump (only if currnent scope is not global <BR>
     */
    public String[] getError() {
        return parser.getError();
    }

    /**
     * Override this method to allow external access to variables
     * in your code.
     *
     * @param name, the name of the variable the parser is requesting
     *              e.g
     *              add this...
     *              <br>
     *              if (name.equals("one") { return new Integer(1) }
     *              <br>
     *              to allow the code
     *              <br>
     *              a=one
     *              <br>
     *              to work in FScript
     * @return Object - currently expected to be String or Integer
     */
    public Object getVar(final String name) throws FSException {
        throw new FSUnsupportedException(name);
    }

    /**
     * Override this method to allow external access to variables
     * in your code.
     * <p>As getVar(String name) but allows an index variable to be
     * passed so code such as :
     * name=list[2]
     * is possible
     *
     * @param name, the name of the variable the parser is requesting
     * @return Object - currently expected to be String, Integer or Double
     */
    public Object getVar(final String name, final Object index) throws FSException {
        throw new FSUnsupportedException(name);
    }

    /**
     * Entry point for parser (checks against extensions)
     **/
    Object getVarEntry(final String name, final Object index) throws FSException {
        int n;
        for (n = 0; n < extensions.size(); n++) {
            try {
                if (index == null) {
                    return ((FSExtension) extensions.elementAt(n)).getVar(name);
                } else {
                    return ((FSExtension) extensions.elementAt(n)).getVar(name, index);
                }
            } catch (final FSUnsupportedException e) {
                //Do nothing continue looping through extensions
            }
        }
        //make call to (hopefully) subclass
        if (index == null) {
            return getVar(name);
        } else {
            return getVar(name, index);
        }
    }

    /**
     * Logical inverse of getVar
     *
     * @param name  the variable name
     * @param value the value to set it to
     */
    public void setVar(final String name, final Object value) throws FSException {
        throw new FSUnsupportedException(name);
    }

    /**
     * Logical inverse of getVar (with index)
     *
     * @param name  the variable name
     * @param index the index into the 'array'
     * @param value the value to set it to
     */
    public void setVar(final String name, final Object index, final Object value) throws FSException {
        throw new FSUnsupportedException(name);
    }


    /**
     * Entry point for parser (checks against extensions)
     **/
    void setVarEntry(final String name, final Object index, final Object value) throws FSException {
        boolean handled = false;
        int n;
        for (n = 0; n < extensions.size(); n++) {
            try {
                if (index == null) {
                    ((FSExtension) extensions.elementAt(n)).setVar(name, value);
                    handled = true;
                } else {
                    ((FSExtension) extensions.elementAt(n)).setVar(name, index, value);
                    handled = true;
                }
            } catch (final FSUnsupportedException e) {
                //Do nothing continue looping through extensions
            }
        }
        //make call to (hopefully) subclass
        if (!handled) {
            setVar(name, value);
        }
    }

    /**
     * Override this call to implement custom functions
     * See the BasicIO class for an example
     *
     * @param name   the function name
     * @param params an ArrayList of parameter values
     * @return an Object, currently expected to be Integer or String
     */
    public Object callFunction(final String name, final Vector params) throws FSException {
        throw new FSUnsupportedException(name);
    }

    /**
     * Entry point for parser (checks against extensions)
     **/
    Object callFunctionEntry(final String name, final Vector params) throws FSException {
        int n;
        for (n = 0; n < extensions.size(); n++) {
            try {
                return ((FSExtension) extensions.elementAt(n)).callFunction(name, params);
            } catch (final FSUnsupportedException e) {
                //Do nothing continue looping through extensions
            }
        }
        //make call to (hopefully) subclass
        return callFunction(name, params);
    }
}
