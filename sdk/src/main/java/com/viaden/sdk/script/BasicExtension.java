package com.viaden.sdk.script;

import java.util.Vector;

public abstract class BasicExtension implements FSExtension {

    public BasicExtension() {
    }

    public Object callFunction(final String name, final Vector params) throws FSException {
        throw new FSUnsupportedException(name);
    }

    public Object getVar(final String name) throws FSException {
        throw new FSUnsupportedException(name);
    }

    public void setVar(final String name, final Object value) throws FSException {
        throw new FSUnsupportedException(name);
    }

    public Object getVar(final String name, final Object index) throws FSException {
        throw new FSUnsupportedException(name);
    }

    public void setVar(final String name, final Object index, final Object value) throws FSException {
        throw new FSUnsupportedException(name);
    }

}

