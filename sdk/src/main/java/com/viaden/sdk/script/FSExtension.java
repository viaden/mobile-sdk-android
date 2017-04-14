package com.viaden.sdk.script;

import java.util.Vector;

interface FSExtension {

    /**
     * <p>getVar is called whenever a variable is read in FScript that has
     * not been defined within the script iteslf</p>
     *
     * @param name the variable name
     * @return the value of the variable (as one of FScript's supported object
     * types)
     **/
    Object getVar(String name) throws FSException;

    /**
     * <p>setVar is called whenever a variable is written to in FScript that has not
     * been defined within the script itself</p>
     *
     * @param name  the variable name
     * @param value the value to assign to the variable
     **/
    void setVar(String name, Object value) throws FSException;

    /**
     * <p>getVar is called whenever a variable is read in FScript that has
     * not been defined within the script iteslf</p>
     * <p> This version allows an index variable to be supplied for array like
     * access</p>
     *
     * @param name  the variable name
     * @param index the index
     * @return the value of the variable (as one of FScript's supported object
     * types)
     **/
    Object getVar(String name, Object index) throws FSException;

    /**
     * <p>setVar is called whenever a variable is written to in FScript that has not
     * been defined within the script itself</p>
     * <p> This version allows an index variable to be supplied for array like
     * access</p>
     *
     * @param name  the variable name
     * @param index the index
     * @param value the value to assign to the variable
     **/
    void setVar(String name, Object index, Object value) throws FSException;

    /**
     * <p>callFunction is called whenever a function call is made in FScript to a
     * function not defined withing hte script itself</p>
     *
     * @param name   the name of the function
     * @param params an array list of parameters passed to the function
     * @return the return value (Object) of the call
     **/
    Object callFunction(String name, Vector params) throws FSException;
}
