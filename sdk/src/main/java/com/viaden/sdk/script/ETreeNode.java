package com.viaden.sdk.script;

class ETreeNode {
    static final int E_OP = 0;
    static final int E_VAL = 1;

    public int type;
    public Object value;
    ETreeNode left;
    ETreeNode right;
    ETreeNode parent;

    public String toString() {
        return "Type=" + type + " Value=" + value;
    }
}
