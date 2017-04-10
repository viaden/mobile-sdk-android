package com.viaden.sdk.script;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

class Parser {
    protected static Hashtable opPrio; //operator priority table
    protected LineLoader code; //the code
    protected LexAnn tok; //tokenizer
    protected int maxLine;
    protected Hashtable vars; //function local variables
    protected Hashtable gVars; //global variables
    protected FScript host; //link to hosting FScript object
    protected Hashtable funcs; //function map
    protected Object retVal; //return value

    protected String error[];

    /**
     * Public constructor
     *
     * @param h a reference to the FScript object
     */
    public Parser(final FScript h) {
        vars = new Hashtable();
        gVars = null;
        funcs = new Hashtable();
        host = h;

        setPrio();
    }

    //only used for function calls - note it is private
    private Parser(final FScript h, final Hashtable l, final Hashtable g, final Hashtable f) {
        vars = l;
        gVars = g;
        funcs = f;
        host = h;
    }

    public final static Object handleNullObject(final Object object) {
        if (object == null) {
            //can also throws an exception
            return new NullObject();
        }
        return object;
    }

    /**
     * cast object to int
     */
    public final static int getIntegerValue(Object object) {
        object = handleNullObject(object);
        if (object instanceof Number) {
            return ((Number) object).intValue();
        }
        if (object instanceof Boolean) {
            return (((Boolean) object).booleanValue()) ? 1 : 0;
        }
        try {
            return Integer.valueOf(object.toString()).intValue();
        } catch (final Exception e) {
        }
        return 0;
    }

    /**
     * cast object to double
     */
    public final static double getDoubleValue(Object object) {
        object = handleNullObject(object);
        if (object instanceof Number) {
            return ((Number) object).doubleValue();
        }
        if (object instanceof Boolean) {
            return (((Boolean) object).booleanValue()) ? 1.0 : 0.0;
        }
        try {
            return Double.valueOf(object.toString()).doubleValue();
        } catch (final Exception e) {
        }
        return 0.0;
    }

    /**
     * cast object to String
     */
    public final static String getStringValue(Object object) {
        object = handleNullObject(object);
        //remove .0 to double
        if (object instanceof Double) {
            final double d = ((Double) object).doubleValue();
            final int i = ((Double) object).intValue();
            if ((d - i) == 0.0)
                return String.valueOf(i);
            else
                return String.valueOf(d);
        }
        return object.toString();
    }

    /**
     * Sets the FScriptLineLoader class to be used for input
     *
     * @param in - the class
     */
    public void setCode(final LineLoader in) {
        code = in;
    }

    /**
     * The main parsing function
     *
     * @param from - the start line number
     * @param to   - the end line number
     *             returns an Object (currently a Integer or String) depending
     *             on the return value of the code parsed, or null if none.
     */
    public Object parse(final int from, final int to) throws IOException, FSException {
        // nothing to do when starting beond the code end
        if (code.lineCount() <= from) return null;
        maxLine = to;
        code.setCurLine(from);
        tok = new LexAnn(code.getLine());
        getNextToken();
        while (tok.ttype != LexAnn.TT_EOF) {
            //a script must always start with a word...
            try {
                parseStmt();
            } catch (final RetException e) {
                return retVal;
            }
            getNextToken();
        }
        return null;
    }

    /**
     * Resets the parser state.
     */
    public void reset() {
        if (vars != null) {
            vars.clear();
        }
        if (gVars != null) {
            gVars.clear();
        }
    }

    //builds the operator priority table
    private void setPrio() {
        if (opPrio == null) {
            opPrio = new Hashtable();
            //from low to high
            opPrio.put(new Integer(LexAnn.TT_LOR), new Integer(1));
            opPrio.put(new Integer(LexAnn.TT_LAND), new Integer(2));
            opPrio.put(new Integer(LexAnn.TT_LEQ), new Integer(5));
            opPrio.put(new Integer(LexAnn.TT_LNEQ), new Integer(5));
            opPrio.put(new Integer(LexAnn.TT_LGR), new Integer(5));
            opPrio.put(new Integer(LexAnn.TT_LGRE), new Integer(5));
            opPrio.put(new Integer(LexAnn.TT_LLS), new Integer(5));
            opPrio.put(new Integer(LexAnn.TT_LLSE), new Integer(5));
            opPrio.put(new Integer(LexAnn.TT_PLUS), new Integer(10));
            opPrio.put(new Integer(LexAnn.TT_MINUS), new Integer(10));
            opPrio.put(new Integer(LexAnn.TT_MULT), new Integer(20));
            opPrio.put(new Integer(LexAnn.TT_DIV), new Integer(20));
            opPrio.put(new Integer(LexAnn.TT_MOD), new Integer(20));
            opPrio.put(new Integer(LexAnn.TT_LEFT), new Integer(20));
            opPrio.put(new Integer(LexAnn.TT_RIGHT), new Integer(20));
        }
    }

    //statement - top level thing
    private void parseStmt() throws IOException, FSException, RetException {
        switch (tok.ttype) {
            case LexAnn.TT_IF:
            case LexAnn.TT_EIF:
            case LexAnn.TT_WHILE:
            case LexAnn.TT_EWHILE:
            case LexAnn.TT_DEFINT:
            case LexAnn.TT_DEFSTRING:
            case LexAnn.TT_DEFFUNC:
            case LexAnn.TT_EDEFFUNC:
            case LexAnn.TT_DEFDOUBLE:
            case LexAnn.TT_DEFOBJECT:
            case LexAnn.TT_RETURN: {
                parseKeyWord();
                break;
            }
            case LexAnn.TT_FUNC: {
                parseFunc();
                break;
            }
            case LexAnn.TT_ARRAY: {
                parseArrayAssign();
                break;
            }
            case LexAnn.TT_WORD: {
                parseAssign();
                break;
            }
            case LexAnn.TT_EOL: {
                tok.nextToken();
                break;
            }
            default: {
                parseError("Expected identifier");
            }
        }
    }

