package com.viaden.sdk.script;

import com.viaden.sdk.Resources;

import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

public class FScriptTest {
    private FScript subject;

    @Before
    public void setUp() throws Exception {
        subject = new FScript() {
            @Override
            public Object callFunction(final String name, final Vector params) throws FSException {
                if (name.equals("println")) {
                    //(void) println(param.....)
                    int n;
                    String s = "";
                    for (n = 0; n < params.size(); n++) {
                        s = s + params.elementAt(n);
                    }
                    System.out.println(s);
                } else if (name.equals("fail")) {
                    if (params.isEmpty()) {
                        fail();
                    } else {
                        fail((String) params.get(0));
                    }
                } else {
                    return super.callFunction(name, params);
                }
                return new NullObject();
            }
        };
    }

    @Test
    public void comparisons() throws Exception {
        execute("comparisons.script");
    }

    @Test
    public void function() throws Exception {
        execute("function.script");
    }

    @Test
    public void ifOp() throws Exception {
        execute("if.script");
    }

    @Test
    public void logic() throws Exception {
        execute("logic.script");
    }

    @Test
    public void math() throws Exception {
        execute("math.script");
    }

    @Test
    public void objects() throws Exception {
        execute("objects.script");
    }

    @Test
    public void recursive() throws Exception {
        execute("recursive.script");
    }

    @Test
    public void variableAccess() throws Exception {
        execute("variable_access.script");
    }

    @Test
    public void variableAssignment() throws Exception {
        execute("variable_assignment.script");
    }

    @Test
    public void whileOp() throws Exception {
        execute("while.script");
    }

    private void execute(final String name) throws Exception {
        subject.load(Resources.asStream(name));
        subject.run();
        assertThat(true).isTrue();
    }
}
