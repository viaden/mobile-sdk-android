package com.viaden.sdk;

import android.support.annotation.Nullable;

import java.util.Arrays;

final class Objects {

    private Objects() {
    }

    static boolean equal(@Nullable final Object a, @Nullable final Object b) {
        return a == b || (a != null && a.equals(b));
    }

    static int hashCode(@Nullable final Object... objects) {
        return Arrays.hashCode(objects);
    }
}