    private void parseFunc() throws IOException, FSException, RetException {
        final String name;
        name = getStringValue(tok.value);
        //should be a '('
        getNextToken();
        parseCallFunc(name);
        getNextToken();
    }

    private void parseArrayAssign() throws IOException, FSException, RetException {
        final String name;
        final Object index;
        final Object val;

        name = getStringValue(tok.value);
        getNextToken(); // should be a '['
        getNextToken(); // should be the index
        index = parseExpr();
        getNextToken(); // should be a ']'

        //getNextToken();
        if (tok.ttype != LexAnn.TT_EQ) {
            parseError("Expected '='");
        } else {
            getNextToken();
            val = parseExpr();
            try {
                host.setVarEntry(name, index, val);
            } catch (final FSException e) {
                parseError(e.getMessage());
            }
        }
    }

    //keywords
    private void parseKeyWord() throws IOException, FSException, RetException {
        switch (tok.ttype) {
            case LexAnn.TT_DEFINT:
            case LexAnn.TT_DEFSTRING:
            case LexAnn.TT_DEFOBJECT:
            case LexAnn.TT_DEFDOUBLE: {
                parseVarDef();
                break;
            }
            case LexAnn.TT_NEW: {
                parseNewObject();
                break;
            }
            case LexAnn.TT_IF: {
                parseIf();
                break;
            }
            case LexAnn.TT_WHILE: {
                parseWhile();
                break;
            }
            case LexAnn.TT_RETURN: {
                parseReturn();
                break;
            }
            case LexAnn.TT_DEFFUNC: {
                parseFunctionDef();
                break;
            }
            default: {
                //we never get here
                parseError("Not a keyword");
            }
        }
    }

    //handles 'return' statements
    private void parseReturn() throws IOException, FSException, RetException {
        getNextToken();
        retVal = parseExpr();
        throw new RetException();
    }

    //Asignment parser
    private void parseAssign() throws IOException, FSException {
        final String name;
        final Object val;
        name = getStringValue(tok.value);
        getNextToken();
        if (tok.ttype != LexAnn.TT_EQ) {
            parseError("Expected '='");
        } else {
            getNextToken();
            if (tok.ttype == LexAnn.TT_NEW) {
                val = parseNewObject();
            } else {
                val = parseExpr();
            }
            if (hasVar(name)) {
                setVar(name, val);
            } else {
                try {
                    host.setVarEntry(name, null, val);
                } catch (final FSException e) {
                    if (host.preDefineVar) {
                        //variables must be defined before use
                        parseError(e.getMessage());
                    } else {
                        //set anyway
                        setVar(name, val);
                    }
                }
            }
        }
    }

    //Handle calls to a function
    private Object parseCallFunc(final String name) throws IOException, FSException {
        final Vector params = new Vector(4);
        final FuncEntry fDef;
        int n;
        final int oldLine;
        Object val;
        val = null;

        //Set up the parameters
        do {
            getNextToken();
            if (tok.ttype == ',') {
                getNextToken();
            } else if (tok.ttype == ')') {
                break;
            }
            params.addElement(parseExpr());
        } while (tok.ttype == ',');

        //Check we have a definition for the function
        if (funcs.containsKey(name)) {
            fDef = (FuncEntry) funcs.get(name);

            //Check params and def match
            if (fDef.paramNames.size() != params.size()) {
                parseError("Expected " +
                        fDef.paramNames.size() +
                        " parameters, Found " + params.size());
            }

            //Create a new parser instance to handle call
            final Parser p;
            final Hashtable locals = new Hashtable();

            //Push the params into the local scope
            for (n = 0; n < fDef.paramNames.size(); n++) {
                locals.put(fDef.paramNames.elementAt(n), params.elementAt(n));
            }
            //watch for recursive calls
            if (gVars == null) {
                p = new Parser(host, locals, vars, funcs);
            } else {
                p = new Parser(host, locals, gVars, funcs);
            }
            //cache the current execution point
            oldLine = code.getCurLine();
            p.setCode(code);

            //let it rip
            val = p.parse(fDef.startLine + 1, fDef.endLine - 1);

            //reset execution point
            code.setCurLine(oldLine);
        } else {
            //See if it looks like a an object method call
            final int pos = name.lastIndexOf(".");
            if (pos >= 0) {
                final String className = name.substring(0, pos);
                final String methodeName = name.substring(pos + 1, name.length());
                //check if this object already exist into the gvar or var
                if (vars.containsKey(className)) {
                    val = evalNativeMethod(vars.get(className), className, methodeName, params);
                } else if (gVars != null && gVars.containsKey(className)) {
                    val = evalNativeMethod(gVars.get(className), className, methodeName, params);
                } else {
                    //peraphs a static method like Math.abs
                    val = evalNativeMethod(null, className, methodeName, params);
                }
            } else {
                //call to FScript to handle extensions/subclasses.
                try {
                    val = host.callFunctionEntry(name, params);
                } catch (final FSException e) {
                    parseError(e.getMessage());
                }
            }
        }
        return val;
    }

    private Object parseNewObject() throws IOException, FSException {
        final Vector params = new Vector(4);
        final Object val;
        val = null;

        getNextToken();
        final String name = (String) tok.value;
        getNextToken();

        //Set up the parameters
        do {
            getNextToken();
            if (tok.ttype == ',') {
                getNextToken();
            } else if (tok.ttype == ')') {
                break;
            }
            params.addElement(parseExpr());
        } while (tok.ttype == ',');
        getNextToken();
        return createNativeObject(name, params);
    }

