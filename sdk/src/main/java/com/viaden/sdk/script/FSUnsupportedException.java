package com.viaden.sdk.script;

public class FSUnsupportedException extends FSException {
    public FSUnsupportedException() {
    }

    /**
     * Exception specifically used to indicate that extensions/subclasses
     * do not support the given function/variable name.
     *
     * @param name the name of function/variable not supported
     **/
    public FSUnsupportedException(final String name) {
        super("Unrecognized External: " + name);
    }
}

