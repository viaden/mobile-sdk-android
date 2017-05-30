package com.viaden.sdk;

import android.support.annotation.Nullable;

import java.util.Arrays;

final class Objects {

    private Objects() {
    }

    public static boolean equal(@Nullable Object a, @Nullable Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int hashCode(@Nullable Object... objects) {
        return Arrays.hashCode(objects);
    }
}