    private Object createNativeObject(final String className, final Vector params) throws FSException {
        if (!host.javaObjects) {
            parseError("java object creation prohibited");
        }
        try {
            final Class c;
            final ClassLoader loader = getClass().getClassLoader();
            if (loader != null) {
                c = loader.loadClass(className);
            } else {
                c = getClass().forName(className);
            }
            if (c == null)
                return null;

            final Object[] o = new Object[params.size()];

            for (int i = 0; i < o.length; i++) {
                o[i] = params.elementAt(i);
            }
            final java.lang.reflect.Constructor[] constructors = c.getDeclaredConstructors();
            for (int i = 0; i < constructors.length; i++) {
                final Class[] classes = constructors[i].getParameterTypes();
                if (checkMethods(classes, o)) {
                    return constructors[i].newInstance(o);
                }
            }
        } catch (final Exception e) {
            parseError("Error Creating new object " + e.getMessage());
        }
        return null;
    }

    private void setNativeField(final Object target, final String className, final String fieldName, final Object value) {
        try {
            final Class c;
            if (target == null) {
                final ClassLoader loader = getClass().getClassLoader();
                if (loader != null) {
                    c = loader.loadClass(className);
                } else {
                    c = getClass().forName(className);
                }
            } else {
                c = target.getClass();
            }
            if (c == null)
                return;
            final java.lang.reflect.Field f = c.getField(fieldName);
            if (f == null)
                return;
            if (target == null)
                f.set(c, value);
            else
                f.set(target, value);
        } catch (final Exception e) {
            //System.out.println("ERROR");
            //e.printStackTrace();
        }
        return;
    }

    private Object evalNativeField(final Object target, final String className, final String fieldName) throws FSException {
        try {
            final Class c;

            if (target == null) {

                if (!host.javaObjects) {
                    parseError("Java object access is prohibited");
                }

                final ClassLoader loader = getClass().getClassLoader();

                if (loader != null) {
                    c = loader.loadClass(className);
                } else {
                    c = getClass().forName(className);
                }
            } else {
                c = target.getClass();
            }

            if (c == null)
                return null;

            final java.lang.reflect.Field f = c.getField(fieldName);

            if (f == null)
                return null;

            if (target == null)
                return f.get(c);
            else
                return f.get(target);
        } catch (final FSException e) {
            //just re-throw these
            throw e;
        } catch (final Exception e) {
            parseError("Error accessing field " + e.getMessage());
        }

        return null;
    }

