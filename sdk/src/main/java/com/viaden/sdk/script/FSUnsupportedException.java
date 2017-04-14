package com.viaden.sdk.script;

class FSUnsupportedException extends FSException {

    /**
     * Exception specifically used to indicate that extensions/subclasses
     * do not support the given function/variable name.
     *
     * @param name the name of function/variable not supported
     **/
    FSUnsupportedException(final String name) {
        super("Unrecognized External: " + name);
    }
}
