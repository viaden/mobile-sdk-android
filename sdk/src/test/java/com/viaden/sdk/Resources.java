package com.viaden.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.lang.Thread.currentThread;

final class Resources {

    private Resources() {
    }

    private static String toString(final InputStream is) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (final IOException ignored) {
        } finally {
            try {
                is.close();
            } catch (final IOException ignored) {
            }
        }
        return sb.toString();
    }

    static String get(final String name) {
        return toString(currentThread().getContextClassLoader().getResourceAsStream(name));
    }
}