    private Object evalNativeMethod(final Object target, final String className,
                                    final String methodeName, final Vector params) throws FSException {

        try {
            final Class c;

            if (target == null) {

                if (!host.javaObjects) {
                    parseError("Java object access is prohibited");
                }

                final ClassLoader loader = getClass().getClassLoader();

                if (loader != null) {
                    c = loader.loadClass(className);
                } else {
                    c = getClass().forName(className);
                }
            } else {
                c = target.getClass();
            }

            if (c == null)
                return null;

            final Object[] o = new Object[params.size()];

            for (int i = 0; i < o.length; i++) {
                o[i] = params.elementAt(i);
            }

            final java.lang.reflect.Method[] methods = c.getDeclaredMethods();

            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(methodeName)) {
                    final Class[] classes = methods[i].getParameterTypes();


                    try {
                        if (target == null) {
                            if (checkMethods(classes, o)) {
                                return methods[i].invoke(c, o);
                            }
                        } else if (checkMethods(classes, o)) {
                            return methods[i].invoke(target, o);
                        }
                    } catch (final IllegalArgumentException e) {
                        parseError("Error Calling method " + e.getMessage());
                    }

                }
            }
        } catch (final FSException e) {
            //just re-throw these
            throw e;
        } catch (final Exception e) {
            parseError("Error Calling method " + e.getMessage());
        }

        return null;
    }

    //used by evalNative method to check that parameters of calling
    //object and native method call match
    private boolean checkMethods(final Class[] c, final Object[] o) {

        int n;
        final int len;

        //easy exit not the same length params
        if (c.length != o.length) {
            return false;
        }

        //check that methods have same types
        len = c.length;
        for (n = 0; n < len; n++) {
            if (!c[n].isInstance(o[n])) {
                return false;
            }
        }

        return true;

    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //- various math op with smart casting support
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    //handles function definitions
    private void parseFunctionDef() throws IOException, FSException {

        final FuncEntry fDef = new FuncEntry();
        Object val;
        String name;
        final String fName;

        fDef.startLine = code.getCurLine();

        getNextToken();

        //should be the function name
        if (tok.ttype != LexAnn.TT_FUNC) {
            parseError("Expected identifier");
        }
        fName = getStringValue(tok.value);
        getNextToken();

        //should be a '('
        if (tok.ttype != '(') {
            parseError("Expected (");
        }

        getNextToken();
        //parse the header...
        while (tok.ttype != ')') {
            if (tok.ttype != LexAnn.TT_DEFINT && tok.ttype != LexAnn.TT_DEFSTRING) {
                parseError("Expected type name");
            }

            val = null; //keep the compiler happy..

            if (tok.ttype == LexAnn.TT_DEFINT) {
                val = new Integer(0);
            } else if (tok.ttype == LexAnn.TT_DEFSTRING) {
                val = new String("");
            }

            getNextToken();

            if (tok.ttype != LexAnn.TT_WORD) {
                parseError("Expected identifier");
            }

            name = getStringValue(tok.value);

            fDef.paramNames.addElement(name);
            fDef.params.put(name, val);

            getNextToken();
            if (tok.ttype == ',') getNextToken();
        }

        //now we just skip to the endfunction

        while ((tok.ttype != LexAnn.TT_EDEFFUNC) && (tok.ttype != LexAnn.TT_EOF)) {
            getNextToken();
            if (tok.ttype == LexAnn.TT_DEFFUNC)
                parseError("Nested functions are illegal");
        }

        fDef.endLine = code.getCurLine();
        getNextToken();

        funcs.put(fName, fDef);

    }

    //Expression parser
    private Object parseExpr() throws IOException, FSException {

        ETreeNode curNode = null;
        boolean end = false;
        final boolean skipTok = false;
        Object val;
        boolean negate = false; //flag for unary minus
        boolean not = false;//flag for unary not.
        boolean prevOp = true;//flag - true if previous value was an operator


        while (!end) {

            switch (tok.ttype) {

                //the various possible 'values'
                case LexAnn.TT_INTEGER:
                case LexAnn.TT_DOUBLE:
                case LexAnn.TT_STRING:
                case LexAnn.TT_WORD:
                case LexAnn.TT_FUNC:
                case LexAnn.TT_OBJECT:
                case LexAnn.TT_ARRAY:
                case LexAnn.TT_DEFOBJECT: {

                    if (!prevOp) {
                        parseError("Expected Operator");
                    } else {

                        val = null;
                        final ETreeNode node = new ETreeNode();
                        node.type = ETreeNode.E_VAL;

                        switch (tok.ttype) {
                            //numbers - just get them
                            case LexAnn.TT_INTEGER: {
                                val = tok.value;
                                break;
                            }
                            case LexAnn.TT_DOUBLE: {
                                val = tok.value;
                                break;
                            }
                            //functions - evaluate them
                            case LexAnn.TT_FUNC: {
                                final String name = getStringValue(tok.value);
                                getNextToken();
                                val = parseCallFunc(name);
                                break;
                            }
                            //arrays - evaluate them
                            case LexAnn.TT_ARRAY: {
                                final String name = getStringValue(tok.value);
                                getNextToken(); //should be a '['
                                getNextToken(); //should be the index
                                final Object index = parseExpr();
                                try {
                                    val = host.getVarEntry(name, index);
                                } catch (final FSException e) {
                                    parseError(e.getMessage());
                                }
                                break;
                            }
                            //variables - resolve them
                            case LexAnn.TT_WORD: {
                                final String name = getStringValue(tok.value);
                                if (hasVar(name)) {
                                    val = getVar(name);
                                } else {
                                    try {
                                        val = host.getVarEntry(name, null);
                                    } catch (final FSException e) {

                                        if (host.preDefineVar) {
                                            //hard defined variable
                                            parseError(e.getMessage());
                                        } else {
                                            //soft defined variable
                                            addVar(name, null);
                                            val = getVar(name);
                                        }
                                    }
                                }
                                break;
                            }
                            //strings - just get again
                            case LexAnn.TT_STRING: {
                                val = getStringValue(tok.value);
                                break;
                            }
                            case LexAnn.TT_DEFOBJECT: {
                                //in this circumstance need to make $var look
                                //like a 'var'
                                getNextToken();
                                final String name = getStringValue(tok.value);
                                if (hasVar(name)) {
                                    val = getVar(name);
                                } else {
                                    try {
                                        val = host.getVarEntry(name, null);
                                    } catch (final FSException e) {
                                        //parseError(e.getMessage());
                                        //soft definised variable
                                        addVar(name, null);
                                        val = getVar(name);
                                    }
                                }
                                break;
                            }

                        }

                        //unary not
                        if (not) {
                            if (val instanceof Integer) {
                                if (((Integer) val).intValue() != 0) {
                                    val = new Integer(0);
                                } else {
                                    val = new Integer(1);
                                }
                                not = false;
                            } else {
                                parseError("Type mismatch for !");
                            }
                        }

                        //unary minus
                        if (negate) {
                            if (val instanceof Integer) {
                                val = new Integer(-((Integer) val).intValue());
                            } else if (val instanceof Double) {
                                val = new Double(-((Double) val).doubleValue());
                            } else {
                                parseError("Type mistmatch for unary -");
                            }
                        }

                        node.value = val;

                        if (curNode != null) {
                            if (curNode.left == null) {
                                curNode.left = node;
                                node.parent = curNode;
                                curNode = node;

                            } else if (curNode.right == null) {
                                curNode.right = node;
                                node.parent = curNode;
                                curNode = node;

                            }
                        } else {
                            curNode = node;
                        }

                        prevOp = false;
                    }
                    break;
                }
                /*opperators - have to be more carefull with these.
                We build an expresion tree - inserting the nodes at the right
                points to get a reasonable approximation to correct opperator
                precidence*/
                case LexAnn.TT_LEQ:
                case LexAnn.TT_LNEQ:
                case LexAnn.TT_MULT:
                case LexAnn.TT_DIV:
                case LexAnn.TT_MOD:
                case LexAnn.TT_LEFT:
                case LexAnn.TT_RIGHT:
                case LexAnn.TT_PLUS:
                case LexAnn.TT_MINUS:
                case LexAnn.TT_LGR:
                case LexAnn.TT_LGRE:
                case LexAnn.TT_LLSE:
                case LexAnn.TT_LLS:
                case LexAnn.TT_NOT:
                case LexAnn.TT_LAND:
                case LexAnn.TT_LOR: {
                    if (prevOp) {
                        if (tok.ttype == LexAnn.TT_MINUS) {
                            negate = true;
                        } else if (tok.ttype == LexAnn.TT_NOT) {
                            not = true;
                        } else {
                            parseError("Expected Expresion");
                        }
                    } else {

                        final ETreeNode node = new ETreeNode();

                        node.type = ETreeNode.E_OP;
                        node.value = new Integer(tok.ttype);

                        if (curNode.parent != null) {

                            final int curPrio = getPrio(tok.ttype);
                            final int parPrio =
                                    getPrio(((Integer) curNode.parent.value).intValue());

                            if (curPrio <= parPrio) {
                                //this nodes parent is the current nodes grandparent
                                node.parent = curNode.parent.parent;
                                //our nodes left leg is now linked into the current nodes
                                //parent
                                node.left = curNode.parent;
                                //hook into grandparent
                                if (curNode.parent.parent != null) {
                                    curNode.parent.parent.right = node;
                                }

                                //the current nodes parent is now us (because of above)
                                curNode.parent = node;
                                //set the current node.
                                curNode = node;
                            } else {
                                //current node's parent's right is now us.
                                curNode.parent.right = node;
                                //our nodes left is the current node.
                                node.left = curNode;
                                //our nodes parent is the current node's parent.
                                node.parent = curNode.parent;
                                //curent nodes parent is now us.
                                curNode.parent = node;
                                //set the current node.
                                curNode = node;
                            }
                        } else {
                            //our node's left is the current node
                            node.left = curNode;
                            //current node's parent is us now
                            //we don't have to set our parent, as it is null.
                            curNode.parent = node;
                            //set current node
                            curNode = node;
                        }
                        prevOp = true;
                    }
                    break;
                }
                case '(':
                    //start of an bracketed expresion, recursively call ourself
                    //to get a value
                {
                    getNextToken();
                    val = parseExpr();

                    final ETreeNode node = new ETreeNode();
                    node.value = val;
                    node.type = ETreeNode.E_VAL;

                    if (curNode != null) {
                        if (curNode.left == null) {
                            curNode.left = node;
                            node.parent = curNode;
                            curNode = node;

                        } else if (curNode.right == null) {
                            curNode.right = node;
                            node.parent = curNode;
                            curNode = node;

                        }
                    } else {
                        curNode = node;
                    }
                    prevOp = false;
                    break;
                }

                default: {
                    end = true;
                }

            }
            if (!end) {
                tok.nextToken();
            }
        }

        //find the top of the tree we just built.
        if (curNode == null) parseError("Missing Expression");
        while (curNode.parent != null) {
            curNode = curNode.parent;
        }
        return evalETree(curNode);

    }

    //convenience function to get operator priority
    private int getPrio(final int op) {
        return ((Integer) opPrio.get(new Integer(op))).intValue();
    }

    //evaluates the expression tree recursively
    private Object evalETree(final ETreeNode node) throws FSException {
        final Object lVal;
        final Object rVal;

        if (node.type == ETreeNode.E_VAL) {
            return node.value;
        }

        lVal = evalETree(node.left);
        rVal = evalETree(node.right);

        switch (((Integer) node.value).intValue()) {
            //call the various eval functions
            case LexAnn.TT_PLUS: {
                return evalPlus(lVal, rVal);
            }
            case LexAnn.TT_MINUS: {
                return evalMinus(lVal, rVal);
            }
            case LexAnn.TT_MULT: {
                return evalMult(lVal, rVal);
            }
            case LexAnn.TT_DIV: {
                return evalDiv(lVal, rVal);
            }
            case LexAnn.TT_LEFT: {
                return evalLeft(lVal, rVal);
            }
            case LexAnn.TT_RIGHT: {
                return evalRight(lVal, rVal);
            }
            case LexAnn.TT_LEQ: {
                return evalEq(lVal, rVal);
            }
            case LexAnn.TT_LNEQ: {
                return evalNEq(lVal, rVal);
            }
            case LexAnn.TT_LLS: {
                return evalLs(lVal, rVal);
            }
            case LexAnn.TT_LLSE: {
                return evalLse(lVal, rVal);
            }
            case LexAnn.TT_LGR: {
                return evalGr(lVal, rVal);
            }
            case LexAnn.TT_LGRE: {
                return evalGre(lVal, rVal);
            }
            case LexAnn.TT_MOD: {
                return evalMod(lVal, rVal);
            }
            case LexAnn.TT_LAND: {
                return evalAnd(lVal, rVal);
            }
            case LexAnn.TT_LOR: {
                return evalOr(lVal, rVal);
            }
        }

        return null;
    }

    //addition
    private Object evalPlus(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Number && rVal instanceof Number) {
            if (lVal instanceof Double || rVal instanceof Double) {
                return new Double(getDoubleValue(lVal) + getDoubleValue(rVal));
            } else {
                return new Integer(getIntegerValue(lVal) + getIntegerValue(rVal));
            }
        } else {
            return new String(getStringValue(lVal) + getStringValue(rVal));
        }
    }

    //subtraction
    private Object evalMinus(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Double || rVal instanceof Double) {
            return new Double(getDoubleValue(lVal) - getDoubleValue(rVal));
        } else {
            return new Integer(getIntegerValue(lVal) - getIntegerValue(rVal));
        }

    }

    //multiplication
    private Object evalMult(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }
        if (lVal instanceof Double || rVal instanceof Double) {
            return new Double(getDoubleValue(lVal) * getDoubleValue(rVal));
        } else {
            return new Integer(getIntegerValue(lVal) * getIntegerValue(rVal));
        }

    }

    //modulus %
    private Object evalMod(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        final int i = getIntegerValue(rVal);

        if (i != 0)
            return new Integer(getIntegerValue(lVal) % i);
        else
            return "NaN";
    }

    //>>
    private Object evalRight(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        return new Integer(getIntegerValue(lVal) >> getIntegerValue(rVal));

    }

    //<<
    private Object evalLeft(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        return new Integer(getIntegerValue(lVal) << getIntegerValue(rVal));
    }

    //Logical AND
    private Object evalAnd(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Double || rVal instanceof Double) {
            return (getDoubleValue(lVal) != 0 && getDoubleValue(rVal) != 0) ? new Integer(1) : new Integer(0);
        } else {
            return (getIntegerValue(lVal) != 0 && getIntegerValue(rVal) != 0) ? new Integer(1) : new Integer(0);
        }


    }

    //Logical Or
    private Object evalOr(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Double || rVal instanceof Double) {
            return (getDoubleValue(lVal) != 0 || getDoubleValue(rVal) != 0) ? new Integer(1) : new Integer(0);
        } else {
            return (getIntegerValue(lVal) != 0 || getIntegerValue(rVal) != 0) ? new Integer(1) : new Integer(0);
        }


    }

    //division always use a Double
    private Object evalDiv(final Object lVal, final Object rVal) throws FSException {


        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Double || rVal instanceof Double) {
            return new Double(getDoubleValue(lVal) / getDoubleValue(rVal));
        } else {
            return new Integer(getIntegerValue(lVal) / getIntegerValue(rVal));
        }

    }

    //logical equal
    private Object evalEq(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Number && rVal instanceof Number) {
            //compare Double
            if (lVal instanceof Double || rVal instanceof Double) {
                return (getDoubleValue(lVal) == getDoubleValue(rVal)) ? new Integer(1) : new Integer(0);
            } else //compare Integer
            {
                return (getIntegerValue(lVal) == getIntegerValue(rVal)) ? new Integer(1) : new Integer(0);
            }
        } else //compare String
        {
            return (getStringValue(lVal).equals(getStringValue(rVal))) ? new Integer(1) : new Integer(0);
        }
    }

    //<
    private Object evalLs(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Number && rVal instanceof Number) {
            //compare Double
            if (lVal instanceof Double || rVal instanceof Double) {
                return (getDoubleValue(lVal) < getDoubleValue(rVal)) ? new Integer(1) : new Integer(0);
            } else //compare Integer
            {
                return (getIntegerValue(lVal) < getIntegerValue(rVal)) ? new Integer(1) : new Integer(0);
            }
        } else //compare String
        {
            return (getStringValue(lVal).compareTo(getStringValue(rVal)) < 0) ? new Integer(1) : new Integer(0);
        }
    }

    //<=
    private Object evalLse(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Number && rVal instanceof Number) {
            //compare Double
            if (lVal instanceof Double || rVal instanceof Double) {
                return (getDoubleValue(lVal) <= getDoubleValue(rVal)) ? new Integer(1) : new Integer(0);
            } else //compare Integer
            {
                return (getIntegerValue(lVal) <= getIntegerValue(rVal)) ? new Integer(1) : new Integer(0);
            }
        } else //compare String
        {
            return (getStringValue(lVal).compareTo(getStringValue(rVal)) <= 0) ? new Integer(1) : new Integer(0);
        }
    }

    //>
    private Object evalGr(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Number && rVal instanceof Number) {
            //compare Double
            if (lVal instanceof Double || rVal instanceof Double) {
                return (getDoubleValue(lVal) > getDoubleValue(rVal)) ? new Integer(1) : new Integer(0);
            } else //compare Integer
            {
                return (getIntegerValue(lVal) > getIntegerValue(rVal)) ? new Integer(1) : new Integer(0);
            }
        } else //compare String
        {
            return (getStringValue(lVal).compareTo(getStringValue(rVal)) > 0) ? new Integer(1) : new Integer(0);
        }
    }

    //>=
    private Object evalGre(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        if (lVal instanceof Number && rVal instanceof Number) {
            //compare Double
            if (lVal instanceof Double || rVal instanceof Double) {
                return (getDoubleValue(lVal) >= getDoubleValue(rVal)) ? new Integer(1) : new Integer(0);
            } else //compare Integer
            {
                return (getIntegerValue(lVal) >= getIntegerValue(rVal)) ? new Integer(1) : new Integer(0);
            }
        } else //compare String
        {
            return (getStringValue(lVal).compareTo(getStringValue(rVal)) >= 0) ? new Integer(1) : new Integer(0);
        }
    }

    //logical inequallity
    private Object evalNEq(final Object lVal, final Object rVal) throws FSException {

        if (!host.softType) {
            if (!lVal.getClass().equals(rVal.getClass())) {
                parseError("Type mismatch");
            }
        }

        return (getIntegerValue(evalEq(lVal, rVal)) == 0) ? new Integer(1) : new Integer(0);
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //- end various math op with smart casting support
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void parseIf() throws IOException, FSException, RetException {
        final Integer val;
        int depth;
        boolean then = false;


        getNextToken();
        val = (Integer) parseExpr();

        //handle the one line if-then construct
        if (tok.ttype == LexAnn.TT_THEN) {
            getNextToken();
            //is this a single line then (or just a optional then)
            if (tok.ttype != LexAnn.TT_EOL) {
                //single line if then construct - run separately
                //tok.pushBack();
                if (val.intValue() != 0) {
                    parseStmt();
                } else {
                    //consume to EOL
                    while (tok.ttype != LexAnn.TT_EOL) {
                        getNextToken();
                    }
                }
                then = true;
            }
        }

        if (!then) {
            if (val.intValue() != 0) {
                getNextToken();
                while ((tok.ttype != LexAnn.TT_EIF) && (tok.ttype != LexAnn.TT_ELSE)) {
                    //run the body of the if
                    parseStmt();
                    getNextToken();
                }
                if (tok.ttype == LexAnn.TT_ELSE) {
                    //skip else clause -
                    //have to do this taking into acount nesting
                    depth = 1;
                    do {
                        getNextToken();
                        if (tok.ttype == LexAnn.TT_IF) depth++;
                        if (tok.ttype == LexAnn.TT_EOF)
                            parseError("can't find endif");
                        if (tok.ttype == LexAnn.TT_EIF) depth--;

                        //A then could indicate a one line
                        //if - then construct, then we don't increment
                        //depth
                        if (tok.ttype == LexAnn.TT_THEN) {

                            getNextToken();
                            if (tok.ttype != LexAnn.TT_EOF) {
                                depth--;
                            }
                            tok.pushBack();
                        }

                    } while (depth > 0);
                    getNextToken();
                } else {
                    getNextToken();
                }

            } else {
                //skip to else clause
                depth = 1;
                do {
                    getNextToken();
                    if (tok.ttype == LexAnn.TT_IF) depth++;
                    if (tok.ttype == LexAnn.TT_EOF)
                        parseError("can't find endif");
                    if ((tok.ttype == LexAnn.TT_EIF)) depth--;
                    if (tok.ttype == LexAnn.TT_ELSE && depth == 1) depth--;
                    //A then could indicate a one line
                    //if - then construct, then we don't increment
                    //depth
                    if (tok.ttype == LexAnn.TT_THEN) {

                        getNextToken();
                        if (tok.ttype != LexAnn.TT_EOF) {
                            depth--;
                        }
                        tok.pushBack();
                    }

                } while (depth > 0);

                if (tok.ttype == LexAnn.TT_ELSE) {
                    getNextToken();
                    getNextToken();
                    //run else clause

                    while (tok.ttype != LexAnn.TT_EIF) {
                        parseStmt();
                        getNextToken();
                    }
                    getNextToken();
                } else {
                    getNextToken();
                }
            }
        }

    }

    private void parseWhile() throws IOException, FSException, RetException {
        //parses the while statement

        Integer val;
        final int startLine;
        int endPos;
        int depth;

        startLine = code.getCurLine();
        getNextToken();
        val = (Integer) parseExpr();
        getNextToken();

        while (val.intValue() != 0) {
            while (tok.ttype != LexAnn.TT_EWHILE) {
                parseStmt();
                getNextToken();
            }

            //reset to start of while loop....
            code.setCurLine(startLine);
            resetTokens();
            getNextToken(); //a 'while' you would imagine.
            val = (Integer) parseExpr();
            getNextToken();
        }
        //skip to endwhile
        depth = 1;
        do {
            getNextToken();
            if (tok.ttype == LexAnn.TT_WHILE) depth++;
            if (tok.ttype == LexAnn.TT_EWHILE) depth--;
            if (tok.ttype == LexAnn.TT_EOF)
                parseError("can't find endwhile");
        } while (depth > 0);

        getNextToken();

    }


    private void parseVarDef() throws IOException, FSException {


        Object val;
        int type = 0;
        String name;

        val = null;

        if (tok.ttype == LexAnn.TT_DEFINT) {
            type = LexAnn.TT_DEFINT;
        } else if (tok.ttype == LexAnn.TT_DEFSTRING) {
            type = LexAnn.TT_DEFSTRING;
        } else if (tok.ttype == LexAnn.TT_DEFDOUBLE) {
            type = LexAnn.TT_DEFDOUBLE;
        } else if (tok.ttype == LexAnn.TT_DEFOBJECT) {
            type = LexAnn.TT_DEFOBJECT;
        } else {
            parseError("Expected 'int','string' 'double' or 'object'");
        }


        do {
            getNextToken();
            if (tok.ttype != LexAnn.TT_WORD) {
                parseError("Expected identifier,");
            }

            name = getStringValue(tok.value);

            switch (type) {
                case LexAnn.TT_DEFINT: {
                    addVar(name, new Integer(0));
                    break;
                }
                case LexAnn.TT_DEFSTRING: {
                    addVar(name, new String(""));
                    break;
                }
                case LexAnn.TT_DEFDOUBLE: {
                    addVar(name, new Double(0));
                    break;
                }
                case LexAnn.TT_DEFOBJECT: {
                    addVar(name, new NullObject());
                    break;
                }
            }

            getNextToken();
            if (tok.ttype == LexAnn.TT_EQ) {
                //getNextToken();
                //setVar(name,parseExpr());

                getNextToken();

                if (tok.ttype == LexAnn.TT_NEW) {
                    val = parseNewObject();
                } else {
                    val = parseExpr();
                }

                if (hasVar(name)) {
                    setVar(name, val);
                } else {
                    try {
                        host.setVarEntry(name, null, val);
                    } catch (final FSException e) {
                        //parseError(e.getMessage());
                        //force to set the variable...
                        setVar(name, val);
                    }
                }
            } else if (tok.ttype != ',' && tok.ttype != LexAnn.TT_EOL) {
                parseError("Expected ','");
            }

        } while (tok.ttype != LexAnn.TT_EOL);

    }

    //format an error message and throw FSException
    private void parseError(String s) throws FSException {
        final String t;
        error = new String[6];

        t = tok.toString();

        //set up our error block

        error[0] = s;
        error[1] = (new Integer(code.getCurLine())).toString();
        error[2] = code.getLine();
        error[3] = t;
        error[4] = vars.toString();
        if (gVars != null) error[5] = gVars.toString();

        //then build the display string
        s = "\n\t" + s;
        final int l = code.getCurLine();
        s = s + "\n\t\t at line:" + l + " ";
        s += "\n\t\t\t  " + code.getLine(l - 2);
        s += "\n\t\t\t  " + code.getLine(l - 1);
        s += "\n\t\t\t> " + code.getLine(l) + " <";
        s += "\n\t\t\t  " + code.getLine(l + 1);
        s += "\n\t\t\t  " + code.getLine(l + 2);
        s = s + "\n\t\t current token:" + t;
        s = s + "\n\t\t Variable dump:" + vars;
        if (gVars != null) {
            s = s + "\n\t\t Globals:" + gVars;
        }
        throw new FSException(s);
    }

    //return the error block
    public String[] getError() {
        return error;
    }

    //Other non SM related routines

    private boolean pComp(final String s) {
        //a compare for tok.sval strings - that avoids the null problem
        final String name = getStringValue(tok.value);

        if (name != null) {
            return name.equals(s);
        } else {
            return false;
        }
    }

    //misc token access routines
    private void getNextToken() throws IOException {

        if ((tok.ttype == LexAnn.TT_EOL) && (code.getCurLine() < maxLine)) {
            code.setCurLine(code.getCurLine() + 1);
            tok.setString(code.getLine());
            tok.nextToken();
        } else if ((tok.ttype == LexAnn.TT_EOL) && (code.getCurLine() >= maxLine)) {
            tok.ttype = LexAnn.TT_EOF; //the only place this gets set
        } else {
            tok.nextToken();
        }
    }

    private void resetTokens() throws IOException {
        tok.setString(code.getLine());
        tok.nextToken();
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //- addVar setVar getVar with smart casting support
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    //variable access routines
    public void addVar(final String name, Object value) throws FSException {
        if (value == null) {
            value = new NullObject();
        }

        final int pos = name.lastIndexOf('.');

        final Object o = vars.get(name);

        if (o == null) {
            vars.put(name, value);
        } else if (o instanceof Double) //cast to double
        {
            vars.put(name, new Double(getDoubleValue(value)));
        } else if (o instanceof Number) //cast to int
        {
            vars.put(name, new Integer(getIntegerValue(value)));
        } else //cast to string
            if (o instanceof String) //cast to string
            {
                vars.put(name, getStringValue(value));
            } else {
                if (pos > 0) {
                    final String className = name.substring(0, pos);
                    final String fieldName = name.substring(pos + 1, name.length());

                    //check if this object already exist into the gvar or var
                    if (vars.containsKey(className)) {
                        setNativeField(vars.get(className),
                                className, fieldName, value);
                    } else if (gVars != null && gVars.containsKey(className)) {
                        setNativeField(gVars.get(className),
                                className, fieldName, value);
                    } else //peraphs a static field like java.io.File.pathSeparator
                    {
                        setNativeField(null, className, fieldName, value);
                    }
                }
                vars.put(name, value);
            }
    }

    public Object getVar(final String name) throws FSException {

        final int pos = name.lastIndexOf('.');
        Object retVal = null;

        if (vars.containsKey(name)) {
            retVal = vars.get(name);
        } else {
            if (gVars != null) {
                if (gVars.containsKey(name)) {
                    retVal = gVars.get(name);
                }
            }
        }

        //only try for objects if there is nothing found locally
        if (pos > 0 && retVal == null) {
            final String className = name.substring(0, pos);
            final String fieldName = name.substring(pos + 1, name.length());

            //check if this object already exist into the gvar or var
            if (vars.containsKey(className)) {
                retVal = evalNativeField(vars.get(className), className, fieldName);
            } else if (gVars != null && gVars.containsKey(className)) {
                retVal = evalNativeField(gVars.get(className), className, fieldName);
            } else //peraphs a static field like java.io.File.pathSeparator
            {
                retVal = evalNativeField(null, className, fieldName);
            }
        }

        return retVal;
    }

    // perform a smart casting
    public void setVar(final String name, Object val) throws FSException {

        final boolean set = false;

        if (val == null) {
            val = new NullObject();
        }


        final int pos = name.lastIndexOf('.');


        Hashtable var = null;
        Object obj = null;

        if (vars.containsKey(name)) {
            obj = vars.get(name);
            var = vars;
        } else {
            if (gVars != null) {
                if (gVars.containsKey(name)) {
                    obj = gVars.get(name);
                    var = gVars;
                }
            }
        }

        // it's usefull to warp any king of Object
        // by this FScript support other type than int double and String
        if (obj == null) {
            vars.put(name, val);
        } else {
            if (obj instanceof String) {
                if (val instanceof String)
                    var.put(name, val);
                else if (host.softType) {
                    var.put(name, getStringValue(val));
                } else {
                    parseError("Assignment Type mismatch");
                }
            } else {
                if (obj instanceof Double) {
                    if (val instanceof Double)
                        var.put(name, val);
                    else if (host.softType) {
                        var.put(name, new Double(getDoubleValue(val)));
                    } else {
                        parseError("Assignment Type mismatch");
                    }

                } else {
                    if (obj instanceof Integer) {
                        if (val instanceof Integer)
                            var.put(name, val);
                        else if (host.softType) {
                            var.put(name, new Integer(getIntegerValue(val)));
                        } else {
                            parseError("Assignment Type mismatch");
                        }
                    } else {
                        // it's usefull to warp any king of Object
                        // by this FScript support other type than int double and String
                        var.put(name, val);
                    }
                }
            }
        }

        if ((pos > 0) && (!set)) {
            final String className = name.substring(0, pos);
            final String fieldName = name.substring(pos + 1, name.length());

            //check if this object already exist into the gvar or var
            if (vars.containsKey(className)) {
                setNativeField(vars.get(className), className, fieldName, val);
            } else if (gVars != null && gVars.containsKey(className)) {
                setNativeField(gVars.get(className), className, fieldName, val);
            } else //peraphs a static field like java.io.File.pathSeparator
            {
                setNativeField(null, className, fieldName, val);
            }
        }
    }

    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    //- end addVar setVar getVar with smart casting support
    //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -


    public boolean hasVar(final String name) {
        //add support to native field
        if (name.indexOf('.') >= 0)
            return true;

        if (gVars == null) {
            return vars.containsKey(name);
        } else {
            return vars.containsKey(name) || gVars.containsKey(name);
        }
    }

}


