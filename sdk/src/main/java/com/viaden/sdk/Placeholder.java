package com.viaden.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Placeholder {
    @NonNull
    private final Context context;
    @NonNull
    private final MergePlaceholders placeholders;

    Placeholder(@NonNull final Context context) {
        this.context = context;
        placeholders = new MergePlaceholders();
    }

    @NonNull
    String format(@NonNull final String template) {
        final StringBuilder builder = new StringBuilder(template);
        final List<String> keys = new ArrayList<>(placeholders.keys);
        for (String key : keys) {
            final String pattern = "%{" + key + "}";
            int start;
            while ((start = builder.indexOf(pattern)) != -1) {
                builder.replace(start, start + pattern.length(), getPlaceholder(key));
            }
        }
        return builder.toString();
    }

    @NonNull
    String getPlaceholder(@NonNull final String key) {
        if (!placeholders.containsKey(key)) {
            placeholders.put(key, Platform.get(context, key));
        }
        final String value = placeholders.get(key);
        return value == null ? "" : value;
    }

    @NonNull
    Map<String, String> getPlaceholders() {
        return placeholders.map;
    }

    void setPlaceholders(@NonNull final Map<String, String> placeholders) {
        this.placeholders.put(placeholders);
    }

    private static class MergePlaceholders {
        @NonNull
        private final Map<String, String> map;
        @NonNull
        private final Set<String> keys;

        private MergePlaceholders() {
            map = new HashMap<>();
            keys = new HashSet<>(Platform.KEYS);
        }

        private boolean containsKey(@NonNull final String key) {
            return map.containsKey(key);
        }

        @Nullable
        private String get(@NonNull final String key) {
            return map.get(key);
        }

        private void put(@NonNull final String key, @Nullable final String value) {
            put(Collections.singletonMap(key, value));
        }

        private void put(@NonNull final Map<String, String> placeholders) {
            this.map.putAll(placeholders);
            keys.clear();
            keys.addAll(Platform.KEYS);
            keys.addAll(map.keySet());
        }
    }
}
