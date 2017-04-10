package com.viaden.sdk.script;

import com.viaden.sdk.Resources;

import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static com.google.common.truth.Truth.assertThat;

public class FScriptTest {
    private FScript subject;

    @Before
    public void setUp() throws Exception {
        subject = new FScript() {
            @Override
            public Object callFunction(final String name, final Vector params) throws FSException {
                //(void) println(param.....)
                if (name.equals("println")) {
                    int n;
                    String s = "";
                    for (n = 0; n < params.size(); n++) {
                        s = s + params.elementAt(n);
                    }
                    System.out.println(s);
                } else {
                    return super.callFunction(name, params);
                }
                return new NullObject();
            }
        };
    }

    @Test
    public void execute() throws Exception {
        subject.load(Resources.asStream("objects.script"));
        subject.run();
        assertThat(true).isTrue();
    }
}
