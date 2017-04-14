package com.viaden.sdk.script;

import java.util.Hashtable;
import java.util.Vector;

class FuncEntry {
    int startLine; //start line of function
    int endLine; //end line of function
    Vector paramNames; //list of parameter names
    Hashtable params; //hashmap of parameters

    FuncEntry() {
        startLine = 0;
        endLine = 0;
        paramNames = new Vector(4);
        params = new Hashtable();
    }

    public String toString() {
        return startLine + " " + endLine + " " + paramNames + " " + params;
    }
}
