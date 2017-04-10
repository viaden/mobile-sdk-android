package com.viaden.sdk.script;

import java.util.Hashtable;
import java.util.Vector;

public class FuncEntry {
    int startLine; //start line of function
    int endLine; //end line of function
    Vector paramNames; //list of parameter names
    Hashtable params; //hashmap of parameters

    public FuncEntry() {
        startLine = 0;
        endLine = 0;
        paramNames = new Vector(4);
        params = new Hashtable();
    }

    public String toString() {
        String s = new String();
        s = startLine + " ";
        s = s + endLine + " ";
        s = s + paramNames + " ";
        s = s + params;
        return s;
    }
}