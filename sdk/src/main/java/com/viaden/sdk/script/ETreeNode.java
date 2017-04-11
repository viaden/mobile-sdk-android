package com.viaden.sdk.script;

class ETreeNode {
    public static final int E_OP = 0;
    public static final int E_VAL = 1;

    public int type;
    public Object value;
    public ETreeNode left;
    public ETreeNode right;
    public ETreeNode parent;

    public String toString() {
        return "Type=" + type + " Value=" + value;
    }
}
